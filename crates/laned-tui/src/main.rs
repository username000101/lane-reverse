//! laned-tui — terminal browser for playlists, tracks and playback URLs.
//!
//! Env: LANED_JWT (bearer), LANED_LDI (device id).
//! Keys: ↑/↓ or j/k — move, Enter — open, Tab — switch pane, q — quit.

use std::env;
use std::io;
use std::time::Duration;

use crossterm::event::{self, Event, KeyCode, KeyEventKind};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Modifier, Style};
use ratatui::text::{Line, Span};
use ratatui::widgets::{Block, Borders, Clear, List, ListItem, ListState, Paragraph, Wrap};
use ratatui::Frame;

use laned_client::LaneClient;

struct Playlist {
    id: String,
    name: String,
}

struct Track {
    id: String,
    title: String,
    artist: String,
}

#[derive(Default)]
struct App {
    playlists: Vec<Playlist>,
    tracks: Vec<Track>,
    playlist_state: ListState,
    track_state: ListState,
    pane: Pane,
    status: String,
    popup: Option<String>,
    last_url: Option<String>,
    quality: String,
}

#[derive(Default, PartialEq)]
enum Pane {
    #[default]
    Playlists,
    Tracks,
}

fn main() {
    let jwt = env::var("LANED_JWT").unwrap_or_else(|_| {
        eprintln!("set LANED_JWT (bearer token) and optionally LANED_LDI");
        std::process::exit(1);
    });
    let ldi = env::var("LANED_LDI").unwrap_or_default();
    let client = LaneClient::new(jwt, ldi);

    let mut app = App::default();
    app.quality = "BASIC".to_string();
    app.status = "loading playlists...".into();
    load_playlists(&client, &mut app);

    let mut terminal = ratatui::init();
    let res = run(&mut terminal, &client, &mut app);
    ratatui::restore();
    if let Err(e) = res {
        eprintln!("error: {e}");
        std::process::exit(1);
    }
}

fn run(terminal: &mut ratatui::DefaultTerminal, client: &LaneClient, app: &mut App) -> io::Result<()> {
    loop {
        terminal.draw(|f| draw(f, app))?;

        if event::poll(Duration::from_millis(250))? {
            if let Event::Key(key) = event::read()? {
                if key.kind != KeyEventKind::Press {
                    continue;
                }
                match key.code {
                    KeyCode::Char('q') => return Ok(()),
                    KeyCode::Esc => {
                        if app.popup.is_some() {
                            app.popup = None;
                        } else {
                            return Ok(());
                        }
                    }
                    KeyCode::Tab => {
                        app.pane = if app.pane == Pane::Playlists {
                            Pane::Tracks
                        } else {
                            Pane::Playlists
                        };
                    }
                    KeyCode::Down | KeyCode::Char('j') => next(app, 1),
                    KeyCode::Up | KeyCode::Char('k') => next(app, -1),
                    KeyCode::Enter => open(client, app),
                    KeyCode::Char('p') => play(app),
                    KeyCode::Char('1') => set_quality(app, "BASIC"),
                    KeyCode::Char('2') => set_quality(app, "HIGH"),
                    KeyCode::Char('3') => set_quality(app, "ULTRA"),
                    KeyCode::Char('r') => {
                        app.status = "reloading playlists...".into();
                        load_playlists(client, app);
                    }
                    _ => {}
                }
            }
        }
    }
}

fn next(app: &mut App, delta: i32) {
    let (state, len) = match app.pane {
        Pane::Playlists => (&mut app.playlist_state, app.playlists.len()),
        Pane::Tracks => (&mut app.track_state, app.tracks.len()),
    };
    if len == 0 {
        return;
    }
    let cur = state.selected().unwrap_or(0) as i32;
    let nxt = (cur + delta).clamp(0, len as i32 - 1) as usize;
    state.select(Some(nxt));
}

fn open(client: &LaneClient, app: &mut App) {
    match app.pane {
        Pane::Playlists => {
            let Some(idx) = app.playlist_state.selected() else {
                return;
            };
            let Some(pl) = app.playlists.get(idx) else {
                return;
            };
            app.status = format!("loading tracks of {}...", pl.id);
            let path = format!("/playlist/{}/tracks", pl.id);
            match client.get(&path, "page=1&pageSize=50") {
                Ok(resp) => {
                    let status = resp.status;
                    let text = resp.text();
                    if !status.is_success() {
                        app.status = format!("HTTP {status}: {text}");
                        return;
                    }
                    app.tracks = parse_tracks(&text);
                    app.track_state.select(if app.tracks.is_empty() { None } else { Some(0) });
                    app.status = format!("{} tracks", app.tracks.len());
                    app.pane = Pane::Tracks;
                }
                Err(e) => app.status = e,
            }
        }
        Pane::Tracks => {
            let Some(idx) = app.track_state.selected() else {
                return;
            };
            let Some(tr) = app.tracks.get(idx) else {
                return;
            };
            let ref_id = app
                .playlist_state
                .selected()
                .and_then(|i| app.playlists.get(i))
                .map(|p| p.id.clone())
                .unwrap_or_default();
            app.status = format!("resolving stream for {}...", tr.id);
            match client.stream(&tr.id, &ref_id, &app.quality) {
                Ok(v) => {
                    let url = v.get("url").and_then(|x| x.as_str()).unwrap_or("(no url)");
                    let ttl = v.get("ttl").and_then(|x| x.as_i64()).unwrap_or(0);
                    app.last_url = Some(url.to_string());
                    app.popup = Some(format!(
                        "{}\n{}\n\nurl:\n{}\n\nttl: {} ms   quality: {}\n\np — play, 1/2/3 — BASIC/HIGH/ULTRA",
                        tr.title, tr.artist, url, ttl, app.quality
                    ));
                    app.status = "stream resolved".into();
                }
                Err(e) => {
                    app.status = e;
                }
            }
        }
    }
}

