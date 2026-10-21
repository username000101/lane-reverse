'use strict';

/**
 * Порт подписи Lane (`libbnit.so` / `BNITManager.cdrl`) на Node.js.
 *
 * Алгоритм восстановлен в docs/ALGORITHM.md, эталон — crates/laned-bnit.
 * Здесь только то, что нужно серверу: HMAC-SHA256, RC4-с-мутацией (тело и
 * `red`-ответы), кастомный base64 и «шифр конверта».
 */

const crypto = require('crypto');

/** 32-байтовый ключ из четырёх кусков в нативном коде. */
const KEY = Buffer.from('SqperSzvbntKmv_CbnngeThis_12303!', 'latin1');

/** Свой алфавит base64; `.` вместо `=` на паддинге. */
const ALPHABET = 'zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM';

const DEFAULT_INODE = '656421';

/* ------------------------------------------------------------------ */
/* кодировки                                                           */
/* ------------------------------------------------------------------ */

function hex(buf) {
  return Buffer.from(buf).toString('hex');
}

function base64Custom(buf) {
  const std = Buffer.from(buf).toString('base64');
  let out = '';
  for (let i = 0; i < std.length; i += 1) {
    const ch = std[i];
    if (ch === '=') {
      out += '.';
      continue;
    }
    let idx;
    if (ch >= 'A' && ch <= 'Z') idx = ch.charCodeAt(0) - 65;
    else if (ch >= 'a' && ch <= 'z') idx = ch.charCodeAt(0) - 97 + 26;
    else if (ch >= '0' && ch <= '9') idx = ch.charCodeAt(0) - 48 + 52;
    else if (ch === '+') idx = 62;
    else idx = 63;
    out += ALPHABET[idx];
  }
  return out;
}

function base64CustomDecode(str) {
  const map = new Map();
  for (let i = 0; i < ALPHABET.length; i += 1) map.set(ALPHABET[i], i);
  let bits = 0;
  let n = 0;
  const out = [];
  for (const ch of str) {
    if (ch === '.' || ch === '=') continue;
    const idx = map.get(ch);
    if (idx === undefined) throw new Error(`bad base64 char: ${ch}`);
    bits = ((bits << 6) | idx) >>> 0;
    n += 6;
    if (n >= 8) {
      n -= 8;
      out.push((bits >> n) & 0xff);
    }
  }
  return Buffer.from(out);
}

/* ------------------------------------------------------------------ */
/* RC4 с мутацией S-бокса (тело запроса и `red`-ответы)                 */
/* ------------------------------------------------------------------ */

function rc4(key, data) {
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i += 1) s[i] = i;

  let j = 0;
  for (let i = 0; i < 256; i += 1) {
    j = (j + s[i] + key[i % key.length]) & 0xff;
    const t = s[i];
    s[i] = s[j];
    s[j] = t;
  }

  const out = Buffer.allocUnsafe(data.length);
  let i = 0;
  j = 0;
  for (let n = 0; n < data.length; n += 1) {
    i = (i + 1) & 0xff;
    j = (j + s[i]) & 0xff;
    const t = s[i];
    s[i] = s[j];
    s[j] = t;

    const k = s[(s[i] + s[j]) & 0xff];
    out[n] = k ^ data[n];

    const idx = (k + i) & 0xff;
    s[idx] = (s[idx] + k) & 0xff;
  }
  return out;
}

/* ------------------------------------------------------------------ */
/* шифр конверта токена (двухключевой KSA + PRGA с переносом)           */
/* ------------------------------------------------------------------ */

function tokenCipher(key2, data) {
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i += 1) s[i] = i;

  let j = 0;
  for (let i = 0; i < 256; i += 1) {
    j = (j + s[i] + KEY[i % 32] + key2[i % key2.length]) & 0xff;
    const t = s[i];
    s[i] = s[j];
    s[j] = t;
  }

  const out = Buffer.allocUnsafe(data.length);
  let i = 0;
  j = 0;
  let carry = 0x5a;
  for (let n = 0; n < data.length; n += 1) {
    i = (i + 1) & 0xff;
    j = (j + s[i]) & 0xff;
    const t0 = s[i];
    s[i] = s[j];
    s[j] = t0;

    const t = ((s[i] + s[j]) & 0xff) ^ carry;
    let v = s[t] ^ data[n];
    v = ((v << 3) | (v >> 5)) & 0xff;
    carry = (carry + v) & 0xff;
    out[n] = carry;
  }
  return out;
}

function tokenDecipher(key2, ct) {
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i += 1) s[i] = i;

  let j = 0;
  for (let i = 0; i < 256; i += 1) {
    j = (j + s[i] + KEY[i % 32] + key2[i % key2.length]) & 0xff;
    const t = s[i];
    s[i] = s[j];
    s[j] = t;
  }

  const out = Buffer.allocUnsafe(ct.length);
  let i = 0;
  j = 0;
  let carry = 0x5a;
  for (let n = 0; n < ct.length; n += 1) {
    i = (i + 1) & 0xff;
    j = (j + s[i]) & 0xff;
    const t0 = s[i];
    s[i] = s[j];
    s[j] = t0;

    const t = ((s[i] + s[j]) & 0xff) ^ carry;
    const k = s[t];
    let v = (ct[n] - carry) & 0xff;
    v = ((v >> 3) | (v << 5)) & 0xff;
    out[n] = k ^ v;
    carry = ct[n];
  }
  return out;
}

/* ------------------------------------------------------------------ */
/* каноническая строка query                                           */
/* ------------------------------------------------------------------ */

