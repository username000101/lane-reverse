'use strict';

/* =========================================================================
   Lane Web — клиент
   ========================================================================= */

const $ = (id) => document.getElementById(id);

const els = {
  landing: $('landing'),
  app: $('app'),
  player: $('player'),
  audio: $('audio'),
  modal: $('auth-modal'),
  authBody: $('auth-body'),
  trackList: $('track-list'),
  playlistList: $('playlist-list'),
  toasts: $('toasts'),
  topLogin: $('top-login'),
  userChip: $('user-chip'),
  quality: $('quality'),
  seek: $('seek'),
  volume: $('volume'),
};

const state = {
  user: null,
  ldi: null,
  playlists: [],
  playlistId: null,
  playlist: null,
  tracks: [],
  page: 1,
  totalPages: 1,
  totalItems: 0,
  current: null,
  queue: [],
  refId: null,
  stream: null,
  quality: localStorage.getItem('lane.quality') || 'HIGH',
  volume: 0.9,
  auth: { url: '', id: '', attempts: 0, run: 0 },
  sessionLost: false,
};

let authRun = 0;
let pollTimer = null;
let streamSeq = 0;
let retriedStream = 0;
let seeking = false;

/* ------------------------------ утилиты ------------------------------- */

const esc = (value) =>
  String(value == null ? '' : value).replace(/[&<>"']/g, (ch) => {
    switch (ch) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      default: return '&#39;';
    }
  });

