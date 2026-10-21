import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';

class BnitHeaders {
  final String coreToken;
  final String traceId;
  final String clientMeta;
  final Uint8List payload;

  BnitHeaders({
    required this.coreToken,
    required this.traceId,
    required this.clientMeta,
    required this.payload,
  });
}

class Bnit {
  static final List<int> key = latin1.encode('SqperSzvbntKmv_CbnngeThis_12303!');
  static const String alphabet =
      'zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM';
  static const String defaultInode = '656421';

  static final Random _secureRandom = Random.secure();

  /// Генерация безопасного deviceId (LDI) для Telegram auth flow.
  static String randomDeviceId([int length = 32]) {
    const chars =
        'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-';
    final bytes = Uint8List(length);
    for (int i = 0; i < length; i++) {
      bytes[i] = _secureRandom.nextInt(256);
    }
    final buffer = StringBuffer();
    for (final b in bytes) {
      buffer.write(chars[b & 63]);
    }
    return buffer.toString();
  }

  /// Кастомный Base64 с подменой алфавита и символом `.` вместо `=`.
  static String customBase64Encode(List<int> bytes) {
    final std = base64Encode(bytes);
    final sb = StringBuffer();
    for (int i = 0; i < std.length; i++) {
      final ch = std[i];
      if (ch == '=') {
        sb.write('.');
        continue;
      }
      final code = ch.codeUnitAt(0);
      int idx;
      if (code >= 65 && code <= 90) {
        idx = code - 65; // A-Z
      } else if (code >= 97 && code <= 122) {
        idx = code - 97 + 26; // a-z
      } else if (code >= 48 && code <= 57) {
        idx = code - 48 + 52; // 0-9
      } else if (ch == '+') {
        idx = 62;
      } else {
        idx = 63;
      }
      sb.write(alphabet[idx]);
    }
    return sb.toString();
  }

  /// Декодирование кастомного Base64.
  static Uint8List customBase64Decode(String str) {
    final map = <String, int>{};
    for (int i = 0; i < alphabet.length; i++) {
      map[alphabet[i]] = i;
    }
    int bits = 0;
    int n = 0;
    final out = <int>[];
    for (int i = 0; i < str.length; i++) {
      final ch = str[i];
      if (ch == '.' || ch == '=') continue;
      final idx = map[ch];
      if (idx == null) throw FormatException('Bad custom base64 char: $ch');
      bits = ((bits << 6) | idx) & 0xFFFFFFFF;
      n += 6;
      if (n >= 8) {
        n -= 8;
        out.add((bits >> n) & 0xFF);
      }
    }
    return Uint8List.fromList(out);
  }

  /// RC4 с мутацией S-бокса (для тела запроса и `red`-ответов).
  static Uint8List rc4(List<int> keyBytes, List<int> data) {
    final s = Uint8List(256);
    for (int i = 0; i < 256; i++) {
      s[i] = i;
    }

    int j = 0;
    for (int i = 0; i < 256; i++) {
      j = (j + s[i] + keyBytes[i % keyBytes.length]) & 0xFF;
      final t = s[i];
      s[i] = s[j];
      s[j] = t;
    }

    final out = Uint8List(data.length);
    int i = 0;
    j = 0;
    for (int n = 0; n < data.length; n++) {
      i = (i + 1) & 0xFF;
      j = (j + s[i]) & 0xFF;
      final t = s[i];
      s[i] = s[j];
      s[j] = t;

      final k = s[(s[i] + s[j]) & 0xFF];
      out[n] = k ^ data[n];

      final idx = (k + i) & 0xFF;
      s[idx] = (s[idx] + k) & 0xFF;
    }
    return out;
  }

  /// Шифр конверта токена (двухключевой KSA + PRGA с переносом).
  static Uint8List tokenCipher(List<int> key2, List<int> data) {
    final s = Uint8List(256);
    for (int i = 0; i < 256; i++) {
      s[i] = i;
    }

    int j = 0;
    for (int i = 0; i < 256; i++) {
      j = (j + s[i] + key[i % 32] + key2[i % key2.length]) & 0xFF;
      final t = s[i];
      s[i] = s[j];
      s[j] = t;
    }

    final out = Uint8List(data.length);
    int i = 0;
    j = 0;
    int carry = 0x5a;
    for (int n = 0; n < data.length; n++) {
      i = (i + 1) & 0xFF;
      j = (j + s[i]) & 0xFF;
      final t0 = s[i];
      s[i] = s[j];
      s[j] = t0;

      final t = ((s[i] + s[j]) & 0xFF) ^ carry;
      int v = s[t] ^ data[n];
      v = ((v << 3) | (v >> 5)) & 0xFF;
      carry = (carry + v) & 0xFF;
      out[n] = carry;
    }
    return out;
  }

  /// Расшифровка конверта токена.
  static Uint8List tokenDecipher(List<int> key2, List<int> ct) {
    final s = Uint8List(256);
    for (int i = 0; i < 256; i++) {
      s[i] = i;
    }

    int j = 0;
    for (int i = 0; i < 256; i++) {
      j = (j + s[i] + key[i % 32] + key2[i % key2.length]) & 0xFF;
      final t = s[i];
      s[i] = s[j];
      s[j] = t;
    }

    final out = Uint8List(ct.length);
    int i = 0;
    j = 0;
    int carry = 0x5a;
    for (int n = 0; n < ct.length; n++) {
      i = (i + 1) & 0xFF;
      j = (j + s[i]) & 0xFF;
      final t0 = s[i];
      s[i] = s[j];
      s[j] = t0;

      final t = ((s[i] + s[j]) & 0xFF) ^ carry;
      final k = s[t];
      int v = (ct[n] - carry) & 0xFF;
      v = ((v >> 3) | (v << 5)) & 0xFF;
      out[n] = k ^ v;
      carry = ct[n];
    }
    return out;
  }

