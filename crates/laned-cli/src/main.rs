//! laned — CLI for the restored Lane request signing.
//!
//!   laned gen --method GET --path /user/playlists
//!   laned dump http-request-dumps/user@playlists.txt
//!   laned get /user/playlists
//!   laned post /user/tracks prefetch=0 --body '{"trackIds":["…"]}'
//!   laned stream 2lEbe9evbwJ1unoQAb0uPk lane_likes BASIC
//!   laned playlists
//!   laned tracks lane_likes 1 20

use std::env;
use std::fs;
use std::process::exit;

use laned_bnit::{base64_decode, build, canonical_query, hmac_sha256, token_decipher};
use laned_client::LaneClient;

fn usage() -> ! {
    eprintln!(
        "usage:\n  \
         laned gen --method M --path /p [--query q] [--body s] [--timestamp ms]\n            \
         [--body-out FILE] [--json]\n  \
         laned dump FILE\n  \
         laned get PATH [QUERY]\n  \
         laned post PATH [QUERY] --body TEXT\n  \
         laned stream SONG_ID [REF_ID] [BASIC|HIGH|ULTRA]\n  \
         laned play SONG_ID [REF_ID] [QUALITY]\n  \
         laned download SONG_ID [REF_ID] [QUALITY] [--out FILE]\n  \
         laned probe-stream SONG_ID [PLAYLIST_ID]\n  \
         laned probe-sign PATH WIRE_QUERY\n  \
         laned auth-new [--len N] [--id STRING]\n  \
         laned auth-url [DEVICE_ID] [--id STRING]\n  \
         laned auth-wait [DEVICE_ID] [--id STRING]\n  \
         laned playlists\n  \
         laned tracks PLAYLIST_ID [PAGE] [PAGE_SIZE]\n\
         \n\
         env: LANED_JWT (bearer), LANED_LDI (device id), LANED_DEBUG=1"
    );
    exit(1)
}

fn main() {
    let args: Vec<String> = env::args().skip(1).collect();
    match args.first().map(String::as_str) {
        Some("gen") => cmd_gen(&args[1..]),
        Some("dump") => cmd_dump(&args[1..]),
        Some("get") => cmd_get(&args[1..]),
        Some("post") => cmd_post(&args[1..]),
        Some("stream") => cmd_stream(&args[1..]),
        Some("play") => cmd_play(&args[1..]),
        Some("download") => cmd_download(&args[1..]),
        Some("probe-stream") => cmd_probe_stream(&args[1..]),
        Some("probe-sign") => cmd_probe_sign(&args[1..]),
        Some("auth-new") => cmd_auth_new(&args[1..]),
        Some("auth-url") => cmd_auth_url(&args[1..]),
        Some("auth-wait") => cmd_auth_wait(&args[1..]),
        Some("playlists") => cmd_simple("/user/playlists", ""),
        Some("tracks") => cmd_tracks(&args[1..]),
        _ => usage(),
    }
}

fn flag<'a>(args: &'a [String], name: &str) -> Option<&'a str> {
    args.iter()
        .position(|a| a == name)
        .and_then(|i| args.get(i + 1))
        .map(String::as_str)
}

fn has(args: &[String], name: &str) -> bool {
    args.iter().any(|a| a == name)
}

fn config_path() -> String {
    env::var("LANED_CONFIG").unwrap_or_else(|_| "laned.json".to_string())
}

fn load_cfg(path: &str) -> serde_json::Value {
    fs::read_to_string(path)
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_else(|| serde_json::json!({}))
}

fn save_cfg(path: &str, v: &serde_json::Value) {
    match serde_json::to_string_pretty(v) {
        Ok(text) => {
            if let Err(e) = fs::write(path, text) {
                eprintln!("cannot write {path}: {e}");
            }
        }
        Err(e) => eprintln!("cannot serialise config: {e}"),
    }
}

fn client() -> LaneClient {
    let cfg = load_cfg(&config_path());
    let jwt = env::var("LANED_JWT")
        .ok()
        .or_else(|| cfg.get("token").and_then(|v| v.as_str()).map(str::to_string))
        .unwrap_or_default();
    let ldi = env::var("LANED_LDI")
        .ok()
        .or_else(|| cfg.get("deviceId").and_then(|v| v.as_str()).map(str::to_string))
        .unwrap_or_default();
    if jwt.is_empty() {
        eprintln!("warning: no token — run `laned auth-wait` or set LANED_JWT");
    }
    LaneClient::new(jwt, ldi)
}

