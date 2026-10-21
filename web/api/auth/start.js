'use strict';

/**
 * POST /api/auth/start
 *
 * Генерирует device id (он же LDI и ключ ожидания в боте) и кладёт его в
 * зашифрованную HttpOnly-cookie. Возвращает ссылку на официального бота.
 */

const { randomDeviceId } = require('../_lib/bnit');
const { setPendingCookie } = require('../_lib/session');
const { json, fail, methodNotAllowed, sameOrigin, guard } = require('../_lib/http');

const BOT = 'lane_music_bot';

module.exports = guard(async (req, res) => {
  if (req.method !== 'POST') return methodNotAllowed(res, ['POST']);
  if (!sameOrigin(req)) return fail(res, 403, 'cross-origin request', 'FORBIDDEN');

  const id = randomDeviceId(32);
  const url = `https://t.me/${BOT}?start=auth${id}`;

  setPendingCookie(res, { id, ts: Date.now() });

  return json(res, 200, {
    id,
    url,
    bot: BOT,
    expiresIn: 20 * 60,
  });
});
