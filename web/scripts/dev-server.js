'use strict';

/**
 * Локальный превью-сервер: статика из public/ + те же serverless-функции,
 * что и на Vercel. Нужен, чтобы проверять морду без деплоя.
 *
 *   node scripts/dev-server.js [--port 4173]
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { URL } = require('url');

const ROOT = path.resolve(__dirname, '..');
const PUBLIC = path.join(ROOT, 'public');
const API = path.join(ROOT, 'api');

const args = process.argv.slice(2);
const portIndex = args.indexOf('--port');
const PORT = Number(portIndex >= 0 ? args[portIndex + 1] : process.env.PORT || 4173);

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.woff2': 'font/woff2',
};

if (!process.env.LANE_SESSION_SECRET) {
  process.env.LANE_SESSION_SECRET = 'local-dev-secret-not-for-production';
}

function sendJson(res, status, payload) {
  res.statusCode = status;
  res.setHeader('content-type', 'application/json; charset=utf-8');
  res.end(JSON.stringify(payload));
}

function serveStatic(req, res, pathname) {
  let rel = decodeURIComponent(pathname);
  if (rel === '/' || rel === '') rel = '/index.html';

  const file = path.join(PUBLIC, rel);
  if (!file.startsWith(PUBLIC)) return sendJson(res, 403, { error: 'forbidden' });

  fs.stat(file, (err, stat) => {
    if (err || !stat.isFile()) {
      return sendJson(res, 404, { error: 'not found' });
    }
    res.statusCode = 200;
    res.setHeader('content-type', MIME[path.extname(file)] || 'application/octet-stream');
    res.setHeader('cache-control', 'no-store');
    if (req.method === 'HEAD') return res.end();
    fs.createReadStream(file).pipe(res);
  });
}

function apiFilePath(pathname) {
  const rel = pathname.replace(/^\/api\//, '').replace(/\/+$/, '');
  if (!rel || rel.includes('..')) return null;
  return path.join(API, ...rel.split('/')) + '.js';
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  if (!url.pathname.startsWith('/api/')) {
    return serveStatic(req, res, url.pathname);
  }

  const file = apiFilePath(url.pathname);
  if (!file || !fs.existsSync(file)) {
    return sendJson(res, 404, { error: 'unknown api route', path: url.pathname });
  }

  req.query = Object.fromEntries(url.searchParams.entries());

  try {
    const handler = require(file);
    if (typeof handler !== 'function') {
      return sendJson(res, 500, { error: 'handler is not a function' });
    }
    await handler(req, res);
  } catch (error) {
    console.error('[dev]', error);
    if (!res.writableEnded) {
      sendJson(res, 500, { error: error.message || 'internal error' });
    }
  }
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`Lane Web dev: http://127.0.0.1:${PORT}`);
});