/** Уникальные имена, сортировка по имени, `name=value` через `&`. */
function canonicalQuery(query) {
  const q = String(query || '').replace(/^\?/, '');
  if (!q) return '';
  const seen = new Set();
  const pairs = [];
  for (const part of q.split('&')) {
    if (!part) continue;
    const eq = part.indexOf('=');
    const name = eq === -1 ? part : part.slice(0, eq);
    const value = eq === -1 ? '' : part.slice(eq + 1);
    if (seen.has(name)) continue;
    seen.add(name);
    pairs.push([name, value]);
  }
  pairs.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
  return pairs.map(([k, v]) => `${k}=${v}`).join('&');
}

/* ------------------------------------------------------------------ */
/* сборка заголовков                                                   */
/* ------------------------------------------------------------------ */

function u32leHex(num) {
  const b = Buffer.allocUnsafe(4);
  b.writeUInt32LE(num >>> 0, 0);
  return b;
}

/**
 * @param {string} method
 * @param {string} path
 * @param {string} query уже канонический
 * @param {Buffer} body открытый текст
 * @param {number} timestamp смещение к текущему времени, мс
 * @param {{bootId?: string, inode?: string, nowMs?: number, randoms?: number[]}} [opts]
 */
function build(method, path, query, body, timestamp = 0, opts = {}) {
  const payloadBody = Buffer.isBuffer(body) ? body : Buffer.from(body || '');

  let randoms = opts.randoms;
  if (!randoms) {
    const raw = crypto.randomBytes(60);
    randoms = [];
    for (let i = 0; i < 15; i += 1) randoms.push(raw.readUInt32LE(i * 4));
  }
  if (randoms.length !== 15) throw new Error('randoms must have 15 u32');

  const now = opts.nowMs == null ? Date.now() : opts.nowMs;
  const bootId = opts.bootId || crypto.randomUUID();
  const inode = opts.inode || DEFAULT_INODE;

  const nonce = Buffer.concat([u32leHex(randoms[0]), u32leHex(randoms[1]), u32leHex(randoms[2]), u32leHex(randoms[3])]);
  const nonceHex = hex(nonce);
  const ts = String(now + timestamp);

  // field = RC4(KEY || hex(nonce) || ts, body); те же байты едут в теле POST
  const rc4Key = Buffer.concat([KEY, Buffer.from(nonceHex, 'latin1'), Buffer.from(ts, 'latin1')]);
  const payload = rc4(rc4Key, payloadBody);

  // msg1 = method:path:query:ts:nonce:field:boot_id:inode
  const parts = [
    Buffer.from(method, 'latin1'),
    Buffer.from(':', 'latin1'),
    Buffer.from(path, 'latin1'),
    Buffer.from(':', 'latin1'),
    Buffer.from(query, 'latin1'),
    Buffer.from(':', 'latin1'),
    Buffer.from(ts, 'latin1'),
    Buffer.from(':', 'latin1'),
    Buffer.from(nonceHex, 'latin1'),
    Buffer.from(':', 'latin1'),
    payload,
    Buffer.from(':', 'latin1'),
    Buffer.from(bootId, 'latin1'),
    Buffer.from(':', 'latin1'),
    Buffer.from(inode, 'latin1'),
  ];
  const digest = crypto.createHmac('sha256', KEY).update(Buffer.concat(parts)).digest();

  const plain = Buffer.from(
    `${hex(digest)}|${ts}|${nonceHex}|${bootId}|${inode}|${payloadBody.length ? '1' : '0'}`,
    'latin1',
  );

  const key2 = Buffer.allocUnsafe(4);
  key2.writeUInt32BE(randoms[4] >>> 0, 0);
  const cipher = tokenCipher(key2, plain);
  const blob = Buffer.concat([key2, cipher]);

  const traceHex = Buffer.concat([u32leHex(randoms[5]), u32leHex(randoms[6]), u32leHex(randoms[7]), u32leHex(randoms[8])]).toString('hex');
  const traceId = `${traceHex.slice(0, 8)}-${traceHex.slice(8, 12)}-${traceHex.slice(12, 16)}-${traceHex.slice(16, 20)}-${traceHex.slice(20, 32)}`;

  const meta = Buffer.concat([9, 10, 11, 12, 13, 14].map((i) => u32leHex(randoms[i])));
  const clientMeta = base64Custom(meta);

  return {
    coreToken: base64Custom(blob),
    traceId,
    clientMeta,
    payload,
    ts,
    nonceHex,
    key2,
  };
}

/** Расшифровка `red`-ответа: RC4*(KEY || X-Resp-Nonce, body). */
function decryptResponse(body, nonce, compressed) {
  const key = Buffer.concat([KEY, Buffer.from(String(nonce || ''), 'latin1')]);
  const data = rc4(key, Buffer.from(body));
  if (!compressed) return data;
  try {
    return require('zlib').gunzipSync(data);
  } catch {
    return data;
  }
}

/** CSPRNG-строка из алфавита 64 символа (подходит для `?start=auth<id>`). */
function randomDeviceId(len = 32) {
  const raw = crypto.randomBytes(len);
  let out = '';
  for (let i = 0; i < len; i += 1) out += ALPHABET[raw[i] & 63];
  return out;
}

module.exports = {
  KEY,
  DEFAULT_INODE,
  build,
  canonicalQuery,
  decryptResponse,
  randomDeviceId,
  rc4,
  tokenCipher,
  tokenDecipher,
  base64Custom,
  base64CustomDecode,
};
