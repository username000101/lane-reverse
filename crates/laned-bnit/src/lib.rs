//! Restored logic of `libbnit.so` / `BNITManager.cdrl`.
//!
//! Generates the three request headers (`X-Core-Token`, `X-Request-Trace-Id`,
//! `X-Client-Meta`) plus the small binary payload, from exactly the five
//! arguments the native method takes:
//! `method`, `path`, `query`, `body`, `timestamp`.
//!
//! Dependencies are only `flate2` (gzip for `red` responses) and `getrandom`
//! (device id). SHA-256, HMAC, RC4, custom Base64 and UUID v4 are implemented
//! here. No device snapshot is needed: the `inode` field from the original
//! `/proc/self/maps` scan is arbitrary — see [`DEFAULT_INODE`].

/// 32-byte HMAC/cipher key, assembled by the native code from four fragments:
/// `"SqperSzv" + "bntKmv_C" + "bnngeThi" + "s_12303!"`.
pub const KEY: [u8; 32] = *b"SqperSzvbntKmv_CbnngeThis_12303!";

/// Custom Base64 alphabet; `.` is used as padding instead of `=`.
const ALPHABET: &[u8; 64] =
    b"zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM";

// ---------------------------------------------------------------------------
// SHA-256
// ---------------------------------------------------------------------------

const K: [u32; 64] = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
];

fn sha256(data: &[u8]) -> [u8; 32] {
    let mut h: [u32; 8] = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
    ];

    let mut msg = Vec::with_capacity(data.len() + 72);
    msg.extend_from_slice(data);
    let bit_len = (data.len() as u64).wrapping_mul(8);
    msg.push(0x80);
    while msg.len() % 64 != 56 {
        msg.push(0);
    }
    msg.extend_from_slice(&bit_len.to_be_bytes());

    for chunk in msg.chunks_exact(64) {
        let mut w = [0u32; 64];
        for i in 0..16 {
            w[i] = u32::from_be_bytes([chunk[4 * i], chunk[4 * i + 1], chunk[4 * i + 2], chunk[4 * i + 3]]);
        }
        for i in 16..64 {
            let s0 = w[i - 15].rotate_right(7) ^ w[i - 15].rotate_right(18) ^ (w[i - 15] >> 3);
            let s1 = w[i - 2].rotate_right(17) ^ w[i - 2].rotate_right(19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16]
                .wrapping_add(s0)
                .wrapping_add(w[i - 7])
                .wrapping_add(s1);
        }

        let (mut a, mut b, mut c, mut d) = (h[0], h[1], h[2], h[3]);
        let (mut e, mut f, mut g, mut hh) = (h[4], h[5], h[6], h[7]);

        for i in 0..64 {
            let s1 = e.rotate_right(6) ^ e.rotate_right(11) ^ e.rotate_right(25);
            let ch = (e & f) ^ ((!e) & g);
            let t1 = hh
                .wrapping_add(s1)
                .wrapping_add(ch)
                .wrapping_add(K[i])
                .wrapping_add(w[i]);
            let s0 = a.rotate_right(2) ^ a.rotate_right(13) ^ a.rotate_right(22);
            let maj = (a & b) ^ (a & c) ^ (b & c);
            let t2 = s0.wrapping_add(maj);

            hh = g;
            g = f;
            f = e;
            e = d.wrapping_add(t1);
            d = c;
            c = b;
            b = a;
            a = t1.wrapping_add(t2);
        }

        h[0] = h[0].wrapping_add(a);
        h[1] = h[1].wrapping_add(b);
        h[2] = h[2].wrapping_add(c);
        h[3] = h[3].wrapping_add(d);
        h[4] = h[4].wrapping_add(e);
        h[5] = h[5].wrapping_add(f);
        h[6] = h[6].wrapping_add(g);
        h[7] = h[7].wrapping_add(hh);
    }

    let mut out = [0u8; 32];
    for i in 0..8 {
        out[4 * i..4 * i + 4].copy_from_slice(&h[i].to_be_bytes());
    }
    out
}

pub fn hmac_sha256(msg: &[u8]) -> [u8; 32] {
    let mut ipad = [0x36u8; 64];
    let mut opad = [0x5cu8; 64];
    for i in 0..32 {
        ipad[i] ^= KEY[i];
        opad[i] ^= KEY[i];
    }

    let mut inner = Vec::with_capacity(64 + msg.len());
    inner.extend_from_slice(&ipad);
    inner.extend_from_slice(msg);
    let ih = sha256(&inner);

    let mut outer = Vec::with_capacity(96);
    outer.extend_from_slice(&opad);
    outer.extend_from_slice(&ih);
    sha256(&outer)
}

