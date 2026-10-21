//! C API for the `bnit` crate.
//!
//! All strings are UTF-8 and NUL-terminated. Everything returned by
//! [`bnit_build`] is owned by the library and must be released with
//! [`bnit_headers_free`].

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int};
use std::panic;
use std::ptr;
use std::slice;

use crate::{build, Output};

/// Result of [`bnit_build`]. `body` is the encrypted request body to send.
#[repr(C)]
pub struct BnitHeaders {
    /// `X-Core-Token:<value>` — pass the part after ':' as the header value.
    pub core_token: *mut c_char,
    /// `X-Request-Trace-Id:<uuid>`
    pub trace_id: *mut c_char,
    /// `X-Client-Meta:<base64>`
    pub client_meta: *mut c_char,
    /// Encrypted body (POST); may be NULL when empty.
    pub body: *mut u8,
    pub body_len: usize,
}

impl Default for BnitHeaders {
    fn default() -> Self {
        BnitHeaders {
            core_token: ptr::null_mut(),
            trace_id: ptr::null_mut(),
            client_meta: ptr::null_mut(),
            body: ptr::null_mut(),
            body_len: 0,
        }
    }
}

fn into_c_string(s: String) -> *mut c_char {
    match CString::new(s) {
        Ok(c) => c.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

unsafe fn as_str<'a>(p: *const c_char) -> Option<&'a str> {
    if p.is_null() {
        return None;
    }
    CStr::from_ptr(p).to_str().ok()
}

fn fill(out: Output, dst: &mut BnitHeaders) {
    dst.core_token = into_c_string(out.core_token);
    dst.trace_id = into_c_string(out.trace_id);
    dst.client_meta = into_c_string(out.client_meta);
    if out.payload.is_empty() {
        dst.body = ptr::null_mut();
        dst.body_len = 0;
    } else {
        let mut v = out.payload;
        v.shrink_to_fit();
        dst.body_len = v.len();
        dst.body = v.as_mut_ptr();
        std::mem::forget(v);
    }
}

/// Build the signature headers and the encrypted body.
///
/// * `method`, `path`, `query` — UTF-8 strings (query without '?').
/// * `body` / `body_len` — plaintext request body (NULL/0 for GET).
/// * `timestamp` — offset added to the current time in ms; 0 means "now".
/// * `out` — caller-allocated struct, filled on success.
///
/// Returns 0 on success, -1 on invalid arguments, -2 on internal panic.
#[no_mangle]
pub unsafe extern "C" fn bnit_build(
    method: *const c_char,
    path: *const c_char,
    query: *const c_char,
    body: *const u8,
    body_len: usize,
    timestamp: i64,
    out: *mut BnitHeaders,
) -> c_int {
    if out.is_null() {
        return -1;
    }
    let (method, path, query) = match (as_str(method), as_str(path), as_str(query)) {
        (Some(m), Some(p), Some(q)) => (m, p, q),
        _ => return -1,
    };
    let body: &[u8] = if body.is_null() || body_len == 0 {
        &[]
    } else {
        slice::from_raw_parts(body, body_len)
    };

    let result = panic::catch_unwind(|| build(method, path, query, body, timestamp));
    match result {
        Ok(r) => {
            let dst = &mut *out;
            *dst = BnitHeaders::default();
            fill(r, dst);
            0
        }
        Err(_) => -2,
    }
}

/// Release everything allocated by [`bnit_build`].
#[no_mangle]
pub unsafe extern "C" fn bnit_headers_free(h: *mut BnitHeaders) {
    if h.is_null() {
        return;
    }
    let h = &mut *h;
    for p in [h.core_token, h.trace_id, h.client_meta] {
        if !p.is_null() {
            drop(CString::from_raw(p));
        }
    }
    if !h.body.is_null() {
        drop(Vec::from_raw_parts(h.body, h.body_len, h.body_len));
    }
    *h = BnitHeaders::default();
}

/// Crate version, static storage.
#[no_mangle]
pub extern "C" fn bnit_version() -> *const c_char {
    concat!(env!("CARGO_PKG_VERSION"), "\0").as_ptr() as *const c_char
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;

    #[test]
    fn c_api_roundtrip() {
        let m = CString::new("GET").unwrap();
        let p = CString::new("/user/playlists").unwrap();
        let q = CString::new("").unwrap();
        let mut h = BnitHeaders::default();
        let rc = unsafe {
            bnit_build(m.as_ptr(), p.as_ptr(), q.as_ptr(), ptr::null(), 0, 0, &mut h)
        };
        assert_eq!(rc, 0);
        unsafe {
            assert!(!h.core_token.is_null());
            assert!(CStr::from_ptr(h.core_token).to_str().unwrap().starts_with("X-Core-Token:"));
            assert_eq!(h.body_len, 0);
            bnit_headers_free(&mut h);
            assert!(h.core_token.is_null());
        }
    }

    #[test]
    fn c_api_rejects_null_args() {
        let mut h = BnitHeaders::default();
        let rc = unsafe { bnit_build(ptr::null(), ptr::null(), ptr::null(), ptr::null(), 0, 0, &mut h) };
        assert_eq!(rc, -1);
    }
}
