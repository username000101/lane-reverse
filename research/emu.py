"""Unicorn-based harness to execute libbnit.so's cdrl() with stubbed libc/JNI."""
import struct, sys, traceback
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM, UC_HOOK_CODE, UC_HOOK_MEM_UNMAPPED, UC_PROT_ALL
from unicorn.arm64_const import *
from elftools.elf.elffile import ELFFile

BNIT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'libs', 'libbnit.so')
BASE = 0x10000000
STUB = 0x30000000          # external import stubs (4 bytes each)
STUBDATA = 0x31000000      # fake data objects (e.g. __sF)
JNITAB = 0x32000000        # JNI function table stubs
HEAP = 0x40000000
JNIENV = 0x50000000
STACK = 0x70000000
STACKSZ = 0x200000
SENTINEL = 0x0BADF00D
SCRATCH = 0x33000000

uc = Uc(UC_ARCH_ARM64, UC_MODE_ARM)


class M:
    @staticmethod
    def rd(a, n):
        return bytes(uc.mem_read(a, n))

    @staticmethod
    def wr(a, b):
        uc.mem_write(a, bytes(b))

    @staticmethod
    def u8(a):
        return uc.mem_read(a, 1)[0]

    @staticmethod
    def u32(a):
        return struct.unpack('<I', uc.mem_read(a, 4))[0]

    @staticmethod
    def s32(a):
        return struct.unpack('<i', uc.mem_read(a, 4))[0]

    @staticmethod
    def u64(a):
        return struct.unpack('<Q', uc.mem_read(a, 8))[0]

    @staticmethod
    def w64(a, v):
        uc.mem_write(a, struct.pack('<Q', v))

    @staticmethod
    def w32(a, v):
        uc.mem_write(a, struct.pack('<I', v))


def reg(r):
    return uc.reg_read(r)


def setreg(r, v):
    uc.reg_write(r, v)


def map_region(addr, size):
    start = addr & ~0xfff
    end = (addr + size + 0xfff) & ~0xfff
    try:
        uc.mem_map(start, end - start, UC_PROT_ALL)
    except Exception:
        pass
    return start


_mapped = []


def _coverage(addr, size):
    """Return list of (start,end) gaps inside [addr,addr+size) not yet mapped."""
    start = addr & ~0xfff
    end = (addr + size + 0xfff) & ~0xfff
    gaps = []
    cur = start
    for (s, e) in sorted(_mapped):
        if e <= cur or s >= end:
            continue
        if s > cur:
            gaps.append((cur, min(s, end)))
        cur = max(cur, e)
        if cur >= end:
            break
    if cur < end:
        gaps.append((cur, end))
    return gaps


def ensure_map(addr, size):
    for (s, e) in _coverage(addr, size):
        try:
            uc.mem_map(s, e - s, UC_PROT_ALL)
            _mapped.append((s, e))
        except Exception:
            pass
    _mapped.sort()


def load_elf(path, base):
    el = ELFFile(open(path, 'rb'))
    for seg in el.iter_segments():
        if seg['p_type'] != 'PT_LOAD':
            continue
        v = base + seg['p_vaddr']
        sz = seg['p_memsz']
        ensure_map(v, sz)
        M.wr(v, seg.data())
    return el


# ---------------- heap ----------------
_heap = [HEAP, 0]


def malloc(n):
    n = (n + 15) & ~15
    if n == 0:
        n = 16
    p = _heap[0]
    _heap[0] += n
    if _heap[0] > HEAP + 0x4000000:
        raise RuntimeError('heap exhausted')
    return p


# ---------------- libc++ std::string ----------------
def st_is_long(p):
    return M.u8(p) & 1


def st_size(p):
    b = M.u8(p)
    return M.u64(p + 8) if (b & 1) else (b >> 1)


def st_data(p):
    return M.u64(p + 16) if st_is_long(p) else p + 1


def st_get(p):
    n = st_size(p)
    if n > 0x10000:
        raise RuntimeError('bad string size %d at 0x%x' % (n, p))
    return M.rd(st_data(p), n)


def st_set(p, data):
    n = len(data)
    if n <= 22:
        M.wr(p, bytes([n << 1]) + data + b'\0' * (23 - n))
    else:
        ptr = malloc(n + 1)
        M.wr(ptr, data + b'\0')
        M.w64(p, n | 1)
        M.w64(p + 8, n)
        M.w64(p + 16, ptr)


