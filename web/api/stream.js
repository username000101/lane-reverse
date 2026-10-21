'use strict';

/**
 * GET /api/stream?trackId=…&refId=…&quality=HIGH
 * Подписывает запрос к /track/stream и отдаёт прямую ссылку на аудио.
 */

const { laneFetch } = require('./_lib/lane');
const { requireSession, sendUpstreamError } = require('./_lib/respond');
const { json, fail, methodNotAllowed, getQuery, guard } = require('./_lib/http');

const QUALITIES = new Set(['BASIC', 'HIGH', 'ULTRA']);

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const session = requireSession(req, res);
  if (!session) return;

  const query = getQuery(req);
  const trackId = String(query.trackId || '').trim();
  if (!/^[A-Za-z0-9_-]{1,100}$/.test(trackId)) {
    return fail(res, 400, 'некорректный trackId', 'BAD_TRACK_ID');
  }

  const requested = String(query.quality || 'HIGH').toUpperCase();
  const quality = QUALITIES.has(requested) ? requested : 'HIGH';
  const refId = String(query.refId || 'lane_likes').slice(0, 120);

  const result = await laneFetch({
    path: '/track/stream',
    query: `trackId=${encodeURIComponent(trackId)}&refId=${encodeURIComponent(refId)}&streamQuality=${quality}`,
    token: session.token,
    ldi: session.ldi,
  });

  if (!result.ok) return sendUpstreamError(res, result);

  const data = result.data || {};
  if (typeof data.url !== 'string' || !/^https?:\/\//.test(data.url)) {
    return fail(res, 502, 'Lane не вернул ссылку на поток', 'NO_STREAM_URL');
  }

  return json(res, 200, {
    trackId,
    quality,
    url: data.url,
    ttl: typeof data.ttl === 'number' ? data.ttl : null,
    playbackToken: data.playbackToken || null,
    fetchedAt: Date.now(),
  });
});
