use std::io::Cursor;
use std::sync::Arc;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use grammers_client::media::Attribute;
use grammers_client::message::{Button, InputMessage, Message, ReplyMarkup};
use grammers_client::update::{Article, CallbackQuery, InlineQuery};
use grammers_client::Client;

use crate::i18n::{Language, Messages};
use crate::lane_client::{clean_invisible, LaneApiClient, LaneTrack};
use crate::session_store::SessionStore;

fn sanitize_filename(s: &str) -> String {
    let cleaned = clean_invisible(s);
    let mut out = String::with_capacity(cleaned.len());
    for c in cleaned.chars() {
        if matches!(c, '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' | '\0') {
            out.push('_');
        } else {
            out.push(c);
        }
    }
    let res = out.trim().to_string();
    if res.is_empty() {
        "track".to_string()
    } else {
        res
    }
}

const PAGE_SIZE: usize = 5;

pub struct BotHandler {
    client: Client,
    sessions: Arc<SessionStore>,
}

impl BotHandler {
    pub fn new(client: Client, sessions: Arc<SessionStore>) -> Self {
        Self { client, sessions }
    }

    fn user_id_from_msg(msg: &Message) -> i64 {
        msg.sender_id().map(|id| id.bare_id_unchecked()).unwrap_or(0)
    }

    async fn get_lang_for_user(&self, user_id: i64) -> Language {
        if let Some(sess) = self.sessions.get_session(user_id).await {
            if let Some(user) = sess.user {
                return Language::from_code(user.interface_language.as_deref());
            }
        }
        Language::Ru
    }

    pub async fn handle_message(&self, msg: Message) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let text = msg.text();
        let trimmed = text.trim();
        let user_id = Self::user_id_from_msg(&msg);
        let lang = self.get_lang_for_user(user_id).await;

        if trimmed == "/start" || trimmed.starts_with("/start ") {
            let param = trimmed.trim_start_matches("/start").trim();
            if param == "auth" || param == "login" {
                self.cmd_login(&msg, user_id, lang).await?;
            } else if param == "playlists" {
                self.cmd_playlists(&msg, user_id, lang).await?;
            } else {
                self.cmd_start(&msg, user_id, lang).await?;
            }
        } else if trimmed == "/help" {
            msg.respond(InputMessage::new().html(Messages::help(lang))).await?;
        } else if trimmed == "/login" || trimmed == "/auth" {
            self.cmd_login(&msg, user_id, lang).await?;
        } else if trimmed == "/profile" {
            self.cmd_profile(&msg, user_id, lang).await?;
        } else if trimmed == "/playlists" {
            self.cmd_playlists(&msg, user_id, lang).await?;
        } else if trimmed.starts_with("/newplaylist") || trimmed.starts_with("/create_playlist") {
            let name = trimmed
                .trim_start_matches("/newplaylist")
                .trim_start_matches("/create_playlist")
                .trim();
            if name.is_empty() {
                msg.respond(InputMessage::new().html(Messages::playlist_create_prompt(lang))).await?;
            } else {
                self.cmd_create_playlist(&msg, user_id, lang, name).await?;
            }
        } else if trimmed.starts_with("/track") || trimmed.starts_with("/song") {
            let song_id = trimmed
                .trim_start_matches("/track")
                .trim_start_matches("/song")
                .trim();
            if song_id.is_empty() {
                let usage = match lang {
                    Language::Ru => "ℹ️ <b>Использование:</b> <code>/track &lt;song_id&gt;</code>\nНапример: <code>/track 7e5qR5wNq6f0z5zP8YhN1g</code>",
                    Language::En => "ℹ️ <b>Usage:</b> <code>/track &lt;song_id&gt;</code>\nFor example: <code>/track 7e5qR5wNq6f0z5zP8YhN1g</code>",
                };
                msg.respond(InputMessage::new().html(usage)).await?;
            } else {
                self.cmd_track_card(&msg, user_id, lang, song_id).await?;
            }
        } else if trimmed.starts_with("/search") {
            let query = trimmed.trim_start_matches("/search").trim();
            if query.is_empty() {
                let usage = match lang {
                    Language::Ru => "ℹ️ <b>Использование:</b> <code>/search &lt;запрос&gt;</code>\nНапример: <code>/search Queen</code>",
                    Language::En => "ℹ️ <b>Usage:</b> <code>/search &lt;query&gt;</code>\nFor example: <code>/search Queen</code>",
                };
                msg.respond(InputMessage::new().html(usage)).await?;
            } else {
                self.cmd_search(&msg, user_id, lang, query).await?;
            }
        }

