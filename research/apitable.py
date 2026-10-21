import os
import re

SRC = "research/apk"
FILES = ["UserApi", "PlatformsApi", "CommentsApi", "OpenApi", "TracksApi"]


def resp_type(sig):
    m = re.search(r'Continuation<\? super (.+?)> (?:continuation|Continuation)', sig)
    if m:
        return m.group(1)
    m = re.search(r'Continuation<\? super (\w+)', sig)
    return m.group(1) if m else ""


def parse(path):
    lines = open(path, encoding="utf-8").read().splitlines()
    rows = []
    for i, line in enumerate(lines):
        m = re.search(r'@(GET|POST|PUT|DELETE|PATCH)\("([^"]+)"\)', line)
        if not m:
            continue
        method, url = m.group(1), m.group(2)
        sig = ""
        for j in range(i, min(i + 5, len(lines))):
            if "Object " in lines[j] and "(" in lines[j]:
                sig = lines[j]
                break
        multipart = any('@Multipart' in l for l in lines[i:min(i + 4, len(lines))])
        params = []
        for kind, name in re.findall(r'@(Query|Path|Header)\("([^"]*)"\)', sig):
            params.append((kind, name, ""))
        for bm in re.finditer(r'@Body\s+([A-Za-z0-9_$.<>?, ]+?)\s+\w+', sig):
            params.append(("Body", "", bm.group(1)))
        if multipart:
            params.append(("Part", "", "multipart"))
        rows.append((method, url, params, resp_type(sig)))
    return rows


rows = []
for f in FILES:
    p = os.path.join(SRC, f + ".java")
    if os.path.exists(p):
        rows.extend(parse(p))

rows.sort(key=lambda r: (r[1].lower(), r[0]))
print("| Метод | Путь | Параметры | Ответ |")
print("|---|---|---|---|")
for method, url, params, resp in rows:
    ps = []
    for kind, name, typ in params:
        if kind in ("Query", "Path", "Header"):
            ps.append(f"{kind.lower()} `{name}`")
        elif kind == "Body":
            ps.append(f"body `{typ}`")
        else:
            ps.append("multipart")
    r = resp.replace("List<? extends ", "List<").replace("PaginatedResult<", "Page<")
    print(f"| {method} | `{url}` | {', '.join(ps) or '—'} | `{r}` |")