def st_reserve(p, n):
    cur = st_size(p)
    cap = (M.u64(p) & ~1) if st_is_long(p) else 22
    if cap >= n:
        return
    data = st_get(p)
    ptr = malloc(n + 1)
    M.wr(ptr, data)
    M.w64(p, n | 1)
    M.w64(p + 8, cur)
    M.w64(p + 16, ptr)


def st_push_back(p, c):
    st_set(p, st_get(p) + bytes([c & 0xff]))


def cstr(p):
    out = bytearray()
    while True:
        b = M.u8(p)
        if b == 0:
            break
        out.append(b)
        p += 1
        if len(out) > 0x10000:
            break
    return bytes(out)


# ---------------- import stubs ----------------
_stub_map = {}
_next_stub = [STUB + 0x1000]
IMPL = {}


_data_blobs = {}
_next_data = [STUBDATA + 0x1000]


def data_blob(name):
    if name in _data_blobs:
        return _data_blobs[name]
    base = _next_data[0]
    _next_data[0] += 0x100
    ensure_map(base, 0x100)
    M.wr(base, b'\0' * 0x100)
    # emulate an Itanium-ABI address point at base+0x20
    M.w64(base + 0x20, base + 0x40)   # vtable[0] -> some readable "code" area
    M.w64(base + 0x38, base + 0x20)   # vtable[3]
    _data_blobs[name] = base + 0x20
    return base + 0x20


DATA_PREFIX = ('_ZTV', '_ZTT', '_ZTI', '_ZTS', '__sF', '_ZNSt6__ndk15ctypeIcE2idE')


def resolve(name):
    if name.startswith(DATA_PREFIX):
        return data_blob(name)
    return stub_for(name)


def stub_for(name):
    if name in _stub_map:
        return _stub_map[name]
    a = _next_stub[0]
    _next_stub[0] += 4
    ensure_map(a, 4)
    M.w32(a, 0xd65f03c0)  # ret
    _stub_map[a] = name
    return a


def stub(name):
    def deco(fn):
        IMPL[name] = fn
        return fn
    return deco


# virtual filesystem
VFS = {}


def vfile(path, data):
    VFS[path] = data


_fds = {}
_next_fd = [3]


@stub('__cxa_finalize')
def _(name=''):
    pass


@stub('__cxa_atexit')
def _(name=''):
    return 0


@stub('__register_atfork')
def _(name=''):
    return 0


def nop_ret(name, value=0):
    def f():
        setreg(UC_ARM64_REG_X0, value)
    IMPL[name] = f


for n in ['__cxa_allocate_exception', '__cxa_free_exception', '__cxa_begin_catch',
          '__cxa_end_catch', '_ZNSt11logic_errorC2EPKc',
          '_ZNSt6__ndk113basic_filebufIcNS_11char_traitsIcEEEC1Ev',
          '_ZNSt6__ndk113basic_filebufIcNS_11char_traitsIcEEED1Ev',
          '_ZNSt6__ndk113basic_istreamIcNS_11char_traitsIcEEED2Ev',
          '_ZNSt6__ndk19basic_iosIcNS_11char_traitsIcEEED2Ev',
          '_ZNSt6__ndk18ios_base4initEPv',
          '_ZNSt6__ndk113basic_filebufIcNS_11char_traitsIcEEE4openEPKcj',
          '_ZNSt6__ndk18ios_base5clearEj',
          '_ZNSt6__ndk113basic_istreamIcNS_11char_traitsIcEEE6sentryC1ERS3_b',
          '_ZNSt6__ndk16localeD1Ev',
          '_ZNSt6__ndk113basic_filebufIcNS_11char_traitsIcEEE5closeEv',
          'fflush', 'fwrite']:
    nop_ret(n, 0)

IMPL['_ZNSt6__ndk113basic_filebufIcNS_11char_traitsIcEEE4openEPKcj'] = lambda: setreg(UC_ARM64_REG_X0, 1)


@stub('__cxa_throw')
def _():
    raise RuntimeError('C++ exception thrown')


@stub('_ZSt9terminatev')
def _():
    raise RuntimeError('std::terminate')


@stub('abort')
def _():
    raise RuntimeError('abort()')


@stub('__stack_chk_fail')
def _():
    raise RuntimeError('stack check fail')


@stub('_Znwm')
def _():
    setreg(UC_ARM64_REG_X0, malloc(reg(UC_ARM64_REG_X0)))


