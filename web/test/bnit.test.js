'use strict';

/**
 * Сверка JS-порта с golden-векторами из crates/laned-bnit/src/lib.rs.
 * Запуск: node web/test/bnit.test.js
 */

const assert = require('node:assert/strict');
const bnit = require('../api/_lib/bnit');

const SEQ = [
  0x01020304, 0x05060708, 0x090a0b0c, 0x0d0e0f10, 0x11121314,
  0x15161718, 0x191a1b1c, 0x1d1e1f20, 0x21222324, 0x25262728,
  0x292a2b2c, 0x2d2e2f30, 0x31323334, 0x35363738, 0x393a3b3c,
];

const opts = {
  bootId: '0123abcd-1111-2222-3333-444455556666',
  inode: '4660',
  nowMs: 1700000000123,
  randoms: SEQ,
};

// 1. golden_vector_matches_native
const out1 = bnit.build('ARG_A', 'ARG_B', 'ARG_C', Buffer.from([1, 2, 3, 4]), 1700000000000, opts);
assert.equal(
  out1.coreToken,
  'bwsrnd7LJ9TcQkA7umSJWKQh5bKrtWhW5FqLkf_O9nRwBCzWn-bP5JjLzSNtJEHwh8RXvE6Me97GTWPWw-cirH1sYxgL_wLhGkXM-lfzI1gCYSFHQplmwtgSjtNi7COj9o_9dWzePefvbPuM6H6_t2Gd0EG3yLh2SJN5luRjtha30vwYm9Ul6r1wOUGa2sEs4LPvwNaHkiWttnR6c2K2',
);
assert.equal(out1.traceId, '18171615-1c1b-1a19-201f-1e1d24232221');
assert.equal(out1.clientMeta, 'fc3QdeAYf9-AgDLIjvhDhr7Kj0tVkFEZ');
assert.deepEqual([...out1.payload], [0xce, 0xca, 0xbb, 0x93]);

// 2. long_body_field_matches_native
const out2 = bnit.build('A', 'B', 'C', Buffer.from([...Array(16).keys()]), 0, opts);
assert.equal(out2.payload.toString('hex'), '6625de9f1b7e72460fc12964636f4ba5');

// 3. known_token_matches_cpp (inode по умолчанию)
const out3 = bnit.build('GET', '/user/playlists', '', Buffer.alloc(0), 0, {
  bootId: '0123abcd-1111-2222-3333-444455556666',
  nowMs: 1783685473123,
  randoms: SEQ,
});
assert.equal(
  out3.coreToken,
  'bwsrnzpeTip2xfBL7IH2StRDgAFzOk2betIB3y2dXSu7nhbwUC9QUfzXBKVU78kNkNVz5HGhr70TvFJy3RdUl6FUQSPO9ZYdDIQudx_jAUsCYyROOlvrBePXCwhr-vcsrpJRZek5AvWDx_Y7hal6n8-MLsPLotpb7fA90UoFqcLoXVxJYayLQQPEObqwXGCELoE8PoqTvAKFndS_AVw56RG.',
);
assert.equal(out3.clientMeta.length, 32);

// 4. раскодировать конверт обратно и проверить поля
const blob = bnit.base64CustomDecode(out3.coreToken);
const key2 = blob.subarray(0, 4);
const plain = bnit.tokenDecipher(key2, blob.subarray(4)).toString('latin1');
const [hmac, ts, nonce, boot, inode, flag] = plain.split('|');
assert.equal(hmac.length, 64);
assert.equal(ts, '1783685473123');
assert.equal(nonce, '04030201080706050c0b0a09100f0e0d');
assert.equal(boot, '0123abcd-1111-2222-3333-444455556666');
assert.equal(inode, '656421');
assert.equal(flag, '0');

// 5. канонизация query
assert.equal(bnit.canonicalQuery('trackId=A&refId=B&streamQuality=C'), 'refId=B&streamQuality=C&trackId=A');
assert.equal(bnit.canonicalQuery('b=2&a=1'), 'a=1&b=2');
assert.equal(bnit.canonicalQuery('a=1&a=2&b=3'), 'a=1&b=3');
assert.equal(bnit.canonicalQuery(''), '');
assert.equal(bnit.canonicalQuery('?prefetch=0'), 'prefetch=0');

// 6. round-trip token cipher
const key2b = out3.key2;
const text = Buffer.from('hello|world|123', 'latin1');
const enc = bnit.tokenCipher(key2b, text);
const dec = bnit.tokenDecipher(key2b, enc);
assert.deepEqual([...dec], [...text]);

// 7. device id
for (let i = 0; i < 50; i += 1) {
  const id = bnit.randomDeviceId(32);
  assert.match(id, /^[A-Za-z0-9_-]{32}$/);
}

console.log('bnit: all golden vectors match');
