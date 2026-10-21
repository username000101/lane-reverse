'use strict';

/**
 * Lane Proxy Gateway — Frontend Controller
 * Minimalist, high-performance, responsive audio workspace.
 */

(function () {
  // --- DOM Elements Cache ---
  const els = {
    viewAuth: document.getElementById('view-auth'),
    viewApp: document.getElementById('view-app'),
    upstreamPill: document.getElementById('upstream-pill'),
    userBar: document.getElementById('user-bar'),
    unauthStatus: document.getElementById('unauth-status'),
    userAvatar: document.getElementById('user-avatar'),
    userName: document.getElementById('user-name'),
    userHandle: document.getElementById('user-handle'),
    btnLogout: document.getElementById('btn-logout'),
    btnOpenConfig: document.getElementById('btn-open-config'),

    // Auth Screen
    authIdle: document.getElementById('auth-idle'),
    authActive: document.getElementById('auth-active'),
    btnStartAuth: document.getElementById('btn-start-auth'),
    tabQr: document.getElementById('tab-qr'),
    tabDirect: document.getElementById('tab-direct'),
    paneQr: document.getElementById('pane-qr'),
    paneDirect: document.getElementById('pane-direct'),
    qrCanvas: document.getElementById('qr-canvas'),
    btnTgLink: document.getElementById('btn-tg-link'),
    authUrlInput: document.getElementById('auth-url-input'),
    btnCopyUrl: document.getElementById('btn-copy-url'),
    btnRestartAuth: document.getElementById('btn-restart-auth'),
    pollMessage: document.getElementById('poll-message'),
    pollTimerText: document.getElementById('poll-timer-text'),
    pollProgress: document.getElementById('poll-progress'),

    // Dashboard
    dashAvatar: document.getElementById('dash-avatar'),
    dashName: document.getElementById('dash-name'),
    dashBadge: document.getElementById('dash-badge'),
    dashLdi: document.getElementById('dash-ldi'),
    btnCopyLdi: document.getElementById('btn-copy-ldi'),
    btnDashExport: document.getElementById('btn-dash-export'),
    shelfNav: document.getElementById('shelf-nav'),
    searchTracks: document.getElementById('search-tracks'),
    btnClearSearch: document.getElementById('btn-clear-search'),
    activePlTitle: document.getElementById('active-pl-title'),
    activePlDetails: document.getElementById('active-pl-details'),
    btnPlayAll: document.getElementById('btn-play-all'),
    tracksList: document.getElementById('tracks-list'),
    tracksEmpty: document.getElementById('tracks-empty'),
    tracksPagination: document.getElementById('tracks-pagination'),
    btnLoadMore: document.getElementById('btn-load-more'),

    // Player
    playerBar: document.getElementById('player-bar'),
    playerCover: document.getElementById('player-cover'),
    playerTitle: document.getElementById('player-title'),
    playerArtist: document.getElementById('player-artist'),
    playerEq: document.getElementById('player-eq'),
    ctrlPrev: document.getElementById('ctrl-prev'),
    ctrlPlay: document.getElementById('ctrl-play'),
    ctrlPlayIcon: document.getElementById('ctrl-play-icon'),
    ctrlNext: document.getElementById('ctrl-next'),
    timeCurrent: document.getElementById('time-current'),
    timeTotal: document.getElementById('time-total'),
    seekSlider: document.getElementById('seek-slider'),
    streamQuality: document.getElementById('stream-quality'),
    btnVolMute: document.getElementById('btn-vol-mute'),
    volIcon: document.getElementById('vol-icon'),
    volumeSlider: document.getElementById('volume-slider'),
    audio: document.getElementById('audio-player'),

    // Modal
    modalConfig: document.getElementById('modal-config'),
    modalConfigBackdrop: document.getElementById('modal-config-backdrop'),
    btnCloseModal: document.getElementById('btn-close-modal'),
    codeConfigText: document.getElementById('code-config-text'),
    btnCopyConfig: document.getElementById('btn-copy-config'),
    btnDownloadConfig: document.getElementById('btn-download-config'),

    // Toasts
    toasts: document.getElementById('toasts'),
  };

  // --- State ---
  const state = {
    session: null,
    auth: {
      id: null,
      url: null,
      pollTimer: null,
      countdownTimer: null,
      startedAt: null,
      expiresAt: null,
      attempts: 0,
      maxAttempts: 25,
    },
    playlists: [],
    activePlaylistId: 'lane_likes',
    tracks: [],
    filteredTracks: [],
    page: 1,
    pageSize: 50,
    hasMore: false,
    searchQuery: '',

    // Player
    currentTrack: null,
    currentTrackIndex: -1,
    isPlaying: false,
    quality: localStorage.getItem('lane_quality') || 'ULTRA',
    volume: parseFloat(localStorage.getItem('lane_volume') || '0.9'),
    isMuted: false,
    isSeeking: false,
    configData: null,
  };

  // --- API Helper ---
  async function api(path, options = {}) {
    const res = await fetch(path, {
      method: options.method || 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {
        'Accept': 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {}),
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
    });

    const data = await res.json().catch(() => null);
    return { ok: res.ok, status: res.status, data };
  }

  // --- Toast Notifications ---
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type === 'success' ? 'toast-success' : ''}`;
    toast.innerHTML = `
      <svg class="icon sm toast-icon"><use href="#${type === 'success' ? 'i-check' : 'i-shield'}"/></svg>
      <span>${escapeHtml(message)}</span>
    `;
    els.toasts.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 260);
    }, 3200);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, (s) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[s]));
  }

  function formatDuration(sec) {
    if (!Number.isFinite(sec) || sec < 0) return '0:00';
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  // --- View Transitions ---
  function showAuthView() {
    state.session = null;
    els.viewAuth.hidden = false;
    els.viewApp.hidden = true;
    els.userBar.hidden = true;
    els.unauthStatus.hidden = false;
    els.playerBar.hidden = true;
    if (state.audio) {
      state.audio.pause();
      state.isPlaying = false;
      updatePlayButtonUI();
    }
  }

  function showAppView(session) {
    state.session = session;
    els.viewAuth.hidden = true;
    els.viewApp.hidden = false;
    els.userBar.hidden = false;
    els.unauthStatus.hidden = true;

    // Render User Details
    const user = session.user || {};
    const name = user.displayedName || user.name || user.username || 'Пользователь';
    const initial = name.trim().charAt(0).toUpperCase() || 'L';

    els.userName.textContent = name;
    els.userAvatar.textContent = initial;
    els.userHandle.textContent = user.username ? `@${user.username}` : (session.ldi ? `ID: ${session.ldi.slice(0, 8)}` : '');

    els.dashName.textContent = name;
    els.dashAvatar.textContent = initial;
    els.dashLdi.textContent = session.ldi || '—';

    // Stop auth polling if active
    stopAuthPolling();

    // Fetch library
    loadPlaylists();
  }

  // --- Auth Flow ---
  async function startAuth() {
    els.btnStartAuth.disabled = true;
    els.btnStartAuth.innerHTML = '<span class="dot-pulse"></span><span>Генерация сессии...</span>';

    const res = await api('/api/auth/start', { method: 'POST' });
    els.btnStartAuth.disabled = false;
    els.btnStartAuth.innerHTML = '<svg class="icon icon-tg"><use href="#i-tg"/></svg><span>Подключиться через Telegram</span>';

    if (!res.ok || !res.data || !res.data.url) {
      showToast(res.data?.error || 'Ошибка инициализации сессии', 'error');
      return;
    }

    const { id, url, expiresIn } = res.data;
    state.auth.id = id;
    state.auth.url = url;
    state.auth.startedAt = Date.now();
    state.auth.expiresAt = Date.now() + (expiresIn || 1200) * 1000;
    state.auth.attempts = 0;

    // Switch card UI
    els.authIdle.hidden = true;
    els.authActive.hidden = false;

    // Render QR Code
    renderQrCode(url);

    // Direct link & input
    els.btnTgLink.href = url;
    els.authUrlInput.value = url;

    // Begin Polling Loop
    startAuthPolling();
  }

  function renderQrCode(text) {
    if (typeof window.qrcode !== 'function') {
      console.warn('qrcode generator not ready');
      return;
    }

    try {
      const qr = window.qrcode(0, 'M');
      qr.addData(text);
      qr.make();

      const canvas = els.qrCanvas;
      const ctx = canvas.getContext('2d');
      const count = qr.getModuleCount();
      const size = canvas.width;
      const pad = 12;
      const cellSize = (size - pad * 2) / count;

      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, size, size);

      ctx.fillStyle = '#09090b';
      for (let r = 0; r < count; r++) {
        for (let c = 0; c < count; c++) {
          if (qr.isDark(r, c)) {
            // Draw clean rounded modules
            const x = pad + c * cellSize;
            const y = pad + r * cellSize;
            ctx.fillRect(x, y, cellSize + 0.5, cellSize + 0.5);
          }
        }
      }
    } catch (err) {
      console.error('QR render error:', err);
    }
  }

  function startAuthPolling() {
    stopAuthPolling();

    // Countdown / status update tick
    state.auth.countdownTimer = setInterval(updatePollTimer, 1000);
    updatePollTimer();

    // Immediate first poll, then interval
    pollAuth();
    state.auth.pollTimer = setInterval(pollAuth, 2500);
  }

  function stopAuthPolling() {
    if (state.auth.pollTimer) {
      clearInterval(state.auth.pollTimer);
      state.auth.pollTimer = null;
    }
    if (state.auth.countdownTimer) {
      clearInterval(state.auth.countdownTimer);
      state.auth.countdownTimer = null;
    }
  }

  function updatePollTimer() {
    const remainMs = Math.max(0, state.auth.expiresAt - Date.now());
    const sec = Math.floor(remainMs / 1000);
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    const timeStr = `${m}:${s < 10 ? '0' : ''}${s}`;

    els.pollTimerText.textContent = `Опрос каждые 2.5 с • Доступно: ${timeStr} • Попытка ${state.auth.attempts}`;

    const totalDur = (state.auth.expiresAt - state.auth.startedAt) || 1200000;
    const progress = Math.min(100, Math.max(0, ((Date.now() - state.auth.startedAt) / totalDur) * 100));
    els.pollProgress.style.width = `${progress}%`;

    if (remainMs <= 0) {
      stopAuthPolling();
      els.pollMessage.textContent = 'Время сессии истекло. Сгенерируйте новый ключ.';
      els.pollMessage.style.color = '#fb7185';
    }
  }

  async function pollAuth() {
    state.auth.attempts++;

    const res = await api('/api/auth/poll');
    if (!res.ok) {
      if (res.status === 401 || res.status === 403) {
        stopAuthPolling();
        showToast('Сессия недействительна. Начните сначала.', 'error');
      }
      return;
    }

    const data = res.data || {};
    if (data.status === 'ok') {
      stopAuthPolling();
      showToast('Авторизация подтверждена!', 'success');
      showAppView({ user: data.user, ldi: data.ldi });
    } else if (data.status === 'expired') {
      stopAuthPolling();
      els.pollMessage.textContent = 'Сессия устарела. Нажмите обновить.';
      els.pollMessage.style.color = '#fb7185';
    }
  }

  async function logout() {
    await api('/api/auth/logout', { method: 'POST' });
    state.configData = null;
    showToast('Вы вышли из системы');
    showAuthView();
  }

  // --- Library & Playlists ---
  async function loadPlaylists() {
    // 1. Load favorite tracks count/tracks
    await loadPlaylistTracks('lane_likes');

    // 2. Fetch user playlists
    const res = await api('/api/playlists');
    if (res.ok && Array.isArray(res.data?.playlists)) {
      state.playlists = res.data.playlists;
      renderPlaylistsNav();
    }
  }

  function renderPlaylistsNav() {
    const nav = els.shelfNav;
    // Keep likes button, remove dynamic user playlist buttons
    const userButtons = nav.querySelectorAll('.dynamic-pl-pill');
    userButtons.forEach((b) => b.remove());

    state.playlists.forEach((pl) => {
      const id = pl.playlistId;
      if (id === 'lane_likes') return; // Handled by pill-likes

      const btn = document.createElement('button');
      btn.className = `shelf-pill dynamic-pl-pill ${state.activePlaylistId === id ? 'active' : ''}`;
      btn.dataset.playlist = id;
      const count = Array.isArray(pl.playlistTracksIds) ? pl.playlistTracksIds.length : (pl.tracksCount || '');
      btn.innerHTML = `
        <span class="pill-name">${escapeHtml(pl.playlistName || 'Плейлист')}</span>
        ${count !== '' ? `<span class="pill-count">${count}</span>` : ''}
      `;
      btn.addEventListener('click', () => {
        selectPlaylist(id, pl.playlistName);
      });
      nav.appendChild(btn);
    });
  }

  function selectPlaylist(id, title = null) {
    if (state.activePlaylistId === id) return;
    state.activePlaylistId = id;

    // Update active pill state
    const allPills = els.shelfNav.querySelectorAll('.shelf-pill');
    allPills.forEach((p) => {
      p.classList.toggle('active', p.dataset.playlist === id);
    });

    els.activePlTitle.textContent = title || (id === 'lane_likes' ? 'Мне нравится' : 'Плейлист');
    loadPlaylistTracks(id);
  }

  async function loadPlaylistTracks(id, page = 1) {
    if (page === 1) {
      els.tracksList.innerHTML = `
        <div class="track-row skeleton"></div>
        <div class="track-row skeleton"></div>
        <div class="track-row skeleton"></div>
        <div class="track-row skeleton"></div>
      `;
      els.tracksEmpty.hidden = true;
      els.tracksPagination.hidden = true;
    }

    const res = await api(`/api/playlist?id=${encodeURIComponent(id)}&page=${page}&pageSize=50`);
    if (!res.ok) {
      if (page === 1) {
        els.tracksList.innerHTML = '';
        els.tracksEmpty.hidden = false;
        els.activePlDetails.textContent = 'Ошибка загрузки плейлиста';
      }
      return;
    }

    const payload = res.data || {};
    const trackItems = Array.isArray(payload.tracks)
      ? payload.tracks
      : (payload.tracks?.items || []);

    if (page === 1) {
      state.tracks = trackItems;
    } else {
      state.tracks = state.tracks.concat(trackItems);
    }

    state.page = page;
    state.hasMore = trackItems.length >= 50;
    els.tracksPagination.hidden = !state.hasMore;

    if (id === 'lane_likes') {
      const total = payload.tracks?.total || state.tracks.length;
      document.getElementById('count-likes').textContent = total;
    }

    els.activePlDetails.textContent = `${state.tracks.length} треков · Lane Audio Stream`;
    applyTrackFilter();
  }

  function applyTrackFilter() {
    const q = state.searchQuery.trim().toLowerCase();
    if (!q) {
      state.filteredTracks = state.tracks;
    } else {
      state.filteredTracks = state.tracks.filter((t) => {
        const title = (t.title || '').toLowerCase();
        const artist = (t.artistsDisplayedName || '').toLowerCase();
        return title.includes(q) || artist.includes(q);
      });
    }

    renderTracksList();
  }

  function renderTracksList() {
    const container = els.tracksList;
    container.innerHTML = '';

    if (!state.filteredTracks.length) {
      els.tracksEmpty.hidden = false;
      return;
    }
    els.tracksEmpty.hidden = true;

    state.filteredTracks.forEach((track, idx) => {
      const row = document.createElement('div');
      const isCurrent = state.currentTrack && state.currentTrack.songId === track.songId;
      row.className = `track-row ${isCurrent ? 'is-active' : ''}`;
      row.dataset.songId = track.songId;

      const durStr = typeof track.duration === 'string'
        ? track.duration
        : (typeof track.duration === 'number' ? formatDuration(track.duration) : '—');

      const coverImg = track.coverUrl
        ? `style="background-image: url('${escapeHtml(track.coverUrl)}')"`
        : '';

      const albumName = track.spData?.album || '';

      row.innerHTML = `
        <div class="track-num-col">
          <span class="track-index">${idx + 1}</span>
          <span class="track-play-hover">
            <svg class="icon sm"><use href="#${isCurrent && state.isPlaying ? 'i-pause' : 'i-play'}"/></svg>
          </span>
        </div>
        <div class="track-title-col">
          <div class="track-cover-thumb" ${coverImg}></div>
          <div class="track-names">
            <div class="track-title">${escapeHtml(track.title || 'Без названия')}</div>
            <div class="track-artist">${escapeHtml(track.artistsDisplayedName || 'Неизвестный исполнитель')}</div>
          </div>
        </div>
        <div class="track-album">${escapeHtml(albumName)}</div>
        <div class="track-dur">${escapeHtml(durStr)}</div>
      `;

      row.addEventListener('click', () => {
        playTrack(track, idx);
      });

      container.appendChild(row);
    });
  }

  // --- Audio Player Engine ---
  async function playTrack(track, index = -1) {
    if (state.currentTrack && state.currentTrack.songId === track.songId) {
      // Toggle pause / play
      if (state.isPlaying) {
        state.audio.pause();
      } else {
        state.audio.play().catch(console.error);
      }
      return;
    }

    state.currentTrack = track;
    state.currentTrackIndex = index;

    // Update bottom bar info
    els.playerBar.hidden = false;
    els.playerTitle.textContent = track.title || 'Без названия';
    els.playerArtist.textContent = track.artistsDisplayedName || 'Неизвестный исполнитель';
    if (track.coverUrl) {
      els.playerCover.style.backgroundImage = `url('${track.coverUrl}')`;
    } else {
      els.playerCover.style.backgroundImage = 'none';
    }

    highlightActiveTrackRow();

    // Fetch stream from signed proxy
    const res = await api(`/api/stream?trackId=${encodeURIComponent(track.songId)}&refId=${encodeURIComponent(state.activePlaylistId)}&quality=${state.quality}`);
    if (!res.ok || !res.data?.url) {
      showToast(res.data?.error || 'Не удалось получить аудиопоток', 'error');
      return;
    }

    state.audio.src = res.data.url;
    state.audio.volume = state.isMuted ? 0 : state.volume;
    state.audio.play().catch((err) => {
      console.warn('Playback error:', err);
    });
  }

  function playNextTrack() {
    if (!state.filteredTracks.length) return;
    let nextIdx = state.currentTrackIndex + 1;
    if (nextIdx >= state.filteredTracks.length) nextIdx = 0;
    playTrack(state.filteredTracks[nextIdx], nextIdx);
  }

  function playPrevTrack() {
    if (!state.filteredTracks.length) return;
    let prevIdx = state.currentTrackIndex - 1;
    if (prevIdx < 0) prevIdx = state.filteredTracks.length - 1;
    playTrack(state.filteredTracks[prevIdx], prevIdx);
  }

  function highlightActiveTrackRow() {
    const rows = els.tracksList.querySelectorAll('.track-row');
    rows.forEach((r) => {
      const isActive = state.currentTrack && r.dataset.songId === state.currentTrack.songId;
      r.classList.toggle('is-active', isActive);
      const iconUse = r.querySelector('.track-play-hover use');
      if (iconUse) {
        iconUse.setAttribute('href', isActive && state.isPlaying ? '#i-pause' : '#i-play');
      }
    });
  }

  function updatePlayButtonUI() {
    els.ctrlPlayIcon.setAttribute('href', state.isPlaying ? '#i-pause' : '#i-play');
    document.body.classList.toggle('is-audio-playing', state.isPlaying);
    document.querySelector('.header').classList.toggle('is-playing', state.isPlaying);
    highlightActiveTrackRow();
  }

  // Audio Event Listeners
  els.audio.addEventListener('play', () => {
    state.isPlaying = true;
    updatePlayButtonUI();
  });

  els.audio.addEventListener('pause', () => {
    state.isPlaying = false;
    updatePlayButtonUI();
  });

  els.audio.addEventListener('ended', () => {
    playNextTrack();
  });

  els.audio.addEventListener('timeupdate', () => {
    if (state.isSeeking) return;
    const cur = els.audio.currentTime || 0;
    const dur = els.audio.duration || 0;

    els.timeCurrent.textContent = formatDuration(cur);
    els.timeTotal.textContent = formatDuration(dur);

    if (dur > 0) {
      const val = (cur / dur) * 1000;
      els.seekSlider.value = val;
      els.seekSlider.style.backgroundSize = `${(val / 1000) * 100}% 100%`;
    }
  });

  // Controls Event Listeners
  els.ctrlPlay.addEventListener('click', () => {
    if (!state.currentTrack && state.filteredTracks.length) {
      playTrack(state.filteredTracks[0], 0);
      return;
    }
    if (state.isPlaying) state.audio.pause();
    else state.audio.play().catch(console.error);
  });

  els.ctrlNext.addEventListener('click', playNextTrack);
  els.ctrlPrev.addEventListener('click', playPrevTrack);

  els.seekSlider.addEventListener('input', () => {
    state.isSeeking = true;
    const dur = els.audio.duration || 0;
    const cur = (els.seekSlider.value / 1000) * dur;
    els.timeCurrent.textContent = formatDuration(cur);
    els.seekSlider.style.backgroundSize = `${(els.seekSlider.value / 1000) * 100}% 100%`;
  });

  els.seekSlider.addEventListener('change', () => {
    const dur = els.audio.duration || 0;
    els.audio.currentTime = (els.seekSlider.value / 1000) * dur;
    state.isSeeking = false;
  });

  // Quality switch (seamless reload at same time)
  els.streamQuality.value = state.quality;
  els.streamQuality.addEventListener('change', async (e) => {
    const newQual = e.target.value;
    state.quality = newQual;
    localStorage.setItem('lane_quality', newQual);

    if (state.currentTrack && !els.audio.paused) {
      const curTime = els.audio.currentTime;
      showToast(`Качество: ${newQual}`);
      const res = await api(`/api/stream?trackId=${encodeURIComponent(state.currentTrack.songId)}&refId=${encodeURIComponent(state.activePlaylistId)}&quality=${newQual}`);
      if (res.ok && res.data?.url) {
        els.audio.src = res.data.url;
        els.audio.currentTime = curTime;
        els.audio.play().catch(console.error);
      }
    }
  });

  // Volume
  els.volumeSlider.value = state.volume * 100;
  els.volumeSlider.style.backgroundSize = `${state.volume * 100}% 100%`;
  els.volumeSlider.addEventListener('input', (e) => {
    const val = parseFloat(e.target.value) / 100;
    state.volume = val;
    state.isMuted = val === 0;
    els.audio.volume = val;
    localStorage.setItem('lane_volume', String(val));
    els.volumeSlider.style.backgroundSize = `${val * 100}% 100%`;
    els.volIcon.setAttribute('href', state.isMuted ? '#i-mute' : '#i-vol');
  });

  els.btnVolMute.addEventListener('click', () => {
    state.isMuted = !state.isMuted;
    els.audio.volume = state.isMuted ? 0 : state.volume;
    els.volIcon.setAttribute('href', state.isMuted ? '#i-mute' : '#i-vol');
    if (!state.isMuted && state.volume === 0) {
      state.volume = 0.5;
      els.audio.volume = 0.5;
      els.volumeSlider.value = 50;
      els.volumeSlider.style.backgroundSize = '50% 100%';
    }
  });

  // --- Search / Filter ---
  els.searchTracks.addEventListener('input', (e) => {
    state.searchQuery = e.target.value;
    els.btnClearSearch.hidden = !state.searchQuery;
    applyTrackFilter();
  });

  els.btnClearSearch.addEventListener('click', () => {
    state.searchQuery = '';
    els.searchTracks.value = '';
    els.btnClearSearch.hidden = true;
    applyTrackFilter();
  });

  // Play All Button
  els.btnPlayAll.addEventListener('click', () => {
    if (state.filteredTracks.length) {
      playTrack(state.filteredTracks[0], 0);
    }
  });

  // Load More Tracks
  els.btnLoadMore.addEventListener('click', () => {
    loadPlaylistTracks(state.activePlaylistId, state.page + 1);
  });

  // Likes pill click
  document.getElementById('pill-likes').addEventListener('click', () => {
    selectPlaylist('lane_likes', 'Мне нравится');
  });

  // --- Config Modal & Export ---
  async function openConfigModal() {
    els.modalConfig.hidden = false;
    els.codeConfigText.textContent = 'Загрузка данных сессии...';

    const res = await api('/api/config');
    if (!res.ok || !res.data) {
      els.codeConfigText.textContent = 'Ошибка загрузки конфигурации.';
      return;
    }

    state.configData = res.data;
    els.codeConfigText.textContent = JSON.stringify(res.data, null, 2);
  }

  function closeConfigModal() {
    els.modalConfig.hidden = true;
  }

  els.btnOpenConfig.addEventListener('click', openConfigModal);
  els.btnDashExport.addEventListener('click', openConfigModal);
  els.btnCloseModal.addEventListener('click', closeConfigModal);
  els.modalConfigBackdrop.addEventListener('click', closeConfigModal);

  els.btnCopyConfig.addEventListener('click', () => {
    if (!state.configData) return;
    navigator.clipboard.writeText(JSON.stringify(state.configData, null, 2)).then(() => {
      showToast('Конфиг скопирован в буфер обмена', 'success');
    });
  });

  els.btnDownloadConfig.addEventListener('click', () => {
    if (!state.configData) return;
    const blob = new Blob([JSON.stringify(state.configData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'laned.json';
    a.click();
    URL.revokeObjectURL(url);
    showToast('Файл laned.json скачан', 'success');
  });

  // Copy Buttons
  els.btnCopyUrl.addEventListener('click', () => {
    if (!state.auth.url) return;
    navigator.clipboard.writeText(state.auth.url).then(() => {
      showToast('Ссылка скопирована', 'success');
    });
  });

  els.btnCopyLdi.addEventListener('click', () => {
    if (!state.session?.ldi) return;
    navigator.clipboard.writeText(state.session.ldi).then(() => {
      showToast('LDI скопирован', 'success');
    });
  });

  // Tab switching in Auth
  els.tabQr.addEventListener('click', () => {
    els.tabQr.classList.add('active');
    els.tabDirect.classList.remove('active');
    els.paneQr.hidden = false;
    els.paneDirect.hidden = true;
  });

  els.tabDirect.addEventListener('click', () => {
    els.tabDirect.classList.add('active');
    els.tabQr.classList.remove('active');
    els.paneDirect.hidden = false;
    els.paneQr.hidden = true;
  });

  els.btnStartAuth.addEventListener('click', startAuth);
  els.btnRestartAuth.addEventListener('click', startAuth);
  els.btnLogout.addEventListener('click', logout);

  // Keyboard Shortcuts
  window.addEventListener('keydown', (e) => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;

    if (e.code === 'Space') {
      e.preventDefault();
      els.ctrlPlay.click();
    } else if (e.code === 'ArrowRight') {
      e.preventDefault();
      els.audio.currentTime = Math.min(els.audio.duration || 0, els.audio.currentTime + 5);
    } else if (e.code === 'ArrowLeft') {
      e.preventDefault();
      els.audio.currentTime = Math.max(0, els.audio.currentTime - 5);
    } else if (e.code === 'Escape') {
      closeConfigModal();
    }
  });

  // Initial Boot
  async function init() {
    // Check existing session
    const res = await api('/api/me');
    if (res.ok && res.data?.ldi) {
      showAppView(res.data);
    } else {
      showAuthView();
    }

    // Ping health for status pill
    api('/api/health').then((h) => {
      if (h.ok) {
        els.upstreamPill.title = `Соединение с ${h.data?.upstream || 'Lane API'}: активно`;
      }
    });
  }

  init();
})();