@stub('_ZdlPv')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('malloc')
def _():
    setreg(UC_ARM64_REG_X0, malloc(reg(UC_ARM64_REG_X0)))


@stub('free')
def _():
    pass


@stub('memcpy')
def _():
    d, s, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    if n:
        M.wr(d, M.rd(s, n))
    setreg(UC_ARM64_REG_X0, d)


@stub('memmove')
def _():
    d, s, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    if n:
        M.wr(d, M.rd(s, n))
    setreg(UC_ARM64_REG_X0, d)


@stub('memset')
def _():
    d, c, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    if n:
        M.wr(d, bytes([c & 0xff]) * n)
    setreg(UC_ARM64_REG_X0, d)


@stub('strlen')
def _():
    p = reg(UC_ARM64_REG_X0)
    n = 0
    while M.u8(p + n) != 0:
        n += 1
    setreg(UC_ARM64_REG_X0, n)


@stub('memchr')
def _():
    p, c, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    data = M.rd(p, n)
    i = data.find(bytes([c & 0xff]))
    setreg(UC_ARM64_REG_X0, p + i if i >= 0 else 0)


@stub('_ZNSt6__ndk112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE7reserveEm')
def _():
    st_reserve(reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1))


@stub('_ZNSt6__ndk112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE9push_backEc')
def _():
    st_push_back(reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1))


@stub('_ZNSt6__ndk112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE6appendEPKcm')
def _():
    p, s, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    st_set(p, st_get(p) + M.rd(s, n))
    setreg(UC_ARM64_REG_X0, p)


@stub('_ZNSt6__ndk112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE6assignEPKc')
def _():
    p, s = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1)
    st_set(p, cstr(s))
    setreg(UC_ARM64_REG_X0, p)


@stub('_ZNSt6__ndk19to_stringEx')
def _():
    v = reg(UC_ARM64_REG_X0)
    if v >= 1 << 63:
        v -= 1 << 64
    dst = reg(UC_ARM64_REG_X8)
    st_set(dst, str(v).encode())
    setreg(UC_ARM64_REG_X0, dst)


@stub('_ZNSt6__ndk19to_stringEm')
def _():
    v = reg(UC_ARM64_REG_X0)
    dst = reg(UC_ARM64_REG_X8)
    st_set(dst, str(v).encode())
    setreg(UC_ARM64_REG_X0, dst)


@stub('gettimeofday')
def _():
    tv = reg(UC_ARM64_REG_X0)
    if tv:
        M.w64(tv, 1700000000)
        M.w64(tv + 8, 123456)
    setreg(UC_ARM64_REG_X0, 0)


@stub('__errno')
def _():
    setreg(UC_ARM64_REG_X0, SCRATCH + 0x800)


@stub('_ZNKSt6__ndk18ios_base6getlocEv')
def _():
    setreg(UC_ARM64_REG_X0, SCRATCH + 0x100)


@stub('_ZNKSt6__ndk16locale9use_facetERNS0_2idE')
def _():
    setreg(UC_ARM64_REG_X0, SCRATCH + 0x200)


@stub('pthread_rwlock_rdlock')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('pthread_rwlock_wrlock')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('pthread_rwlock_unlock')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('dl_iterate_phdr')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('syscall')
def _():
    setreg(UC_ARM64_REG_X0, -1)


@stub('fprintf')
def _():
    setreg(UC_ARM64_REG_X0, 0)


@stub('stat')
def _():
    pathp, bufp = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1)
    path = cstr(pathp).decode('latin1')
    if bufp:
        M.wr(bufp, b'\0' * 128)
        M.w64(bufp + 8, 0x1234)      # st_ino
        M.w32(bufp + 16, 0o100644)   # st_mode
        M.w64(bufp + 48, len(VFS.get(path, b'')))
    setreg(UC_ARM64_REG_X0, 0 if path in VFS else -1)


@stub('__open_2')
def _():
    path = cstr(reg(UC_ARM64_REG_X0)).decode('latin1')
    if path in VFS:
        fd = _next_fd[0]
        _next_fd[0] += 1
        _fds[fd] = [path, 0]
        setreg(UC_ARM64_REG_X0, fd)
    else:
        setreg(UC_ARM64_REG_X0, -1)


@stub('read')
def _():
    fd, buf, n = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2)
    if fd in _fds:
        path, off = _fds[fd]
        data = VFS[path][off:off + n]
        M.wr(buf, data)
        _fds[fd][1] = off + len(data)
        setreg(UC_ARM64_REG_X0, len(data))
    else:
        setreg(UC_ARM64_REG_X0, -1)