function safeUrl(value) {
  if (!value) return '';
  try {
    const url = new URL(String(value), location.href);
    if (url.protocol !== 'https:' && url.protocol !== 'http:') return '';
    if (url.protocol === 'http:' && url.hostname !== 'localhost') url.protocol = 'https:';
    return url.href.replace(/["'\\\s]/g, '');
  } catch {
    return '';
  }
}

function coverStyle(url) {
  const safe = safeUrl(url);
  const gradient = 'linear-gradient(135deg,#2c1c58,#0d1428)';
  return safe
    ? `background-image:url('${safe}'),${gradient}`
    : `background-image:${gradient}`;
}

function plural(count, one, few, many) {
  const n = Math.abs(count) % 100;
  const n1 = n % 10;
  if (n > 10 && n < 20) return many;
  if (n1 > 1 && n1 < 5) return few;
  if (n1 === 1) return one;
  return many;
}

function fmtTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const total = Math.floor(seconds);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function initials(name) {
  const parts = String(name || '?').trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

function setFill(input, percent) {
  if (!input) return;
  input.style.setProperty('--fill', `${Math.max(0, Math.min(100, percent))}%`);
}

/* -------------------------------- API ---------------------------------- */
async function api(path, options = {}) {
  const res = await fetch(path, {
    method: options.method || 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      accept: 'application/json',
      ...(options.body ? { 'content-type': 'application/json' } : {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  let data = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }

  if (!res.ok) {
    const error = new Error((data && (data.error || data.message)) || `HTTP ${res.status}`);
    error.status = res.status;
    error.code = data && data.code;
    if (res.status === 401) handleSessionLost(error);
    throw error;
  }

  return data;
}

/* ------------------------------- тосты --------------------------------- */

function toast(message, kind = 'info') {
  const node = document.createElement('div');
  node.className = `toast${kind === 'error' ? ' error' : ''}`;
  node.textContent = message;
  els.toasts.appendChild(node);
  setTimeout(() => {
    node.classList.add('is-out');
    setTimeout(() => node.remove(), 240);
  }, 4200);
}

/* ------------------------------ сессия --------------------------------- */

function handleSessionLost(error) {
  if (state.sessionLost) return;
  state.sessionLost = true;
  toast((error && error.message) || 'Сессия истекла', 'error');
  setTimeout(() => resetToLanding(), 900);
}

function renderUser() {
  const user = state.user || {};
  const name =
    user.displayedName || user.username || user.name || user.laneId || 'Слушатель';
  const sub = user.username ? `@${user.username}` : user.laneId ? `lane id ${user.laneId}` : 'Telegram';

  $('top-name').textContent = name;
  $('profile-name').textContent = name;
  $('profile-sub').textContent = sub;

  const avatarUrl = safeUrl(user.avatarUrl || user.avatar || user.profileImageUrl);
  for (const [id, node] of [
    ['profile-avatar', $('profile-avatar')],
    ['top-avatar', $('top-avatar')],
  ]) {
    node.textContent = avatarUrl ? '' : initials(name);
    node.style.backgroundImage = avatarUrl
      ? `url("${avatarUrl}"), linear-gradient(140deg,#7c3aed,#db2777)`
      : 'linear-gradient(140deg,#7c3aed,#db2777)';
    node.style.backgroundSize = avatarUrl ? 'cover' : '';
    node.style.backgroundPosition = 'center';
  }
}

function enterApp(me) {
  state.user = (me && me.user) || null;
  state.ldi = (me && me.ldi) || null;
  state.sessionLost = false;

  els.landing.hidden = true;
  els.app.hidden = false;
  document.body.classList.add('in-app');
  els.topLogin.hidden = true;
  els.userChip.hidden = false;

  renderUser();
  loadPlaylists();
  window.scrollTo({ top: 0, behavior: 'auto' });
}

function showLanding() {
  els.landing.hidden = false;
  els.app.hidden = true;
  els.player.hidden = true;
  document.body.classList.remove('in-app');
  els.topLogin.hidden = false;
  els.userChip.hidden = true;
}

function resetToLanding() {
  try {
    els.audio.pause();
  } catch {}
  els.audio.removeAttribute('src');
  els.audio.load();

  state.user = null;
  state.ldi = null;
  state.playlists = [];
  state.playlistId = null;
  state.playlist = null;
  state.tracks = [];
  state.current = null;
  state.queue = [];
  state.stream = null;
  state.sessionLost = false;
  els.playlistList.innerHTML = '';
  els.trackList.innerHTML = '';

  showLanding();
}

async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  } catch {}
  resetToLanding();
  toast('Вы вышли из аккаунта');
}

/* ----------------------------- плейлисты -------------------------------- */

function playlistCount(playlist) {
  if (Array.isArray(playlist.playlistTracksIds)) return playlist.playlistTracksIds.length;
  if (typeof playlist.tracksCount === 'number') return playlist.tracksCount;
  return 0;
}

function renderPlaylists() {
  if (!state.playlists.length) {
    els.playlistList.innerHTML = '<div class="pl-empty">Плейлистов нет</div>';
    $('playlist-count').textContent = '0';
    return;
  }

  $('playlist-count').textContent = String(state.playlists.length);
  els.playlistList.innerHTML = state.playlists
    .map((playlist) => {
      const id = String(playlist.playlistId || '');
      const active = id === state.playlistId;
      const count = playlistCount(playlist);
      return `
        <button class="pl${active ? ' is-active' : ''}" data-id="${esc(id)}" role="listitem" title="${esc(
          playlist.playlistName || id,
        )}">
          <span class="cover sm" style="${coverStyle(playlist.playlistImageUrl)}"></span>
          <span class="pl-main">
            <span class="pl-name">${esc(playlist.playlistName || id || 'Без названия')}</span>
            <span class="pl-sub">${count ? `${count} ${plural(count, 'трек', 'трека', 'треков')}` : 'плейлист'}</span>
          </span>
        </button>`;
    })
    .join('');
}

async function loadPlaylists() {
  try {
    const data = await api('/api/playlists');
    state.playlists = Array.isArray(data.playlists) ? data.playlists : [];
    renderPlaylists();

    if (!state.playlists.length) {
      showEmptyLibrary(
        'Пока пусто',
        'В аккаунте нет плейлистов. Добавь что-нибудь в приложении Lane — и обнови страницу.',
      );
      return;
    }

    const wanted =
      state.playlists.find((p) => p.playlistId === parseHash()) ||
      state.playlists.find((p) => p.playlistId === 'lane_likes') ||
      state.playlists[0];

    await openPlaylist(wanted.playlistId, { push: false });
  } catch (error) {
    if (error.code === 'SESSION_EXPIRED') return;
    showEmptyLibrary('Не удалось загрузить плейлисты', error.message);
  }
}

function parseHash() {
  const match = /^#\/p\/(.+)$/.exec(location.hash || '');
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return null;
  }
}

function setHash(id) {
  const hash = `#/p/${encodeURIComponent(id)}`;
  if (location.hash !== hash) history.pushState(null, '', hash);
}

function renderLibrarySkeleton(id) {
  const cached = state.playlists.find((p) => p.playlistId === id);
  state.playlist = cached || { playlistId: id, playlistName: id };
  $('pl-cover').style.backgroundImage = 'linear-gradient(135deg,#2c1c58,#0d1428)';
  $('pl-name').textContent = (cached && cached.playlistName) || 'Загрузка…';
  $('pl-desc').textContent = '';
  $('pl-count').textContent = '…';
  $('pl-source').textContent = 'Lane';
  $('pl-quality').textContent = qualityLabel();
  $('library-empty').hidden = true;
  $('lib-more').hidden = true;

  els.trackList.innerHTML = Array.from({ length: 8 })
    .map(
      () => `
      <div class="track track-skeleton" aria-hidden="true">
        <div class="t-index"><span class="skel skel-num"></span></div>
        <span class="cover sm skel"></span>
        <div class="t-main"><span class="skel skel-line"></span><span class="skel skel-line short"></span></div>
        <span></span>
        <div class="t-dur"><span class="skel skel-num"></span></div>
      </div>`,
    )
    .join('');
}

async function openPlaylist(id, options = {}) {
  if (!id) return;
  if (options.push) setHash(id);
  if (state.playlistId === id && state.tracks.length && !options.force) {
    return;
  }

  state.playlistId = id;
  state.playlist = null;
  state.tracks = [];
  state.page = 1;
  state.totalPages = 1;
  state.totalItems = 0;
  state.queue = [];
  renderPlaylists();
  renderLibrarySkeleton(id);

  try {
    const data = await api(
      `/api/playlist?id=${encodeURIComponent(id)}&page=1&pageSize=50`,
    );
    if (state.playlistId !== id) return;

    state.playlist = data.playlist || state.playlists.find((p) => p.playlistId === id) || { playlistId: id };
    const page = data.tracks || {};
    state.tracks = Array.isArray(page.items) ? page.items : [];
    state.page = Number(page.page) || 1;
    state.totalPages = Number(page.totalPages) || 1;
    state.totalItems = Number(page.totalItems) || state.tracks.length;

    renderLibrary();
  } catch (error) {
    if (state.playlistId !== id) return;
    if (error.code === 'SESSION_EXPIRED') return;
    showEmptyLibrary('Не удалось открыть плейлист', error.message);
  }
}

function qualityLabel() {
  return `${state.quality} · ${{ BASIC: '128', HIGH: '192', ULTRA: '320' }[state.quality] || '192'}`;
}

function renderLibrary() {
  const playlist = state.playlist || {};
  const cover = playlist.playlistImageUrl;
  $('pl-cover').style.cssText = coverStyle(cover);

  $('pl-name').textContent = playlist.playlistName || playlist.playlistId || 'Плейлист';
  $('pl-desc').textContent = playlist.playlistDescription || '';
  $('pl-desc').hidden = !playlist.playlistDescription;

  const count = state.totalItems || state.tracks.length;
  $('pl-count').textContent = `${count} ${plural(count, 'трек', 'трека', 'треков')}`;
  $('pl-source').textContent = playlist.platform ? `Lane · ${playlist.platform}` : 'Lane';
  $('pl-quality').textContent = qualityLabel();

  $('library-empty').hidden = true;
  renderTracks();
  renderMore();
}

function renderTracks() {
  if (!state.tracks.length) {
    showEmptyLibrary('Здесь пока нет треков', 'Плейлист пуст — добавь музыку в приложении Lane.');
    return;
  }

  els.trackList.innerHTML = state.tracks
    .map((track, index) => {
      const active = state.current && track.songId === state.current.songId;
      const title = track.title || 'Без названия';
      const artist = track.artistsDisplayedName || 'Неизвестный исполнитель';
      const indexCell = active
        ? '<span class="bars" aria-hidden="true"><i></i><i></i><i></i></span>'
        : `<span class="t-num">${index + 1}</span><svg class="i t-play" aria-hidden="true"><use href="#i-play"/></svg>`;

      return `
        <div class="track${active ? ' is-active' : ''}" data-i="${index}" role="button" tabindex="0"
             aria-label="${esc(title)} — ${esc(artist)}">
          <div class="t-index">${indexCell}</div>
          <span class="cover sm" style="${coverStyle(track.coverUrl)}"></span>
          <div class="t-main">
            <div class="t-title">${esc(title)}</div>
            <div class="t-artist">${esc(artist)}</div>
          </div>
          <div class="t-badges">${track.explicit ? '<span class="badge-explicit">E</span>' : ''}</div>
          <div class="t-dur">${esc(track.duration || '—')}</div>
        </div>`;
    })
    .join('');
}

function renderMore() {
  const more = $('lib-more');
  if (state.page < state.totalPages) {
    more.hidden = false;
    $('lib-page').textContent = `стр. ${state.page} из ${state.totalPages}`;
    $('load-more').disabled = false;
    $('load-more').textContent = 'Показать ещё';
  } else {
    more.hidden = true;
  }
}

async function loadMore() {
  if (!state.playlistId || state.page >= state.totalPages) return;
  const button = $('load-more');
  button.disabled = true;
  button.textContent = 'Загружаем…';

  const id = state.playlistId;
  try {
    const data = await api(
      `/api/playlist?id=${encodeURIComponent(id)}&page=${state.page + 1}&pageSize=50`,
    );
    if (state.playlistId !== id) return;
    const page = data.tracks || {};
    const items = Array.isArray(page.items) ? page.items : [];
    state.tracks = [...state.tracks, ...items];
    state.page = Number(page.page) || state.page + 1;
    state.totalPages = Number(page.totalPages) || state.totalPages;
    state.totalItems = Number(page.totalItems) || state.totalItems;
    renderTracks();
    renderMore();
    renderLibraryMeta();
  } catch (error) {
    toast(error.message, 'error');
    button.disabled = false;
    button.textContent = 'Показать ещё';
  }
}

function renderLibraryMeta() {
  const count = state.totalItems || state.tracks.length;
  $('pl-count').textContent = `${count} ${plural(count, 'трек', 'трека', 'треков')}`;
}

function showEmptyLibrary(title, text) {
  $('pl-name').textContent = (state.playlist && state.playlist.playlistName) || 'Плейлист';
  $('library-empty').hidden = false;
  $('library-empty').querySelector('h3').textContent = title || 'Пусто';
  $('library-empty').querySelector('p').textContent = text || '';
  els.trackList.innerHTML = '';
  $('lib-more').hidden = true;
}

/* ------------------------------- плеер --------------------------------- */

function setBuffer(loading) {
  const button = $('play');
  button.classList.toggle('is-loading', Boolean(loading));
}

function updatePlayIcon() {
  const icon = $('play').querySelector('use');
  icon.setAttribute('href', els.audio.paused ? '#i-play' : '#i-pause');
  $('play').setAttribute('aria-label', els.audio.paused ? 'Играть' : 'Пауза');
  els.player.classList.toggle('is-playing', !els.audio.paused);
}

function updateSeekUI() {
  const duration = Number.isFinite(els.audio.duration) ? els.audio.duration : 0;
  const current = els.audio.currentTime || 0;
  $('t-cur').textContent = fmtTime(current);
  $('t-dur').textContent = duration ? fmtTime(duration) : (state.current && state.current.duration) || '0:00';
  if (!seeking) {
    const percent = duration ? (current / duration) * 100 : 0;
    els.seek.value = String(Math.round(percent * 10));
    setFill(els.seek, percent);
  }
}

function renderNow() {
  const track = state.current;
  if (!track) {
    $('now-title').textContent = 'Ничего не играет';
    $('now-artist').textContent = 'Выбери трек';
    $('now-cover').style.cssText = coverStyle('');
    return;
  }

  $('now-title').textContent = track.title || 'Без названия';
  $('now-artist').textContent = track.artistsDisplayedName || 'Неизвестный исполнитель';
  $('now-cover').style.cssText = coverStyle(track.coverUrl);
  els.player.hidden = false;
  document.title = `${track.title || 'Lane Web'} — Lane Web`;
  updateSeekUI();
}

function markActiveTrack() {
  const rows = els.trackList.querySelectorAll('.track');
  rows.forEach((row) => {
    const index = Number(row.dataset.i);
    const track = state.tracks[index];
    const active = Boolean(track && state.current && track.songId === state.current.songId);
    row.classList.toggle('is-active', active);
    if (active) {
      row.querySelector('.t-index').innerHTML =
        '<span class="bars" aria-hidden="true"><i></i><i></i><i></i></span>';
    } else {
      const num = row.querySelector('.t-index');
      if (num && !num.querySelector('.t-num')) {
        num.innerHTML = `<span class="t-num">${index + 1}</span><svg class="i t-play" aria-hidden="true"><use href="#i-play"/></svg>`;
      }
    }
  });
}

async function playTrack(track, list, refId) {
  if (!track) return;
  if (!track.songId) {
    toast('У трека нет songId — нечего запрашивать', 'error');
    return;
  }
  state.current = track;
  state.queue = Array.isArray(list) && list.length ? list.slice() : [track];
  state.refId = refId || state.playlistId;
  retriedStream = 0;
  renderNow();
  markActiveTrack();
  setBuffer(true);
  await fetchStream({ autoplay: true });
}

function playIndex(index) {
  const track = state.tracks[index];
  if (!track) return;
  playTrack(track, state.tracks, state.playlistId);
}

async function fetchStream(options = {}) {
  const track = state.current;
  if (!track) return;
  const run = ++streamSeq;
  setBuffer(true);

  const refId = state.refId || state.playlistId || 'lane_likes';
  const url =
    `/api/stream?trackId=${encodeURIComponent(track.songId)}` +
    `&refId=${encodeURIComponent(refId)}&quality=${state.quality}`;

  try {
    const data = await api(url);
    if (run !== streamSeq) return;

    state.stream = {
      url: data.url,
      quality: data.quality,
      expiresAt: Date.now() + Math.max(30000, (data.ttl || 90000)) - 5000,
    };

    const resumeAt = Number(options.resumeAt) || 0;
    els.audio.src = data.url;
    if (resumeAt > 0) {
      els.audio.addEventListener(
        'loadedmetadata',
        () => {
          try {
            els.audio.currentTime = resumeAt;
          } catch {}
        },
        { once: true },
      );
    }

    if (options.autoplay) {
      try {
        await els.audio.play();
      } catch (error) {
        if (error && error.name === 'NotAllowedError') {
          toast('Нажми play ещё раз — браузер требует жест пользователя');
        } else if (error) {
          toast(`Не удалось воспроизвести: ${error.message}`, 'error');
        }
      }
    }
    updateQualityLabel();
  } catch (error) {
    if (run !== streamSeq) return;
    setBuffer(false);
    if (error.code === 'SESSION_EXPIRED') return;
    toast(`Не удалось получить поток: ${error.message}`, 'error');
  } finally {
    if (run === streamSeq) setBuffer(false);
  }
}

function updateQualityLabel() {
  $('pl-quality').textContent = qualityLabel();
}

async function togglePlay() {
  if (!state.current) {
    if (state.tracks.length) playIndex(0);
    return;
  }

  if (els.audio.paused) {
    if (!state.stream || Date.now() > state.stream.expiresAt) {
      await fetchStream({ autoplay: true, resumeAt: els.audio.currentTime });
      return;
    }
    try {
      await els.audio.play();
    } catch (error) {
      toast(`Не удалось воспроизвести: ${error.message}`, 'error');
    }
  } else {
    els.audio.pause();
  }
}

function step(delta) {
  if (!state.queue.length) return;
  const index = state.queue.findIndex((t) => state.current && t.songId === state.current.songId);
  let next = index === -1 ? 0 : index + delta;
  if (next < 0) next = state.queue.length - 1;
  if (next >= state.queue.length) next = 0;
  playTrack(state.queue[next], state.queue, state.refId);
}

function shufflePlay() {
  if (!state.tracks.length) return;
  const list = state.tracks.slice();
  for (let i = list.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [list[i], list[j]] = [list[j], list[i]];
  }
  playTrack(list[0], list, state.playlistId);
  toast('Перемешали плейлист');
}

function playAll() {
  if (!state.tracks.length) return;
  playTrack(state.tracks[0], state.tracks, state.playlistId);
}

async function changeQuality(quality) {
  if (!['BASIC', 'HIGH', 'ULTRA'].includes(quality)) return;
  const wasPlaying = Boolean(state.current) && !els.audio.paused;
  const position = els.audio.currentTime || 0;
  state.quality = quality;
  localStorage.setItem('lane.quality', quality);
  updateQualityLabel();
  if (state.current && state.stream) {
    await fetchStream({ autoplay: wasPlaying, resumeAt: position });
  }
}

function setVolume(value) {
  const volume = Math.max(0, Math.min(1, value));
  state.volume = volume;
  els.audio.volume = volume;
  els.volume.value = String(Math.round(volume * 100));
  setFill(els.volume, volume * 100);
  localStorage.setItem('lane.volume', String(volume));
}

/* ------------------------------- модалка -------------------------------- */

function openAuth() {
  els.modal.hidden = false;
  document.body.classList.add('modal-open');
  startAuth();
}

function closeAuth() {
  els.modal.hidden = true;
  document.body.classList.remove('modal-open');
  stopPolling();
  authRun += 1;
}

function stopPolling() {
  if (pollTimer) clearTimeout(pollTimer);
  pollTimer = null;
}

function authShell(inner) {
  els.authBody.innerHTML = inner;
}

function renderAuthLoading() {
  authShell(`
    <div class="auth-icon"><svg class="i"><use href="#i-telegram"/></svg></div>
    <h3 class="auth-title">Вход через Telegram</h3>
    <p class="auth-text">Готовим ключ сессии и ссылку на официального бота…</p>
    <div class="auth-wait"><span class="spinner"></span><span class="auth-status">Пару секунд</span></div>
  `);
}

function renderAuthWaiting() {
  const { url, id } = state.auth;
  authShell(`
    <div class="auth-icon"><svg class="i"><use href="#i-telegram"/></svg></div>
    <h3 class="auth-title">Подтверди вход в Telegram</h3>
    <p class="auth-text">
      Открой <b>@lane_music_bot</b> и нажми <b>Start</b>. Страница сама заметит
      подтверждение — обновлять ничего не нужно.
    </p>
    <div class="auth-wait">
      <span class="spinner"></span>
      <span class="auth-status" id="auth-status">Ждём подтверждения…</span>
    </div>
    <div class="auth-progress"><span id="auth-bar" style="width:0%"></span></div>
    <div class="auth-actions">
      <a class="btn btn-primary" href="${esc(url)}" target="_blank" rel="noopener noreferrer">
        <svg class="i"><use href="#i-telegram"/></svg>
        Открыть @lane_music_bot
      </a>
      <button class="btn btn-ghost" id="auth-copy">
        <svg class="i"><use href="#i-link"/></svg>
        Скопировать ссылку
      </button>
    </div>
    <div class="auth-id">ключ сессии: ${esc(id)}</div>
    <p class="auth-note"><svg class="i"><use href="#i-shield"/></svg> ключ одноразовый и живёт 20 минут</p>
  `);

  const copy = $('auth-copy');
  if (copy) {
    copy.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(state.auth.url);
        copy.querySelector('span')?.remove();
        copy.textContent = 'Ссылка скопирована';
        setTimeout(() => {
          copy.innerHTML = '<svg class="i"><use href="#i-link"/></svg> Скопировать ссылку';
        }, 1800);
      } catch {
        toast('Не удалось скопировать — выдели ссылку вручную', 'error');
      }
    });
  }

  updateAuthProgress();
}

