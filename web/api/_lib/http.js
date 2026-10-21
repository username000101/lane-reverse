'use strict';

/** Мелкие помощники для serverless-функций Vercel (Node runtime). */

function applyBaseHeaders(res) {
  res.setHeader('Cache-Control', 'no-store, max-age=0');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
}

function json(res, status, payload) {
  applyBaseHeaders(res);
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.end(JSON.stringify(payload));
}

function fail(res, status, message, code) {
  json(res, status, { error: message, code: code || null });
}

function methodNotAllowed(res, allow) {
  res.setHeader('Allow', allow.join(', '));
  fail(res, 405, 'method not allowed', 'METHOD_NOT_ALLOWED');
}

function getQuery(req) {
  if (req.query && typeof req.query === 'object') return req.query;
  try {
    const url = new URL(req.url || '/', 'http://local');
    return Object.fromEntries(url.searchParams.entries());
  } catch {
    return {};
  }
}

/** CORS/CSRF-защита: POST принимаем только со своего origin (если он прислан). */
function sameOrigin(req) {
  const origin = req.headers.origin;
  if (!origin) return true; // curl / same-origin fetch без заголовка
  const host = req.headers.host;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

function guard(handler) {
  return async function guarded(req, res) {
    try {
      await handler(req, res);
    } catch (err) {
      console.error('[lane-web]', err && err.stack ? err.stack : err);
      if (!res.writableEnded) {
        fail(res, err && err.status ? err.status : 500, 'внутренняя ошибка сервера', 'INTERNAL');
      }
    }
  };
}

function noSession(res) {
  fail(res, 401, 'нужен вход через Telegram', 'NO_SESSION');
}

module.exports = {
  applyBaseHeaders,
  json,
  fail,
  methodNotAllowed,
  getQuery,
  sameOrigin,
  guard,
  noSession,
};