// ---------------------------------------------------------------------------
// RC4 variant used for the body field (and verifyMagic)
//
// Not plain RC4: right after emitting a byte the original also mutates one
// S-box entry — k = S[(S[i]+S[j])&0xff]; S[(k+i)&0xff] += k. Without it the
// keystream matches only for very short inputs.
// ---------------------------------------------------------------------------

fn rc4(key: &[u8], data: &[u8]) -> Vec<u8> {
    let mut s = [0u8; 256];
    for (i, v) in s.iter_mut().enumerate() {
        *v = i as u8;
    }
    let mut j: usize = 0;
    for i in 0..256 {
        j = (j + s[i] as usize + key[i % key.len()] as usize) & 0xff;
        s.swap(i, j);
    }

    let mut out = Vec::with_capacity(data.len());
    let (mut i, mut j) = (0usize, 0usize);
    for &b in data {
        i = (i + 1) & 0xff;
        j = (j + s[i] as usize) & 0xff;
        s.swap(i, j);
        let k = s[(s[i] as usize + s[j] as usize) & 0xff];
        out.push(k ^ b);
        let idx = k.wrapping_add(i as u8) as usize;
        s[idx] = s[idx].wrapping_add(k);
    }
    out
}

// ---------------------------------------------------------------------------
// Token cipher: RC4-like KSA with two keys, chained/rotated PRGA
// ---------------------------------------------------------------------------

fn token_cipher(key2: &[u8], data: &[u8]) -> Vec<u8> {
    let mut s = [0u8; 256];
    for (i, v) in s.iter_mut().enumerate() {
        *v = i as u8;
    }
    let mut j: usize = 0;
    for i in 0..256 {
        j = (j + s[i] as usize + KEY[i % 32] as usize + key2[i % key2.len()] as usize) & 0xff;
        s.swap(i, j);
    }

    let mut out = Vec::with_capacity(data.len());
    let (mut i, mut j) = (0usize, 0usize);
    let mut carry: u8 = 0x5a;
    for &b in data {
        i = (i + 1) & 0xff;
        j = (j + s[i] as usize) & 0xff;
        s.swap(i, j);
        let t = ((s[i].wrapping_add(s[j])) ^ carry) as usize;
        let v = s[t] ^ b;
        let v = v.rotate_left(3);
        carry = carry.wrapping_add(v);
        out.push(carry);
    }
    out
}

/// Inverse of [`token_cipher`]: recover the token envelope from a header value.
/// `key2` is the first 4 bytes of the base64-decoded value.
pub fn token_decipher(key2: &[u8], ct: &[u8]) -> Vec<u8> {
    let mut s = [0u8; 256];
    for (i, v) in s.iter_mut().enumerate() {
        *v = i as u8;
    }
    let mut j: usize = 0;
    for i in 0..256 {
        j = (j + s[i] as usize + KEY[i % 32] as usize + key2[i % key2.len()] as usize) & 0xff;
        s.swap(i, j);
    }

    let mut out = Vec::with_capacity(ct.len());
    let (mut i, mut j) = (0usize, 0usize);
    let mut carry: u8 = 0x5a;
    for &c in ct {
        i = (i + 1) & 0xff;
        j = (j + s[i] as usize) & 0xff;
        s.swap(i, j);
        let t = ((s[i].wrapping_add(s[j])) ^ carry) as usize;
        let k = s[t];
        let v = c.wrapping_sub(carry);
        out.push(k ^ v.rotate_right(3));
        carry = c;
    }
    out
}

fn hex(bytes: &[u8]) -> String {
    const D: &[u8; 16] = b"0123456789abcdef";
    let mut s = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        s.push(D[(b >> 4) as usize] as char);
        s.push(D[(b & 0x0f) as usize] as char);
    }
    s
}

fn base64_encode(data: &[u8]) -> String {
    let mut out = String::with_capacity((data.len() + 2) / 3 * 4);
    for chunk in data.chunks(3) {
        let n = chunk.len();
        let v = (chunk[0] as u32) << 16
            | if n > 1 { (chunk[1] as u32) << 8 } else { 0 }
            | if n > 2 { chunk[2] as u32 } else { 0 };
        out.push(ALPHABET[((v >> 18) & 0x3f) as usize] as char);
        out.push(ALPHABET[((v >> 12) & 0x3f) as usize] as char);
        out.push(if n > 1 { ALPHABET[((v >> 6) & 0x3f) as usize] as char } else { '.' });
        out.push(if n > 2 { ALPHABET[(v & 0x3f) as usize] as char } else { '.' });
    }
    out
}