fn print_json(text: &str) {
    match serde_json::from_str::<serde_json::Value>(text) {
        Ok(v) => println!("{}", serde_json::to_string_pretty(&v).unwrap_or(text.to_string())),
        Err(_) => println!("{text}"),
    }
}

fn cmd_simple(path: &str, query: &str) {
    let c = client();
    match c.get(path, query) {
        Ok(resp) => {
            let status = resp.status;
            let text = resp.text();
            if !status.is_success() {
                eprintln!("HTTP {status}");
            }
            print_json(&text);
        }
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    }
}

fn cmd_get(args: &[String]) {
    let path = args.first().unwrap_or_else(|| usage());
    let query = args.get(1).map(String::as_str).unwrap_or("");
    cmd_simple(path, query);
}

fn cmd_post(args: &[String]) {
    let path = args.first().unwrap_or_else(|| usage());
    let query = args.get(1).map(String::as_str).unwrap_or("");
    let body = flag(args, "--body").unwrap_or("");

    let c = client();
    match c.post(path, query, body.as_bytes()) {
        Ok(resp) => {
            let status = resp.status;
            let text = resp.text();
            if !status.is_success() {
                eprintln!("HTTP {status}");
            }
            print_json(&text);
        }
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    }
}

fn cmd_stream(args: &[String]) {
    let id = args.first().unwrap_or_else(|| usage());
    let ref_id = args.get(1).map(String::as_str).unwrap_or("");
    let quality = args.get(2).map(String::as_str).unwrap_or("BASIC");

    let c = client();
    match c.stream(id, ref_id, quality) {
        Ok(v) => println!("{}", serde_json::to_string_pretty(&v).unwrap()),
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    }
}

/// Warnings for a hand-written device id (we do not reject it on purpose —
/// experimenting with the server's tolerance is a valid use case).
fn device_id_warnings(id: &str) -> Vec<String> {
    let mut w = Vec::new();
    if id.is_empty() {
        w.push("empty id".to_string());
    }
    if id.len() > 60 {
        w.push(format!(
            "{} chars: Telegram cuts `start` at 64, and `auth` takes 4",
            id.len()
        ));
    }
    if !id.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-') {
        w.push("characters outside A-Za-z0-9_- may break the Telegram link".to_string());
    }
    w
}

/// Pick a device id: `--id`, then positional, then the config file.
fn pick_device_id(args: &[String]) -> Option<String> {
    flag(args, "--id")
        .map(str::to_string)
        .or_else(|| args.first().filter(|a| !a.starts_with("--")).cloned())
}

/// Generate a fresh device id (LDI + login link) and remember it.
fn cmd_auth_new(args: &[String]) {
    let len: usize = flag(args, "--len").and_then(|s| s.parse().ok()).unwrap_or(32);
    let path = config_path();
    let id = match flag(args, "--id") {
        Some(custom) if !custom.is_empty() => {
            for w in device_id_warnings(custom) {
                eprintln!("warning: {w}");
            }
            custom.to_string()
        }
        _ => laned_bnit::random_device_id(len),
    };
    let mut cfg = load_cfg(&path);
    let inode = laned_bnit::random_inode();
    cfg["deviceId"] = serde_json::Value::String(id.clone());
    cfg["inode"] = serde_json::Value::String(inode.clone());
    cfg["token"] = serde_json::Value::Null;
    cfg["user"] = serde_json::Value::Null;
    save_cfg(&path, &cfg);
    println!("deviceId: {id}");
    println!("inode   : {inode}");
    println!("saved to : {path}");
    println!("login    : https://t.me/lane_music_bot?start=auth{id}");
}

/// Print the Telegram bot deep link the app opens for login.
fn cmd_auth_url(args: &[String]) {
    let path = config_path();
    let id = pick_device_id(args)
        .or_else(|| load_cfg(&path).get("deviceId").and_then(|v| v.as_str()).map(str::to_string))
        .unwrap_or_else(|| {
            eprintln!("no device id: pass one or run `laned auth-new` (config: {path})");
            exit(1);
        });
    println!("https://t.me/lane_music_bot?start=auth{id}");
}

