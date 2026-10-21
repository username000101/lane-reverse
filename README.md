# laned — клиент Lane с восстановленной подписью

Воркспейс из трёх крейтов вокруг алгоритма из `libbnit.so`
(`BNITManager.cdrl`): подпись запросов, CLI-клиент и TUI.

| Крейт | Что |
|---|---|
| [`laned-bnit`](crates/laned-bnit) | сама логика: 3 заголовка + шифрованное тело, C API |
| [`laned-cli`](crates/laned-cli) | библиотека `LaneClient` (подписанный HTTP) + бинарь `laned` |
| [`laned-tui`](crates/laned-tui) | терминальный браузер: плейлисты → треки → ссылка на поток |
| [`laned-bot`](crates/laned-bot) | Telegram-бот на `grammers` (поиск, скачивание в чат, плейлисты, профиль) |

```
JSON-тело ──RC4*(KEY‖nonce‖ts)──► тело запроса (для POST)
query: дедуп по имени + сортировка + join("name=value", "&")
HMAC-SHA256(KEY, "method:path:query:ts:nonce:field:boot:inode") → x-core-token
red-ответ: RC4*(KEY‖X-Resp-Nonce) [+ gzip]
```

## Сборка и тесты

```bash
cargo build --release
cargo test
```

## CLI `laned`

Креды берутся из окружения:

```powershell
$env:LANED_JWT = '<bearer token>'
$env:LANED_LDI = '594c423d806a926e'
```

```powershell
cargo run --release -p laned-cli -- playlists
cargo run --release -p laned-cli -- tracks lane_likes 1 50
cargo run --release -p laned-cli -- stream 2lEbe9evbwJ1unoQAb0uPk lane_likes BASIC
cargo run --release -p laned-cli -- play 2lEbe9evbwJ1unoQAb0uPk lane_likes BASIC
cargo run --release -p laned-cli -- download 2lEbe9evbwJ1unoQAb0uPk lane_likes BASIC --out track.mp3
cargo run --release -p laned-cli -- get /user/recent
cargo run --release -p laned-cli -- auth-new [--id STRING]
cargo run --release -p laned-cli -- auth-url
cargo run --release -p laned-cli -- auth-wait <android_id> --out token.json
cargo run --release -p laned-cli -- post /user/tracks prefetch=0 --body '{"trackIds":["…"]}'
```

Офлайн-команды (без сети):

```powershell
cargo run -p laned-cli -- gen --method GET --path /user/playlists
cargo run -p laned-cli -- dump http-request-dumps/user@recent.txt
cargo run -p laned-cli -- inode
```

`stream` — то, ради чего всё делалось: `GET /track/stream` возвращает
`{url, trackId, playbackToken?, ttl}`. `refId` — контекст (CLI принимает любое
значение, TUI подставляет id плейлиста), `streamQuality` — `BASIC`/`HIGH`/`ULTRA`.

`play` берёт `url` и запускает внешний плеер: сначала `LANED_PLAYER` (если задан),
потом `mpv` / `ffplay` / `vlc`, а если ничего не нашлось — скачивает трек во
временный файл и открывает системным обработчиком. `download` просто сохраняет
поток в файл.

## TUI

```powershell
cargo run --release -p laned-tui
```

`↑`/`↓` или `j`/`k` — выбор, `Enter` — открыть плейлист / получить ссылку на
трек, `p` — проиграть полученную ссылку, `1`/`2`/`3` — качество
(`BASIC`/`HIGH`/`ULTRA`), `Tab` — переключить панель, `r` — перезагрузить,
`q` — выход. Ссылка и `ttl` показываются во всплывающем окне.

## C API

```c
#include "bnit.h"

bnit_headers h;
if (bnit_build("GET", "/user/playlists", "", NULL, 0, 0, &h) == 0) {
    /* h.core_token / h.trace_id / h.client_meta; для POST — h.body/h.body_len */
    bnit_headers_free(&h);
}
```

`cargo build --release` даёт `laned_bnit.dll` + `laned_bnit.dll.lib`
(Windows), `liblaned_bnit.so` / `.a` (Linux). Заголовок — [`include/bnit.h`](include/bnit.h),
пример — [`examples/c_example.c`](examples/c_example.c).

## Структура

```
crates/laned-bnit/   ядро: SHA-256/HMAC, RC4*, шифр токена, base64, UUID
crates/laned-cli/    LaneClient + CLI (gen/dump/get/post/stream/…)
crates/laned-tui/    ratatui-интерфейс
include/bnit.h       C API
docs/                алгоритм, эндпоинты, ответы, отчёт
research/            скрипты разбора оригинальной .so + dex
libs/                исходные .so
http-request-dumps/  перехваченные запросы
```

## Веб-версия

[`web/`](web) — та же подпись, портированная на Node.js, и красивый браузерный
клиент: лендинг, вход через официального `@lane_music_bot`, плейлисты и плеер.
Браузер не видит ни JWT, ни ключа: `/api/*` — обычные Vercel Functions, сессия
лежит в зашифрованной HttpOnly-cookie.

Живая версия: <https://lane-web-rouge.vercel.app>

Подробности, переменные окружения и локальный запуск — в [web/README.md](web/README.md).

## Документация

- [docs/API-REFERENCE.md](docs/API-REFERENCE.md) — все 84 эндпоинта из APK, загрузка файлов.
- [docs/AUTH.md](docs/AUTH.md) — вход через Telegram-бота без приложения (device id, /auth, токен).
- [docs/ALGORITHM.md](docs/ALGORITHM.md) — алгоритм по слоям.
- [docs/ENDPOINTS.md](docs/ENDPOINTS.md) — что подписывается, `/track/stream`, `red`.
- [docs/RESPONSES.md](docs/RESPONSES.md) — схемы ответов.
- [docs/REPORT.md](docs/REPORT.md) — отчёт по реверсу.

## Важно

- Ключ и алгоритм живут в клиенте: это обфускация, а не аутентификация клиента.
- Используйте только со своими учётными записями и в рамках правил сервиса.