  /// Каноническая строка query параметров (уникальные, отсортированные).
  static String canonicalQuery(String query) {
    final q = query.startsWith('?') ? query.substring(1) : query;
    if (q.isEmpty) return '';
    final seen = <String>{};
    final pairs = <MapEntry<String, String>>[];
    for (final part in q.split('&')) {
      if (part.isEmpty) continue;
      final eq = part.indexOf('=');
      final name = eq == -1 ? part : part.substring(0, eq);
      final value = eq == -1 ? '' : part.substring(eq + 1);
      if (seen.contains(name)) continue;
      seen.add(name);
      pairs.add(MapEntry(name, value));
    }
    pairs.sort((a, b) => a.key.compareTo(b.key));
    return pairs.map((e) => '${e.key}=${e.value}').join('&');
  }

  static Uint8List _u32le(int num) {
    final b = Uint8List(4);
    ByteData.view(b.buffer).setUint32(0, num & 0xFFFFFFFF, Endian.little);
    return b;
  }

  static Uint8List _u32be(int num) {
    final b = Uint8List(4);
    ByteData.view(b.buffer).setUint32(0, num & 0xFFFFFFFF, Endian.big);
    return b;
  }

  static String _bytesToHex(List<int> bytes) {
    final sb = StringBuffer();
    for (final b in bytes) {
      sb.write(b.toRadixString(16).padLeft(2, '0'));
    }
    return sb.toString();
  }

  /// Сборка подписи запроса к Lane API.
  static BnitHeaders build({
    required String method,
    required String path,
    String query = '',
    List<int>? body,
    int timestamp = 0,
    String? bootId,
    String? inode,
    int? nowMs,
    List<int>? randoms,
  }) {
    final bodyBytes = body != null ? Uint8List.fromList(body) : Uint8List(0);

    List<int> randList = randoms ?? [];
    if (randList.isEmpty) {
      randList = List<int>.generate(15, (_) => _secureRandom.nextInt(0xFFFFFFFF));
    }

    final now = nowMs ?? DateTime.now().millisecondsSinceEpoch;
    final bId = bootId ?? _randomUuid();
    final inod = inode ?? defaultInode;

    final nonce = Uint8List(16);
    for (int i = 0; i < 4; i++) {
      nonce.setRange(i * 4, (i + 1) * 4, _u32le(randList[i]));
    }
    final nonceHex = _bytesToHex(nonce);
    final ts = (now + timestamp).toString();

    // field = RC4(KEY || hex(nonce) || ts, body);
    final rc4Key = [
      ...key,
      ...latin1.encode(nonceHex),
      ...latin1.encode(ts),
    ];
    final payload = rc4(rc4Key, bodyBytes);

    // msg1 = method:path:query:ts:nonce:field:boot_id:inode
    final msg1Bytes = [
      ...latin1.encode(method),
      0x3a, // ':'
      ...latin1.encode(path),
      0x3a,
      ...latin1.encode(query),
      0x3a,
      ...latin1.encode(ts),
      0x3a,
      ...latin1.encode(nonceHex),
      0x3a,
      ...payload, // raw bytes!
      0x3a,
      ...latin1.encode(bId),
      0x3a,
      ...latin1.encode(inod),
    ];

    final hmacDigest = Hmac(sha256, key).convert(msg1Bytes);
    final hmacHex = hmacDigest.toString();

    // Envelope
    final flag = bodyBytes.isNotEmpty ? '1' : '0';
    final envelope = '$hmacHex|$ts|$nonceHex|$bId|$inod|$flag';

    final key2 = _u32be(randList[4]);
    final encryptedEnvelope = tokenCipher(key2, latin1.encode(envelope));
    final tokenBlob = [...key2, ...encryptedEnvelope];
    final coreToken = customBase64Encode(tokenBlob);

    // traceId from randoms[5..8]
    final traceBytes = Uint8List(16);
    for (int i = 0; i < 4; i++) {
      traceBytes.setRange(i * 4, (i + 1) * 4, _u32le(randList[5 + i]));
    }
    final traceId = _formatUuid(traceBytes);

    // clientMeta from randoms[9..14]
    final metaBytes = Uint8List(24);
    for (int i = 0; i < 6; i++) {
      metaBytes.setRange(i * 4, (i + 1) * 4, _u32le(randList[9 + i]));
    }
    final clientMeta = customBase64Encode(metaBytes);

    return BnitHeaders(
      coreToken: coreToken,
      traceId: traceId,
      clientMeta: clientMeta,
      payload: payload,
    );
  }

  static String _randomUuid() {
    final b = Uint8List(16);
    for (int i = 0; i < 16; i++) {
      b[i] = _secureRandom.nextInt(256);
    }
    b[6] = (b[6] & 0x0F) | 0x40; // v4
    b[8] = (b[8] & 0x3F) | 0x80; // variant
    return _formatUuid(b);
  }

  static String _formatUuid(List<int> b) {
    final hex = _bytesToHex(b);
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}';
  }
}