/// Poll `GET /auth/{id}` until the bot confirms the login, then store the token.
fn cmd_auth_wait(args: &[String]) {
    let path = config_path();
    let mut cfg = load_cfg(&path);
    let id = pick_device_id(args)
        .or_else(|| cfg.get("deviceId").and_then(|v| v.as_str()).map(str::to_string))
        .unwrap_or_else(|| {
            eprintln!("no device id: pass one or run `laned auth-new` (config: {path})");
            exit(1);
        });

    println!("open in Telegram: https://t.me/lane_music_bot?start=auth{id}");
    println!("polling /auth/{id} (3s x 20)...");

    // LDI must be the same value the bot was given
    let c = LaneClient::new("", id.clone());
    for attempt in 1..=20 {
        match c.get(&format!("/auth/{id}"), "") {
            Ok(resp) if resp.is_success() => {
                let text = resp.text();
                if let Ok(v) = serde_json::from_str::<serde_json::Value>(&text) {
                    if let Some(token) = v.get("token").and_then(|x| x.as_str()) {
                        println!("\ntoken: {token}");
                        println!(
                            "isFirstAuth: {}",
                            v.get("isFirstAuth").and_then(|x| x.as_bool()).unwrap_or(false)
                        );
                        if let Some(user) = v.get("user") {
                            println!(
                                "user:\n{}",
                                serde_json::to_string_pretty(user).unwrap_or_default()
                            );
                        }
                        cfg["deviceId"] = serde_json::Value::String(id.clone());
                        if cfg.get("inode").and_then(|v| v.as_str()).is_none() {
                            // arbitrary, but part of the stored session
                            cfg["inode"] =
                                serde_json::Value::String(laned_bnit::random_inode());
                        }
                        cfg["token"] = serde_json::Value::String(token.to_string());
                        cfg["user"] = v.get("user").cloned().unwrap_or(serde_json::Value::Null);
                        save_cfg(&path, &cfg);
                        println!("\nsaved to {path}");
                        return;
                    }
                }
                eprintln!("[{attempt}/20] 200 but unexpected body: {text}");
            }
            Ok(resp) => eprintln!("[{attempt}/20] {}", resp.status),
            Err(e) => eprintln!("[{attempt}/20] {e}"),
        }
        std::thread::sleep(std::time::Duration::from_secs(3));
    }
    eprintln!("no token after 60s");
    exit(1);
}

/// Resolve a stream and play it (mpv/ffplay/vlc, or the system handler).
fn cmd_play(args: &[String]) {
    let id = args.first().unwrap_or_else(|| usage());
    let ref_id = args.get(1).map(String::as_str).unwrap_or("");
    let quality = args.get(2).map(String::as_str).unwrap_or("BASIC");

    let c = client();
    let v = match c.stream(id, ref_id, quality) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    };
    let url = v.get("url").and_then(|x| x.as_str()).unwrap_or("");
    if url.is_empty() {
        eprintln!("no url in response: {v}");
        exit(1);
    }
    println!("url : {url}");
    println!("ttl : {}", v.get("ttl").and_then(|x| x.as_i64()).unwrap_or(0));

    if let Err(e) = laned_client::play_url(url) {
        eprintln!("{e}");
        exit(1);
    }
}

/// Resolve a stream and save it to a file.
fn cmd_download(args: &[String]) {
    let id = args.first().unwrap_or_else(|| usage());
    let ref_id = args.get(1).map(String::as_str).unwrap_or("");
    let quality = args.get(2).map(String::as_str).unwrap_or("BASIC");
    let out = flag(args, "--out")
        .map(str::to_string)
        .unwrap_or_else(|| format!("{id}.audio"));

    let c = client();
    let v = match c.stream(id, ref_id, quality) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    };
    let url = v.get("url").and_then(|x| x.as_str()).unwrap_or("");
    if url.is_empty() {
        eprintln!("no url in response: {v}");
        exit(1);
    }
    println!("url : {url}");
    match laned_client::download_url(url, std::path::Path::new(&out)) {
        Ok(n) => println!("saved {n} bytes to {out}"),
        Err(e) => {
            eprintln!("{e}");
            exit(1);
        }
    }
}

