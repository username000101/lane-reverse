"""Build PLT stub -> imported symbol map for libbnit.so."""
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from elfutil import ELF
from capstone import Cs, CS_ARCH_ARM64, CS_MODE_ARM

E = ELF(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'libs', 'libbnit.so'))
MD = Cs(CS_ARCH_ARM64, CS_MODE_ARM)
MD.detail = True

# relocation offset -> symbol name from .rela.plt
import struct
rela = E.section('.rela.plt')
dynsym = E.section('.dynsym')
dynstr = E.section('.dynstr')
strtab = E.data[dynstr['offset']:dynstr['offset'] + dynstr['size']]

def symname(idx):
    off = dynsym['offset'] + idx * 24
    name, info, other, shndx, value, size = struct.unpack_from('<IBBHQQ', E.data, off)
    end = strtab.find(b'\0', name)
    return strtab[name:end].decode('latin1')

got2sym = {}
for i in range(rela['size'] // 24):
    off = rela['offset'] + i * 24
    r_offset, r_info, r_addend = struct.unpack_from('<QQq', E.data, off)
    sym = r_info >> 32
    got2sym[r_offset] = symname(sym)

plt = E.section('.plt')
out = []
stubs = {}
for a in range(plt['addr'] + 0x20, plt['addr'] + plt['size'], 16):
    insns = list(MD.disasm(E.read(a, 16), a))
    if len(insns) < 2:
        continue
    # adrp x16, page
    base = None
    got = None
    for ins in insns:
        if ins.mnemonic == 'adrp':
            base = ins.operands[1].imm
        elif ins.mnemonic == 'ldr' and base is not None:
            got = base + ins.operands[1].mem.disp
        elif ins.mnemonic == 'add' and base is not None:
            got = base + ins.operands[2].imm
    name = got2sym.get(got, '?')
    stubs[a] = name
    out.append('0x%06x  %s' % (a, name))

if __name__ == '__main__':
    print('\n'.join(out))