function updateAuthProgress() {
  const status = $('auth-status');
  const bar = $('auth-bar');
  if (!status) return;
  const elapsed = Math.floor((Date.now() - (state.auth.startedAt || Date.now())) / 1000);
  status.innerHTML = `Ждём подтверждения… <b>${elapsed} с</b>`;
  if (bar) bar.style.width = `${Math.min(100, (elapsed / 120) * 100)}%`;
}

function renderAuthSuccess() {
  authShell(`
    <div class="auth-icon is-ok"><svg class="i"><use href="#i-check"/></svg></div>
    <h3 class="auth-title">Готово</h3>
    <p class="auth-text">Telegram подтвердил аккаунт. Подключаемся к Lane и подтягиваем плейлисты…</p>
    <ul class="auth-success-list">
      <li><svg class="i"><use href="#i-check"/></svg> сессия сохранена в защищённой cookie</li>
      <li><svg class="i"><use href="#i-check"/></svg> токен не доступен JavaScript страницы</li>
    </ul>
  `);
}

function renderAuthTimeout() {
  const { url } = state.auth;
  authShell(`
    <div class="auth-icon is-error"><svg class="i"><use href="#i-link"/></svg></div>
    <h3 class="auth-title">Пока не подтверждено</h3>
    <p class="auth-text">
      Бот не получил команду <b>Start</b>. Открой ссылку ещё раз и нажми Start —
      или продолжи ждать, ключ сессии ещё действует.
    </p>
    <div class="auth-actions">
      <a class="btn btn-primary" href="${esc(url)}" target="_blank" rel="noopener noreferrer">
        <svg class="i"><use href="#i-telegram"/></svg>
        Открыть @lane_music_bot
      </a>
      <button class="btn btn-ghost" id="auth-resume">Продолжить ожидание</button>
    </div>
    <div class="auth-id">ключ сессии: ${esc(state.auth.id)}</div>
  `);

  $('auth-resume')?.addEventListener('click', () => {
    state.auth.attempts = 0;
    state.auth.startedAt = Date.now();
    renderAuthWaiting();
    poll(++authRun);
  });
}

