'use strict';

/** GET /api/me — профиль из зашифрованной сессии (без запроса в Lane). */

const { requireSession } = require('./_lib/respond');
const { json, methodNotAllowed, guard } = require('./_lib/http');

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const session = requireSession(req, res);
  if (!session) return;

  return json(res, 200, {
    user: session.user || null,
    ldi: session.ldi,
    since: session.iat || null,
  });
});
