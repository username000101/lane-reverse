#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Language {
    En,
    Ru,
}

impl Language {
    pub fn from_code(code: Option<&str>) -> Self {
        match code.map(|s| s.to_ascii_lowercase()).as_deref() {
            Some("ru") | Some("ru-ru") | Some("rus") => Language::Ru,
            _ => Language::En,
        }
    }
}

pub struct Messages;

impl Messages {
    pub fn welcome(lang: Language, is_auth: bool, name: &str) -> String {
        let clean_name = crate::lane_client::clean_invisible(name);
        let name_display = if clean_name.is_empty() { "User" } else { &clean_name };
        match lang {
            Language::Ru => {
                if is_auth {
                    format!(
                        "👋 <b>Добро пожаловать в Lane Music Bot!</b>\n\n\
                         Вы авторизованы как <b>{name_display}</b>.\n\n\
                         <b>Доступные команды:</b>\n\
                         • /search &lt;запрос&gt; — поиск треков в Lane\n\
                         • /playlists — ваши плейлисты\n\
                         • /profile — ваш профиль\n\
                         • /help — справка по командам"
                    )
                } else {
                    "👋 <b>Добро пожаловать в Lane Music Bot!</b>\n\n\
                     Для доступа к музыке и вашим плейлистам необходимо авторизоваться через официального бота @lane_music_bot.\n\n\
                     Используйте кнопку ниже для авторизации."
                        .to_string()
                }
            }
            Language::En => {
                if is_auth {
                    format!(
                        "👋 <b>Welcome to Lane Music Bot!</b>\n\n\
                         You are signed in as <b>{name_display}</b>.\n\n\
                         <b>Commands:</b>\n\
                         • /search &lt;query&gt; — search tracks on Lane\n\
                         • /playlists — view your playlists\n\
                         • /profile — view your profile\n\
                         • /help — help & command list"
                    )
                } else {
                    "👋 <b>Welcome to Lane Music Bot!</b>\n\n\
                     To access music and playlists, please log in via the official @lane_music_bot.\n\n\
                     Click the button below to sign in."
                        .to_string()
                }
            }
        }
    }

    pub fn help(lang: Language) -> String {
        match lang {
            Language::Ru => {
                "📖 <b>Справка по командам бота:</b>\n\n\
                 • /search &lt;текст&gt; — Поиск треков (с карточками трека, скачиванием и плейлистами)\n\
                 • /playlists — Список ваших плейлистов в Lane\n\
                 • /newplaylist &lt;название&gt; — Создать новый плейлист в Lane\n\
                 • /track &lt;song_id&gt; — Карточка трека (лайк, добавление в плейлист, скачивание)\n\
                 • /profile — Просмотр профиля аккаунта Lane\n\
                 • /login — Авторизация через официального бота @lane_music_bot\n\
                 • /help — Данное справочное сообщение"
                    .to_string()
            }
            Language::En => {
                "📖 <b>Bot Commands Help:</b>\n\n\
                 • /search &lt;query&gt; — Search tracks (with track cards, download, and playlists)\n\
                 • /playlists — View your Lane playlists\n\
                 • /newplaylist &lt;name&gt; — Create a new Lane playlist\n\
                 • /track &lt;song_id&gt; — Track card (like, add to playlist, download)\n\
                 • /profile — View your Lane account profile\n\
                 • /login — Authorize via official @lane_music_bot\n\
                 • /help — This help message"
                    .to_string()
            }
        }
    }

    pub fn auth_prompt(lang: Language, _url: &str) -> (String, String) {
        match lang {
            Language::Ru => (
                format!(
                    "🔐 <b>Авторизация в Lane</b>\n\n\
                     1. Нажмите кнопку ниже для перехода в официального бота @lane_music_bot.\n\
                     2. Нажмите <b>Запустить / Start</b> в нём.\n\
                     3. Бот автоматически зафиксирует авторизацию и сохранит ваш профиль."
                ),
                "🔗 Войти через @lane_music_bot".to_string(),
            ),
            Language::En => (
                format!(
                    "🔐 <b>Lane Authorization</b>\n\n\
                     1. Click the button below to open the official @lane_music_bot.\n\
                     2. Press <b>Start</b> in that bot.\n\
                     3. We will automatically detect your login and link your session."
                ),
                "🔗 Log in via @lane_music_bot".to_string(),
            ),
        }
    }

