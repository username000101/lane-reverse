'use strict';

/**
 * GET /api/auth/poll
 *
 * Опрашивает `/auth/<id>` тем же device id, что ушёл боту. Пока пользователь
 * не нажал Start, Lane отвечает не-200 — отдаём `pending`. На 200 сохраняем
 * токен в зашифрованную сессию и отдаём профиль.
 */

const { laneFetch, describe } = require('../_lib/lane');
const {
  getCookie,
  unseal,
  COOKIE_PENDING,
  makeSession,
  setSessionCookie,
  clearPendingCookie,
} = require('../_lib/session');
const { json, fail, methodNotAllowed, guard } = require('../_lib/http');

module.exports = guard(async (req, res) => {
  if (req.method !== 'GET') return methodNotAllowed(res, ['GET']);

  const pending = unseal(getCookie(req, COOKIE_PENDING));
  if (!pending || typeof pending.id !== 'string' || !/^[A-Za-z0-9_-]{8,64}$/.test(pending.id)) {
    return json(res, 200, { status: 'expired' });
  }

  // id всегда берём из подписанной cookie, а не из query: иначе через прокси
  // можно было бы опрашивать чужую сессию, зная её device id.
  const result = await laneFetch({
    path: `/auth/${pending.id}`,
    ldi: pending.id,
    method: 'GET',
  });

  if (result.status === 200 && result.data && typeof result.data.token === 'string' && result.data.token) {
    const user = result.data.user || null;
    const session = makeSession(result.data.token, pending.id, user);
    setSessionCookie(res, session);
    clearPendingCookie(res);

    return json(res, 200, {
      status: 'ok',
      isFirstAuth: Boolean(result.data.isFirstAuth),
      user,
      ldi: pending.id,
    });
  }

  if (result.status === 200) {
    const info = describe(result);
    return fail(res, 502, `неожиданный ответ авторизации: ${info.message}`, 'BAD_AUTH_RESPONSE');
  }

  return json(res, 200, {
    status: 'pending',
    upstream: result.status,
    code: result.errorCode || null,
  });
});