/// Percent-decode a query component (`+` stays a literal plus).
fn pct_decode(s: &str) -> String {
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
        out.push(b[i]);
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

/// Candidate ways the server may canonicalise the query before signing.
fn query_variants(wire: &str) -> Vec<(&'static str, String, String)> {
    let mut seen = std::collections::HashSet::new();
    let pairs: Vec<(String, String)> = wire
        .split('&')
        .filter(|s| !s.is_empty())
        .map(|kv| match kv.split_once('=') {
            Some((k, v)) => (k.to_string(), v.to_string()),
            None => (kv.to_string(), String::new()),
        })
        .filter(|(k, _)| seen.insert(k.clone()))
        .collect();

    let join = |v: &[(String, String)]| {
        v.iter()
            .map(|(k, val)| format!("{k}={val}"))
            .collect::<Vec<_>>()
            .join("&")
    };
    let mut sorted = pairs.clone();
    sorted.sort_by(|a, b| a.0.cmp(&b.0));

    let names_raw = pairs.iter().map(|(k, _)| k.clone()).collect::<Vec<_>>().join("&");
    let names_sorted = sorted.iter().map(|(k, _)| k.clone()).collect::<Vec<_>>().join("&");
    let sorted_q = join(&sorted);

    let decoded: Vec<(String, String)> = pairs
        .iter()
        .map(|(k, v)| (k.clone(), pct_decode(v)))
        .collect();
    let decoded_q = join(&decoded);
    let mut decoded_sorted = decoded.clone();
    decoded_sorted.sort_by(|a, b| a.0.cmp(&b.0));
    let decoded_sorted_q = join(&decoded_sorted);

    let lower_hex = sorted_q.to_lowercase();
    let double = sorted_q.replace('%', "%25");

    vec![
        ("sign=raw            wire=raw", wire.to_string(), wire.to_string()),
        ("sign=sorted         wire=raw", sorted_q.clone(), wire.to_string()),
        ("sign=names          wire=raw", names_raw, wire.to_string()),
        ("sign=names_sorted   wire=raw", names_sorted, wire.to_string()),
        ("sign=sorted         wire=sorted", sorted_q.clone(), sorted_q.clone()),
        ("sign=decoded        wire=raw", decoded_q, wire.to_string()),
        ("sign=decoded_sorted wire=raw", decoded_sorted_q.clone(), wire.to_string()),
        ("sign=lowercase_hex  wire=raw", lower_hex, wire.to_string()),
        ("sign=double_encoded wire=raw", double, wire.to_string()),
        (
            "sign=encoded+plus   wire=raw",
            sorted_q.replace("%20", "+"),
            wire.to_string(),
        ),
        (
            "sign=decoded+plus   wire=raw",
            decoded_sorted_q.replace(' ', "+"),
            wire.to_string(),
        ),
        (
            "sign=decoded_sorted wire=decoded",
            decoded_sorted_q.clone(),
            decoded_sorted_q,
        ),
    ]
}

/// Try different canonical forms of the query in `msg1`, sending the same URL.
fn cmd_probe_sign(args: &[String]) {
    let path = args.first().unwrap_or_else(|| usage()).clone();
    let wire = args.get(1).cloned().unwrap_or_default();
    let cases = query_variants(&wire);

    let c = LaneClient::new(
        env::var("LANED_JWT").unwrap_or_default(),
        env::var("LANED_LDI").unwrap_or_default(),
    );
    for (label, sign, w) in cases {
        match c.send_with_sign("GET", &path, &w, &sign, &[]) {
            Ok(resp) => {
                let status = resp.status;
                let text = resp.text();
                let shown: String = if text.chars().all(|c| !c.is_control()) {
                    text.chars().take(120).collect()
                } else {
                    format!("<{} bytes binary>", text.len())
                };
                println!("{label:<34} -> {status}  {shown}");
            }
            Err(e) => println!("{label:<34} -> ERROR {e}"),
        }
    }
    println!("\nsigned query was: {wire}");
}

/// Try several parameter shapes against /track/stream in one run.
fn cmd_probe_stream(args: &[String]) {
    let id = args.first().unwrap_or_else(|| usage()).clone();
    let pl = args.get(1).cloned().unwrap_or_default();

    let cases: Vec<(&str, String, String, &str)> = vec![
        ("ref=playlist q=BASIC", pl.clone(), "BASIC".into(), "0"),
        ("ref=empty    q=BASIC", String::new(), "BASIC".into(), "0"),
        ("ref=playlist q=HIGH", pl.clone(), "HIGH".into(), "0"),
        ("ref=playlist q=128 kbps", pl.clone(), "128 kbps".into(), "0"),
        ("ref=test     q=BASIC", "test".into(), "BASIC".into(), "0"),
        ("ref=playlist q=BASIC accept-red=1", pl.clone(), "BASIC".into(), "1"),
    ];

    for (label, ref_id, quality, red) in cases {
        let c = LaneClient::new(
            env::var("LANED_JWT").unwrap_or_default(),
            env::var("LANED_LDI").unwrap_or_default(),
        )
        .with_accept_red(red);
        let q = format!("trackId={id}&refId={ref_id}&streamQuality={quality}");
        match c.get("/track/stream", &q) {
            Ok(resp) => {
                let status = resp.status;
                let headers = resp.headers.clone();
                let text = resp.text();
                let snippet: String = text.chars().take(160).collect();
                let mut hint = String::new();
                if !status.is_success() {
                    for name in ["www-authenticate", "content-type", "server", "x-request-id"] {
                        if let Some(v) = headers.get(name) {
                            hint.push_str(&format!(" [{name}: {}]", v.to_str().unwrap_or("?")));
                        }
                    }
                }
                println!("{label:<38} -> {status}  {snippet}{hint}");
            }
            Err(e) => println!("{label:<38} -> ERROR {e}"),
        }
    }
}

fn cmd_tracks(args: &[String]) {
    let id = args.first().unwrap_or_else(|| usage());
    let page = args.get(1).map(String::as_str).unwrap_or("1");
    let size = args.get(2).map(String::as_str).unwrap_or("50");
    cmd_simple(&format!("/playlist/{id}/tracks"), &format!("page={page}&pageSize={size}"));
}

// ---------------------------------------------------------------------------
// offline commands
// ---------------------------------------------------------------------------

fn cmd_gen(args: &[String]) {
    let method = flag(args, "--method").unwrap_or("GET");
    let path = flag(args, "--path").unwrap_or_else(|| usage());
    let query = flag(args, "--query").unwrap_or("");
    let body = flag(args, "--body").unwrap_or("");
    let ts: i64 = flag(args, "--timestamp").and_then(|s| s.parse().ok()).unwrap_or(0);

    // --query is the wire form; the signature is computed over the decoded one
    let signed_query = canonical_query(query);
    let out = build(method, path, &signed_query, body.as_bytes(), ts);

    if let Some(file) = flag(args, "--body-out") {
        if let Err(e) = fs::write(file, &out.payload) {
            eprintln!("cannot write {file}: {e}");
            exit(1);
        }
    }

    let core = out.core_token.trim_start_matches("X-Core-Token:");
    let trace = out.trace_id.trim_start_matches("X-Request-Trace-Id:");
    let meta = out.client_meta.trim_start_matches("X-Client-Meta:");

    if has(args, "--json") {
        println!(
            "{{\"core\":\"{}\",\"trace\":\"{}\",\"meta\":\"{}\",\"body_len\":{}}}",
            core, trace, meta, out.payload.len()
        );
    } else {
        println!("x-core-token: {core}");
        println!("x-request-trace-id: {trace}");
        println!("x-client-meta: {meta}");
        if !out.payload.is_empty() {
            println!("body: {} bytes (encrypted)", out.payload.len());
        }
    }
}

fn cmd_dump(args: &[String]) {
    let file = args.first().unwrap_or_else(|| usage());
    let raw = match fs::read(file) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("cannot read {file}: {e}");
            exit(1);
        }
    };

    let (head, body) = split_dump(&raw);
    let head = String::from_utf8_lossy(head).replace('\u{feff}', "");

    let first = head.lines().next().unwrap_or("");
    let mut it = first.split_whitespace();
    let method = it.next().unwrap_or("?").to_string();
    let url = it.next().unwrap_or("").to_string();
    let target = url.split("laneapi.com").nth(1).unwrap_or("");
    let (path, query) = match target.split_once('?') {
        Some((p, q)) => (p.to_string(), canonical_query(q)),
        None => (target.to_string(), String::new()),
    };

    let token = header(&head, "x-core-token").unwrap_or_else(|| {
        eprintln!("no x-core-token in {file}");
        exit(1);
    });
    let declared: usize = header(&head, "content-length")
        .and_then(|v| v.trim().parse().ok())
        .unwrap_or(0);
    let wire_body = &body[..body.len().min(declared)];

    let raw_token = match base64_decode(&token) {
        Some(v) if v.len() > 4 => v,
        _ => {
            eprintln!("bad x-core-token");
            exit(1);
        }
    };
    let (key2, ct) = raw_token.split_at(4);
    let plain = token_decipher(key2, ct);
    let parts: Vec<&[u8]> = plain.split(|&b| b == b'|').collect();
    if parts.len() < 6 {
        eprintln!("unexpected token envelope");
        exit(1);
    }
    let digest = String::from_utf8_lossy(parts[0]).to_string();
    let ts = String::from_utf8_lossy(parts[1]).to_string();
    let nonce = String::from_utf8_lossy(parts[2]).to_string();
    let boot = String::from_utf8_lossy(parts[3]).to_string();
    let inode = String::from_utf8_lossy(parts[4]).to_string();
    let flag = String::from_utf8_lossy(parts[5]).to_string();

    let mut msg1 = Vec::new();
    for chunk in [
        method.as_bytes(),
        b":",
        path.as_bytes(),
        b":",
        query.as_bytes(),
        b":",
        ts.as_bytes(),
        b":",
        nonce.as_bytes(),
        b":",
        wire_body,
        b":",
        boot.as_bytes(),
        b":",
        inode.as_bytes(),
    ] {
        msg1.extend_from_slice(chunk);
    }
    let calc = hex_str(&hmac_sha256(&msg1));

    println!("file    : {file}");
    println!("method  : {method}");
    println!("path    : {path}");
    println!("query   : {query}");
    println!("ts      : {ts}");
    println!("boot_id : {boot}");
    println!("inode   : {inode}");
    println!("flag    : {flag}");
    println!("body    : {} bytes (declared {declared})", wire_body.len());
    println!("hmac    : {}", if calc == digest { "MATCH" } else { "MISMATCH" });
    if calc != digest {
        println!("  token : {digest}");
        println!("  calc  : {calc}");
    }
}