    pub fn auth_success(lang: Language, name: &str) -> String {
        let clean_name = crate::lane_client::clean_invisible(name);
        let name_display = if clean_name.is_empty() { "User" } else { &clean_name };
        match lang {
            Language::Ru => format!("✅ <b>Вход выполнен успешно!</b>\nДобро пожаловать, <b>{name_display}</b>! Теперь вам доступны /search, /playlists и /profile."),
            Language::En => format!("✅ <b>Login successful!</b>\nWelcome, <b>{name_display}</b>! You can now use /search, /playlists, and /profile."),
        }
    }

    pub fn auth_timeout(lang: Language) -> String {
        match lang {
            Language::Ru => "⏱ <b>Время ожидания ответа Lane истекло.</b>\nСерверы Lane не зафиксировали вход за отведённое время. Повторите попытку через /login.".to_string(),
            Language::En => "⏱ <b>Lane authorization timed out.</b>\nLane servers did not confirm authorization in time. Please try again via /login.".to_string(),
        }
    }

    pub fn not_authorized(lang: Language) -> String {
        match lang {
            Language::Ru => "⚠️ <b>Вы ещё не авторизованы!</b>\nДля выполнения этой команды необходимо войти в аккаунт Lane через /login.".to_string(),
            Language::En => "⚠️ <b>You are not logged in!</b>\nPlease log in to your Lane account using /login first.".to_string(),
        }
    }

    pub fn loading_profile(lang: Language) -> String {
        match lang {
            Language::Ru => "⏳ <i>Запрашиваем данные профиля у серверов Lane...</i>".to_string(),
            Language::En => "⏳ <i>Requesting profile data from Lane servers...</i>".to_string(),
        }
    }

    pub fn loading_playlists(lang: Language) -> String {
        match lang {
            Language::Ru => "⏳ <i>Запрашиваем список плейлистов у серверов Lane...</i>".to_string(),
            Language::En => "⏳ <i>Requesting playlists from Lane servers...</i>".to_string(),
        }
    }

    pub fn loading_playlist_page(lang: Language) -> String {
        match lang {
            Language::Ru => "⏳ <i>Загружаем страницу плейлиста с серверов Lane...</i>".to_string(),
            Language::En => "⏳ <i>Loading playlist page from Lane servers...</i>".to_string(),
        }
    }

    pub fn searching(lang: Language, query: &str) -> String {
        match lang {
            Language::Ru => format!("🔍 <i>Ожидаем ответ от серверов Lane по запросу «{query}»...</i>"),
            Language::En => format!("🔍 <i>Waiting for Lane servers response for «{query}»...</i>"),
        }
    }

    pub fn resolving_stream(lang: Language, title: &str) -> String {
        match lang {
            Language::Ru => format!("⏳ <i>Запрашиваем аудиопоток на серверах Lane для «{title}»...</i>"),
            Language::En => format!("⏳ <i>Requesting audio stream from Lane servers for «{title}»...</i>"),
        }
    }

    pub fn no_results(lang: Language, query: &str) -> String {
        match lang {
            Language::Ru => format!("❌ По запросу «<b>{query}</b>» треков не найдено."),
            Language::En => format!("❌ No tracks found for «<b>{query}</b>»."),
        }
    }

    pub fn search_header(lang: Language, query: &str, page: usize, total_pages: usize, total_count: usize) -> String {
        match lang {
            Language::Ru => format!(
                "🔍 <b>Результаты поиска:</b> «{query}»\n📄 <b>Страница:</b> {page}/{total_pages} (всего {total_count} треков)\n"
            ),
            Language::En => format!(
                "🔍 <b>Search results:</b> «{query}»\n📄 <b>Page:</b> {page}/{total_pages} ({total_count} tracks found)\n"
            ),
        }
    }

    pub fn playlist_header(lang: Language, name: &str, page: i64, total_pages: i64, total_items: i64) -> String {
        match lang {
            Language::Ru => format!(
                "📁 <b>Плейлист:</b> {name}\n📄 <b>Страница:</b> {page}/{total_pages} (всего {total_items} треков)\n"
            ),
            Language::En => format!(
                "📁 <b>Playlist:</b> {name}\n📄 <b>Page:</b> {page}/{total_pages} ({total_items} tracks total)\n"
            ),
        }
    }

