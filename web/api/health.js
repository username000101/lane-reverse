'use strict';

/** GET /api/health — проверка, что функция жива (без обращения в Lane). */

const { BASE } = require('./_lib/lane');
const { json, guard } = require('./_lib/http');

module.exports = guard(async (req, res) => {
  return json(res, 200, {
    ok: true,
    service: 'lane-web',
    upstream: BASE,
    time: new Date().toISOString(),
  });
});