function renderAuthError(message, retry) {
  authShell(`
    <div class="auth-icon is-error"><svg class="i"><use href="#i-close"/></svg></div>
    <h3 class="auth-title">Не получилось</h3>
    <p class="auth-text" id="auth-error-text">${esc(message || 'Неизвестная ошибка')}</p>
    <div class="auth-actions">
      <button class="btn btn-primary" id="auth-retry">Попробовать снова</button>
    </div>
    <p class="auth-note"><svg class="i"><use href="#i-shield"/></svg> аккаунт и пароль не запрашиваются</p>
  `);
  $('auth-retry')?.addEventListener('click', retry || startAuth);
}

async function startAuth() {
  const run = ++authRun;
  stopPolling();
  renderAuthLoading();

  try {
    const data = await api('/api/auth/start', { method: 'POST' });
    if (run !== authRun) return;

    state.auth = {
      url: data.url,
      id: data.id,
      attempts: 0,
      startedAt: Date.now(),
      run,
    };
    renderAuthWaiting();
    poll(run);
  } catch (error) {
    if (run !== authRun) return;
    renderAuthError(error.message, startAuth);
  }
}

async function poll(run) {
  if (run !== authRun) return;
  if (els.modal.hidden) return;

  state.auth.attempts += 1;

  try {
    const data = await api('/api/auth/poll');
    if (run !== authRun) return;

    if (data.status === 'ok') {
      stopPolling();
      renderAuthSuccess();
      setTimeout(() => finishAuth(), 900);
      return;
    }

    if (data.status === 'expired') {
      stopPolling();
      renderAuthError('Ключ ожидания истёк. Начни заново — это займёт пару секунд.', startAuth);
      return;
    }

    updateAuthProgress();

    if (state.auth.attempts >= 40) {
      renderAuthTimeout();
      return;
    }

    pollTimer = setTimeout(() => poll(run), 3000);
  } catch (error) {
    if (run !== authRun) return;
    if (state.auth.attempts >= 40) {
      renderAuthTimeout();
      return;
    }
    updateAuthProgress();
    pollTimer = setTimeout(() => poll(run), 4000);
  }
}