fn set_quality(app: &mut App, q: &str) {
    app.quality = q.to_string();
    app.status = format!("quality: {q}");
}

fn play(app: &mut App) {
    match app.last_url.clone() {
        Some(url) => {
            app.status = "starting player...".into();
            std::thread::spawn(move || {
                let _ = laned_client::play_url(&url);
            });
        }
        None => app.status = "nothing to play: press Enter on a track first".into(),
    }
}

fn load_playlists(client: &LaneClient, app: &mut App) {
    match client.get("/user/playlists", "") {
        Ok(resp) => {
            let status = resp.status;
            let text = resp.text();
            if !status.is_success() {
                app.status = format!("HTTP {status}: {text}");
                return;
            }
            let v: serde_json::Value = match serde_json::from_str(&text) {
                Ok(v) => v,
                Err(e) => {
                    app.status = format!("bad json: {e}");
                    return;
                }
            };
            app.playlists = v
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .map(|p| Playlist {
                            id: p.get("playlistId").and_then(|x| x.as_str()).unwrap_or("").to_string(),
                            name: p.get("playlistName").and_then(|x| x.as_str()).unwrap_or("?").to_string(),
                        })
                        .collect()
                })
                .unwrap_or_default();
            app.playlist_state.select(if app.playlists.is_empty() { None } else { Some(0) });
            app.status = format!("{} playlists", app.playlists.len());
        }
        Err(e) => app.status = e,
    }
}

fn parse_tracks(text: &str) -> Vec<Track> {
    let v: serde_json::Value = match serde_json::from_str(text) {
        Ok(v) => v,
        Err(_) => return Vec::new(),
    };
    v.get("items")
        .and_then(|i| i.as_array())
        .map(|arr| {
            arr.iter()
                .map(|t| Track {
                    id: t.get("songId").and_then(|x| x.as_str()).unwrap_or("").to_string(),
                    title: t.get("title").and_then(|x| x.as_str()).unwrap_or("?").to_string(),
                    artist: t
                        .get("artistsDisplayedName")
                        .and_then(|x| x.as_str())
                        .unwrap_or("")
                        .to_string(),
                })
                .collect()
        })
        .unwrap_or_default()
}

fn draw(f: &mut Frame, app: &mut App) {
    let chunks = Layout::default()
        .direction(Direction::Vertical)
        .constraints([Constraint::Min(3), Constraint::Length(1)])
        .split(f.area());

    let panes = Layout::default()
        .direction(Direction::Horizontal)
        .constraints([Constraint::Percentage(38), Constraint::Percentage(62)])
        .split(chunks[0]);

    let active = |p: &Pane| {
        if &app.pane == p {
            Style::default().fg(Color::Yellow)
        } else {
            Style::default().fg(Color::Gray)
        }
    };

    let pl_items: Vec<ListItem> = app
        .playlists
        .iter()
        .map(|p| ListItem::new(p.name.clone()))
        .collect();
    let pl_list = List::new(pl_items)
        .block(
            Block::default()
                .borders(Borders::ALL)
                .title(" playlists ")
                .border_style(active(&Pane::Playlists)),
        )
        .highlight_style(Style::default().add_modifier(Modifier::REVERSED))
        .highlight_symbol("> ");
    f.render_stateful_widget(pl_list, panes[0], &mut app.playlist_state);

    let tr_items: Vec<ListItem> = app
        .tracks
        .iter()
        .map(|t| {
            ListItem::new(Line::from(vec![
                Span::raw(t.title.clone()),
                Span::styled(format!("  — {}", t.artist), Style::default().fg(Color::DarkGray)),
            ]))
        })
        .collect();
    let tr_list = List::new(tr_items)
        .block(
            Block::default()
                .borders(Borders::ALL)
                .title(" tracks ")
                .border_style(active(&Pane::Tracks)),
        )
        .highlight_style(Style::default().add_modifier(Modifier::REVERSED))
        .highlight_symbol("> ");
    f.render_stateful_widget(tr_list, panes[1], &mut app.track_state);

    let status = Paragraph::new(app.status.clone()).style(Style::default().fg(Color::Cyan));
    f.render_widget(status, chunks[1]);

    if let Some(text) = &app.popup {
        let area = centered_rect(80, 60, f.area());
        f.render_widget(Clear, area);
        let p = Paragraph::new(text.as_str())
            .block(Block::default().borders(Borders::ALL).title(" playback "))
            .wrap(Wrap { trim: false });
        f.render_widget(p, area);
    }
}

fn centered_rect(px: u16, py: u16, r: Rect) -> Rect {
    let vertical = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Percentage((100 - py) / 2),
            Constraint::Percentage(py),
            Constraint::Percentage((100 - py) / 2),
        ])
        .split(r);
    Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage((100 - px) / 2),
            Constraint::Percentage(px),
            Constraint::Percentage((100 - px) / 2),
        ])
        .split(vertical[1])[1]
}
