'use strict';

/** Общие ответы для ручек, которым нужна сессия. */

const { json, fail } = require('./http');
const { readSession, clearSessionCookie } = require('./session');
const { describe } = require('./lane');

/**
 * Возвращает сессию или отправляет 401 и `null`.
 * @returns {{token: string, ldi: string, user: any}|null}
 */
function requireSession(req, res) {
  const session = readSession(req);
  if (!session) {
    fail(res, 401, 'нужен вход через Telegram', 'NO_SESSION');
    return null;
  }
  return session;
}

/**
 * Прокидывает ошибку Lane API наружу. 401 означает, что токен больше не
 * принимают: чистим cookie, чтобы браузер не зацикливался на битой сессии.
 */
function sendUpstreamError(res, result) {
  const info = describe(result);
  if (result.status === 401) {
    clearSessionCookie(res);
    return json(res, 401, {
      error: 'сессия истекла — войдите через Telegram заново',
      code: 'SESSION_EXPIRED',
      upstream: info,
    });
  }
  return json(res, 502, {
    error: info.message,
    code: info.code || 'UPSTREAM',
    upstream: info,
  });
}

module.exports = { requireSession, sendUpstreamError };