@stub('close')
def _():
    fd = reg(UC_ARM64_REG_X0)
    _fds.pop(fd, None)
    setreg(UC_ARM64_REG_X0, 0)


@stub('vsnprintf')
def _():
    out, size, fmt, va = (reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1),
                          reg(UC_ARM64_REG_X2), reg(UC_ARM64_REG_X3))
    f = cstr(fmt).decode('latin1')
    # decode va_list (AArch64): {stack, gr_top, vr_top, gr_offs, vr_offs}
    stack = M.u64(va)
    gr_top = M.u64(va + 8)
    gr_offs = M.s32(va + 24)

    def next_int():
        nonlocal stack, gr_offs
        if gr_offs < 0:
            p = (gr_top + gr_offs) & 0xffffffffffffffff
            gr_offs += 8
        else:
            p = stack
            stack += 8
        return M.u64(p)

    res = ''
    i = 0
    while i < len(f):
        c = f[i]
        if c == '%' and i + 1 < len(f):
            j = i + 1
            while j < len(f) and f[j] in '0123456789.#-+ ':
                j += 1
            conv = f[j] if j < len(f) else ''
            if conv in 'xX':
                v = next_int() & 0xffffffff
                width = int(f[i + 1:j].replace('0', '0') or '0')
                res += ('%0*x' % (width, v)) if width else format(v, 'x')
            elif conv == 's':
                p = next_int()
                res += cstr(p).decode('latin1')
            elif conv in 'diu':
                res += str(next_int() & 0xffffffff)
            else:
                res += c
            i = j + 1
        else:
            res += c
            i += 1
    data = res.encode('latin1')[:size - 1]
    M.wr(out, data + b'\0')
    setreg(UC_ARM64_REG_X0, len(data))


# ---------------- internal patches ----------------
RAND = {'seq': [0x11111111, 0x22222222, 0x33333333, 0x44444444,
                0x55555555, 0x66666666, 0x77777777, 0x88888888,
                0x99999999, 0xAAAAAAAA, 0xBBBBBBBB, 0xCCCCCCCC,
                0xDDDDDDDD, 0xEEEEEEEE, 0x01020304, 0x05060708],
        'i': 0}

CUR = {'path': None, 'lines': []}


def patch(addr, fn):
    a = BASE + addr
    M.w32(a, 0xd65f03c0)
    HANDLERS[a] = fn


HANDLERS = {}


def h_rand():
    v = RAND['seq'][RAND['i'] % len(RAND['seq'])]
    RAND['i'] += 1
    setreg(UC_ARM64_REG_X0, v)


def h_open_stream():
    obj, sp = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1)
    path = st_get(sp).decode('latin1')
    CUR['path'] = path
    CUR['lines'] = VFS.get(path, b'').split(b'\n')
    CUR['i'] = 0
    M.w64(obj + 0x108, 1)
    M.w64(obj + 0x88, 1)
    M.w64(obj, 0x40)          # fake vtable ptr; [0x40-0x18]==0 -> delta 0


def h_getline():
    ist, sp = reg(UC_ARM64_REG_X0), reg(UC_ARM64_REG_X1)
    lines = CUR['lines']
    if CUR['i'] >= len(lines):
        st_set(sp, b'')
        setreg(UC_ARM64_REG_X0, ist)
        return
    line = lines[CUR['i']]
    CUR['i'] += 1
    st_set(sp, line)
    setreg(UC_ARM64_REG_X0, ist)


# ---------------- JNI ----------------
JNI_IMPL = {}
JNI_OBJ = {}
_obj_id = [1]


def new_obj(kind, val):
    o = 0x60000000 + _obj_id[0] * 8
    _obj_id[0] += 1
    JNI_OBJ[o] = (kind, val)
    return o


def getj(x):
    return JNI_OBJ.get(x)


def jni(index):
    def deco(fn):
        JNI_IMPL[index] = fn
        return fn
    return deco


@jni(4)   # GetVersion
def _():
    setreg(UC_ARM64_REG_X0, 0x10006)


@jni(6)   # FindClass
def _():
    setreg(UC_ARM64_REG_X0, 0x60000000)


@jni(167)  # NewStringUTF
def _():
    s = cstr(reg(UC_ARM64_REG_X1))
    setreg(UC_ARM64_REG_X0, new_obj('str', s))


