"""Minimal ELF64 reader + helpers for libbnit.so analysis (no external deps)."""
import struct

class ELF:
    def __init__(self, path):
        self.path = path
        self.data = open(path, 'rb').read()
        d = self.data
        assert d[:4] == b'\x7fELF'
        self.is64 = d[4] == 2
        self.le = d[5] == 1
        e = '<' if self.le else '>'
        self.e = e
        (self.e_type, self.e_machine) = struct.unpack_from(e + 'HH', d, 16)
        (self.e_entry,) = struct.unpack_from(e + 'Q', d, 24)
        (self.e_phoff,) = struct.unpack_from(e + 'Q', d, 32)
        (self.e_shoff,) = struct.unpack_from(e + 'Q', d, 40)
        (self.e_flags, self.e_ehsize, self.e_phentsize, self.e_phnum,
         self.e_shentsize, self.e_shnum, self.e_shstrndx) = struct.unpack_from(e + 'IHHHHHH', d, 48)
        self._load_sections()
        self._load_segments()

    def _load_sections(self):
        self.sections = []
        e = self.e
        shstr = None
        for i in range(self.e_shnum):
            off = self.e_shoff + i * self.e_shentsize
            (name, typ, flags, addr, offset, size, link, info, align, entsize) = \
                struct.unpack_from(e + 'IIQQQQIIQQ', self.data, off)
            self.sections.append(dict(idx=i, name_off=name, type=typ, flags=flags,
                                      addr=addr, offset=offset, size=size, link=link,
                                      info=info, align=align, entsize=entsize))
        shstr = self.sections[self.e_shstrndx]
        strtab = self.data[shstr['offset']:shstr['offset'] + shstr['size']]
        for s in self.sections:
            end = strtab.find(b'\0', s['name_off'])
            s['name'] = strtab[s['name_off']:end].decode('latin1')

    def _load_segments(self):
        self.segments = []
        for i in range(self.e_phnum):
            off = self.e_phoff + i * self.e_phentsize
            (typ, flags, offset, vaddr, paddr, filesz, memsz, align) = \
                struct.unpack_from(self.e + 'IIQQQQQQ', self.data, off)
            self.segments.append(dict(type=typ, flags=flags, offset=offset, vaddr=vaddr,
                                      filesz=filesz, memsz=memsz, align=align))

    def section(self, name):
        for s in self.sections:
            if s['name'] == name:
                return s
        raise KeyError(name)

    def off2va(self, off):
        for p in self.segments:
            if p['type'] == 1 and p['offset'] <= off < p['offset'] + p['filesz']:
                return p['vaddr'] + (off - p['offset'])
        return None

    def va2off(self, va):
        for p in self.segments:
            if p['type'] == 1 and p['vaddr'] <= va < p['vaddr'] + p['filesz']:
                return p['offset'] + (va - p['vaddr'])
        # NOBITS (.bss) fallback
        for s in self.sections:
            if s['type'] == 8 and s['addr'] <= va < s['addr'] + s['size']:
                return None
        return None

    def read(self, va, n):
        off = self.va2off(va)
        if off is None:
            return b'\0' * n
        return self.data[off:off + n]

    def u32(self, va):
        return struct.unpack_from(self.e + 'I', self.read(va, 4), 0)[0]

    def u64(self, va):
        return struct.unpack_from(self.e + 'Q', self.read(va, 8), 0)[0]

    def cstr(self, va, maxlen=512):
        raw = self.read(va, maxlen)
        end = raw.find(b'\0')
        if end >= 0:
            raw = raw[:end]
        return raw.decode('latin1')

    def cstr_bytes(self, va, maxlen=512):
        raw = self.read(va, maxlen)
        end = raw.find(b'\0')
        return raw if end < 0 else raw[:end]

    def is_mapped(self, va):
        return self.va2off(va) is not None

    def dumps(self):
        out = []
        out.append('ELF type=%d machine=0x%x' % (self.e_type, self.e_machine))
        for s in self.sections:
            out.append('[%2d] %-20s %08x %06x %06x' % (s['idx'], s['name'], s['addr'], s['offset'], s['size']))
        return '\n'.join(out)


# --- obfuscated literal decoder observed in JNI_OnLoad ---
def dec_jni_name(src: bytes, key_period=13, key_base=0x4a):
    return bytes((((i % key_period) + key_base) ^ b) & 0xff for i, b in enumerate(src))
