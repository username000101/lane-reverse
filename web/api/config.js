'use strict';

/**
 * GET /api/config
 *
 * Экспорт сессии в формате laned.json (для crates/laned-cli и crates/laned-tui).
 */

const { requireSession } = require('./_lib/respond');
const { json, methodNotAllowed, guard } = require('./_lib/http');

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const session = requireSession(req, res);
  if (!session) return;

  return json(res, 200, {
    deviceId: session.ldi,
    inode: '668035',
    token: session.token,
    user: session.user || null,
  });
});