/// Decode a custom-Base64 string (`.` padding is ignored).
pub fn base64_decode(s: &str) -> Option<Vec<u8>> {
    let mut bits: u32 = 0;
    let mut n: u32 = 0;
    let mut out = Vec::new();
    for ch in s.bytes() {
        if ch == b'.' || ch == b'=' {
            continue;
        }
        let idx = ALPHABET.iter().position(|&c| c == ch)? as u32;
        bits = (bits << 6) | idx;
        n += 6;
        if n >= 8 {
            n -= 8;
            out.push((bits >> n) as u8);
        }
    }
    Some(out)
}

fn random_bytes<const N: usize>() -> [u8; N] {
    let mut buf = [0u8; N];

    #[cfg(unix)]
    {
        use std::io::Read;
        if let Ok(mut f) = std::fs::File::open("/dev/urandom") {
            if f.read_exact(&mut buf).is_ok() {
                return buf;
            }
        }
    }

    // Fallback: time-seeded xorshift; only used when /dev/urandom is unavailable.
    let mut seed = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos() as u64)
        .unwrap_or(0x9e3779b97f4a7c15)
        | 1;
    for b in buf.iter_mut() {
        seed ^= seed << 13;
        seed ^= seed >> 7;
        seed ^= seed << 17;
        *b = seed as u8;
    }
    buf
}

fn random_u32_15() -> [u32; 15] {
    let raw = random_bytes::<60>();
    let mut out = [0u32; 15];
    for i in 0..15 {
        out[i] = u32::from_le_bytes([raw[4 * i], raw[4 * i + 1], raw[4 * i + 2], raw[4 * i + 3]]);
    }
    out
}

fn make_uuid_v4() -> String {
    let mut b = random_bytes::<16>();
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    let h = hex(&b);
    format!(
        "{}-{}-{}-{}-{}",
        &h[0..8], &h[8..12], &h[12..16], &h[16..20], &h[20..32]
    )
}

fn now_ms() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

/// Default value of the `inode` field.
///
/// The original reads it from `/proc/self/maps` (first `.apk` line under
/// `/data/app/`), but the value travels **inside** the token envelope: the
/// server decrypts it and cannot compare it with anything on the device.
/// So any number works; we keep the one from the reference snapshot for
/// reproducible test vectors.
pub const DEFAULT_INODE: &str = "656421";

// ---------------------------------------------------------------------------
// public API
// ---------------------------------------------------------------------------

/// Knobs that are normally derived automatically; used by tests.
#[derive(Default, Clone)]
pub struct Options {
    pub boot_id: Option<String>,
    pub inode: Option<String>,
    pub now_ms: Option<i64>,
    /// 15 `u32` in call order (4 nonce, 1 cipher key, 4 trace, 6 meta).
    pub randoms: Option<[u32; 15]>,
}

#[derive(Debug, Clone)]
pub struct Output {
    pub core_token: String,   // "X-Core-Token:<base64>"
    pub trace_id: String,     // "X-Request-Trace-Id:<uuid>"
    pub client_meta: String,  // "X-Client-Meta:<base64>"
    pub payload: Vec<u8>,     // RC4(body): the encrypted body to send for POST
}

/// Same five arguments as `BNITManager.cdrl`.
/// Canonical form of the query that goes into `msg1`.
///
/// The server signs the **decoded** query: values (and names) are
/// percent-decoded first, then de-duplicated, sorted by name and joined as
/// `name=value` with `&`.
///
/// So `q=golden%20resort` is signed as `q=golden resort`, and `q=%D0%B3...`
/// as raw UTF-8 text. Only the decoded form matters, not the wire encoding.
pub fn canonical_query(query: &str) -> String {
    let mut seen = std::collections::HashSet::new();
    let mut pairs: Vec<(String, String)> = query
        .split('&')
        .filter(|s| !s.is_empty())
        .map(|kv| match kv.split_once('=') {
            Some((k, v)) => (pct_decode(k), pct_decode(v)),
            None => (pct_decode(kv), String::new()),
        })
        .filter(|(k, _)| seen.insert(k.clone()))
        .collect();
    pairs.sort_by(|a, b| a.0.cmp(&b.0));
    pairs
        .iter()
        .map(|(k, v)| format!("{k}={v}"))
        .collect::<Vec<_>>()
        .join("&")
}

