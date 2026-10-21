use std::time::Duration;
use laned_bnit::{build, canonical_query, decrypt_response};
use reqwest::header::HeaderMap;
use reqwest::{Client, Method, StatusCode};
use serde::{Deserialize, Serialize};
use url::Url;

pub const DEFAULT_BASE: &str = "https://ru.laneapi.com";

pub fn clean_invisible(s: &str) -> String {
    let is_invisible = |c: char| -> bool {
        c.is_whitespace()
            || matches!(
                c,
                '\u{3164}'   // HANGUL FILLER
                | '\u{2800}' // BRAILLE PATTERN BLANK
                | '\u{200B}' // ZERO WIDTH SPACE
                | '\u{200C}' // ZERO WIDTH NON-JOINER
                | '\u{200D}' // ZERO WIDTH JOINER
                | '\u{200E}' // LEFT-TO-RIGHT MARK
                | '\u{200F}' // RIGHT-TO-LEFT MARK
                | '\u{2060}' // WORD JOINER
                | '\u{FEFF}' // ZERO WIDTH NO-BREAK SPACE / BOM
                | '\u{180E}' // MONGOLIAN VOWEL SEPARATOR
                | '\u{00AD}' // SOFT HYPHEN
            )
    };
    let trimmed = s.trim_matches(is_invisible);
    trimmed.replace('\u{3164}', "")
}

#[derive(Clone)]
pub struct LaneApiClient {
    http: Client,
    base: String,
    jwt: String,
    ldi: String,
}

#[derive(Debug, Clone)]
pub struct LaneHttpError {
    pub method: String,
    pub path: String,
    pub query: String,
    pub status: StatusCode,
    pub headers: Vec<(String, String)>,
    pub request_body: String,
    pub response_body: String,
}

impl std::fmt::Display for LaneHttpError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "HTTP {} on {} {}", self.status, self.method, self.path)
    }
}

impl std::error::Error for LaneHttpError {}

impl From<String> for LaneHttpError {
    fn from(s: String) -> Self {
        LaneHttpError {
            method: "REQUEST".to_string(),
            path: "".to_string(),
            query: "".to_string(),
            status: StatusCode::INTERNAL_SERVER_ERROR,
            headers: Vec::new(),
            request_body: String::new(),
            response_body: s,
        }
    }
}

impl LaneHttpError {
    pub fn log_error(&self) {
        log::error!(
            "Lane API Error: {} {} (Query: '{}', Status: {})\nSent Headers:\n{}\nResponse:\n{}",
            self.method,
            self.path,
            self.query,
            self.status,
            self.format_headers(),
            self.response_body
        );
    }

    pub fn format_headers(&self) -> String {
        self.headers
            .iter()
            .map(|(k, v)| format!("  {k}: {v}"))
            .collect::<Vec<_>>()
            .join("\n")
    }