        Ok(())
    }

    async fn cmd_start(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = self.sessions.get_client(user_id).await;
        let is_auth = lane_client.is_some();
        let name = if let Some(sess) = self.sessions.get_session(user_id).await {
            sess.user
                .and_then(|u| u.displayed_name.or(u.user_name))
                .unwrap_or_else(|| "User".to_string())
        } else {
            "User".to_string()
        };

        let welcome_text = Messages::welcome(lang, is_auth, &name);

        if !is_auth {
            let dev_id = laned_bnit::random_device_id(32);
            let url = format!("https://t.me/lane_music_bot?start=auth{dev_id}");
            let (_, btn_text) = Messages::auth_prompt(lang, &url);

            let markup = ReplyMarkup::from_buttons(&[vec![Button::url(btn_text, url)]]);
            msg.respond(InputMessage::new().html(welcome_text).reply_markup(markup)).await?;

            // Spawn background auth polling
            self.spawn_auth_poll(user_id, dev_id, msg.clone(), lang);
        } else {
            let markup = ReplyMarkup::from_buttons(&[
                vec![
                    Button::data(Messages::btn_profile(lang), b"refresh_profile"),
                    Button::data(Messages::btn_playlists(lang), b"user_playlists"),
                ],
            ]);
            msg.respond(InputMessage::new().html(welcome_text).reply_markup(markup)).await?;
        }

        Ok(())
    }

    async fn cmd_login(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let dev_id = laned_bnit::random_device_id(32);
        let url = format!("https://t.me/lane_music_bot?start=auth{dev_id}");
        let (prompt_text, btn_text) = Messages::auth_prompt(lang, &url);

        let markup = ReplyMarkup::from_buttons(&[vec![Button::url(btn_text, url)]]);
        msg.respond(InputMessage::new().html(prompt_text).reply_markup(markup)).await?;

        self.spawn_auth_poll(user_id, dev_id, msg.clone(), lang);
        Ok(())
    }

    fn spawn_auth_poll(&self, user_id: i64, dev_id: String, msg: Message, lang: Language) {
        let sessions = Arc::clone(&self.sessions);

        tokio::spawn(async move {
            log::debug!("Started auth polling for deviceId: {dev_id} (user {user_id})");
            for attempt in 1..=20 {
                tokio::time::sleep(Duration::from_secs(3)).await;
                match LaneApiClient::poll_auth(&dev_id).await {
                    Ok(Some(res)) => {
                        if let Some(token) = res.token {
                            log::debug!("Auth successful for deviceId {dev_id} on attempt {attempt}");

                            // Initialize client to fetch fresh account and playlists details concurrently
                            let lane_client = LaneApiClient::new(&token, &dev_id);
                            let (acc_res, pl_res) = tokio::join!(
                                lane_client.get_account(),
                                lane_client.get_playlists()
                            );

                            let user_info = match acc_res {
                                Ok(acc) => Some(acc),
                                Err(e) => {
                                    log::warn!("Could not fetch full account after auth: {e}");
                                    res.user
                                }
                            };

                            let playlists_count = match pl_res {
                                Ok(ref pls) => {
                                    for pl in pls {
                                        if pl.playlist_id == "lane_likes" {
                                            sessions.update_liked_tracks(&pl.playlist_tracks_ids).await;
                                        }
                                    }
                                    pls.len()
                                }
                                Err(_) => user_info.as_ref().map(|u| u.user_playlists.len() + 1).unwrap_or(0),
                            };

                            let user_lang = user_info
                                .as_ref()
                                .map(|u| Language::from_code(u.interface_language.as_deref()))
                                .unwrap_or(lang);

                            let user_name = user_info
                                .as_ref()
                                .and_then(|u| u.displayed_name.clone().or_else(|| u.user_name.clone()))
                                .unwrap_or_else(|| "User".to_string());

                            sessions.save_session(user_id, dev_id.clone(), token, user_info.clone()).await;

                            let success_header = Messages::auth_success(user_lang, &user_name);

                            let (full_text, markup) = if let Some(ref u) = user_info {
                                let profile_card = Messages::profile_card(
                                    user_lang,
                                    u.displayed_name.as_deref().unwrap_or("—"),
                                    u.user_name.as_deref().unwrap_or("—"),
                                    u.lane_id.as_deref().unwrap_or("—"),
                                    u.country_code.as_deref().unwrap_or("—"),
                                    u.interface_language.as_deref().unwrap_or("—"),
                                    playlists_count,
                                    u.equipped_badge_id.as_deref().unwrap_or("—"),
                                );

                                let text = format!("{success_header}\n\n{profile_card}");
                                let markup = ReplyMarkup::from_buttons(&[
                                    vec![
                                        Button::data(Messages::btn_playlists(user_lang), b"user_playlists"),
                                        Button::data(Messages::btn_refresh(user_lang), b"refresh_profile"),
                                    ],
                                    vec![Button::data(Messages::btn_relogin(user_lang), b"action_login")],
                                ]);
                                (text, markup)
                            } else {
                                let markup = ReplyMarkup::from_buttons(&[
                                    vec![
                                        Button::data(Messages::btn_playlists(user_lang), b"user_playlists"),
                                        Button::data(Messages::btn_profile(user_lang), b"refresh_profile"),
                                    ],
                                ]);
                                (success_header, markup)
                            };

                            let _ = msg.respond(InputMessage::new().html(full_text).reply_markup(markup)).await;
                            return;
                        }
                    }
                    Ok(None) => {}
                    Err(e) => {
                        log::warn!("Auth poll error: {e}");
                    }
                }
            }
            log::debug!("Auth polling timed out for deviceId: {dev_id}");
            let _ = msg.respond(InputMessage::new().html(Messages::auth_timeout(lang))).await;
        });
    }

    async fn cmd_profile(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                msg.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                return Ok(());
            }
        };

        let status_msg = msg
            .respond(InputMessage::new().html(Messages::loading_profile(lang)))
            .await?;

        let (acc_res, pl_res) = tokio::join!(
            lane_client.get_account(),
            lane_client.get_playlists()
        );

        match acc_res {
            Ok(user) => {
                self.sessions.update_user_profile(user_id, user.clone()).await;

                let playlists_count = match pl_res {
                    Ok(pls) => pls.len(),
                    Err(_) => user.user_playlists.len() + 1,
                };

                let card = Messages::profile_card(
                    lang,
                    user.displayed_name.as_deref().unwrap_or("—"),
                    user.user_name.as_deref().unwrap_or("—"),
                    user.lane_id.as_deref().unwrap_or("—"),
                    user.country_code.as_deref().unwrap_or("—"),
                    user.interface_language.as_deref().unwrap_or("—"),
                    playlists_count,
                    user.equipped_badge_id.as_deref().unwrap_or("—"),
                );

                let markup = ReplyMarkup::from_buttons(&[
                    vec![
                        Button::data(Messages::btn_refresh(lang), b"refresh_profile"),
                        Button::data(Messages::btn_playlists(lang), b"user_playlists"),
                    ],
                    vec![Button::data(Messages::btn_relogin(lang), b"action_login")],
                ]);

                status_msg.edit(InputMessage::new().html(card).reply_markup(markup)).await?;
            }
            Err(e) => {
                status_msg.edit(InputMessage::new().html(Messages::api_error(lang, &e))).await?;
            }
        }
        Ok(())
    }

    async fn cmd_playlists(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                msg.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                return Ok(());
            }
        };

        let status_msg = msg
            .respond(InputMessage::new().html(Messages::loading_playlists(lang)))
            .await?;

        match lane_client.get_playlists().await {
            Ok(playlists) => {
                for pl in &playlists {
                    if pl.playlist_id == "lane_likes" {
                        self.sessions.update_liked_tracks(&pl.playlist_tracks_ids).await;
                    }
                }

                if playlists.is_empty() {
                    let markup = ReplyMarkup::from_buttons(&[
                        vec![Button::data(Messages::btn_new_playlist(lang), b"prompt_create_pl")],
                    ]);
                    status_msg
                        .edit(InputMessage::new().text(if lang == Language::Ru {
                            "У вас нет плейлистов."
                        } else {
                            "You have no playlists."
                        }).reply_markup(markup))
                        .await?;
                    return Ok(());
                }

                let header = Messages::playlists_title(lang, playlists.len());
                let mut list_body = String::new();
                let mut buttons = Vec::new();

                for (idx, pl) in playlists.iter().enumerate() {
                    let num = idx + 1;
                    let tracks_count = pl.playlist_tracks_ids.len();
                    let icon = if pl.playlist_id == "lane_likes" { "❤️" } else { "📁" };
                    list_body.push_str(&format!(
                        "<b>{num}.</b> {icon} <b>{}</b> ({tracks_count} треков)\n",
                        pl.playlist_name
                    ));

                    let btn_text = format!("{icon} #{num} {}", Messages::truncate_str(&pl.playlist_name, 24));
                    let cb_data = format!("pl:{}:1", pl.playlist_id);
                    buttons.push(vec![Button::data(btn_text, cb_data.as_bytes())]);
                }

                buttons.push(vec![
                    Button::data(Messages::btn_new_playlist(lang), b"prompt_create_pl"),
                    Button::data(Messages::btn_refresh(lang), b"user_playlists"),
                ]);

                let full_text = format!("{header}\n\n{list_body}");
                let markup = ReplyMarkup::from_buttons(&buttons);
                status_msg.edit(InputMessage::new().html(full_text).reply_markup(markup)).await?;
            }
            Err(e) => {
                status_msg.edit(InputMessage::new().html(Messages::api_error(lang, &e))).await?;
            }
        }

        Ok(())
    }

    async fn prime_liked_tracks_if_needed(&self, client: &LaneApiClient) {
        if self.sessions.is_liked_empty().await {
            if let Ok(playlists) = client.get_playlists().await {
                for pl in &playlists {
                    if pl.playlist_id == "lane_likes" {
                        self.sessions.update_liked_tracks(&pl.playlist_tracks_ids).await;
                    }
                }
            }
        }
    }

    async fn cmd_create_playlist(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
        name: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                msg.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                return Ok(());
            }
        };

        let status_text = match lang {
            Language::Ru => format!("⏳ <i>Создаём плейлист «{name}» на серверах Lane...</i>"),
            Language::En => format!("⏳ <i>Creating playlist «{name}» on Lane servers...</i>"),
        };
        let status_msg = msg.respond(InputMessage::new().html(status_text)).await?;

        let creator_lid = if let Some(sess) = self.sessions.get_session(user_id).await {
            sess.user.and_then(|u| u.lane_id).unwrap_or_default()
        } else {
            String::new()
        };

        match lane_client.create_playlist(name, "", &creator_lid).await {
            Ok(_) => {
                let success_text = Messages::playlist_created_success(lang, name);
                let markup = ReplyMarkup::from_buttons(&[
                    vec![Button::data(Messages::btn_playlists(lang), b"user_playlists")],
                ]);
                status_msg.edit(InputMessage::new().html(success_text).reply_markup(markup)).await?;
            }
            Err(e) => {
                status_msg.edit(InputMessage::new().html(Messages::api_error(lang, &e))).await?;
            }
        }
        Ok(())
    }

    async fn cmd_track_card(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
        song_id: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                msg.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                return Ok(());
            }
        };

        let status_msg = msg
            .respond(InputMessage::new().html(Messages::loading_track(lang)))
            .await?;

        self.prime_liked_tracks_if_needed(&lane_client).await;

        match self.sessions.get_or_fetch_track(&lane_client, song_id).await {
            Some(track) => {
                let (card_text, markup) = self.render_track_card(&track, user_id, lang).await;
                status_msg.edit(InputMessage::new().html(card_text).reply_markup(markup)).await?;
            }
            None => {
                let not_found = match lang {
                    Language::Ru => format!("❌ Трек с ID <code>{song_id}</code> не найден."),
                    Language::En => format!("❌ Track with ID <code>{song_id}</code> was not found."),
                };
                status_msg.edit(InputMessage::new().html(not_found)).await?;
            }
        }
        Ok(())
    }

    async fn render_track_card(
        &self,
        track: &LaneTrack,
        user_id: i64,
        lang: Language,
    ) -> (String, ReplyMarkup) {
        let is_liked = self.sessions.is_track_liked(&track.song_id).await;
        let card_text = Messages::track_card(lang, track);

        let like_cb = if is_liked {
            format!("unlike:{}", track.song_id)
        } else {
            format!("like:{}", track.song_id)
        };
        let like_btn = Button::data(Messages::btn_like(lang, is_liked), like_cb.as_bytes());
        let dl_cb = format!("dl:{}", track.song_id);
        let dl_btn = Button::data(Messages::btn_download(lang), dl_cb.as_bytes());

        let pl_cb = format!("add_to_pl:{}", track.song_id);
        let pl_btn = Button::data(Messages::btn_add_to_playlist(lang), pl_cb.as_bytes());

        let mut rows = vec![
            vec![like_btn, dl_btn],
            vec![pl_btn],
        ];

        if self.sessions.get_last_view(user_id).await.is_some() {
            rows.push(vec![Button::data(Messages::btn_back(lang), b"tc_back")]);
        }

        (card_text, ReplyMarkup::from_buttons(&rows))
    }

    async fn render_track_card_inline_markup(
        &self,
        track: &LaneTrack,
        _user_id: i64,
        lang: Language,
    ) -> ReplyMarkup {
        let is_liked = self.sessions.is_track_liked(&track.song_id).await;
        let like_cb = if is_liked {
            format!("unlike:{}", track.song_id)
        } else {
            format!("like:{}", track.song_id)
        };
        let like_btn = Button::data(Messages::btn_like(lang, is_liked), like_cb.as_bytes());
        let dl_cb = format!("dl:{}", track.song_id);
        let dl_btn = Button::data(Messages::btn_download(lang), dl_cb.as_bytes());

        let pl_cb = format!("add_to_pl:{}", track.song_id);
        let pl_btn = Button::data(Messages::btn_add_to_playlist(lang), pl_cb.as_bytes());

        ReplyMarkup::from_buttons(&[
            vec![like_btn, dl_btn],
            vec![pl_btn],
        ])
    }

    async fn edit_query_message(&self, query: &CallbackQuery, new_msg: InputMessage) {
        if let Err(e) = query.answer().edit(new_msg).await {
            log::warn!("Failed to edit query message: {e}");
        }
    }

    async fn cmd_search(
        &self,
        msg: &Message,
        user_id: i64,
        lang: Language,
        query: &str,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                msg.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                return Ok(());
            }
        };

        let status_msg = msg
            .respond(InputMessage::new().html(Messages::searching(lang, query)))
            .await?;

        match lane_client.search(query).await {
            Ok(tracks) => {
                if tracks.is_empty() {
                    status_msg.edit(InputMessage::new().html(Messages::no_results(lang, query))).await?;
                    return Ok(());
                }

                let search_id = format!("{:x}", SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis());
                self.sessions.store_search(search_id.clone(), query.to_string(), tracks.clone()).await;
                self.sessions.set_last_view(user_id, format!("sp:{search_id}:1")).await;

                let (page_text, markup) = self.render_search_page(&search_id, query, &tracks, 1, lang);
                status_msg.edit(InputMessage::new().html(page_text).reply_markup(markup)).await?;
            }
            Err(e) => {
                status_msg.edit(InputMessage::new().text(format!("❌ Ошибка поиска: {e}"))).await?;
            }
        }

        Ok(())
    }

    fn render_search_page(
        &self,
        search_id: &str,
        query: &str,
        tracks: &[LaneTrack],
        page: usize,
        lang: Language,
    ) -> (String, ReplyMarkup) {
        let total_items = tracks.len();
        let total_pages = (total_items + PAGE_SIZE - 1) / PAGE_SIZE;
        let page = page.max(1).min(total_pages);

        let start_idx = (page - 1) * PAGE_SIZE;
        let end_idx = (start_idx + PAGE_SIZE).min(total_items);
        let slice = &tracks[start_idx..end_idx];

        let mut body = Messages::search_header(lang, query, page, total_pages, total_items);
        body.push('\n');

        let mut info_row = Vec::new();
        let mut dl_row = Vec::new();

        for (i, t) in slice.iter().enumerate() {
            let item_num = start_idx + i + 1;
            let summary = Messages::track_summary(
                item_num,
                &t.display_title(),
                &t.display_artist(),
                &t.display_album(),
                &t.display_duration(),
                &t.platform,
            );
            body.push_str(&summary);
            body.push('\n');

            let info_label = format!("ℹ️ #{item_num}");
            let info_cb = format!("tc:{}", t.song_id);
            info_row.push(Button::data(info_label, info_cb.as_bytes()));

            let btn_label = Messages::download_button(item_num);
            let cb_data = format!("dl:{}", t.song_id);
            dl_row.push(Button::data(btn_label, cb_data.as_bytes()));
        }

        let mut all_rows = Vec::new();
        if !info_row.is_empty() {
            all_rows.push(info_row);
        }
        if !dl_row.is_empty() {
            all_rows.push(dl_row);
        }

        // Pagination row if multiple pages
        if total_pages > 1 {
            let mut page_row = Vec::new();
            if page > 1 {
                let prev_cb = format!("sp:{search_id}:{}", page - 1);
                page_row.push(Button::data("⬅️", prev_cb.as_bytes()));
            }

            for p in 1..=total_pages {
                let label = if p == page {
                    format!("• {p} •")
                } else {
                    format!("{p}")
                };
                let cb = format!("sp:{search_id}:{p}");
                page_row.push(Button::data(label, cb.as_bytes()));
            }

            if page < total_pages {
                let next_cb = format!("sp:{search_id}:{}", page + 1);
                page_row.push(Button::data("➡️", next_cb.as_bytes()));
            }
            all_rows.push(page_row);
        }

        (body, ReplyMarkup::from_buttons(&all_rows))
    }

    pub async fn handle_callback_query(
        &self,
        query: CallbackQuery,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let data_bytes = query.data();
        let data = String::from_utf8_lossy(data_bytes);
        let user_id = query.sender_id().bare_id_unchecked();
        let lang = self.get_lang_for_user(user_id).await;

        if data == "refresh_profile" {
            let _ = query.answer().text("⏳ Обновляем профиль у Lane...").send().await;
            if let Ok(msg) = query.load_message().await {
                let _ = self.cmd_profile(&msg, user_id, lang).await;
            }
        } else if data == "user_playlists" {
            let _ = query.answer().text("⏳ Загружаем плейлисты у Lane...").send().await;
            if let Ok(msg) = query.load_message().await {
                let _ = self.cmd_playlists(&msg, user_id, lang).await;
            }
        } else if data == "action_login" {
            let _ = query.answer().send().await;
            if let Ok(msg) = query.load_message().await {
                let _ = self.cmd_login(&msg, user_id, lang).await;
            }
        } else if data == "prompt_create_pl" {
            let _ = query.answer().send().await;
            if let Ok(msg) = query.load_message().await {
                let text = Messages::playlist_create_prompt(lang);
                let markup = ReplyMarkup::from_buttons(&[
                    vec![Button::data(Messages::btn_playlists(lang), b"user_playlists")],
                ]);
                let _ = msg.respond(InputMessage::new().html(text).reply_markup(markup)).await;
            }
        } else if data.starts_with("sp:") {
            // Search pagination: sp:<search_id>:<page>
            let parts: Vec<&str> = data.split(':').collect();
            if parts.len() == 3 {
                let search_id = parts[1];
                let page: usize = parts[2].parse().unwrap_or(1);
                let _ = query.answer().send().await;

                if let Some(cached) = self.sessions.get_search(search_id).await {
                    self.sessions.set_last_view(user_id, format!("sp:{search_id}:{page}")).await;
                    let (text, markup) = self.render_search_page(search_id, &cached.query, &cached.tracks, page, lang);
                    let _ = query.answer().edit(InputMessage::new().html(text).reply_markup(markup)).await;
                } else {
                    let _ = query.answer().alert("Поиск устарел. Повторите /search").send().await;
                }
            }
        } else if data.starts_with("pl:") {
            // Playlist pagination: pl:<playlist_id>:<page>
            let parts: Vec<&str> = data.split(':').collect();
            if parts.len() == 3 {
                let playlist_id = parts[1];
                let page: i64 = parts[2].parse().unwrap_or(1);
                let _ = query.answer().text("⏳ Загружаем страницу плейлиста у Lane...").send().await;

                if let Some(lane_client) = self.sessions.get_client(user_id).await {
                    if let Ok(msg) = query.load_message().await {
                        let _ = msg.edit(InputMessage::new().html(Messages::loading_playlist_page(lang))).await;
                        match lane_client.get_playlist_tracks(playlist_id, page, PAGE_SIZE as i64).await {
                            Ok(tracks_page) => {
                                self.sessions.set_last_view(user_id, format!("pl:{playlist_id}:{page}")).await;
                                self.sessions.cache_tracks(&tracks_page.items).await;
                                let (text, markup) = self.render_playlist_page(playlist_id, &tracks_page, lang);
                                let _ = msg.edit(InputMessage::new().html(text).reply_markup(markup)).await;
                            }
                            Err(e) => {
                                let _ = msg.edit(InputMessage::new().html(Messages::api_error(lang, &e))).await;
                            }
                        }
                    }
                }
            }
        } else if data.starts_with("tc:") {
            // View track card: tc:<song_id>
            let song_id = data.trim_start_matches("tc:");
            let _ = query.answer().text("⏳ Загружаем карточку трека...").send().await;

            if let Some(lane_client) = self.sessions.get_client(user_id).await {
                self.prime_liked_tracks_if_needed(&lane_client).await;
                match self.sessions.get_or_fetch_track(&lane_client, song_id).await {
                    Some(track) => {
                        let (card_text, markup) = self.render_track_card(&track, user_id, lang).await;
                        self.edit_query_message(&query, InputMessage::new().html(card_text).reply_markup(markup)).await;
                    }
                    None => {
                        let not_found = match lang {
                            Language::Ru => "❌ Трек не найден.".to_string(),
                            Language::En => "❌ Track not found.".to_string(),
                        };
                        self.edit_query_message(&query, InputMessage::new().html(not_found)).await;
                    }
                }
            }
        } else if data == "tc_back" {
            let _ = query.answer().send().await;
            if let Some(last_view) = self.sessions.get_last_view(user_id).await {
                if last_view.starts_with("sp:") {
                    let parts: Vec<&str> = last_view.split(':').collect();
                    if parts.len() == 3 {
                        let search_id = parts[1];
                        let page: usize = parts[2].parse().unwrap_or(1);
                        if let Some(cached) = self.sessions.get_search(search_id).await {
                            let (text, markup) = self.render_search_page(search_id, &cached.query, &cached.tracks, page, lang);
                            self.edit_query_message(&query, InputMessage::new().html(text).reply_markup(markup)).await;
                        }
                    }
                } else if last_view.starts_with("pl:") {
                    let parts: Vec<&str> = last_view.split(':').collect();
                    if parts.len() == 3 {
                        let playlist_id = parts[1];
                        let page: i64 = parts[2].parse().unwrap_or(1);
                        if let Some(lane_client) = self.sessions.get_client(user_id).await {
                            if let Ok(tracks_page) = lane_client.get_playlist_tracks(playlist_id, page, PAGE_SIZE as i64).await {
                                self.sessions.cache_tracks(&tracks_page.items).await;
                                let (text, markup) = self.render_playlist_page(playlist_id, &tracks_page, lang);
                                self.edit_query_message(&query, InputMessage::new().html(text).reply_markup(markup)).await;
                            }
                        }
                    }
                }
            }
        } else if data.starts_with("like:") {
            // Add track to lane_likes
            let song_id = data.trim_start_matches("like:");
            let toast = match lang {
                Language::Ru => "❤️ Добавлено в Любимые!",
                Language::En => "❤️ Added to Liked!",
            };
            if let Some(lane_client) = self.sessions.get_client(user_id).await {
                match lane_client.add_track_to_playlist("lane_likes", song_id).await {
                    Ok(()) => {
                        self.sessions.set_track_liked(song_id, true).await;
                        let _ = query.answer().text(toast).send().await;
                        if let Some(track) = self.sessions.get_or_fetch_track(&lane_client, song_id).await {
                            let (card_text, markup) = self.render_track_card(&track, user_id, lang).await;
                            self.edit_query_message(&query, InputMessage::new().html(card_text).reply_markup(markup)).await;
                        }
                    }
                    Err(e) => {
                        let _ = query.answer().alert("Ошибка добавления в любимые").send().await;
                        self.edit_query_message(&query, InputMessage::new().html(Messages::api_error(lang, &e))).await;
                    }
                }
            }
        } else if data.starts_with("unlike:") {
            // Remove track from lane_likes
            let song_id = data.trim_start_matches("unlike:");
            let toast = match lang {
                Language::Ru => "💔 Удалено из Любимых",
                Language::En => "💔 Removed from Liked",
            };
            if let Some(lane_client) = self.sessions.get_client(user_id).await {
                match lane_client.remove_track_from_playlist("lane_likes", song_id).await {
                    Ok(()) => {
                        self.sessions.set_track_liked(song_id, false).await;
                        let _ = query.answer().text(toast).send().await;
                        if let Some(track) = self.sessions.get_or_fetch_track(&lane_client, song_id).await {
                            let (card_text, markup) = self.render_track_card(&track, user_id, lang).await;
                            self.edit_query_message(&query, InputMessage::new().html(card_text).reply_markup(markup)).await;
                        }
                    }
                    Err(e) => {
                        let _ = query.answer().alert("Ошибка удаления из любимых").send().await;
                        self.edit_query_message(&query, InputMessage::new().html(Messages::api_error(lang, &e))).await;
                    }
                }
            }
        } else if data.starts_with("add_to_pl:") {
            // Prompt user with playlists list to add track
            let song_id = data.trim_start_matches("add_to_pl:");
            self.sessions.set_active_track(user_id, song_id).await;
            let _ = query.answer().text("⏳ Загружаем список плейлистов...").send().await;

            if let Some(lane_client) = self.sessions.get_client(user_id).await {
                match lane_client.get_playlists().await {
                    Ok(playlists) => {
                        let track = self.sessions.get_or_fetch_track(&lane_client, song_id).await;
                        let title = track.as_ref().map(|t| t.display_title()).unwrap_or_else(|| song_id.to_string());
                        let prompt_text = Messages::playlist_add_prompt(lang, &title);

                        let mut buttons = Vec::new();
                        for pl in &playlists {
                            let icon = if pl.playlist_id == "lane_likes" { "❤️" } else { "📁" };
                            let label = format!("{icon} {}", Messages::truncate_str(&pl.playlist_name, 26));
                            let cb = format!("pla:{}", pl.playlist_id);
                            buttons.push(vec![Button::data(label, cb.as_bytes())]);
                        }
                        buttons.push(vec![
                            Button::data(Messages::btn_new_playlist(lang), b"prompt_create_pl"),
                            Button::data(Messages::btn_back(lang), format!("tc:{song_id}").as_bytes()),
                        ]);

                        let markup = ReplyMarkup::from_buttons(&buttons);
                        self.edit_query_message(&query, InputMessage::new().html(prompt_text).reply_markup(markup)).await;
                    }
                    Err(e) => {
                        self.edit_query_message(&query, InputMessage::new().html(Messages::api_error(lang, &e))).await;
                    }
                }
            }
        } else if data.starts_with("pla:") {
            // Target playlist selected: pla:<playlist_id>
            let playlist_id = data.trim_start_matches("pla:");
            if let Some(song_id) = self.sessions.get_active_track(user_id).await {
                let _ = query.answer().text("⏳ Добавляем в плейлист...").send().await;
                if let Some(lane_client) = self.sessions.get_client(user_id).await {
                    match lane_client.add_track_to_playlist(playlist_id, &song_id).await {
                        Ok(()) => {
                            if playlist_id == "lane_likes" {
                                self.sessions.set_track_liked(&song_id, true).await;
                            }
                            if let Some(track) = self.sessions.get_or_fetch_track(&lane_client, &song_id).await {
                                let (card_text, markup) = self.render_track_card(&track, user_id, lang).await;
                                let notice = match lang {
                                    Language::Ru => "✅ <b>Трек успешно добавлен в плейлист!</b>\n\n",
                                    Language::En => "✅ <b>Track successfully added to playlist!</b>\n\n",
                                };
                                let full_text = format!("{notice}{card_text}");
                                self.edit_query_message(&query, InputMessage::new().html(full_text).reply_markup(markup)).await;
                            }
                        }
                        Err(e) => {
                            self.edit_query_message(&query, InputMessage::new().html(Messages::api_error(lang, &e))).await;
                        }
                    }
                }
            } else {
                let _ = query.answer().alert("Сессия добавления трека устарела").send().await;
            }
        } else if data.starts_with("dl:") {
            // Download track: dl:<song_id>
            let song_id = data.trim_start_matches("dl:");
            if query.is_from_inline() {
                let toast = match lang {
                    Language::Ru => "📥 Отправляю трек вам в личные сообщения с ботом...",
                    Language::En => "📥 Sending track to your private messages with bot...",
                };
                let _ = query.answer().text(toast).send().await;
                self.download_and_send_track(None, user_id, song_id, lang).await?;
            } else {
                let cached_track = if let Some(lane_client) = self.sessions.get_client(user_id).await {
                    self.sessions.get_or_fetch_track(&lane_client, song_id).await
                } else {
                    self.sessions.find_cached_track(song_id).await
                };
                let popup_text = if let Some(t) = &cached_track {
                    format!("⏳ {} — {}", t.display_artist(), t.display_title())
                } else if lang == Language::Ru {
                    "⏳ Загружаем трек...".to_string()
                } else {
                    "⏳ Downloading track...".to_string()
                };
                let _ = query.answer().text(popup_text).send().await;

                if let Ok(msg) = query.load_message().await {
                    self.download_and_send_track(Some(&msg), user_id, song_id, lang).await?;
                }
            }
        }

        Ok(())
    }

    fn render_playlist_page(
        &self,
        playlist_id: &str,
        page_data: &crate::lane_client::PlaylistTracksPage,
        lang: Language,
    ) -> (String, ReplyMarkup) {
        let total_items = page_data.total_items;
        let total_pages = page_data.total_pages.max(1);
        let current_page = page_data.page.max(1);

        let pl_title = if playlist_id == "lane_likes" {
            "Любимые треки"
        } else {
            playlist_id
        };

        let mut body = Messages::playlist_header(lang, pl_title, current_page, total_pages, total_items);
        body.push('\n');

        let mut info_row = Vec::new();
        let mut dl_row = Vec::new();
        let start_idx = ((current_page - 1) * (PAGE_SIZE as i64)) as usize;

        for (i, t) in page_data.items.iter().enumerate() {
            let num = start_idx + i + 1;
            body.push_str(&Messages::track_summary(
                num,
                &t.display_title(),
                &t.display_artist(),
                &t.display_album(),
                &t.display_duration(),
                &t.platform,
            ));
            body.push('\n');

            let info_label = format!("ℹ️ #{num}");
            let info_cb = format!("tc:{}", t.song_id);
            info_row.push(Button::data(info_label, info_cb.as_bytes()));

            let btn_label = Messages::download_button(num);
            let cb = format!("dl:{}", t.song_id);
            dl_row.push(Button::data(btn_label, cb.as_bytes()));
        }

        let mut all_rows = Vec::new();
        if !info_row.is_empty() {
            all_rows.push(info_row);
        }
        if !dl_row.is_empty() {
            all_rows.push(dl_row);
        }

        if total_pages > 1 {
            let mut page_row = Vec::new();
            if current_page > 1 {
                let prev_cb = format!("pl:{playlist_id}:{}", current_page - 1);
                page_row.push(Button::data("⬅️", prev_cb.as_bytes()));
            }

            for p in 1..=total_pages {
                let label = if p == current_page {
                    format!("• {p} •")
                } else {
                    format!("{p}")
                };
                let cb = format!("pl:{playlist_id}:{p}");
                page_row.push(Button::data(label, cb.as_bytes()));
            }

            if current_page < total_pages {
                let next_cb = format!("pl:{playlist_id}:{}", current_page + 1);
                page_row.push(Button::data("➡️", next_cb.as_bytes()));
            }
            all_rows.push(page_row);
        }

        all_rows.push(vec![Button::data("🔙 К плейлистам", b"user_playlists")]);

        (body, ReplyMarkup::from_buttons(&all_rows))
    }

    async fn download_and_send_track(
        &self,
        msg: Option<&Message>,
        user_id: i64,
        song_id: &str,
        lang: Language,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let lane_client = match self.sessions.get_client(user_id).await {
            Some(c) => c,
            None => {
                if let Some(m) = msg {
                    m.respond(InputMessage::new().html(Messages::not_authorized(lang))).await?;
                }
                return Ok(());
            }
        };

        // Check if track metadata is in cache or fetch it
        let cached_track = self.sessions.get_or_fetch_track(&lane_client, song_id).await;
        let track_title = cached_track
            .as_ref()
            .map(|t| t.display_title())
            .unwrap_or_else(|| format!("Track {song_id}"));
        let track_artist = cached_track
            .as_ref()
            .map(|t| t.display_artist())
            .unwrap_or_else(|| "Lane Music".to_string());
        let track_album = cached_track
            .as_ref()
            .map(|t| t.display_album())
            .unwrap_or_else(|| "—".to_string());
        let track_duration = cached_track
            .as_ref()
            .map(|t| Duration::from_secs(t.duration_seconds()))
            .unwrap_or_else(|| Duration::from_secs(180));

        let display_name = format!("{track_artist} — {track_title}");

        // 1. Initial status: Resolving stream on Lane
        let user_peer = grammers_session::types::PeerId::user_unchecked(user_id).to_ambient_ref();
        let status_msg = if let Some(m) = msg {
            m.respond(InputMessage::new().html(Messages::resolving_stream(lang, &display_name)))
                .await
                .ok()
        } else {
            self.client
                .send_message(user_peer, InputMessage::new().html(Messages::resolving_stream(lang, &display_name)))
                .await
                .ok()
        };

        // 2. Resolve stream URL
        let mut stream_url: Option<String> = None;

        if let Ok(res) = lane_client.get_stream(song_id, "search", "HIGH").await {
            stream_url = Some(res.url);
        } else if let Ok(res) = lane_client.get_stream(song_id, "search", "BASIC").await {
            stream_url = Some(res.url);
        }

        if stream_url.is_none() {
            if let Some(t) = &cached_track {
                if let Some(sc) = &t.sc_data {
                    if let Some(u) = &sc.stream_url {
                        stream_url = Some(u.clone());
                    }
                }
            }
        }

        let resolved_url = match stream_url {
            Some(url) => url,
            None => {
                log::warn!("Stream resolution failed for track {song_id}");
                if let Some(sm) = status_msg {
                    let _ = sm
                        .edit(InputMessage::new().html(Messages::download_error(
                            lang,
                            &display_name,
                            if lang == Language::Ru {
                                "Аудиопоток недоступен на серверах Lane"
                            } else {
                                "Audio stream not available on Lane servers"
                            },
                        )))
                        .await;
                }
                return Ok(());
            }
        };

        // 3. Status: Downloading audio from Lane
        if let Some(ref sm) = status_msg {
            let _ = sm
                .edit(InputMessage::new().html(Messages::downloading_status(lang, &display_name)))
                .await;
        }

        let raw_audio_bytes = match lane_client.download_audio(&resolved_url).await {
            Ok(bytes) => bytes,
            Err(e) => {
                if let Some(ref sm) = status_msg {
                    let _ = sm
                        .edit(InputMessage::new().html(Messages::download_error(
                            lang,
                            &display_name,
                            &format!("Сбой загрузки: {e}"),
                        )))
                        .await;
                }
                return Ok(());
            }
        };

        // 4. Status: Uploading to Telegram
        if let Some(ref sm) = status_msg {
            let _ = sm
                .edit(InputMessage::new().html(Messages::uploading_status(lang, &display_name)))
                .await;
        }

        // Download cover image if available
        let mut cover_bytes: Option<Vec<u8>> = None;
        if let Some(t) = &cached_track {
            if let Some(cover_url) = &t.cover_url {
                if let Ok(cb) = lane_client.download_audio(cover_url).await {
                    cover_bytes = Some(cb);
                }
            }
        }

        // Embed ID3v2 metadata & album cover into audio stream
        let final_audio_bytes = {
            use id3::{frame::Picture, frame::PictureType, Tag, TagLike, Version};
            let mut tag = Tag::new();
            tag.set_title(&track_title);
            tag.set_artist(&track_artist);
            if track_album != "—" && !track_album.is_empty() {
                tag.set_album(&track_album);
            }
            tag.set_duration((track_duration.as_secs() * 1000) as u32);

            if let Some(ref cb) = cover_bytes {
                tag.add_frame(Picture {
                    mime_type: "image/jpeg".to_string(),
                    picture_type: PictureType::CoverFront,
                    description: "Cover".to_string(),
                    data: cb.clone(),
                });
            }

            let mut tagged = Vec::new();
            let mut cursor = Cursor::new(&mut tagged);
            if let Err(e) = tag.write_to(&mut cursor, Version::Id3v24) {
                log::debug!("Failed to write ID3v2 tags: {e}");
                raw_audio_bytes
            } else {
                tagged.extend_from_slice(&raw_audio_bytes);
                tagged
            }
        };

        // 5. Upload stream to Telegram via grammers
        let size = final_audio_bytes.len();
        let clean_artist = sanitize_filename(&track_artist);
        let clean_title = sanitize_filename(&track_title);
        let filename = format!("{clean_artist} - {clean_title}.mp3");

        let mut cursor = Cursor::new(final_audio_bytes);
        let uploaded = match self.client.upload_stream(&mut cursor, size, filename).await {
            Ok(up) => up,
            Err(e) => {
                if let Some(ref sm) = status_msg {
                    let _ = sm
                        .edit(InputMessage::new().html(Messages::download_error(
                            lang,
                            &display_name,
                            &format!("Сбой отправки в Telegram: {e}"),
                        )))
                        .await;
                }
                return Ok(());
            }
        };

        // Upload thumbnail for Telegram message if available
        let mut thumb_uploaded = None;
        if let Some(ref cb) = cover_bytes {
            let csize = cb.len();
            let mut ccursor = Cursor::new(cb.clone());
            if let Ok(cup) = self.client.upload_stream(&mut ccursor, csize, "thumb.jpg".to_string()).await {
                thumb_uploaded = Some(cup);
            }
        }

        // 6. Delete old status message as explicitly requested
        if let Some(sm) = status_msg {
            let _ = sm.delete().await;
        }

        // 7. Send audio message with full metadata
        let mut audio_msg = InputMessage::new()
            .document(uploaded)
            .mime_type("audio/mpeg")
            .attribute(Attribute::Audio {
                duration: track_duration,
                title: Some(track_title),
                performer: Some(track_artist),
            });

        if let Some(thumb) = thumb_uploaded {
            audio_msg = audio_msg.thumbnail(thumb);
        }

        if let Some(m) = msg {
            m.respond(audio_msg).await?;
        } else {
            self.client.send_message(user_peer, audio_msg).await?;
        }
        Ok(())
    }

    pub async fn handle_inline_query(
        &self,
        query: InlineQuery,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let raw_text = query.text().trim();
        let user_id = query.sender_id().bare_id_unchecked();
        let lang = self.get_lang_for_user(user_id).await;
        let lane_client = self.sessions.get_client(user_id).await;

        let lower = raw_text.to_lowercase();

        // 1. Empty or help
        if raw_text.is_empty() || lower == "help" || lower == "/help" || lower == "?" {
            let articles = vec![
                Article::new(
                    Messages::inline_help_search_title(lang),
                    InputMessage::new().html(Messages::inline_help_search_text(lang)),
                )
                .description(Messages::inline_help_search_desc(lang))
                .id("help_search"),

                Article::new(
                    Messages::inline_help_playlists_title(lang),
                    InputMessage::new().html(Messages::inline_help_playlists_text(lang)),
                )
                .description(Messages::inline_help_playlists_desc(lang))
                .id("help_playlists"),

                Article::new(
                    Messages::inline_help_profile_title(lang),
                    InputMessage::new().html(Messages::inline_help_profile_text(lang)),
                )
                .description(Messages::inline_help_profile_desc(lang))
                .id("help_profile"),

                Article::new(
                    Messages::inline_help_track_title(lang),
                    InputMessage::new().html(Messages::inline_help_track_text(lang)),
                )
                .description(Messages::inline_help_track_desc(lang))
                .id("help_track"),
            ];

            let mut answer = query.answer(articles).cache_time(5).private();
            if lane_client.is_none() {
                answer = answer.switch_pm(Messages::inline_not_authorized_title(lang), "auth");
            }
            let _ = answer.send().await;
            return Ok(());
        }

        // 2. Playlists: "playlists", "/playlists", "pl", "/pl"
        if lower == "playlists" || lower == "/playlists" || lower == "pl" || lower == "/pl" {
            let client = match lane_client {
                Some(c) => c,
                None => {
                    let article = Article::new(
                        Messages::inline_not_authorized_title(lang),
                        InputMessage::new().html(Messages::not_authorized(lang)),
                    )
                    .description(Messages::inline_not_authorized_desc(lang))
                    .id("auth_required");
                    let _ = query
                        .answer(vec![article])
                        .switch_pm(Messages::inline_not_authorized_title(lang), "auth")
                        .cache_time(0)
                        .private()
                        .send()
                        .await;
                    return Ok(());
                }
            };

            match client.get_playlists().await {
                Ok(playlists) => {
                    for pl in &playlists {
                        if pl.playlist_id == "lane_likes" {
                            self.sessions.update_liked_tracks(&pl.playlist_tracks_ids).await;
                        }
                    }

                    if playlists.is_empty() {
                        let empty_text = if lang == Language::Ru {
                            "📂 <b>У вас нет плейлистов</b>\n\nСоздайте плейлист командой <code>/newplaylist &lt;название&gt;</code> в личных сообщениях с ботом."
                        } else {
                            "📂 <b>You have no playlists</b>\n\nCreate a playlist with <code>/newplaylist &lt;name&gt;</code> in private chat with bot."
                        };
                        let article = Article::new("📂 Плейлисты отсутствуют", InputMessage::new().html(empty_text))
                            .description(if lang == Language::Ru { "Создайте плейлист через бота" } else { "Create playlist via bot" })
                            .id("empty_playlists");
                        let _ = query.answer(vec![article]).cache_time(5).private().send().await;
                        return Ok(());
                    }

                    let mut articles = Vec::new();
                    for (i, pl) in playlists.iter().enumerate() {
                        let icon = if pl.playlist_id == "lane_likes" { "❤️" } else { "📁" };
                        let title = format!("{icon} {}", pl.playlist_name);
                        let count = pl.playlist_tracks_ids.len();
                        let desc = format!("Всего треков: {count}");

                        let msg_text = format!(
                            "{icon} <b>Плейлист:</b> {}\n🎵 <b>Количество треков:</b> {}\n🔑 <b>ID:</b> <code>{}</code>",
                            pl.playlist_name, count, pl.playlist_id
                        );
                        let markup = ReplyMarkup::from_buttons(&[
                            vec![Button::data("📂 Открыть плейлист", format!("pl:{}:1", pl.playlist_id).as_bytes())],
                        ]);

                        let mut art = Article::new(title, InputMessage::new().html(msg_text).reply_markup(markup))
                            .description(desc)
                            .id(format!("pl_{i}_{}", pl.playlist_id));
                        if let Some(ref img) = pl.playlist_image_url {
                            if img.starts_with("http") {
                                art = art.thumb_url(img);
                            }
                        }
                        articles.push(art);
                    }

                    let _ = query.answer(articles).cache_time(5).private().send().await;
                }
                Err(e) => {
                    log::warn!("Error fetching playlists for inline query: {e}");
                    let err_art = Article::new(
                        "❌ Ошибка загрузки плейлистов",
                        InputMessage::new().html(Messages::api_error(lang, &e)),
                    ).id("pl_err");
                    let _ = query.answer(vec![err_art]).cache_time(0).private().send().await;
                }
            }
            return Ok(());
        }

        // 3. Profile: "profile", "/profile", "me", "/me"
        if lower == "profile" || lower == "/profile" || lower == "me" || lower == "/me" {
            let client = match lane_client {
                Some(c) => c,
                None => {
                    let article = Article::new(
                        Messages::inline_not_authorized_title(lang),
                        InputMessage::new().html(Messages::not_authorized(lang)),
                    )
                    .description(Messages::inline_not_authorized_desc(lang))
                    .id("auth_required");
                    let _ = query
                        .answer(vec![article])
                        .switch_pm(Messages::inline_not_authorized_title(lang), "auth")
                        .cache_time(0)
                        .private()
                        .send()
                        .await;
                    return Ok(());
                }
            };

            let (acc_res, pl_res) = tokio::join!(
                client.get_account(),
                client.get_playlists()
            );

            match acc_res {
                Ok(user) => {
                    self.sessions.update_user_profile(user_id, user.clone()).await;
                    let playlists_count = match pl_res {
                        Ok(ref pls) => {
                            for pl in pls {
                                if pl.playlist_id == "lane_likes" {
                                    self.sessions.update_liked_tracks(&pl.playlist_tracks_ids).await;
                                }
                            }
                            pls.len()
                        }
                        Err(_) => user.user_playlists.len() + 1,
                    };

                    let card = Messages::profile_card(
                        lang,
                        user.displayed_name.as_deref().unwrap_or("—"),
                        user.user_name.as_deref().unwrap_or("—"),
                        user.lane_id.as_deref().unwrap_or("—"),
                        user.country_code.as_deref().unwrap_or("—"),
                        user.interface_language.as_deref().unwrap_or("—"),
                        playlists_count,
                        user.equipped_badge_id.as_deref().unwrap_or("—"),
                    );

                    let markup = ReplyMarkup::from_buttons(&[
                        vec![
                            Button::data(Messages::btn_refresh(lang), b"refresh_profile"),
                            Button::data(Messages::btn_playlists(lang), b"user_playlists"),
                        ],
                    ]);

                    let clean_name = crate::lane_client::clean_invisible(user.displayed_name.as_deref().unwrap_or("User"));
                    let article = Article::new(
                        format!("👤 {}", clean_name),
                        InputMessage::new().html(card).reply_markup(markup),
                    )
                    .description(format!("Username: @{} | Плейлистов: {playlists_count}", user.user_name.as_deref().unwrap_or("—")))
                    .id("user_profile");

                    let _ = query.answer(vec![article]).cache_time(5).private().send().await;
                }
                Err(e) => {
                    let err_art = Article::new(
                        "❌ Ошибка загрузки профиля",
                        InputMessage::new().html(Messages::api_error(lang, &e)),
                    ).id("profile_err");
                    let _ = query.answer(vec![err_art]).cache_time(0).private().send().await;
                }
            }
            return Ok(());
        }

        // 4. Track by ID: "track <song_id>", "/track <song_id>", "song <song_id>"
        if lower.starts_with("track ") || lower.starts_with("/track ") || lower.starts_with("song ") || lower.starts_with("/song ") {
            let song_id = raw_text
                .trim_start_matches("/track")
                .trim_start_matches("track")
                .trim_start_matches("/song")
                .trim_start_matches("song")
                .trim();

            if song_id.is_empty() {
                let article = Article::new(
                    Messages::inline_help_track_title(lang),
                    InputMessage::new().html(Messages::inline_help_track_text(lang)),
                )
                .description("Введите идентификатор трека: track <song_id>")
                .id("track_hint");
                let _ = query.answer(vec![article]).cache_time(5).private().send().await;
                return Ok(());
            }

            let client = match lane_client {
                Some(c) => c,
                None => {
                    let article = Article::new(
                        Messages::inline_not_authorized_title(lang),
                        InputMessage::new().html(Messages::not_authorized(lang)),
                    )
                    .description(Messages::inline_not_authorized_desc(lang))
                    .id("auth_required");
                    let _ = query
                        .answer(vec![article])
                        .switch_pm(Messages::inline_not_authorized_title(lang), "auth")
                        .cache_time(0)
                        .private()
                        .send()
                        .await;
                    return Ok(());
                }
            };

            self.prime_liked_tracks_if_needed(&client).await;

            match self.sessions.get_or_fetch_track(&client, song_id).await {
                Some(track) => {
                    let card_text = Messages::track_card(lang, &track);
                    let markup = self.render_track_card_inline_markup(&track, user_id, lang).await;

                    let title = format!("🎵 {} — {}", track.display_artist(), track.display_title());
                    let desc = format!("💿 {} | ⏱ {} | 🌐 {}", track.display_album(), track.display_duration(), track.platform);

                    let mut art = Article::new(title, InputMessage::new().html(card_text).reply_markup(markup))
                        .description(desc)
                        .id(format!("trk_{song_id}"));
                    if let Some(ref cover) = track.cover_url {
                        if cover.starts_with("http") {
                            art = art.thumb_url(cover);
                        }
                    }

                    let _ = query.answer(vec![art]).cache_time(5).private().send().await;
                }
                None => {
                    let not_found_title = format!("❌ Трек с ID {} не найден", song_id);
                    let art = Article::new(
                        not_found_title,
                        InputMessage::new().text(format!("Track with ID {song_id} not found.")),
                    )
                    .id("track_not_found");
                    let _ = query.answer(vec![art]).cache_time(5).private().send().await;
                }
            }
            return Ok(());
        }

        // 5. Search: "search <query>", "/search <query>", or any direct text query
        let query_str = if lower.starts_with("search ") || lower.starts_with("/search ") {
            raw_text
                .trim_start_matches("/search")
                .trim_start_matches("search")
                .trim()
        } else {
            raw_text
        };

        if query_str.is_empty() {
            let article = Article::new(
                Messages::inline_help_search_title(lang),
                InputMessage::new().html(Messages::inline_help_search_text(lang)),
            )
            .description(Messages::inline_help_search_desc(lang))
            .id("search_hint");
            let _ = query.answer(vec![article]).cache_time(5).private().send().await;
            return Ok(());
        }

        let client = match lane_client {
            Some(c) => c,
            None => {
                let article = Article::new(
                    Messages::inline_not_authorized_title(lang),
                    InputMessage::new().html(Messages::not_authorized(lang)),
                )
                .description(Messages::inline_not_authorized_desc(lang))
                .id("auth_required");
                let _ = query
                    .answer(vec![article])
                    .switch_pm(Messages::inline_not_authorized_title(lang), "auth")
                    .cache_time(0)
                    .private()
                    .send()
                    .await;
                return Ok(());
            }
        };

        self.prime_liked_tracks_if_needed(&client).await;

        match client.search(query_str).await {
            Ok(tracks) => {
                if tracks.is_empty() {
                    let no_res_art = Article::new(
                        Messages::inline_no_results_title(lang, query_str),
                        InputMessage::new().html(Messages::no_results(lang, query_str)),
                    )
                    .description(Messages::inline_no_results_desc(lang))
                    .id("no_results");
                    let _ = query.answer(vec![no_res_art]).cache_time(5).private().send().await;
                    return Ok(());
                }

                self.sessions.cache_tracks(&tracks).await;

                let mut articles = Vec::with_capacity(tracks.len().min(30));
                for (idx, t) in tracks.iter().take(30).enumerate() {
                    let card_text = Messages::track_card(lang, t);
                    let markup = self.render_track_card_inline_markup(t, user_id, lang).await;

                    let title = format!("🎵 {} — {}", t.display_artist(), t.display_title());
                    let desc = format!("💿 {} | ⏱ {} | 🌐 {}", t.display_album(), t.display_duration(), t.platform);

                    let mut art = Article::new(title, InputMessage::new().html(card_text).reply_markup(markup))
                        .description(desc)
                        .id(format!("s_{idx}_{}", t.song_id));

                    if let Some(ref cover) = t.cover_url {
                        if cover.starts_with("http") {
                            art = art.thumb_url(cover);
                        }
                    }
                    articles.push(art);
                }

                let _ = query.answer(articles).cache_time(5).private().send().await;
            }
            Err(e) => {
                log::warn!("Inline search error: {e}");
                let err_art = Article::new(
                    "❌ Ошибка поиска",
                    InputMessage::new().html(Messages::api_error(lang, &e)),
                ).id("search_err");
                let _ = query.answer(vec![err_art]).cache_time(0).private().send().await;
            }
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::tempdir;

    fn sample_track(id: &str, title: &str) -> LaneTrack {
        LaneTrack {
            song_id: id.to_string(),
            title: title.to_string(),
            artists_displayed_name: "Artist".to_string(),
            cover_url: None,
            duration: "3:30".to_string(),
            platform: "spotify".to_string(),
            sp_data: None,
            sc_data: None,
        }
    }

    #[test]
    fn test_track_duration_parsing() {
        let t1 = sample_track("1", "Track 1");
        assert_eq!(t1.duration_seconds(), 210);

        let mut t2 = sample_track("2", "Track 2");
        t2.duration = "1:02:03".to_string();
        assert_eq!(t2.duration_seconds(), 3723);
    }

    #[tokio::test]
    async fn test_session_store_and_search_cache() {
        let dir = tempdir().unwrap();
        let path = dir.path().join("test_sessions.json");
        let store = SessionStore::new(&path);

        assert!(store.get_session(123).await.is_none());

        store
            .save_session(123, "dev_1".to_string(), "tok_1".to_string(), None)
            .await;

        let sess = store.get_session(123).await.unwrap();
        assert_eq!(sess.telegram_id, 123);
        assert_eq!(sess.device_id, "dev_1");
        assert_eq!(sess.token, "tok_1");

        // Test search caching
        let tracks = vec![sample_track("s1", "Song 1"), sample_track("s2", "Song 2")];
        store.store_search("q1".to_string(), "queen".to_string(), tracks).await;

        let cached = store.get_search("q1").await.unwrap();
        assert_eq!(cached.query, "queen");
        assert_eq!(cached.tracks.len(), 2);

        let found = store.find_cached_track("s2").await.unwrap();
        assert_eq!(found.title, "Song 2");
    }

    #[test]
    fn test_i18n_messages() {
        let ru_msg = Messages::searching(Language::Ru, "queen");
        assert!(ru_msg.contains("Ожидаем ответ от серверов Lane"));

        let en_msg = Messages::searching(Language::En, "queen");
        assert!(ru_msg != en_msg);
        assert!(en_msg.contains("Waiting for Lane servers response"));
    }

    #[test]
    fn test_clean_invisible_and_filename() {
        let raw = "\u{3164}username000101";
        assert_eq!(clean_invisible(raw), "username000101");

        let raw_spaces = "  \u{200b}Rick Astley\u{feff}  ";
        assert_eq!(clean_invisible(raw_spaces), "Rick Astley");

        let bad_filename = "AC/DC: Highway to Hell?";
        assert_eq!(sanitize_filename(bad_filename), "AC_DC_ Highway to Hell_");
    }

    #[tokio::test]
    async fn test_track_card_and_like_store() {
        let dir = tempdir().unwrap();
        let store = SessionStore::new(dir.path().join("sess.json"));

        let t = sample_track("song_123", "Bohemian Rhapsody");
        let card_ru = Messages::track_card(Language::Ru, &t);
        assert!(card_ru.contains("Bohemian Rhapsody"));
        assert!(card_ru.contains("song_123"));

        let card_en = Messages::track_card(Language::En, &t);
        assert!(card_en.contains("Bohemian Rhapsody"));
        assert!(card_en.contains("song_123"));

        assert!(!store.is_track_liked("song_123").await);
        store.set_track_liked("song_123", true).await;
        assert!(store.is_track_liked("song_123").await);

        store.set_track_liked("song_123", false).await;
        assert!(!store.is_track_liked("song_123").await);

        store.update_liked_tracks(&["song_123".to_string(), "song_456".to_string()]).await;
        assert!(store.is_track_liked("song_123").await);
        assert!(store.is_track_liked("song_456").await);
    }

    #[tokio::test]
    async fn test_session_store_views_and_active_track() {
        let dir = tempdir().unwrap();
        let store = SessionStore::new(dir.path().join("sess.json"));

        assert_eq!(store.get_last_view(101).await, None);
        store.set_last_view(101, "sp:search1:2".to_string()).await;
        assert_eq!(store.get_last_view(101).await.as_deref(), Some("sp:search1:2"));

        assert_eq!(store.get_active_track(101).await, None);
        store.set_active_track(101, "trk_abc").await;
        assert_eq!(store.get_active_track(101).await.as_deref(), Some("trk_abc"));
    }

    #[test]
    fn test_inline_i18n_strings() {
        let ru_title = Messages::inline_help_search_title(Language::Ru);
        assert!(ru_title.contains("Поиск треков"));

        let en_title = Messages::inline_help_search_title(Language::En);
        assert!(en_title.contains("Search tracks"));

        let ru_pl = Messages::inline_help_playlists_desc(Language::Ru);
        assert!(ru_pl.contains("плейлистов"));

        let no_res = Messages::inline_no_results_title(Language::Ru, "Unknown");
        assert!(no_res.contains("Unknown"));
    }

    #[test]
    fn test_inline_command_matching() {
        let parse_inline = |raw: &str| -> (&'static str, String) {
            let trimmed = raw.trim();
            let lower = trimmed.to_lowercase();
            if trimmed.is_empty() || lower == "help" || lower == "/help" || lower == "?" {
                ("help", String::new())
            } else if lower == "playlists" || lower == "/playlists" || lower == "pl" || lower == "/pl" {
                ("playlists", String::new())
            } else if lower == "profile" || lower == "/profile" || lower == "me" || lower == "/me" {
                ("profile", String::new())
            } else if lower.starts_with("track ") || lower.starts_with("/track ") || lower.starts_with("song ") || lower.starts_with("/song ") {
                let id = trimmed
                    .trim_start_matches("/track")
                    .trim_start_matches("track")
                    .trim_start_matches("/song")
                    .trim_start_matches("song")
                    .trim();
                ("track", id.to_string())
            } else {
                let q = if lower.starts_with("search ") || lower.starts_with("/search ") {
                    trimmed.trim_start_matches("/search").trim_start_matches("search").trim()
                } else {
                    trimmed
                };
                ("search", q.to_string())
            }
        };

        assert_eq!(parse_inline(""), ("help", "".to_string()));
        assert_eq!(parse_inline("/help"), ("help", "".to_string()));
        assert_eq!(parse_inline("playlists"), ("playlists", "".to_string()));
        assert_eq!(parse_inline("/pl"), ("playlists", "".to_string()));
        assert_eq!(parse_inline("profile"), ("profile", "".to_string()));
        assert_eq!(parse_inline("/me"), ("profile", "".to_string()));
        assert_eq!(parse_inline("track 12345"), ("track", "12345".to_string()));
        assert_eq!(parse_inline("/song abcdef"), ("track", "abcdef".to_string()));
        assert_eq!(parse_inline("search Queen"), ("search", "Queen".to_string()));
        assert_eq!(parse_inline("/search Bohemian"), ("search", "Bohemian".to_string()));
        assert_eq!(parse_inline("Bohemian Rhapsody"), ("search", "Bohemian Rhapsody".to_string()));
    }

    #[tokio::test]
    async fn test_live_search_if_configured() {
        let candidates = [
            "../lane_bot_sessions.json",
            "crates/lane_bot_sessions.json",
            "lane_bot_sessions.json",
        ];
        for path in candidates {
            if let Ok(content) = std::fs::read_to_string(path) {
                if let Ok(v) = serde_json::from_str::<serde_json::Value>(&content) {
                    if let Some(obj) = v.as_object().and_then(|m| m.values().next()) {
                        if let (Some(token), Some(dev_id)) = (
                            obj.get("token").and_then(|t| t.as_str()),
                            obj.get("device_id").and_then(|d| d.as_str()),
                        ) {
                            let client = LaneApiClient::new(token, dev_id);
                            match client.search("Queen").await {
                                Ok(tracks) => {
                                    println!("Live search found {} tracks!", tracks.len());
                                    assert!(!tracks.is_empty(), "Search results should not be empty");
                                    return;
                                }
                                Err(e) => {
                                    panic!("Live search failed: {e:?}");
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