    pub fn track_summary(
        idx: usize,
        title: &str,
        artist: &str,
        album: &str,
        duration: &str,
        platform: &str,
    ) -> String {
        format!(
            "<b>{idx}.</b> 🎵 <b>{title}</b>\n\
                👤 {artist} | 💿 {album}\n\
                ⏱ {duration} | 🌐 {platform}\n"
        )
    }

    pub fn download_button(idx: usize) -> String {
        format!("⬇️ #{idx}")
    }

    pub fn downloading_status(lang: Language, title: &str) -> String {
        match lang {
            Language::Ru => format!("📥 <i>Скачиваем аудиопоток с серверов Lane для «{title}»...</i>"),
            Language::En => format!("📥 <i>Downloading audio stream from Lane servers for «{title}»...</i>"),
        }
    }

    pub fn uploading_status(lang: Language, title: &str) -> String {
        match lang {
            Language::Ru => format!("📤 <i>Отправляем файл в Telegram «{title}»...</i>"),
            Language::En => format!("📤 <i>Uploading «{title}» to Telegram...</i>"),
        }
    }

    pub fn download_error(lang: Language, title: &str, reason: &str) -> String {
        match lang {
            Language::Ru => format!("❌ Не удалось загрузить «{title}»: {reason}"),
            Language::En => format!("❌ Failed to download «{title}»: {reason}"),
        }
    }

    pub fn api_error(lang: Language, err: &crate::lane_client::LaneHttpError) -> String {
        let quote = err.to_blockquote_html();
        match lang {
            Language::Ru => format!(
                "❌ <b>Произошла ошибка при обращении к Lane API</b> (HTTP {})\n\n\
                 {}\n\n\
                 💬 <i>Пожалуйста, скопируйте этот контекст ошибки и отправьте в поддержку (контакт указан в описании бота).</i>",
                err.status,
                quote
            ),
            Language::En => format!(
                "❌ <b>Lane API Error</b> (HTTP {})\n\n\
                 {}\n\n\
                 💬 <i>Please copy this error report and send it to support (contact is listed in the bot description).</i>",
                err.status,
                quote
            ),
        }
    }

    pub fn profile_card(
        lang: Language,
        name: &str,
        user_name: &str,
        lane_id: &str,
        country: &str,
        interface_lang: &str,
        playlists_count: usize,
        badge: &str,
    ) -> String {
        let clean_name = crate::lane_client::clean_invisible(name);
        let name_val = if clean_name.is_empty() { "—" } else { &clean_name };
        let clean_user = crate::lane_client::clean_invisible(user_name);
        let user_val = if clean_user.is_empty() { "—" } else { &clean_user };

        match lang {
            Language::Ru => format!(
                "👤 <b>Профиль пользователя Lane</b>\n\n\
                 🏷 <b>Имя:</b> {name_val}\n\
                 🆔 <b>Username:</b> @{user_val}\n\
                 🔑 <b>Lane ID:</b> <code>{lane_id}</code>\n\
                 🌍 <b>Страна:</b> {country}\n\
                 🌐 <b>Язык интерфейса:</b> {interface_lang}\n\
                 🎵 <b>Количество плейлистов:</b> {playlists_count}\n\
                 🎖 <b>Значок:</b> {badge}"
            ),
            Language::En => format!(
                "👤 <b>Lane User Profile</b>\n\n\
                 🏷 <b>Name:</b> {name_val}\n\
                 🆔 <b>Username:</b> @{user_val}\n\
                 🔑 <b>Lane ID:</b> <code>{lane_id}</code>\n\
                 🌍 <b>Country:</b> {country}\n\
                 🌐 <b>Interface language:</b> {interface_lang}\n\
                 🎵 <b>Playlists count:</b> {playlists_count}\n\
                 🎖 <b>Badge:</b> {badge}"
            ),
        }
    }

    pub fn playlists_title(lang: Language, count: usize) -> String {
        match lang {
            Language::Ru => format!("📂 <b>Ваши плейлисты ({count}):</b>\n\nВыберите плейлист для просмотра треков:"),
            Language::En => format!("📂 <b>Your Playlists ({count}):</b>\n\nSelect a playlist to view tracks:"),
        }
    }

    pub fn truncate_str(s: &str, max_len: usize) -> String {
        if s.chars().count() > max_len {
            let res: String = s.chars().take(max_len.saturating_sub(1)).collect();
            format!("{res}…")
        } else {
            s.to_string()
        }
    }