fn header(head: &str, name: &str) -> Option<String> {
    head.lines()
        .find(|l| l.to_ascii_lowercase().starts_with(&format!("{name}:")))
        .and_then(|l| l.split_once(':'))
        .map(|(_, v)| v.trim().to_string())
}

fn split_dump(raw: &[u8]) -> (&[u8], &[u8]) {
    for pat in [&b"\r\n \r\n"[..], &b"\r\n\r\n"[..], &b"\n \n"[..], &b"\n\n"[..]] {
        if let Some(pos) = find(raw, pat) {
            return (&raw[..pos], &raw[pos + pat.len()..]);
        }
    }
    (raw, &[])
}

fn find(hay: &[u8], needle: &[u8]) -> Option<usize> {
    hay.windows(needle.len()).position(|w| w == needle)
}

fn hex_str(b: &[u8]) -> String {
    const D: &[u8; 16] = b"0123456789abcdef";
    let mut s = String::with_capacity(b.len() * 2);
    for &x in b {
        s.push(D[(x >> 4) as usize] as char);
        s.push(D[(x & 0x0f) as usize] as char);
    }
    s
}

#[cfg(test)]
mod tests {
    use super::query_variants;

    #[test]
    fn variants_are_built_as_expected() {
        let v = query_variants("trackId=ABC&refId=lane_likes&streamQuality=BASIC");
        assert_eq!(v[0].1, "trackId=ABC&refId=lane_likes&streamQuality=BASIC");
        assert_eq!(v[1].1, "refId=lane_likes&streamQuality=BASIC&trackId=ABC");
        assert_eq!(v[2].1, "trackId&refId&streamQuality");
        assert_eq!(v[3].1, "refId&streamQuality&trackId");
        assert_eq!(v[4].2, "refId=lane_likes&streamQuality=BASIC&trackId=ABC");
    }
}