@jni(169)  # GetStringUTFChars
def _():
    s = getj(reg(UC_ARM64_REG_X1))[1]
    p = malloc(len(s) + 1)
    M.wr(p, s + b'\0')
    setreg(UC_ARM64_REG_X0, p)


@jni(170)  # ReleaseStringUTFChars
def _():
    pass


@jni(171)  # GetArrayLength
def _():
    setreg(UC_ARM64_REG_X0, len(getj(reg(UC_ARM64_REG_X1))[1]))


@jni(172)  # NewObjectArray
def _():
    n = reg(UC_ARM64_REG_X1)
    setreg(UC_ARM64_REG_X0, new_obj('arr', [None] * n))


@jni(173)  # GetObjectArrayElement
def _():
    arr = getj(reg(UC_ARM64_REG_X1))[1]
    setreg(UC_ARM64_REG_X0, arr[reg(UC_ARM64_REG_X2)] or 0)


@jni(174)  # SetObjectArrayElement
def _():
    arr = getj(reg(UC_ARM64_REG_X1))[1]
    arr[reg(UC_ARM64_REG_X2)] = reg(UC_ARM64_REG_X3)


@jni(176)  # NewByteArray
def _():
    n = reg(UC_ARM64_REG_X1)
    setreg(UC_ARM64_REG_X0, new_obj('bytes', bytearray(n)))


@jni(184)  # GetByteArrayElements
def _():
    b = getj(reg(UC_ARM64_REG_X1))[1]
    p = malloc(len(b))
    M.wr(p, bytes(b))
    setreg(UC_ARM64_REG_X0, p)


@jni(192)  # ReleaseByteArrayElements
def _():
    pass


@jni(208)  # SetByteArrayRegion
def _():
    h, start, ln, buf = (reg(UC_ARM64_REG_X1), reg(UC_ARM64_REG_X2),
                         reg(UC_ARM64_REG_X3), reg(UC_ARM64_REG_X4))
    b = getj(h)[1]
    b[start:start + ln] = M.rd(buf, ln)


@jni(215)  # RegisterNatives
def _():
    setreg(UC_ARM64_REG_X0, 0)


# ---------------- setup ----------------
def setup():
    ensure_map(0, 0x2000)          # TLS / canary at 0x28
    M.w64(0x28, 0)
    ensure_map(STUB, 0x20000)
    ensure_map(STUBDATA, 0x10000)
    ensure_map(JNITAB, 0x4000)
    ensure_map(HEAP, 0x4000000)
    ensure_map(JNIENV, 0x4000)
    ensure_map(SCRATCH, 0x4000)
    ensure_map(STACK, STACKSZ)

    el = load_elf(BNIT, BASE)

    # fake locale/ctype vtable: [loc]=SCRATCH+0x300 ; [SCRATCH+0x300+0x38]=widen stub
    M.w64(SCRATCH + 0x100, SCRATCH + 0x300)   # locale object -> vtable
    M.w64(SCRATCH + 0x300 + 0x38, stub_for('__widen'))
    IMPL['__widen'] = lambda: setreg(UC_ARM64_REG_X0, reg(UC_ARM64_REG_X1))
    M.w64(SCRATCH + 0x200, SCRATCH + 0x380)   # ctype object -> vtable
    M.w64(SCRATCH + 0x380 + 0x38, stub_for('__widen'))

    # relocations
    for sec in el.iter_sections():
        if sec.header.sh_type != 'SHT_RELA':
            continue
        symtab = el.get_section(sec['sh_link'])
        for rel in sec.iter_relocations():
            off = BASE + rel['r_offset']
            typ = rel['r_info_type']
            symidx = rel['r_info_sym']
            add = rel['r_addend']
            if typ == 1027:  # RELATIVE
                M.w64(off, BASE + add)
            elif typ in (1025, 1026, 257):  # GLOB_DAT, JUMP_SLOT, ABS64
                name = symtab.get_symbol(symidx).name
                if typ == 257:
                    M.w64(off, resolve(name) + add)
                else:
                    M.w64(off, resolve(name))
            else:
                pass

    # JNI: table of function pointers to tiny 'ret' stubs
    JNICODE = 0x34000000
    ensure_map(JNICODE, 0x1000)
    M.w64(JNIENV, JNITAB)
    for idx in range(256):
        code = JNICODE + idx * 4
        M.w32(code, 0xd65f03c0)
        M.w64(JNITAB + idx * 8, code)
    # JavaVM
    VM = JNIENV + 0x800
    VMTAB = JNIENV + 0x1000
    VMCODE = 0x35000000
    ensure_map(VMCODE, 0x1000)
    M.w64(VM, VMTAB)
    for i in range(9):
        code = VMCODE + i * 4
        M.w32(code, 0xd65f03c0)
        M.w64(VMTAB + i * 8, code)

    patch(0xbc9c, h_rand)
    patch(0xbdd4, h_open_stream)
    patch(0xbee8, h_getline)
    return el