    #[allow(dead_code)]
    pub fn open_playlist_btn(idx: usize, name: &str) -> String {
        let truncated = Self::truncate_str(name, 28);
        format!("📁 #{idx} {truncated}")
    }

    pub fn btn_playlists(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "📂 Мои плейлисты",
            Language::En => "📂 My Playlists",
        }
    }

    pub fn btn_profile(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "👤 Профиль",
            Language::En => "👤 Profile",
        }
    }

    pub fn btn_refresh(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "🔄 Обновить",
            Language::En => "🔄 Refresh",
        }
    }

    pub fn btn_relogin(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "🔐 Войти заново",
            Language::En => "🔐 Log in again",
        }
    }

    pub fn track_card(lang: Language, track: &crate::lane_client::LaneTrack) -> String {
        let title = track.display_title();
        let artist = track.display_artist();
        let album = track.display_album();
        let duration = track.display_duration();
        let platform = &track.platform;
        let song_id = &track.song_id;

        match lang {
            Language::Ru => format!(
                "🎵 <b>{title}</b>\n\n\
                 👤 <b>Исполнитель:</b> {artist}\n\
                 💿 <b>Альбом:</b> {album}\n\
                 ⏱ <b>Длительность:</b> {duration}\n\
                 🌐 <b>Платформа:</b> {platform}\n\
                 🆔 <b>ID трека:</b> <code>{song_id}</code>"
            ),
            Language::En => format!(
                "🎵 <b>{title}</b>\n\n\
                 👤 <b>Artist:</b> {artist}\n\
                 💿 <b>Album:</b> {album}\n\
                 ⏱ <b>Duration:</b> {duration}\n\
                 🌐 <b>Platform:</b> {platform}\n\
                 🆔 <b>Track ID:</b> <code>{song_id}</code>"
            ),
        }
    }

    pub fn btn_like(lang: Language, is_liked: bool) -> &'static str {
        match (lang, is_liked) {
            (Language::Ru, true) => "💔 В любимых",
            (Language::Ru, false) => "❤️ В любимые",
            (Language::En, true) => "💔 Liked",
            (Language::En, false) => "❤️ Like",
        }
    }

    pub fn btn_add_to_playlist(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "➕ В плейлист",
            Language::En => "➕ Add to playlist",
        }
    }

    pub fn btn_download(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "⬇️ Скачать",
            Language::En => "⬇️ Download",
        }
    }

    pub fn btn_new_playlist(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "➕ Создать плейлист",
            Language::En => "➕ New Playlist",
        }
    }

    pub fn btn_back(lang: Language) -> &'static str {
        match lang {
            Language::Ru => "🔙 Назад",
            Language::En => "🔙 Back",
        }
    }

    pub fn playlist_add_prompt(lang: Language, title: &str) -> String {
        match lang {
            Language::Ru => format!(
                "📂 <b>Добавление трека в плейлист:</b>\n🎵 <b>{title}</b>\n\n\
                 <i>Выберите плейлист, в который хотите сохранить трек:</i>"
            ),
            Language::En => format!(
                "📂 <b>Add track to playlist:</b>\n🎵 <b>{title}</b>\n\n\
                 <i>Select a playlist to add this track:</i>"
            ),
        }
    }

    pub fn playlist_created_success(lang: Language, name: &str) -> String {
        match lang {
            Language::Ru => format!("✅ <b>Плейлист «{name}» успешно создан!</b>\n\nТеперь вы можете добавлять в него треки из поиска."),
            Language::En => format!("✅ <b>Playlist «{name}» created successfully!</b>\n\nYou can now add tracks to it from search."),
        }
    }

    pub fn playlist_create_prompt(lang: Language) -> String {
        match lang {
            Language::Ru => "✏️ <b>Создание плейлиста</b>\n\nОтправьте команду:\n<code>/newplaylist Название плейлиста</code>".to_string(),
            Language::En => "✏️ <b>Create Playlist</b>\n\nSend command:\n<code>/newplaylist Playlist Name</code>".to_string(),
        }
    }

    pub fn loading_track(lang: Language) -> String {
        match lang {
            Language::Ru => "⏳ <i>Запрашиваем данные трека у серверов Lane...</i>".to_string(),
            Language::En => "⏳ <i>Requesting track data from Lane servers...</i>".to_string(),
        }
    }
}
