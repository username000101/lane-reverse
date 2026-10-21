use std::env;
use std::sync::Arc;
use grammers_client::update::Update;
use grammers_client::Client;
use grammers_mtsender::{SenderPool, UpdatesConfiguration};
use grammers_session::storages::SqliteSession;

mod handlers;
mod i18n;
mod lane_client;
mod session_store;

use handlers::BotHandler;
use session_store::SessionStore;

// Standard public Telegram client API keys for open-source apps (can be overridden via env)
const DEFAULT_API_ID: i32 = 2040;
const DEFAULT_API_HASH: &str = "b18441a1ff607e10a989891a5462e627";

fn print_help() {
    println!(
        "Usage: laned-bot [OPTIONS]\n\n\
         Options:\n\
           --token <TOKEN>         Telegram Bot Token (or BOT_TOKEN / TG_BOT_TOKEN env)\n\
           --api-id <ID>           Telegram API ID (or TG_API_ID env)\n\
           --api-hash <HASH>       Telegram API Hash (or TG_API_HASH env)\n\
           --session <PATH>        Path to session db (default: laned_bot.session)\n\
           --sessions-file <PATH>  Path to Lane user sessions JSON (default: lane_bot_sessions.json)\n\
           --help, -h              Print this help"
    );
}

fn get_arg(args: &[String], name: &str) -> Option<String> {
    args.iter()
        .position(|a| a == name)
        .and_then(|i| args.get(i + 1).cloned())
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info")).init();

    let args: Vec<String> = env::args().collect();
    if args.iter().any(|a| a == "--help" || a == "-h") {
        print_help();
        return Ok(());
    }

    let bot_token = get_arg(&args, "--token")
        .or_else(|| env::var("BOT_TOKEN").ok())
        .or_else(|| env::var("TG_BOT_TOKEN").ok())
        .unwrap_or_default();

    if bot_token.is_empty() {
        eprintln!("Error: Telegram Bot Token is required.");
        eprintln!("Provide it via --token <TOKEN> or set BOT_TOKEN environment variable.");
        eprintln!("Example: laned-bot --token 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        std::process::exit(1);
    }

    let api_id: i32 = get_arg(&args, "--api-id")
        .or_else(|| env::var("TG_API_ID").ok())
        .and_then(|s| s.parse().ok())
        .unwrap_or(DEFAULT_API_ID);

    let api_hash = get_arg(&args, "--api-hash")
        .or_else(|| env::var("TG_API_HASH").ok())
        .unwrap_or_else(|| DEFAULT_API_HASH.to_string());

    let session_path = get_arg(&args, "--session")
        .or_else(|| env::var("TG_SESSION_PATH").ok())
        .unwrap_or_else(|| "laned_bot.session".to_string());

    let sessions_file = get_arg(&args, "--sessions-file")
        .or_else(|| env::var("LANE_SESSIONS_FILE").ok())
        .unwrap_or_else(|| "lane_bot_sessions.json".to_string());

    log::info!("Starting Lane Telegram Bot (grammers)...");
    log::info!("Session DB: {session_path}");
    log::info!("Lane sessions file: {sessions_file}");

    let session_store = Arc::new(SessionStore::new(&sessions_file));

    // Initialize Grammers MTProto client
    let session = Arc::new(
        SqliteSession::open(&session_path)
            .await
            .map_err(|e| format!("Failed to open session {session_path}: {e}"))?,
    );

    let pool = SenderPool::new(session, api_id);
    let handle = pool.handle;
    let runner = pool.runner;
    let updates = pool.updates;

    tokio::spawn(runner.run());

    let client = Client::new(handle);

    // Sign in as bot if not already authorized
    if !client.is_authorized().await? {
        log::info!("Authorizing bot with token...");
        client.bot_sign_in(&bot_token, &api_hash).await?;
        log::info!("Bot authorization successful!");
    } else {
        log::info!("Bot already authorized from existing session.");
    }

    let handler = Arc::new(BotHandler::new(client.clone(), session_store));

    log::info!("Subscribing to Telegram updates stream...");
    let mut stream = client
        .stream_updates(updates, UpdatesConfiguration::default())
        .await
        .map_err(|e| format!("Failed to stream updates: {e}"))?;

    log::info!("Bot is running and listening for events! Press Ctrl+C to stop.");

    while let Ok(update) = stream.next().await {
        let handler_clone = Arc::clone(&handler);

        tokio::spawn(async move {
            match update {
                Update::NewMessage(msg) => {
                    if let Err(e) = handler_clone.handle_message(msg.into_inner()).await {
                        log::error!("Error handling message: {e}");
                    }
                }
                Update::CallbackQuery(query) => {
                    if let Err(e) = handler_clone.handle_callback_query(query).await {
                        log::error!("Error handling callback query: {e}");
                    }
                }
                Update::InlineQuery(query) => {
                    if let Err(e) = handler_clone.handle_inline_query(query).await {
                        log::error!("Error handling inline query: {e}");
                    }
                }
                _ => {}
            }
        });
    }

    Ok(())
}
