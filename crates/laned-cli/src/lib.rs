//! Minimal signed HTTP client for the Lane API.
//!
//! Every request is signed with [`laned_bnit::build`]. Two details matter:
//! * the signed query is canonicalised (unique names, sorted, `name=value`);
//! * for POST the encrypted body returned by the signer is what goes on the wire.
//! Responses with `X-Core-Red: 1` are decrypted with `X-Resp-Nonce`.

use laned_bnit::{build, canonical_query, decrypt_response};
use reqwest::blocking::{Client, Response};
use reqwest::header::HeaderMap;
use reqwest::{Method, StatusCode};
use url::Url;

pub const DEFAULT_BASE: &str = "https://ru.laneapi.com";

pub struct LaneClient {
    http: Client,
    base: String,
    jwt: String,
    ldi: String,
    accept_red: String,
    inode: Option<String>,
}

pub struct LaneResponse {
    pub status: StatusCode,
    pub body: Vec<u8>,
    pub headers: HeaderMap,
}

impl LaneResponse {
    pub fn text(&self) -> String {
        String::from_utf8_lossy(&self.body).to_string()
    }

    pub fn is_success(&self) -> bool {
        self.status.is_success()
    }
}

impl LaneClient {
    pub fn new(jwt: impl Into<String>, ldi: impl Into<String>) -> Self {
        LaneClient {
            http: Client::new(),
            base: DEFAULT_BASE.to_string(),
            jwt: jwt.into(),
            ldi: ldi.into(),
            accept_red: "1".to_string(),
            inode: None,
        }
    }

    pub fn with_base(mut self, base: impl Into<String>) -> Self {
        self.base = base.into();
        self
    }

    pub fn with_accept_red(mut self, v: &str) -> Self {
        self.accept_red = v.to_string();
        self
    }

    /// Override the (unverifiable) inode field; ignored when LANED_INODE is set.
    pub fn with_inode(mut self, v: Option<String>) -> Self {
        self.inode = v;
        self
    }

    /// Signed GET. `query` is the raw string after '?' (empty for none).
    pub fn get(&self, path: &str, query: &str) -> Result<LaneResponse, String> {
        self.send("GET", path, query, &[])
    }

    /// Signed POST. `body` is the *plaintext* body; the encrypted bytes are sent.
    pub fn post(&self, path: &str, query: &str, body: &[u8]) -> Result<LaneResponse, String> {
        self.send("POST", path, query, body)
    }

    pub fn send(
        &self,
        method: &str,
        path: &str,
        query: &str,
        body: &[u8],
    ) -> Result<LaneResponse, String> {
        // sign the decoded form, but send the query exactly as given
        let signed = canonical_query(query);
        self.send_with_sign(method, path, query, &signed, body)
    }

    /// Send `wire_query` but sign `sign_query` (they may differ on purpose).
    pub fn send_with_sign(
        &self,
        method: &str,
        path: &str,
        wire_query: &str,
        sign_query: &str,
        body: &[u8],
    ) -> Result<LaneResponse, String> {
        let mut url = Url::parse(&format!("{}{}", self.base, path))
            .map_err(|e| format!("bad url: {e}"))?;
        if !wire_query.is_empty() {
            url.set_query(Some(wire_query));
        }

        let signed_path = url.path().to_string();
        // LANED_INODE lets you check whether the server cares about the
        // inode field taken from /proc/self/maps.
        let inode = std::env::var("LANED_INODE").ok().filter(|v| !v.is_empty()).or_else(|| self.inode.clone());
        let out = match inode {
            Some(v) => {
                let mut opts = laned_bnit::Options::default();
                opts.inode = Some(v);
                laned_bnit::build_with(method, &signed_path, sign_query, body, 0, &opts)
            }
            None => build(method, &signed_path, sign_query, body, 0),
        };

        let m = Method::from_bytes(method.as_bytes()).map_err(|e| e.to_string())?;
        if std::env::var("LANED_DEBUG").is_ok() {
            eprintln!("[debug] {method} {url}");
            eprintln!("[debug] signed path={signed_path:?} query={sign_query:?}");
            eprintln!("[debug] token={}", strip(&out.core_token, "X-Core-Token:"));
            eprintln!("[debug] ldi={:?} accept-red={:?}", self.ldi, self.accept_red);
            eprintln!("[debug] body {} bytes", out.payload.len());
        }

        let mut req = self.http.request(m, url);
        if !self.jwt.is_empty() {
            req = req.header("authorization", format!("Bearer {}", self.jwt));
        }
        let mut req = req
            .header("x-core-token", strip(&out.core_token, "X-Core-Token:"))
            .header("x-request-trace-id", strip(&out.trace_id, "X-Request-Trace-Id:"))
            .header("x-client-meta", strip(&out.client_meta, "X-Client-Meta:"))
            .header("ldi", &self.ldi)
            .header("x-accept-red", &self.accept_red)
            .header("x-app-version", "207")
            .header("x-platform", "android")
            .header("x-theme", "dark")
            .header("accept-language", "ru")
            .header("tz", "Europe/Moscow")
            .header("user-agent", "LaneMusic/1.0 (Android; Mobile)");

        if !out.payload.is_empty() {
            req = req
                .header("content-type", "application/json; charset=UTF-8")
                .body(out.payload);
        }

        let resp: Response = req.send().map_err(|e| format!("request failed: {e}"))?;
        let status = resp.status();
        let headers = resp.headers().clone();
        let raw = resp.bytes().map_err(|e| e.to_string())?.to_vec();

        let red = header(&headers, "x-core-red") == Some("1");
        let compressed = header(&headers, "x-core-compressed") == Some("1");
        let nonce = header(&headers, "x-resp-nonce").unwrap_or("").to_string();
        let body = if red && !nonce.is_empty() {
            decrypt_response(&raw, &nonce, compressed)
        } else {
            raw
        };

        Ok(LaneResponse {
            status,
            body,
            headers,
        })
    }