def code_hook(uc_, address, size, data):
    if address in HANDLERS:
        HANDLERS[address]()
        return
    if STUB <= address < STUB + 0x20000:
        name = _stub_map.get(address)
        fn = IMPL.get(name)
        if fn:
            fn()
        return
    if 0x34000000 <= address < 0x34001000:
        idx = (address - 0x34000000) // 4
        fn = JNI_IMPL.get(idx)
        if fn:
            fn()
        return
    if 0x35000000 <= address < 0x35001000:
        idx = (address - 0x35000000) // 4
        if idx == 6:  # GetEnv
            penv = reg(UC_ARM64_REG_X1)
            M.w64(penv, JNIENV)
            setreg(UC_ARM64_REG_X0, 0)
        else:
            setreg(UC_ARM64_REG_X0, 0)
        return


def reset():
    """Reset mutable VM state between runs."""
    _heap[0] = HEAP
    M.wr(HEAP, b'\0' * 0x100000)
    M.wr(STACK, b'\0' * STACKSZ)
    M.w64(0x28, 0)
    JNI_OBJ.clear()
    _obj_id[0] = 1
    RAND['i'] = 0
    CUR['path'] = None
    CUR['lines'] = []
    CUR['i'] = 0


def run_cdrl(a, b, c, blob, num, rand_seq=None):
    reset()
    if rand_seq:
        RAND['seq'] = rand_seq
    RAND['i'] = 0
    ja, jb, jc = new_obj('str', a), new_obj('str', b), new_obj('str', c)
    jd = new_obj('bytes', bytearray(blob))
    sp = STACK + STACKSZ - 0x1000
    setreg(UC_ARM64_REG_SP, sp)
    setreg(UC_ARM64_REG_LR, SENTINEL)
    setreg(UC_ARM64_REG_PC, BASE + 0x88b4)
    setreg(UC_ARM64_REG_X0, JNIENV)
    setreg(UC_ARM64_REG_X1, 0)
    setreg(UC_ARM64_REG_X2, ja)
    setreg(UC_ARM64_REG_X3, jb)
    setreg(UC_ARM64_REG_X4, jc)
    setreg(UC_ARM64_REG_X5, jd)
    setreg(UC_ARM64_REG_X6, num)
    try:
        uc.emu_start(BASE + 0x88b4, SENTINEL, timeout=60 * 1000000, count=200000000)
    except Exception as e:
        print('EMU ERROR at pc=0x%x: %s' % (reg(UC_ARM64_REG_PC), e))
        raise
    return reg(UC_ARM64_REG_X0)


def dump_result(h):
    kind, arr = getj(h)
    out = []
    for i, e in enumerate(arr):
        k, v = getj(e)
        out.append(bytes(v))
    return out


if __name__ == '__main__':
    setup()
    uc.hook_add(UC_HOOK_CODE, code_hook)
    vfile('/dev/urandom', bytes(range(1, 256)) * 4)
    vfile('/proc/sys/kernel/random/boot_id', b'0123abcd-1111-2222-3333-444455556666\n')
    maps = (b'12c00000-12c01000 r--p 00000000 fd:00 1234 /data/app/~~abc==/com.skiy.lane.app-xyz==/base.apk\n'
            b'70000000-70001000 r-xp 00000000 fd:00 1 /system/lib64/libc.so\n')
    vfile('/proc/self/maps', maps)
    vfile('/data/app/~~abc==/com.skiy.lane.app-xyz==/base.apk', b'PK\x03\x04' + b'APKDATA' * 100)
    r = run_cdrl(b'AAA', b'BBB', b'CCC', b'\x01\x02\x03\x04', 1700000000000)
    print('result handle 0x%x' % r)
    try:
        outs = dump_result(r)
        for i, o in enumerate(outs):
            print(i, len(o), repr(o[:200]))
    except Exception as e:
        print('dump failed:', e)
