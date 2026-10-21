'use strict';

/**
 * GET /api/playlist?id=…&page=1&pageSize=50
 * Метаданные плейлиста + страница треков одним ответом.
 */

const { laneFetch } = require('./_lib/lane');
const { requireSession, sendUpstreamError } = require('./_lib/respond');
const { json, fail, methodNotAllowed, getQuery, guard } = require('./_lib/http');

function clampInt(value, min, max, fallback) {
  const n = Number.parseInt(String(value == null ? '' : value), 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const session = requireSession(req, res);
  if (!session) return;

  const query = getQuery(req);
  const id = String(query.id || '').trim();
  if (!/^[A-Za-z0-9_.~-]{1,100}$/.test(id)) {
    return fail(res, 400, 'некорректный id плейлиста', 'BAD_PLAYLIST_ID');
  }

  const page = clampInt(query.page, 1, 10000, 1);
  const pageSize = clampInt(query.pageSize, 1, 50, 50);
  const segment = encodeURIComponent(id);

  const [meta, tracks] = await Promise.allSettled([
    laneFetch({ path: `/playlist/${segment}`, token: session.token, ldi: session.ldi }),
    laneFetch({
      path: `/playlist/${segment}/tracks`,
      query: `page=${page}&pageSize=${pageSize}`,
      token: session.token,
      ldi: session.ldi,
    }),
  ]);

  if (tracks.status === 'rejected') throw tracks.reason;
  if (!tracks.value.ok) return sendUpstreamError(res, tracks.value);

  return json(res, 200, {
    playlist: meta.status === 'fulfilled' && meta.value.ok ? meta.value.data : null,
    tracks: tracks.value.data,
    page,
    pageSize,
  });
});
