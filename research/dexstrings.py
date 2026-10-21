"""Extract strings from DEX files inside an APK (or raw .dex)."""
import struct
import sys
import zipfile


def uleb128(data, p):
    shift = 0
    val = 0
    while True:
        b = data[p]
        p += 1
        val |= (b & 0x7F) << shift
        if not (b & 0x80):
            return val, p
        shift += 7


def dex_strings(data: bytes):
    if data[:4] != b'dex\n':
        return []
    size, off = struct.unpack_from('<II', data, 0x38)
    out = []
    for i in range(size):
        so = struct.unpack_from('<I', data, off + 4 * i)[0]
        _, p = uleb128(data, so)
        end = data.find(b'\x00', p)
        if end < 0:
            end = len(data)
        out.append(data[p:end])
    return out


def main(path):
    if path.endswith('.apk'):
        z = zipfile.ZipFile(path)
        all_strings = []
        for name in sorted(n for n in z.namelist() if n.endswith('.dex')):
            s = dex_strings(z.read(name))
            print('%s: %d strings' % (name, len(s)), file=sys.stderr)
            all_strings.extend(s)
    else:
        all_strings = dex_strings(open(path, 'rb').read())

    for s in all_strings:
        sys.stdout.write(s.decode('utf-8', 'replace').replace('\n', ' ') + '\n')


if __name__ == '__main__':
    main(sys.argv[1])
