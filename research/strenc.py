"""Find XOR-obfuscated string literals (key = (i%13)+0x4A) referenced from code."""
import os
import sys, struct
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from elfutil import ELF
from capstone import Cs, CS_ARCH_ARM64, CS_MODE_ARM

E = ELF(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'libs', 'libbnit.so'))
MD = Cs(CS_ARCH_ARM64, CS_MODE_ARM)
MD.detail = True
T = E.section('.text')


def dec(raw):
    return bytes(((i % 13) + 0x4a) ^ b for i, b in enumerate(raw))


def printable_prefix(bs):
    out = []
    for b in bs:
        if 32 <= b < 127:
            out.append(chr(b))
        else:
            break
    return ''.join(out)


def scan(lo, hi):
    adrp = None
    res = {}
    for ins in MD.disasm(E.read(lo, hi - lo), lo):
        if ins.mnemonic == 'adrp':
            adrp = ins.operands[1].imm
        elif ins.mnemonic == 'adr':
            res.setdefault(ins.operands[1].imm, set()).add(ins.address)
        elif adrp is not None and ins.mnemonic == 'add' and len(ins.operands) == 3:
            res.setdefault(adrp + ins.operands[2].imm, set()).add(ins.address)
            adrp = None
        else:
            if ins.mnemonic != 'adrp':
                adrp = None
    return res


refs = scan(T['addr'], T['addr'] + T['size'])
found = 0
for addr, sites in sorted(refs.items()):
    if not (E.section('.rodata')['addr'] <= addr < E.section('.rodata')['addr'] + E.section('.rodata')['size']):
        continue
    raw = E.read(addr, 64)
    s = printable_prefix(dec(raw))
    if len(s) >= 5 and s.isprintable():
        print('0x%06x  len?%-3d %-42r  refs: %s' % (addr, len(s), s, ', '.join('0x%x' % x for x in sorted(sites))))
        found += 1
print('---', found)
