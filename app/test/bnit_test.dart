import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:lane_app/core/crypto/bnit.dart';

void main() {
  const seq = [
    0x01020304, 0x05060708, 0x090a0b0c, 0x0d0e0f10, 0x11121314,
    0x15161718, 0x191a1b1c, 0x1d1e1f20, 0x21222324, 0x25262728,
    0x292a2b2c, 0x2d2e2f30, 0x31323334, 0x35363738, 0x393a3b3c,
  ];

  test('canonicalQuery works correctly', () {
    expect(
      Bnit.canonicalQuery('trackId=A&refId=B&streamQuality=C'),
      'refId=B&streamQuality=C&trackId=A',
    );
    expect(Bnit.canonicalQuery('b=2&a=1'), 'a=1&b=2');
    expect(Bnit.canonicalQuery('a=1&a=2&b=3'), 'a=1&b=3');
    expect(Bnit.canonicalQuery(''), '');
    expect(Bnit.canonicalQuery('?prefetch=0'), 'prefetch=0');
  });

  test('golden vector 1 matches native and golden assertions', () {
    final out1 = Bnit.build(
      method: 'ARG_A',
      path: 'ARG_B',
      query: 'ARG_C',
      body: [1, 2, 3, 4],
      timestamp: 1700000000000,
      bootId: '0123abcd-1111-2222-3333-444455556666',
      inode: '4660',
      nowMs: 1700000000123,
      randoms: seq,
    );

    expect(
      out1.coreToken,
      'bwsrnd7LJ9TcQkA7umSJWKQh5bKrtWhW5FqLkf_O9nRwBCzWn-bP5JjLzSNtJEHwh8RXvE6Me97GTWPWw-cirH1sYxgL_wLhGkXM-lfzI1gCYSFHQplmwtgSjtNi7COj9o_9dWzePefvbPuM6H6_t2Gd0EG3yLh2SJN5luRjtha30vwYm9Ul6r1wOUGa2sEs4LPvwNaHkiWttnR6c2K2',
    );
    expect(out1.traceId, '18171615-1c1b-1a19-201f-1e1d24232221');
    expect(out1.clientMeta, 'fc3QdeAYf9-AgDLIjvhDhr7Kj0tVkFEZ');
    expect(out1.payload, equals(Uint8List.fromList([0xce, 0xca, 0xbb, 0x93])));
  });

  test('golden vector 2: long body field matches native', () {
    final body = List<int>.generate(16, (i) => i);
    final out2 = Bnit.build(
      method: 'A',
      path: 'B',
      query: 'C',
      body: body,
      timestamp: 0,
      bootId: '0123abcd-1111-2222-3333-444455556666',
      inode: '4660',
      nowMs: 1700000000123,
      randoms: seq,
    );

    final hexStr = out2.payload
        .map((b) => b.toRadixString(16).padLeft(2, '0'))
        .join();
    expect(hexStr, '6625de9f1b7e72460fc12964636f4ba5');
  });

  test('golden vector 3: known token matches C++ / Rust (default inode)', () {
    final out3 = Bnit.build(
      method: 'GET',
      path: '/user/playlists',
      query: '',
      body: [],
      timestamp: 0,
      bootId: '0123abcd-1111-2222-3333-444455556666',
      nowMs: 1783685473123,
      randoms: seq,
    );

    expect(
      out3.coreToken,
      'bwsrnzpeTip2xfBL7IH2StRDgAFzOk2betIB3y2dXSu7nhbwUC9QUfzXBKVU78kNkNVz5HGhr70TvFJy3RdUl6FUQSPO9ZYdDIQudx_jAUsCYyROOlvrBePXCwhr-vcsrpJRZek5AvWDx_Y7hal6n8-MLsPLotpb7fA90UoFqcLoXVxJYayLQQPEObqwXGCELoE8PoqTvAKFndS_AVw56RG.',
    );
    expect(out3.clientMeta.length, 32);

    // Decode envelope and verify internal fields
    final blob = Bnit.customBase64Decode(out3.coreToken);
    final key2 = blob.sublist(0, 4);
    final plainBytes = Bnit.tokenDecipher(key2, blob.sublist(4));
    final plain = latin1.decode(plainBytes);
    final parts = plain.split('|');

    expect(parts[0].length, 64); // HMAC length
    expect(parts[1], '1783685473123'); // ts
    expect(parts[2], '04030201080706050c0b0a09100f0e0d'); // nonce
    expect(parts[3], '0123abcd-1111-2222-3333-444455556666'); // bootId
    expect(parts[4], '656421'); // default inode
    expect(parts[5], '0'); // flag (no body)
  });

  test('round-trip token cipher', () {
    final key2 = Uint8List.fromList([10, 20, 30, 40]);
    final text = latin1.encode('hello|world|123');
    final enc = Bnit.tokenCipher(key2, text);
    final dec = Bnit.tokenDecipher(key2, enc);
    expect(dec, equals(text));
  });

  test('randomDeviceId produces valid Telegram start params', () {
    final regex = RegExp(r'^[A-Za-z0-9_-]{32}$');
    for (int i = 0; i < 50; i++) {
      final id = Bnit.randomDeviceId(32);
      expect(regex.hasMatch(id), isTrue);
    }
  });
}
