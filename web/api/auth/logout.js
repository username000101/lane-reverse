'use strict';

/** POST /api/auth/logout — забыть сессию и ожидание. */

const { clearSessionCookie, clearPendingCookie } = require('./_lib/session');
const { json, methodNotAllowed, sameOrigin, fail, guard } = require('./_lib/http');

module.exports = guard(async (req, res) => {
  if (req.method !== 'POST') return methodNotAllowed(res, ['POST']);
  if (!sameOrigin(req)) return fail(res, 403, 'cross-origin request', 'FORBIDDEN');

  clearSessionCookie(res);
  clearPendingCookie(res);
  return json(res, 200, { ok: true });
});