    /// Resolve a playable URL for a track.
    pub fn stream(
        &self,
        track_id: &str,
        ref_id: &str,
        quality: &str,
    ) -> Result<serde_json::Value, String> {
        let q = format!("trackId={track_id}&refId={ref_id}&streamQuality={quality}");
        let resp = self.get("/track/stream", &q)?;
        if !resp.is_success() {
            return Err(format!(
                "HTTP {} ({} bytes): {}",
                resp.status,
                resp.body.len(),
                resp.text()
            ));
        }
        serde_json::from_str(&resp.text()).map_err(|e| {
            let ct = header(&resp.headers, "content-type").unwrap_or("?");
            let red = header(&resp.headers, "x-core-red").unwrap_or("-");
            let nonce = header(&resp.headers, "x-resp-nonce").unwrap_or("-");
            let gz = header(&resp.headers, "x-core-compressed").unwrap_or("-");
            let head_hex: String = resp
                .body
                .iter()
                .take(16)
                .map(|b| format!("{b:02x}"))
                .collect::<Vec<_>>()
                .join(" ");
            format!(
                "bad json: {e}\n  status={} ct={ct} red={red} gzip={gz} nonce={nonce} len={} head={head_hex}",
                resp.status,
                resp.body.len()
            )
        })
    }
}

fn strip<'a>(s: &'a str, prefix: &str) -> &'a str {
    s.strip_prefix(prefix).unwrap_or(s)
}

fn header<'a>(headers: &'a HeaderMap, name: &str) -> Option<&'a str> {
    headers.get(name).and_then(|v| v.to_str().ok())
}

// ---------------------------------------------------------------------------
// playback helpers
// ---------------------------------------------------------------------------

use std::io::{Read, Write};
use std::path::Path;
use std::process::Command;

fn have(prog: &str) -> bool {
    Command::new(prog)
        .arg("--version")
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null())
        .status()
        .map(|s| s.success())
        .unwrap_or(false)
}

/// Download a resolved stream URL to `path`, printing progress to stderr.
pub fn download_url(url: &str, path: &Path) -> Result<u64, String> {
    let client = Client::new();
    let mut resp = client.get(url).send().map_err(|e| format!("request failed: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!("HTTP {} for {url}", resp.status()));
    }
    let total = resp.content_length();
    let mut file = std::fs::File::create(path).map_err(|e| format!("cannot create {}: {e}", path.display()))?;
    let mut buf = [0u8; 64 * 1024];
    let mut done: u64 = 0;
    loop {
        let n = resp.read(&mut buf).map_err(|e| format!("read failed: {e}"))?;
        if n == 0 {
            break;
        }
        file.write_all(&buf[..n]).map_err(|e| format!("write failed: {e}"))?;
        done += n as u64;
        match total {
            Some(t) => eprint!("\r  {done} / {t} bytes"),
            None => eprint!("\r  {done} bytes"),
        }
    }
    eprintln!();
    Ok(done)
}

/// Play a resolved stream URL.
///
/// Order: `LANED_PLAYER` env → mpv/ffplay/vlc → download + system default handler.
pub fn play_url(url: &str) -> Result<(), String> {
    if let Ok(player) = std::env::var("LANED_PLAYER") {
        if !player.trim().is_empty() {
            let status = Command::new(&player)
                .arg(url)
                .status()
                .map_err(|e| format!("cannot start {player}: {e}"))?;
            return if status.success() {
                Ok(())
            } else {
                Err(format!("{player} exited with {status}"))
            };
        }
    }

    let candidates: [(&str, &[&str]); 3] = [
        ("mpv", &["--no-video", "--really-quiet"]),
        ("ffplay", &["-nodisp", "-autoexit", "-loglevel", "quiet"]),
        ("vlc", &["--play-and-exit", "--intf", "dummy"]),
    ];
    for (prog, args) in candidates {
        if !have(prog) {
            continue;
        }
        eprintln!("playing with {prog}...");
        let status = Command::new(prog)
            .args(args)
            .arg(url)
            .status()
            .map_err(|e| format!("cannot start {prog}: {e}"))?;
        return if status.success() {
            Ok(())
        } else {
            Err(format!("{prog} exited with {status}"))
        };
    }

    // no CLI player: fetch the file and hand it to the OS
    let ext = url
        .split(['/', '?', '#'])
        .next_back()
        .and_then(|seg| seg.rsplit_once('.').map(|(_, e)| e.to_string()))
        .filter(|e| e.len() <= 4 && e.chars().all(|c| c.is_ascii_alphanumeric()))
        .unwrap_or_else(|| "audio".to_string());
    let tmp = std::env::temp_dir().join(format!("laned-{}.{}", std::process::id(), ext));
    eprintln!("no mpv/ffplay/vlc found, downloading to {} ...", tmp.display());
    download_url(url, &tmp)?;

    #[cfg(windows)]
    let status = Command::new("cmd")
        .args(["/C", "start", ""])
        .arg(&tmp)
        .status();
    #[cfg(target_os = "macos")]
    let status = Command::new("open").arg(&tmp).status();
    #[cfg(all(unix, not(target_os = "macos")))]
    let status = Command::new("xdg-open").arg(&tmp).status();

    let status = status.map_err(|e| format!("cannot open {}: {e}", tmp.display()))?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("system handler exited with {status}; file is at {}", tmp.display()))
    }
}
