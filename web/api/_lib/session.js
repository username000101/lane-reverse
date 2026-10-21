'use strict';

/**
 * Cookies + зашифрованная сессия.
 *
 * JWT Lane живёт только в HttpOnly-cookie, зашифрованной AES-256-GCM:
 * браузер не может её прочитать или подделать, а функции Vercel остаются
 * без состояния (никакой БД не нужно).
 */

const crypto = require('crypto');

const COOKIE_SESSION = 'lane_session';
const COOKIE_PENDING = 'lane_pending';

const PENDING_TTL = 20 * 60; // 20 минут
const SESSION_TTL = 30 * 24 * 60 * 60; // 30 дней

const DEV_SECRET = 'lane-web-dev-secret-change-me';

function sessionKey() {
  const secret = process.env.LANE_SESSION_SECRET || DEV_SECRET;
  return crypto.createHash('sha256').update(`lane-web:v1:${secret}`).digest();
}

function seal(payload) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', sessionKey(), iv);
  const ct = Buffer.concat([cipher.update(JSON.stringify(payload), 'utf8'), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), ct]).toString('base64url');
}

function unseal(value) {
  if (typeof value !== 'string' || value.length < 40) return null;
  try {
    const raw = Buffer.from(value, 'base64url');
    if (raw.length < 29) return null;
    const iv = raw.subarray(0, 12);
    const tag = raw.subarray(12, 28);
    const ct = raw.subarray(28);
    const decipher = crypto.createDecipheriv('aes-256-gcm', sessionKey(), iv);
    decipher.setAuthTag(tag);
    const plain = Buffer.concat([decipher.update(ct), decipher.final()]).toString('utf8');
    const obj = JSON.parse(plain);
    return obj && typeof obj === 'object' ? obj : null;
  } catch {
    return null;
  }
}

function parseCookies(req) {
  const header = (req && req.headers && req.headers.cookie) || '';
  const out = {};
  for (const part of header.split(';')) {
    const eq = part.indexOf('=');
    if (eq === -1) continue;
    const name = part.slice(0, eq).trim();
    if (!name) continue;
    out[name] = decodeURIComponent(part.slice(eq + 1).trim());
  }
  return out;
}

function getCookie(req, name) {
  return parseCookies(req)[name];
}

function cookieString(name, value, maxAge) {
  return [
    `${name}=${encodeURIComponent(value)}`,
    'Path=/',
    'HttpOnly',
    'Secure',
    'SameSite=Lax',
    `Max-Age=${maxAge}`,
  ].join('; ');
}

function appendCookie(res, value) {
  const prev = res.getHeader('Set-Cookie');
  if (!prev) res.setHeader('Set-Cookie', [value]);
  else if (Array.isArray(prev)) res.setHeader('Set-Cookie', [...prev, value]);
  else res.setHeader('Set-Cookie', [prev, value]);
}

function setSessionCookie(res, session) {
  appendCookie(res, cookieString(COOKIE_SESSION, seal(session), SESSION_TTL));
}

function clearSessionCookie(res) {
  appendCookie(res, cookieString(COOKIE_SESSION, '', 0));
}

function setPendingCookie(res, pending) {
  appendCookie(res, cookieString(COOKIE_PENDING, seal(pending), PENDING_TTL));
}

function clearPendingCookie(res) {
  appendCookie(res, cookieString(COOKIE_PENDING, '', 0));
}

/** Валидная сессия из cookie или null. */
function readSession(req) {
  const data = unseal(getCookie(req, COOKIE_SESSION));
  if (!data || typeof data.token !== 'string' || typeof data.ldi !== 'string') return null;
  if (!data.token || !/^[A-Za-z0-9_-]{1,100}$/.test(data.ldi)) return null;
  return data;
}

/** Сессия с запасом по размеру cookie: если профиль не влезает — урезаем. */
function makeSession(token, ldi, user) {
  const base = { v: 1, token, ldi, iat: Date.now(), user: user || null };
  const trySeal = (session) => seal(session).length;

  if (trySeal(base) <= 3600) return base;

  const compactUser = user
    ? {
        laneId: user.laneId || null,
        displayedName: user.displayedName || user.username || user.name || null,
        username: user.username || null,
        avatarUrl: user.avatarUrl || user.avatar || null,
      }
    : null;
  const compact = { ...base, user: compactUser };
  if (trySeal(compact) <= 3600) return compact;

  return { ...base, user: null };
}

module.exports = {
  COOKIE_SESSION,
  COOKIE_PENDING,
  SESSION_TTL,
  PENDING_TTL,
  seal,
  unseal,
  parseCookies,
  getCookie,
  appendCookie,
  setSessionCookie,
  clearSessionCookie,
  setPendingCookie,
  clearPendingCookie,
  readSession,
  makeSession,
};