/// Decode `%XX` escapes and `+` (form-encoding semantics) to UTF-8 text.
pub fn pct_decode(s: &str) -> String {
    let b = s.as_bytes();
    let mut out: Vec<u8> = Vec::with_capacity(b.len());
    let mut i = 0;
    while i < b.len() {
        if b[i] == b'%' && i + 2 < b.len() {
            if let (Ok(h), Ok(l)) = (
                u8::from_str_radix(&s[i + 1..i + 2], 16),
                u8::from_str_radix(&s[i + 2..i + 3], 16),
            ) {
                out.push(h * 16 + l);
                i += 3;
                continue;
            }
        }
        if b[i] == b'+' {
            out.push(b' ');
            i += 1;
            continue;
        }
        out.push(b[i]);
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

/// Decrypt a `red` response body (see `X-Core-Red` / `X-Resp-Nonce`).
///
/// `plain = RC4*(KEY32 ‖ nonce, body)`, then gzip when `X-Core-Compressed: 1`.
pub fn decrypt_response(body: &[u8], nonce: &str, compressed: bool) -> Vec<u8> {
    let mut key = Vec::with_capacity(KEY.len() + nonce.len());
    key.extend_from_slice(&KEY);
    key.extend_from_slice(nonce.as_bytes());
    let data = rc4(&key, body);
    if !compressed {
        return data;
    }
    use std::io::Read;
    let mut out = Vec::new();
    match flate2::read::GzDecoder::new(&data[..]).read_to_end(&mut out) {
        Ok(_) => out,
        Err(_) => data,
    }
}

pub fn build(method: &str, path: &str, query: &str, body: &[u8], timestamp: i64) -> Output {
    build_with(method, path, query, body, timestamp, &Options::default())
}

pub fn build_with(
    method: &str,
    path: &str,
    query: &str,
    body: &[u8],
    timestamp: i64,
    opts: &Options,
) -> Output {
    let rnd = opts.randoms.unwrap_or_else(random_u32_15);
    let now = opts.now_ms.unwrap_or_else(now_ms);
    let boot = opts.boot_id.clone().unwrap_or_else(make_uuid_v4);
    let inode = opts
        .inode
        .clone()
        .unwrap_or_else(|| DEFAULT_INODE.to_string());

    // nonce: 4 u32 little-endian, hex encoded
    let mut nonce = [0u8; 16];
    for i in 0..4 {
        nonce[4 * i..4 * i + 4].copy_from_slice(&rnd[i].to_le_bytes());
    }
    let nonce_hex = hex(&nonce);

    let ts = (now + timestamp).to_string();

    // field: RC4(key || nonce || time, body)
    let mut rkey = Vec::with_capacity(32 + nonce_hex.len() + ts.len());
    rkey.extend_from_slice(&KEY);
    rkey.extend_from_slice(nonce_hex.as_bytes());
    rkey.extend_from_slice(ts.as_bytes());
    let payload = rc4(&rkey, body);

    // msg1 + HMAC-SHA256
    let mut msg1 = Vec::new();
    msg1.extend_from_slice(method.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(path.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(query.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(ts.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(nonce_hex.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(&payload);
    msg1.push(b':');
    msg1.extend_from_slice(boot.as_bytes());
    msg1.push(b':');
    msg1.extend_from_slice(inode.as_bytes());
    let digest = hmac_sha256(&msg1);

    // encrypted token body
    let mut plain = hex(&digest).into_bytes();
    plain.push(b'|');
    plain.extend_from_slice(ts.as_bytes());
    plain.push(b'|');
    plain.extend_from_slice(nonce_hex.as_bytes());
    plain.push(b'|');
    plain.extend_from_slice(boot.as_bytes());
    plain.push(b'|');
    plain.extend_from_slice(inode.as_bytes());
    plain.push(b'|');
    plain.push(if body.is_empty() { b'0' } else { b'1' });

    let key2 = rnd[4].to_be_bytes();
    let ct = token_cipher(&key2, &plain);
    let mut blob = Vec::with_capacity(4 + ct.len());
    blob.extend_from_slice(&key2);
    blob.extend_from_slice(&ct);

    // trace id: UUID from 4 u32 little-endian
    let mut u = String::with_capacity(32);
    for i in 0..4 {
        u.push_str(&hex(&rnd[5 + i].to_le_bytes()));
    }
    let trace = format!(
        "X-Request-Trace-Id:{}-{}-{}-{}-{}",
        &u[0..8], &u[8..12], &u[12..16], &u[16..20], &u[20..32]
    );

    // client meta: base64 of 6 u32 little-endian
    let mut meta = Vec::with_capacity(24);
    for i in 0..6 {
        meta.extend_from_slice(&rnd[9 + i].to_le_bytes());
    }

    Output {
        core_token: format!("X-Core-Token:{}", base64_encode(&blob)),
        trace_id: trace,
        client_meta: format!("X-Client-Meta:{}", base64_encode(&meta)),
        payload,
    }
}

pub mod capi;

#[cfg(test)]
mod tests {
    use super::*;

    const SEQ: [u32; 15] = [
        0x01020304, 0x05060708, 0x090a0b0c, 0x0d0e0f10, 0x11121314,
        0x15161718, 0x191a1b1c, 0x1d1e1f20, 0x21222324, 0x25262728,
        0x292a2b2c, 0x2d2e2f30, 0x31323334, 0x35363738, 0x393a3b3c,
    ];

    #[test]
    fn golden_vector_matches_native() {
        let opts = Options {
            boot_id: Some("0123abcd-1111-2222-3333-444455556666".into()),
            inode: Some("4660".into()),
            now_ms: Some(1700000000123),
            randoms: Some(SEQ),
        };
        let out = build_with("ARG_A", "ARG_B", "ARG_C", &[1, 2, 3, 4], 1700000000000, &opts);

        assert_eq!(
            out.core_token,
            "X-Core-Token:bwsrnd7LJ9TcQkA7umSJWKQh5bKrtWhW5FqLkf_O9nRwBCzWn-bP5JjLzSNtJEHwh8RXvE6Me97GTWPWw-cirH1sYxgL_wLhGkXM-lfzI1gCYSFHQplmwtgSjtNi7COj9o_9dWzePefvbPuM6H6_t2Gd0EG3yLh2SJN5luRjtha30vwYm9Ul6r1wOUGa2sEs4LPvwNaHkiWttnR6c2K2"
        );
        assert_eq!(out.trace_id, "X-Request-Trace-Id:18171615-1c1b-1a19-201f-1e1d24232221");
        assert_eq!(out.client_meta, "X-Client-Meta:fc3QdeAYf9-AgDLIjvhDhr7Kj0tVkFEZ");
        assert_eq!(out.payload, vec![0xce, 0xca, 0xbb, 0x93]);
    }

    #[test]
    fn long_body_field_matches_native() {
        // 16-byte body: only the mutated PRGA reproduces the native field.
        let opts = Options {
            boot_id: Some("0123abcd-1111-2222-3333-444455556666".into()),
            inode: Some("4660".into()),
            now_ms: Some(1700000000123),
            randoms: Some(SEQ),
        };
        let body: Vec<u8> = (0u8..16).collect();
        let out = build_with("A", "B", "C", &body, 0, &opts);
        assert_eq!(hex(&out.payload), "6625de9f1b7e72460fc12964636f4ba5");
    }

    #[test]
    fn default_inode_is_used() {
        let opts = Options {
            boot_id: Some("0123abcd-1111-2222-3333-444455556666".into()),
            inode: None,
            now_ms: Some(1783685473123),
            randoms: Some(SEQ),
        };
        let with_default = build_with("GET", "/user/playlists", "", &[], 0, &opts);
        let opts2 = Options {
            inode: Some(DEFAULT_INODE.to_string()),
            ..opts
        };
        let explicit = build_with("GET", "/user/playlists", "", &[], 0, &opts2);
        assert_eq!(with_default.core_token, explicit.core_token);
    }

    #[test]
    fn known_token_matches_cpp() {
        let opts = Options {
            boot_id: Some("0123abcd-1111-2222-3333-444455556666".into()),
            inode: None,                       // DEFAULT_INODE
            now_ms: Some(1783685473123),
            randoms: Some(SEQ),
        };
        let out = build_with("GET", "/user/playlists", "", &[], 0, &opts);
        assert_eq!(
            out.core_token,
            "X-Core-Token:bwsrnzpeTip2xfBL7IH2StRDgAFzOk2betIB3y2dXSu7nhbwUC9QUfzXBKVU78kNkNVz5HGhr70TvFJy3RdUl6FUQSPO9ZYdDIQudx_jAUsCYyROOlvrBePXCwhr-vcsrpJRZek5AvWDx_Y7hal6n8-MLsPLotpb7fA90UoFqcLoXVxJYayLQQPEObqwXGCELoE8PoqTvAKFndS_AVw56RG."
        );
    }

    #[test]
    fn default_call_shape() {
        let out = build("GET", "/user/playlists", "", &[], 0);
        // 13 = len("X-Core-Token:")
        assert_eq!(out.core_token.len() - 13, 216);
        assert!(out.core_token.ends_with('.'));
        assert_eq!(out.client_meta.len() - 14, 32);
        assert!(out.trace_id.starts_with("X-Request-Trace-Id:"));
        assert!(out.payload.is_empty());
    }
}

#[cfg(test)]
mod canonical_tests {
    use super::*;

    #[test]
    fn query_is_sorted_and_deduplicated() {
        assert_eq!(canonical_query(""), "");
        assert_eq!(canonical_query("prefetch=0"), "prefetch=0");
        assert_eq!(
            canonical_query("trackId=A&refId=B&streamQuality=C"),
            "refId=B&streamQuality=C&trackId=A"
        );
        assert_eq!(canonical_query("b=2&a=1"), "a=1&b=2");
        assert_eq!(canonical_query("a=1&a=2"), "a=1");
    }

    #[test]
    fn canonical_query_matches_known_good_dump() {
        // playlist@lane_likes@tracks.txt: page/pageSize already sorted
        assert_eq!(
            canonical_query("page=1&pageSize=50"),
            "page=1&pageSize=50"
        );
    }

    #[test]
    fn red_response_roundtrip() {
        let nonce = "abc123";
        let plain = br#"{"url":"https://cdn.example/x.mp3","ttl":90000}"#;
        let mut key = Vec::new();
        key.extend_from_slice(&KEY);
        key.extend_from_slice(nonce.as_bytes());
        let ct = rc4(&key, plain);
        assert_ne!(ct, plain);
        assert_eq!(decrypt_response(&ct, nonce, false), plain);
    }
}

/// Generate a device id suitable for `LDI` and the Telegram login link.
///
/// Alphabet is `A-Za-z0-9_-` (what Telegram allows in a `start` parameter),
/// and the result is uniform: 64 symbols, 256 % 64 == 0.
///
/// Keep it <= 60 chars: the link is `?start=auth<id>`, and Telegram caps the
/// whole parameter at 64 characters.
pub fn random_device_id(len: usize) -> String {
    const ALPHABET: &[u8; 64] =
        b"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";
    let len = len.clamp(16, 60);
    let mut out = String::with_capacity(len);
    let mut buf = [0u8; 32];
    while out.len() < len {
        if getrandom::getrandom(&mut buf).is_err() {
            buf = random_bytes::<32>();
        }
        for b in buf {
            out.push(ALPHABET[(b & 0x3f) as usize] as char);
            if out.len() == len {
                break;
            }
        }
    }
    out
}

#[cfg(test)]
mod device_id_tests {
    use super::*;

    #[test]
    fn device_id_is_urlsafe_and_bounded() {
        for len in [16, 32, 60, 100] {
            let id = random_device_id(len);
            let want = len.clamp(16, 60);
            assert_eq!(id.len(), want);
            assert!(id.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-'));
        }
        assert_ne!(random_device_id(32), random_device_id(32));
    }
}

/// Random 6-digit number for the unverifiable `inode` field.
pub fn random_inode() -> String {
    let mut b = [0u8; 4];
    if getrandom::getrandom(&mut b).is_err() {
        b = random_bytes::<4>();
    }
    let n = u32::from_le_bytes(b) % 900_000 + 100_000;
    n.to_string()
}

#[cfg(test)]
mod decode_tests {
    use super::*;

    #[test]
    fn query_is_decoded_before_signing() {
        assert_eq!(canonical_query("q=test%20drive"), "q=test drive");
        assert_eq!(canonical_query("q=test+drive"), "q=test drive");
        assert_eq!(canonical_query("q=%D0%B3%D0%BE%D0%BB"), "q=гол");
        assert_eq!(
            canonical_query("platform=all&q=test%20drive&ver=1"),
            "platform=all&q=test drive&ver=1"
        );
        // sorted after decoding, names de-duplicated
        assert_eq!(canonical_query("b=2&a=1&a=3"), "a=1&b=2");
    }

    #[test]
    fn encoded_ampersand_does_not_split() {
        assert_eq!(canonical_query("q=a%26b"), "q=a&b");
    }
}
