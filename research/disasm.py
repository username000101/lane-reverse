"""Annotating AArch64 disassembler for libbnit.so."""
import os
import sys, struct
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from elfutil import ELF
from capstone import Cs, CS_ARCH_ARM64, CS_MODE_ARM
from capstone.arm64 import ARM64_OP_REG, ARM64_OP_IMM, ARM64_OP_MEM

E = ELF(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'libs', 'libbnit.so'))
MD = Cs(CS_ARCH_ARM64, CS_MODE_ARM)
MD.detail = True

# function starts from FDE
from elftools.elf.elffile import ELFFile
_f = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'libs', 'libbnit.so'), 'rb')
_el = ELFFile(_f)
FDE = sorted({e['initial_location'] for e in _el.get_dwarf_info().EH_CFI_entries()
              if e.__class__.__name__ == 'FDE'})
FDE_SET = set(FDE)

try:
    import pltmap
    PLT = pltmap.stubs
except Exception:
    PLT = {}

def func_of(addr):
    import bisect
    i = bisect.bisect_right(FDE, addr) - 1
    return FDE[i] if i >= 0 else None

def code(va, size):
    return E.read(va, size)

def printable(va, maxlen=80):
    b = E.read(va, maxlen)
    end = b.find(b'\0')
    if end < 0:
        end = maxlen
    b = b[:end]
    if len(b) < 2:
        return None
    ok = sum(1 for c in b if 32 <= c < 127 or c in (9, 10, 13))
    if ok / len(b) < 0.85:
        return None
    return b.decode('latin1')

def describe(va):
    s = printable(va)
    if s is not None:
        return 'str %r' % s
    sec = None
    for x in E.sections:
        if x['addr'] <= va < x['addr'] + x['size']:
            sec = x['name']
    val = struct.unpack_from('<Q', E.read(va, 8), 0)[0]
    return '%s @0x%x = 0x%x' % (sec or '?', va, val)

def disasm(va, size, annotate=True, show_raw=False, only_calls=False):
    out = []
    prev_adrp = None
    for ins in MD.disasm(code(va, size), va):
        text = ins.mnemonic + ('\t' + ins.op_str if ins.op_str else '')
        note = ''
        if annotate:
            m = ins.mnemonic
            if m == 'adrp':
                prev_adrp = ins.operands[1].imm
            elif m in ('add', 'ldr', 'str', 'ldrb', 'strb', 'ldur', 'stur', 'ldp', 'stp') and prev_adrp is not None:
                if len(ins.operands) >= 2 and ins.operands[1].type == ARM64_OP_IMM:
                    target = prev_adrp + ins.operands[1].imm
                    note = '; ' + describe(target) + '  (0x%x)' % target
                prev_adrp = None
            elif m in ('adr',):
                t = ins.operands[1].imm
                note = '; ' + describe(t) + '  (0x%x)' % t
            elif m == 'bl' or m == 'b':
                t = ins.operands[0].imm
                f = func_of(t)
                if t in PLT:
                    note = '; -> IMPORT %s' % PLT[t]
                elif t in FDE_SET:
                    note = '; -> func_%x' % t
                elif f:
                    note = '; -> func_%x+0x%x' % (f, t - f)
            elif m == 'br' or m == 'blr':
                pass
            if m not in ('adrp',) and not (m in ('add','ldr','str') and prev_adrp is not None):
                pass
        if only_calls and ins.mnemonic not in ('bl', 'blr', 'b'):
            continue
        line = '0x%06x  %-40s' % (ins.address, text)
        if show_raw:
            line = '0x%06x  %-10s %-34s' % (ins.address, ins.bytes.hex(), text)
        out.append(line + note)
        if only_calls:
            continue
    return '\n'.join(out)

def xrefs(target, lo=None, hi=None):
    """Find adrp+add / adrp+ldr / adr references to target across .text."""
    text = E.section('.text')
    lo = text['addr'] if lo is None else lo
    hi = text['addr'] + text['size'] if hi is None else hi
    res = []
    pages = {}
    for ins in MD.disasm(E.read(lo, hi - lo), lo):
        m = ins.mnemonic
        if m == 'adrp':
            pages[ins.operands[0].reg] = ins.operands[1].imm
            continue
        if m == 'adr':
            if ins.operands[1].imm == target:
                res.append(ins.address)
            continue
        t = None
        if m in ('add', 'ldr', 'str', 'ldrb', 'strb', 'ldrh', 'strh', 'ldp', 'stp',
                 'ldur', 'stur', 'ldurb', 'sturb', 'ldrsw', 'ldrsw'):
            try:
                ops = ins.operands
                base_reg = None
                if m == 'add' and len(ops) >= 3 and ops[1].type == ARM64_OP_REG:
                    base_reg = ops[1].reg
                    if base_reg in pages:
                        t = pages[base_reg] + ops[2].imm
                elif len(ops) >= 2 and ops[1].type == ARM64_OP_MEM:
                    base_reg = ops[1].mem.base
                    if base_reg in pages:
                        t = pages[base_reg] + ops[1].mem.disp
            except Exception:
                pass
        if t == target:
            res.append(ins.address)
        # invalidate written registers
        try:
            _, written = ins.regs_access()
            for r in written:
                pages.pop(r, None)
        except Exception:
            pages.clear()
    return res

if __name__ == '__main__':
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('addr', type=lambda x: int(x, 0))
    ap.add_argument('size', nargs='?', type=lambda x: int(x, 0), default=None)
    ap.add_argument('--calls', action='store_true')
    ap.add_argument('--raw', action='store_true')
    a = ap.parse_args()
    size = a.size
    if size is None:
        nxt = [f for f in FDE if f > a.addr]
        size = (nxt[0] if nxt else E.section('.text')['addr'] + E.section('.text')['size']) - a.addr
    print(disasm(a.addr, size, show_raw=a.raw, only_calls=a.calls))
