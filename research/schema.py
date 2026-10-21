"""Infer a Markdown schema from JSON samples.

Usage:
    python tools/schema.py response.json
    python tools/schema.py samples/*.json > RESPONSES.md

Multiple samples are merged: fields present in every sample are required,
fields missing somewhere are marked optional. Types seen are unioned.
"""
import json
import sys
from collections import OrderedDict


class Schema:
    def __init__(self):
        self.types = set()
        self.fields = OrderedDict()   # name -> Schema
        self.items = None             # Schema for array elements
        self.examples = []
        self.present = 0

    def merge(self, value, seen_count):
        self.present += 1
        if value is None:
            self.types.add('null')
            return
        if isinstance(value, bool):
            self.types.add('bool')
        elif isinstance(value, int):
            self.types.add('int')
        elif isinstance(value, float):
            self.types.add('float')
        elif isinstance(value, str):
            self.types.add('string')
        elif isinstance(value, list):
            self.types.add('array')
            if self.items is None:
                self.items = Schema()
            for v in value:
                self.items.merge(v, len(value))
        elif isinstance(value, dict):
            self.types.add('object')
            for k, v in value.items():
                if k not in self.fields:
                    self.fields[k] = Schema()
                self.fields[k].merge(v, len(value))
        if len(self.examples) < 3 and not isinstance(value, (dict, list)):
            if value not in self.examples:
                self.examples.append(value)


def type_name(t):
    order = ['object', 'array', 'string', 'int', 'float', 'bool', 'null']
    return ' | '.join(x for x in order if x in t) or '?'


def render(s: Schema, name: str, indent: int, total: int, out: list):
    pad = '  ' * indent
    optional = '' if s.present >= total else ' _(optional)_'
    ex = ''
    if s.examples:
        ex = '  e.g. `%s`' % ', '.join(repr(e)[:40] for e in s.examples[:2])
    if 'object' in s.types and s.fields:
        out.append('%s- `%s`: object%s%s' % (pad, name, optional, ex))
        for k, v in s.fields.items():
            render(v, k, indent + 1, s.present, out)
        return
    if 'array' in s.types and s.items is not None:
        inner = type_name(s.items.types)
        out.append('%s- `%s`: array<%s>%s%s' % (pad, name, inner, optional, ex))
        if s.items.fields:
            for k, v in s.items.fields.items():
                render(v, k, indent + 1, s.items.present, out)
        return
    out.append('%s- `%s`: %s%s%s' % (pad, name, type_name(s.types), optional, ex))


def main(paths):
    root = Schema()
    total = 0
    for p in paths:
        with open(p, encoding='utf-8') as f:
            data = json.load(f)
        root.merge(data, 1)
        total += 1

    out = ['# Схема ответа (%d образец(ов))' % total, '']
    render(root, 'root', 0, total, out)
    print('\n'.join(out))


if __name__ == '__main__':
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(1)
    main(args)