    pub fn to_blockquote_html(&self) -> String {
        let headers_str = self.format_headers();
        let query_str = if self.query.is_empty() { "—" } else { &self.query };
        let body_str = if self.request_body.is_empty() { "—" } else { &self.request_body };
        let resp_str = if self.response_body.is_empty() { "—" } else { &self.response_body };

        let esc = |s: &str| -> String {
            s.replace('&', "&amp;")
                .replace('<', "&lt;")
                .replace('>', "&gt;")
        };

        format!(
            "<blockquote>Endpoint: {} {}\nQuery: {}\nStatus: {}\nRequest Body: {}\nHeaders:\n{}\n\nResponse:\n{}</blockquote>",
            esc(&self.method),
            esc(&self.path),
            esc(query_str),
            self.status,
            esc(body_str),
            esc(&headers_str),
            esc(resp_str)
        )
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SpotifyData {
    pub album: Option<String>,
    #[serde(rename = "mp4128FileId")]
    pub mp4128_file_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SoundCloudData {
    #[serde(rename = "streamUrl")]
    pub stream_url: Option<String>,
    #[serde(rename = "s3Key")]
    pub s3_key: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaneTrack {
    #[serde(rename = "songId")]
    pub song_id: String,
    #[serde(default)]
    pub title: String,
    #[serde(rename = "artistsDisplayedName", default)]
    pub artists_displayed_name: String,
    #[serde(rename = "coverUrl")]
    pub cover_url: Option<String>,
    #[serde(default)]
    pub duration: String,
    #[serde(default)]
    pub platform: String,
    #[serde(rename = "spData")]
    pub sp_data: Option<SpotifyData>,
    #[serde(rename = "scData")]
    pub sc_data: Option<SoundCloudData>,
}

impl LaneTrack {
    pub fn display_album(&self) -> String {
        if let Some(sp) = &self.sp_data {
            if let Some(alb) = &sp.album {
                let cleaned = clean_invisible(alb);
                if !cleaned.is_empty() {
                    return cleaned;
                }
            }
        }
        "—".to_string()
    }

    pub fn display_artist(&self) -> String {
        let cleaned = clean_invisible(&self.artists_displayed_name);
        if cleaned.is_empty() {
            "Unknown Artist".to_string()
        } else {
            cleaned
        }
    }

    pub fn display_title(&self) -> String {
        let cleaned = clean_invisible(&self.title);
        if cleaned.is_empty() {
            "Untitled".to_string()
        } else {
            cleaned
        }
    }

    pub fn display_duration(&self) -> String {
        let cleaned = clean_invisible(&self.duration);
        if cleaned.is_empty() {
            "—".to_string()
        } else {
            cleaned
        }
    }

    pub fn duration_seconds(&self) -> u64 {
        let parts: Vec<&str> = self.duration.split(':').collect();
        if parts.len() == 2 {
            let m: u64 = parts[0].parse().unwrap_or(0);
            let s: u64 = parts[1].parse().unwrap_or(0);
            m * 60 + s
        } else if parts.len() == 3 {
            let h: u64 = parts[0].parse().unwrap_or(0);
            let m: u64 = parts[1].parse().unwrap_or(0);
            let s: u64 = parts[2].parse().unwrap_or(0);
            h * 3600 + m * 60 + s
        } else {
            self.duration.parse().unwrap_or(0)
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResultItem {
    pub r#type: String,
    pub track: Option<LaneTrack>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CombinedSearchResponse {
    #[serde(default)]
    pub results: Vec<SearchResultItem>,
    #[serde(rename = "searchToken")]
    pub search_token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrackStreamingResult {
    pub url: String,
    #[serde(rename = "trackId")]
    pub track_id: Option<String>,
    pub ttl: Option<i64>,
    #[serde(rename = "playbackToken")]
    pub playback_token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LanePlaylist {
    #[serde(rename = "playlistId")]
    pub playlist_id: String,
    #[serde(rename = "playlistName")]
    pub playlist_name: String,
    #[serde(rename = "playlistImageUrl")]
    pub playlist_image_url: Option<String>,
    #[serde(rename = "playlistTracksIds", default)]
    pub playlist_tracks_ids: Vec<String>,
    #[serde(rename = "playlistDescription")]
    pub playlist_description: Option<String>,
    pub platform: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlaylistTracksPage {
    #[serde(default)]
    pub items: Vec<LaneTrack>,
    #[serde(default)]
    pub page: i64,
    #[serde(rename = "pageSize", default)]
    pub page_size: i64,
    #[serde(rename = "totalItems", default)]
    pub total_items: i64,
    #[serde(rename = "totalPages", default)]
    pub total_pages: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaneAccountUser {
    #[serde(rename = "laneId")]
    pub lane_id: Option<String>,
    #[serde(rename = "displayedName")]
    pub displayed_name: Option<String>,
    #[serde(rename = "userName")]
    pub user_name: Option<String>,
    #[serde(rename = "avatarUrl")]
    pub avatar_url: Option<String>,
    #[serde(rename = "countryCode")]
    pub country_code: Option<String>,
    #[serde(rename = "interfaceLanguage")]
    pub interface_language: Option<String>,
    #[serde(rename = "userPlaylists", default)]
    pub user_playlists: Vec<String>,
    #[serde(rename = "equippedBadgeId")]
    pub equipped_badge_id: Option<String>,
    #[serde(rename = "telegramId")]
    pub telegram_id: Option<i64>,
}

impl LaneAccountUser {
    #[allow(dead_code)]
    pub fn clean_displayed_name(&self) -> String {
        let raw = self.displayed_name.as_deref().unwrap_or("—");
        let cleaned = clean_invisible(raw);
        if cleaned.is_empty() {
            "User".to_string()
        } else {
            cleaned
        }
    }

    #[allow(dead_code)]
    pub fn clean_user_name(&self) -> String {
        let raw = self.user_name.as_deref().unwrap_or("—");
        let cleaned = clean_invisible(raw);
        if cleaned.is_empty() {
            "—".to_string()
        } else {
            cleaned
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreatePlaylistRequest {
    #[serde(rename = "playlistName")]
    pub playlist_name: String,
    #[serde(rename = "playlistDescription")]
    pub playlist_description: String,
    #[serde(rename = "playlistTracks")]
    pub playlist_tracks: Vec<String>,
    #[serde(rename = "playlistImageUrl")]
    pub playlist_image_url: String,
    #[serde(rename = "creatorLid")]
    pub creator_lid: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreatePlaylistResponse {
    #[serde(rename = "playlistId")]
    pub playlist_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrackIdsRequest {
    #[serde(rename = "trackIds")]
    pub track_ids: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaneAuthResponse {
    pub token: Option<String>,
    #[serde(rename = "isFirstAuth")]
    pub is_first_auth: Option<bool>,
    pub user: Option<LaneAccountUser>,
}

impl LaneApiClient {
    pub fn new(jwt: impl Into<String>, ldi: impl Into<String>) -> Self {
        Self {
            http: Client::builder()
                .timeout(Duration::from_secs(30))
                .build()
                .unwrap_or_else(|_| Client::new()),
            base: DEFAULT_BASE.to_string(),
            jwt: jwt.into(),
            ldi: ldi.into(),
        }
    }

    #[allow(dead_code)]
    pub fn with_base(mut self, base: impl Into<String>) -> Self {
        self.base = base.into();
        self
    }

    #[allow(dead_code)]
    pub fn jwt(&self) -> &str {
        &self.jwt
    }

    #[allow(dead_code)]
    pub fn ldi(&self) -> &str {
        &self.ldi
    }

    pub async fn send_signed(
        &self,
        method: &str,
        path: &str,
        query: &str,
        body: &[u8],
    ) -> Result<(StatusCode, Vec<u8>, HeaderMap), LaneHttpError> {
        let signed_query = canonical_query(query);
        let mut url = Url::parse(&format!("{}{}", self.base, path))
            .map_err(|e| format!("Invalid URL: {e}"))?;
        if !query.is_empty() {
            url.set_query(Some(query));
        }

        let signed_path = url.path().to_string();
        let out = build(method, &signed_path, &signed_query, body, 0);

        let safe_headers = vec![
            ("x-core-token".to_string(), "[PRESENT]".to_string()),
            (
                "x-request-trace-id".to_string(),
                strip(&out.trace_id, "X-Request-Trace-Id:").to_string(),
            ),
            (
                "x-client-meta".to_string(),
                strip(&out.client_meta, "X-Client-Meta:").to_string(),
            ),
            ("ldi".to_string(), "[REDACTED]".to_string()),
            ("x-accept-red".to_string(), "1".to_string()),
            ("x-app-version".to_string(), "207".to_string()),
            ("x-platform".to_string(), "android".to_string()),
            ("x-theme".to_string(), "dark".to_string()),
            ("accept-language".to_string(), "ru".to_string()),
            ("tz".to_string(), "Europe/Moscow".to_string()),
            (
                "user-agent".to_string(),
                "LaneMusic/1.0 (Android; Mobile)".to_string(),
            ),
            (
                "authorization".to_string(),
                if self.jwt.is_empty() {
                    "[NONE]".to_string()
                } else {
                    "Bearer [REDACTED]".to_string()
                },
            ),
        ];

        let m = Method::from_bytes(method.as_bytes()).map_err(|e| e.to_string())?;
        let mut req = self
            .http
            .request(m, url)
            .header("x-core-token", strip(&out.core_token, "X-Core-Token:"))
            .header(
                "x-request-trace-id",
                strip(&out.trace_id, "X-Request-Trace-Id:"),
            )
            .header("x-client-meta", strip(&out.client_meta, "X-Client-Meta:"))
            .header("ldi", &self.ldi)
            .header("x-accept-red", "1")
            .header("x-app-version", "207")
            .header("x-platform", "android")
            .header("x-theme", "dark")
            .header("accept-language", "ru")
            .header("tz", "Europe/Moscow")
            .header("user-agent", "LaneMusic/1.0 (Android; Mobile)");

        if !self.jwt.is_empty() {
            req = req.header("authorization", format!("Bearer {}", self.jwt));
        }

        if !out.payload.is_empty() {
            req = req
                .header("content-type", "application/json; charset=UTF-8")
                .body(out.payload);
        }

        let resp = req
            .send()
            .await
            .map_err(|e| format!("Request failed: {e}"))?;
        let status = resp.status();
        let headers = resp.headers().clone();
        let raw_bytes = resp
            .bytes()
            .await
            .map_err(|e| format!("Failed to read response bytes: {e}"))?
            .to_vec();

        let is_red = header(&headers, "x-core-red") == Some("1");
        let is_compressed = header(&headers, "x-core-compressed") == Some("1");
        let nonce = header(&headers, "x-resp-nonce").unwrap_or("");

        let body_bytes = if is_red && !nonce.is_empty() {
            decrypt_response(&raw_bytes, nonce, is_compressed)
        } else {
            raw_bytes
        };

        if !status.is_success() {
            let err = LaneHttpError {
                method: method.to_string(),
                path: path.to_string(),
                query: signed_query,
                status,
                headers: safe_headers,
                request_body: if body.is_empty() {
                    String::new()
                } else {
                    String::from_utf8_lossy(body).to_string()
                },
                response_body: String::from_utf8_lossy(&body_bytes).to_string(),
            };
            err.log_error();
            return Err(err);
        }

        Ok((status, body_bytes, headers))
    }

    /// Search for tracks, artists, albums across platforms.
    pub async fn search(&self, query: &str) -> Result<Vec<LaneTrack>, LaneHttpError> {
        let q = format!("platform=all&q={}&ver=1", query);
        let (_, body, _) = self.send_signed("GET", "/platforms/search", &q, &[]).await?;

        let parsed: CombinedSearchResponse = serde_json::from_slice(&body)
            .map_err(|e| format!("Failed to parse search response: {e}"))?;

        let tracks: Vec<LaneTrack> = parsed
            .results
            .into_iter()
            .filter_map(|item| {
                if item.r#type == "track" {
                    item.track
                } else {
                    None
                }
            })
            .collect();

        Ok(tracks)
    }

    /// Resolve a playable audio stream URL.
    pub async fn get_stream(
        &self,
        track_id: &str,
        ref_id: &str,
        quality: &str,
    ) -> Result<TrackStreamingResult, LaneHttpError> {
        let ref_val = if ref_id.is_empty() { "search" } else { ref_id };
        let q = format!("refId={ref_val}&streamQuality={quality}&trackId={track_id}");
        let (_, body, _) = self.send_signed("GET", "/track/stream", &q, &[]).await?;

        serde_json::from_slice::<TrackStreamingResult>(&body)
            .map_err(|e| format!("Failed to parse stream response: {e}").into())
    }

    /// Fetch user playlists.
    pub async fn get_playlists(&self) -> Result<Vec<LanePlaylist>, LaneHttpError> {
        let (_, body, _) = self.send_signed("GET", "/user/playlists", "", &[]).await?;

        serde_json::from_slice::<Vec<LanePlaylist>>(&body)
            .map_err(|e| format!("Failed to parse playlists: {e}").into())
    }

    /// Fetch tracks of a playlist with pagination.
    pub async fn get_playlist_tracks(
        &self,
        playlist_id: &str,
        page: i64,
        page_size: i64,
    ) -> Result<PlaylistTracksPage, LaneHttpError> {
        let path = format!("/playlist/{playlist_id}/tracks");
        let q = format!("page={page}&pageSize={page_size}");
        let (_, body, _) = self.send_signed("GET", &path, &q, &[]).await?;

        serde_json::from_slice::<PlaylistTracksPage>(&body)
            .map_err(|e| format!("Failed to parse playlist tracks: {e}").into())
    }

    /// Fetch account details (/account).
    pub async fn get_account(&self) -> Result<LaneAccountUser, LaneHttpError> {
        let (_, body, _) = self.send_signed("GET", "/account", "", &[]).await?;

        serde_json::from_slice::<LaneAccountUser>(&body)
            .map_err(|e| format!("Failed to parse account: {e}").into())
    }

    /// Poll authorization status from /auth/{device_id}.
    pub async fn poll_auth(device_id: &str) -> Result<Option<LaneAuthResponse>, LaneHttpError> {
        let client = LaneApiClient::new("", device_id);
        let path = format!("/auth/{device_id}");
        let (status, body, _) = client.send_signed("GET", &path, "", &[]).await?;
        if status.is_success() {
            let res: LaneAuthResponse = serde_json::from_slice(&body)
                .map_err(|e| format!("Failed to parse auth response: {e}"))?;
            if res.token.is_some() {
                return Ok(Some(res));
            }
        }
        Ok(None)
    }

    /// Fetch full track metadata by track IDs via /user/tracks.
    pub async fn get_tracks(&self, track_ids: &[&str]) -> Result<Vec<LaneTrack>, LaneHttpError> {
        let req = TrackIdsRequest {
            track_ids: track_ids.iter().map(|s| s.to_string()).collect(),
        };
        let body = serde_json::to_vec(&req)
            .map_err(|e| format!("Failed to serialize TrackIdsRequest: {e}"))?;
        let (_, resp_bytes, _) = self.send_signed("POST", "/user/tracks", "", &body).await?;

        serde_json::from_slice::<Vec<LaneTrack>>(&resp_bytes)
            .map_err(|e| format!("Failed to parse tracks from /user/tracks: {e}").into())
    }

    /// Fetch a single track by ID.
    pub async fn get_track(&self, track_id: &str) -> Result<Option<LaneTrack>, LaneHttpError> {
        let tracks = self.get_tracks(&[track_id]).await?;
        Ok(tracks.into_iter().next())
    }

    /// Create a new playlist.
    pub async fn create_playlist(
        &self,
        name: &str,
        description: &str,
        creator_lid: &str,
    ) -> Result<String, LaneHttpError> {
        let req = CreatePlaylistRequest {
            playlist_name: name.to_string(),
            playlist_description: description.to_string(),
            playlist_tracks: Vec::new(),
            playlist_image_url: String::new(),
            creator_lid: creator_lid.to_string(),
        };
        let body = serde_json::to_vec(&req)
            .map_err(|e| format!("Failed to serialize CreatePlaylistRequest: {e}"))?;
        let (_, resp_bytes, _) = self.send_signed("POST", "/create-playlist", "", &body).await?;

        if let Ok(res) = serde_json::from_slice::<CreatePlaylistResponse>(&resp_bytes) {
            Ok(res.playlist_id)
        } else {
            let text = String::from_utf8_lossy(&resp_bytes).trim().to_string();
            Ok(text)
        }
    }

    /// Add a track to a playlist (including "lane_likes").
    pub async fn add_track_to_playlist(
        &self,
        playlist_id: &str,
        song_id: &str,
    ) -> Result<(), LaneHttpError> {
        let q = format!("playlistId={playlist_id}");
        let body = serde_json::to_vec(&vec![song_id])
            .map_err(|e| format!("Failed to serialize trackIds: {e}"))?;
        let _ = self.send_signed("POST", "/user/playlist/add-tracks", &q, &body).await?;
        Ok(())
    }

    /// Remove a track from a playlist (including "lane_likes").
    pub async fn remove_track_from_playlist(
        &self,
        playlist_id: &str,
        song_id: &str,
    ) -> Result<(), LaneHttpError> {
        let q = format!("playlistId={playlist_id}&trackId={song_id}");
        let _ = self.send_signed("GET", "/user/playlist/remove-track", &q, &[]).await?;
        Ok(())
    }

    /// Delete a playlist.
    #[allow(dead_code)]
    pub async fn delete_playlist(
        &self,
        playlist_id: &str,
    ) -> Result<(), LaneHttpError> {
        let q = format!("playlistId={playlist_id}");
        let _ = self.send_signed("GET", "/delete-playlist", &q, &[]).await?;
        Ok(())
    }

    /// Download audio bytes from a resolved URL or direct stream URL.
    pub async fn download_audio(&self, url: &str) -> Result<Vec<u8>, String> {
        let resp = self
            .http
            .get(url)
            .send()
            .await
            .map_err(|e| format!("Download request failed: {e}"))?;

        if !resp.status().is_success() {
            return Err(format!("Download failed with status: {}", resp.status()));
        }

        let bytes = resp
            .bytes()
            .await
            .map_err(|e| format!("Failed to read stream bytes: {e}"))?;

        Ok(bytes.to_vec())
    }
}

fn strip<'a>(s: &'a str, prefix: &str) -> &'a str {
    s.strip_prefix(prefix).unwrap_or(s)
}

fn header<'a>(headers: &'a HeaderMap, name: &str) -> Option<&'a str> {
    headers.get(name).and_then(|v| v.to_str().ok())
}

#[allow(dead_code)]
fn urlencoding(input: &str) -> String {
    url::form_urlencoded::byte_serialize(input.as_bytes()).collect()
}