async function finishAuth() {
  closeAuth();

  try {
    const res = await fetch('/api/me', { headers: { accept: 'application/json' }, credentials: 'same-origin' });
    if (!res.ok) throw new Error('профиль недоступен');
    const me = await res.json();
    enterApp(me);
    toast('Вошли — можно слушать');
  } catch (error) {
    toast(`Не удалось открыть сессию: ${error.message}`, 'error');
    showLanding();
  }
}

/* ------------------------------ события -------------------------------- */

function wireStatic() {
  $('hero-login').addEventListener('click', openAuth);
  els.topLogin.addEventListener('click', openAuth);
  $('auth-close').addEventListener('click', closeAuth);
  els.modal.addEventListener('click', (event) => {
    if (event.target.dataset.close === '1') closeAuth();
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !els.modal.hidden) closeAuth();
  });

  els.playlistList.addEventListener('click', (event) => {
    const button = event.target.closest('.pl');
    if (!button) return;
    openPlaylist(button.dataset.id, { push: true });
  });

  els.trackList.addEventListener('click', (event) => {
    const row = event.target.closest('.track');
    if (!row || row.classList.contains('track-skeleton')) return;
    playIndex(Number(row.dataset.i));
  });

  els.trackList.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    const row = event.target.closest('.track');
    if (!row || row.classList.contains('track-skeleton')) return;
    event.preventDefault();
    playIndex(Number(row.dataset.i));
  });

  $('play-all').addEventListener('click', playAll);
  $('shuffle').addEventListener('click', shufflePlay);
  $('load-more').addEventListener('click', loadMore);

  $('play').addEventListener('click', togglePlay);
  $('prev').addEventListener('click', () => step(-1));
  $('next').addEventListener('click', () => step(1));

  $('top-logout').addEventListener('click', logout);
  $('side-logout').addEventListener('click', logout);

  els.quality.addEventListener('change', () => changeQuality(els.quality.value));

  els.volume.addEventListener('input', () => setVolume(Number(els.volume.value) / 100));

  els.seek.addEventListener('input', () => {
    seeking = true;
    const percent = Number(els.seek.value) / 10;
    const duration = Number.isFinite(els.audio.duration) ? els.audio.duration : 0;
    setFill(els.seek, percent);
    $('t-cur').textContent = fmtTime((percent / 100) * duration);
  });

  els.seek.addEventListener('change', () => {
    const percent = Number(els.seek.value) / 10;
    const duration = Number.isFinite(els.audio.duration) ? els.audio.duration : 0;
    if (duration) {
      try {
        els.audio.currentTime = (percent / 100) * duration;
      } catch {}
    }
    seeking = false;
  });

  els.audio.addEventListener('play', updatePlayIcon);
  els.audio.addEventListener('pause', updatePlayIcon);
  els.audio.addEventListener('timeupdate', updateSeekUI);
  els.audio.addEventListener('loadedmetadata', updateSeekUI);
  els.audio.addEventListener('durationchange', updateSeekUI);
  els.audio.addEventListener('waiting', () => setBuffer(true));
  els.audio.addEventListener('playing', () => setBuffer(false));
  els.audio.addEventListener('canplay', () => setBuffer(false));

  els.audio.addEventListener('ended', () => {
    if (state.queue.length > 1) step(1);
    else updatePlayIcon();
  });

  els.audio.addEventListener('error', async () => {
    if (!state.current || retriedStream >= 1) return;
    retriedStream += 1;
    const position = els.audio.currentTime || 0;
    toast('Ссылка на поток обновилась — продолжаем');
    await fetchStream({ autoplay: true, resumeAt: position });
  });

  document.addEventListener('keydown', (event) => {
    if (event.target.matches('input, select, textarea, button, a')) return;
    if (event.code !== 'Space') return;
    if (els.app.hidden) return;
    event.preventDefault();
    togglePlay();
  });

  window.addEventListener('popstate', () => {
    if (els.app.hidden) return;
    const id = parseHash();
    if (id && id !== state.playlistId) openPlaylist(id, { push: false });
  });
}

function init() {
  wireStatic();

  if (!['BASIC', 'HIGH', 'ULTRA'].includes(state.quality)) state.quality = 'HIGH';
  els.quality.value = state.quality;
  updateQualityLabel();

  let volume = 0.9;
  try {
    const stored = localStorage.getItem('lane.volume');
    if (stored != null) volume = Number(stored);
  } catch {}
  if (!Number.isFinite(volume)) volume = 0.9;
  setVolume(volume);

  (async () => {
    try {
      const res = await fetch('/api/me', { headers: { accept: 'application/json' }, credentials: 'same-origin' });
      if (res.ok) {
        const me = await res.json();
        enterApp(me);
        return;
      }
    } catch {}
    showLanding();
  })();
}

document.addEventListener('DOMContentLoaded', init);
