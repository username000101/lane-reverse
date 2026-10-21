use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};
use tokio::sync::RwLock;
use serde::{Deserialize, Serialize};

use crate::lane_client::{LaneAccountUser, LaneApiClient, LaneTrack};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserSession {
    pub telegram_id: i64,
    pub device_id: String,
    pub token: String,
    pub user: Option<LaneAccountUser>,
    pub updated_at: u64,
}

#[derive(Debug, Clone)]
pub struct CachedSearch {
    pub query: String,
    pub tracks: Vec<LaneTrack>,
    pub created_at: u64,
}

pub struct SessionStore {
    file_path: PathBuf,
    sessions: RwLock<HashMap<i64, UserSession>>,
    searches: RwLock<HashMap<String, CachedSearch>>,
    tracks: RwLock<HashMap<String, LaneTrack>>,
    liked_tracks: RwLock<HashSet<String>>,
    last_views: RwLock<HashMap<i64, String>>,
    active_tracks: RwLock<HashMap<i64, String>>,
    fallback_client: Option<LaneApiClient>,
}

impl SessionStore {
    pub fn new(path: impl Into<PathBuf>) -> Self {
        let file_path = path.into();
        let sessions = Self::load_file(&file_path);

        let fallback_client = Self::try_load_fallback();

        Self {
            file_path,
            sessions: RwLock::new(sessions),
            searches: RwLock::new(HashMap::new()),
            tracks: RwLock::new(HashMap::new()),
            liked_tracks: RwLock::new(HashSet::new()),
            last_views: RwLock::new(HashMap::new()),
            active_tracks: RwLock::new(HashMap::new()),
            fallback_client,
        }
    }

