'use strict';

/** GET /api/playlists — плейлисты пользователя. */

const { laneFetch } = require('./_lib/lane');
const { requireSession, sendUpstreamError } = require('./_lib/respond');
const { json, methodNotAllowed, guard } = require('./_lib/http');

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const session = requireSession(req, res);
  if (!session) return;

  const result = await laneFetch({
    path: '/user/playlists',
    token: session.token,
    ldi: session.ldi,
  });

  if (!result.ok) return sendUpstreamError(res, result);

  return json(res, 200, {
    playlists: Array.isArray(result.data) ? result.data : [],
  });
});
