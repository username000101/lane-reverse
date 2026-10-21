'use strict';

/**
 * Подписанный fetch к Lane API. Ключ и заголовки живут только на сервере.
 */

const bnit = require('./bnit');

const BASE = (process.env.LANE_API_BASE || 'https://ru.laneapi.com').replace(/\/+$/, '');
const APP_VERSION = '207';
const TIMEOUT_MS = Number(process.env.LANE_TIMEOUT_MS || 12000);

class LaneError extends Error {
  constructor(message, status = 0, code = '') {
    super(message);
    this.name = 'LaneError';
    this.status = status;
    this.code = code;
  }
}

function requestHeaders({ token, ldi, extra }) {
  const headers = {
    'x-accept-red': '0',
    'x-app-version': APP_VERSION,
    'x-platform': 'android',
    'x-theme': 'dark',
    'accept-language': 'ru',
    'tz': 'Europe/Moscow',
    'user-agent': 'LaneMusic/1.0 (Android; Mobile)',
    accept: 'application/json',
  };
  if (token) headers.authorization = `Bearer ${token}`;
  if (ldi) headers.ldi = ldi;
  return Object.assign(headers, extra || {});
}

/**
 * @param {object} opts
 * @param {string} opts.path
 * @param {string} [opts.method]
 * @param {string} [opts.query] сырая строка после `?`
 * @param {any} [opts.body] объект (будет зашифрован) или Buffer
 * @param {string} [opts.token] Bearer
 * @param {string} [opts.ldi]
 * @param {string} [opts.inode]
 */
async function laneFetch(opts) {
  const method = (opts.method || 'GET').toUpperCase();
  const path = opts.path;
  if (typeof path !== 'string' || !path.startsWith('/')) throw new LaneError('bad path');
  if (path.includes('?') || path.includes('#')) throw new LaneError('path must not contain query');

  const query = bnit.canonicalQuery(opts.query || '');

  let bodyBuf = Buffer.alloc(0);
  if (opts.body != null && method !== 'GET' && method !== 'HEAD') {
    bodyBuf = Buffer.isBuffer(opts.body) ? opts.body : Buffer.from(JSON.stringify(opts.body), 'utf8');
  }

  const signed = bnit.build(method, path, query, bodyBuf, 0, { inode: opts.inode });

  const headers = requestHeaders({
    token: opts.token,
    ldi: opts.ldi,
    extra: bodyBuf.length ? { 'content-type': 'application/json; charset=UTF-8' } : null,
  });
  headers['x-core-token'] = signed.coreToken;
  headers['x-request-trace-id'] = signed.traceId;
  headers['x-client-meta'] = signed.clientMeta;

  const url = `${BASE}${path}${query ? `?${query}` : ''}`;

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: bodyBuf.length ? signed.payload : undefined,
      redirect: 'manual',
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch (err) {
    const reason = err && err.name === 'TimeoutError' ? 'upstream timeout' : `upstream fetch failed: ${err.message}`;
    throw new LaneError(reason, 504, 'UPSTREAM');
  }

  let buffer = Buffer.from(await response.arrayBuffer());

  const red = response.headers.get('x-core-red') === '1';
  const compressed = response.headers.get('x-core-compressed') === '1';
  const nonce = response.headers.get('x-resp-nonce') || '';
  if (red && nonce) buffer = bnit.decryptResponse(buffer, nonce, compressed);

  const text = buffer.toString('utf8');
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  const errorCode = response.headers.get('x-error-code') || (data && data.code) || '';

  return {
    status: response.status,
    ok: response.status >= 200 && response.status < 300,
    headers: response.headers,
    buffer,
    text,
    data,
    errorCode,
  };
}

/** Человеческое описание ошибки upstream для клиента. */
function describe(result) {
  const code = result.errorCode || '';
  const map = {
    MISSING_SIGNATURE_TOKEN: 'сервер не принял подпись запроса',
    INVALID_SIGNATURE_TOKEN: 'сервер отклонил подпись запроса',
    TIMESTAMP_EXPIRED: 'часы разошлись, подпись устарела',
    INVALID_TRACK_IDS_BODY: 'некорректный список треков',
  };
  const message =
    (result.data && (result.data.message || result.data.error)) ||
    map[code] ||
    `Lane API вернул ${result.status}`;
  return { status: result.status, code: code || null, message: String(message) };
}

module.exports = {
  BASE,
  LaneError,
  laneFetch,
  describe,
};