    fn now_secs() -> u64 {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0)
    }

    fn try_load_fallback() -> Option<LaneApiClient> {
        // Try LANED_JWT / LANED_LDI env
        if let (Ok(jwt), Ok(ldi)) = (std::env::var("LANED_JWT"), std::env::var("LANED_LDI")) {
            if !jwt.is_empty() {
                return Some(LaneApiClient::new(jwt, ldi));
            }
        }

        // Try well-known json config locations
        let candidates = [
            "laned.json",
            "crates/laned.json",
            "C:\\algorithm-reimpl\\crates\\laned.json",
            "C:\\algorithm-reimpl\\laned.json",
        ];

        for path in candidates {
            if let Ok(content) = fs::read_to_string(path) {
                if let Ok(v) = serde_json::from_str::<serde_json::Value>(&content) {
                    if let Some(token) = v.get("token").and_then(|t| t.as_str()) {
                        let ldi = v.get("deviceId").and_then(|d| d.as_str()).unwrap_or("");
                        if !token.is_empty() {
                            log::debug!("Loaded fallback Lane credentials from {path}");
                            return Some(LaneApiClient::new(token, ldi));
                        }
                    }
                }
            }
        }
        None
    }

    fn load_file(path: &Path) -> HashMap<i64, UserSession> {
        if let Ok(content) = fs::read_to_string(path) {
            if let Ok(map) = serde_json::from_str::<HashMap<i64, UserSession>>(&content) {
                return map;
            }
        }
        HashMap::new()
    }

    async fn persist(&self) {
        let map = self.sessions.read().await;
        if let Ok(json) = serde_json::to_string_pretty(&*map) {
            let _ = fs::write(&self.file_path, json);
        }
    }

    pub async fn get_client(&self, tg_id: i64) -> Option<LaneApiClient> {
        let map = self.sessions.read().await;
        if let Some(sess) = map.get(&tg_id) {
            if !sess.token.is_empty() {
                return Some(LaneApiClient::new(&sess.token, &sess.device_id));
            }
        }
        // If not found in user sessions, use fallback if available
        self.fallback_client.clone()
    }

    pub async fn get_session(&self, tg_id: i64) -> Option<UserSession> {
        let map = self.sessions.read().await;
        map.get(&tg_id).cloned()
    }

    pub async fn save_session(
        &self,
        tg_id: i64,
        device_id: String,
        token: String,
        user: Option<LaneAccountUser>,
    ) {
        let sess = UserSession {
            telegram_id: tg_id,
            device_id,
            token,
            user,
            updated_at: Self::now_secs(),
        };

        {
            let mut map = self.sessions.write().await;
            map.insert(tg_id, sess);
        }
        self.persist().await;
    }

    pub async fn update_user_profile(&self, tg_id: i64, user: LaneAccountUser) {
        let mut map = self.sessions.write().await;
        if let Some(sess) = map.get_mut(&tg_id) {
            sess.user = Some(user);
            sess.updated_at = Self::now_secs();
        }
        drop(map);
        self.persist().await;
    }

    pub async fn cache_tracks(&self, tracks_slice: &[LaneTrack]) {
        let mut t_map = self.tracks.write().await;
        if t_map.len() > 2000 {
            t_map.clear();
        }
        for t in tracks_slice {
            t_map.insert(t.song_id.clone(), t.clone());
        }
    }

    pub async fn store_search(&self, search_id: String, query: String, tracks: Vec<LaneTrack>) {
        self.cache_tracks(&tracks).await;

        let mut searches = self.searches.write().await;
        // Purge searches older than 1 hour if size > 100
        if searches.len() > 100 {
            let now = Self::now_secs();
            searches.retain(|_, v| now - v.created_at < 3600);
        }
        searches.insert(
            search_id,
            CachedSearch {
                query,
                tracks,
                created_at: Self::now_secs(),
            },
        );
    }

    pub async fn get_search(&self, search_id: &str) -> Option<CachedSearch> {
        let searches = self.searches.read().await;
        searches.get(search_id).cloned()
    }

    pub async fn find_cached_track(&self, song_id: &str) -> Option<LaneTrack> {
        {
            let t_map = self.tracks.read().await;
            if let Some(t) = t_map.get(song_id) {
                return Some(t.clone());
            }
        }
        let searches = self.searches.read().await;
        for s in searches.values() {
            if let Some(t) = s.tracks.iter().find(|t| t.song_id == song_id) {
                return Some(t.clone());
            }
        }
        None
    }

    pub async fn is_track_liked(&self, song_id: &str) -> bool {
        let set = self.liked_tracks.read().await;
        set.contains(song_id)
    }

    pub async fn is_liked_empty(&self) -> bool {
        let set = self.liked_tracks.read().await;
        set.is_empty()
    }

    pub async fn set_track_liked(&self, song_id: &str, liked: bool) {
        let mut set = self.liked_tracks.write().await;
        if liked {
            set.insert(song_id.to_string());
        } else {
            set.remove(song_id);
        }
    }

    pub async fn update_liked_tracks(&self, track_ids: &[String]) {
        let mut set = self.liked_tracks.write().await;
        for id in track_ids {
            set.insert(id.clone());
        }
    }

    pub async fn set_last_view(&self, user_id: i64, view: String) {
        let mut map = self.last_views.write().await;
        map.insert(user_id, view);
    }

    pub async fn get_last_view(&self, user_id: i64) -> Option<String> {
        let map = self.last_views.read().await;
        map.get(&user_id).cloned()
    }

    pub async fn set_active_track(&self, user_id: i64, song_id: &str) {
        let mut map = self.active_tracks.write().await;
        map.insert(user_id, song_id.to_string());
    }

    pub async fn get_active_track(&self, user_id: i64) -> Option<String> {
        let map = self.active_tracks.read().await;
        map.get(&user_id).cloned()
    }

    pub async fn get_or_fetch_track(&self, client: &LaneApiClient, song_id: &str) -> Option<LaneTrack> {
        if let Some(t) = self.find_cached_track(song_id).await {
            return Some(t);
        }
        if let Ok(Some(track)) = client.get_track(song_id).await {
            let mut map = self.tracks.write().await;
            map.insert(song_id.to_string(), track.clone());
            return Some(track);
        }
        None
    }
}
