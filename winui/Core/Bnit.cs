using System.Buffers.Binary;
using System.Security.Cryptography;
using System.Text;

namespace LaneWinUI.Core;

public record BnitOutput(
    string CoreToken,
    string TraceId,
    string ClientMeta,
    byte[] Payload
);

public static class Bnit
{
    public static readonly byte[] Key = "SqperSzvbntKmv_CbnngeThis_12303!"u8.ToArray();
    public const string Alphabet = "zxcvbnmasdfghjklqwertyuiop1234567890-_QWERTYUIOPASDFGHJKLZXCVBNM";
    public const string DefaultInode = "656421";

    public static string CustomBase64Encode(ReadOnlySpan<byte> data)
    {
        var sb = new StringBuilder((data.Length + 2) / 3 * 4);
        for (int i = 0; i < data.Length; i += 3)
        {
            int n = Math.Min(3, data.Length - i);
            uint v = (uint)data[i] << 16;
            if (n > 1) v |= (uint)data[i + 1] << 8;
            if (n > 2) v |= (uint)data[i + 2];

            sb.Append(Alphabet[(int)((v >> 18) & 0x3F)]);
            sb.Append(Alphabet[(int)((v >> 12) & 0x3F)]);
            sb.Append(n > 1 ? Alphabet[(int)((v >> 6) & 0x3F)] : '.');
            sb.Append(n > 2 ? Alphabet[(int)(v & 0x3F)] : '.');
        }
        return sb.ToString();
    }

    public static byte[] CustomBase64Decode(string s)
    {
        uint bits = 0;
        int n = 0;
        var outList = new List<byte>();
        foreach (char ch in s)
        {
            if (ch == '.' || ch == '=') continue;
            int idx = Alphabet.IndexOf(ch);
            if (idx == -1) continue;
            bits = (bits << 6) | (uint)idx;
            n += 6;
            if (n >= 8)
            {
                n -= 8;
                outList.Add((byte)(bits >> n));
            }
        }
        return outList.ToArray();
    }

    public static byte[] Rc4(ReadOnlySpan<byte> key, ReadOnlySpan<byte> data)
    {
        byte[] s = new byte[256];
        for (int x = 0; x < 256; x++) s[x] = (byte)x;

        int j = 0;
        for (int x = 0; x < 256; x++)
        {
            j = (j + s[x] + key[x % key.Length]) & 0xFF;
            (s[x], s[j]) = (s[j], s[x]);
        }

        byte[] output = new byte[data.Length];
        int i = 0;
        j = 0;
        for (int n = 0; n < data.Length; n++)
        {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            (s[i], s[j]) = (s[j], s[i]);

            byte k = s[(s[i] + s[j]) & 0xFF];
            output[n] = (byte)(k ^ data[n]);

            int idx = (k + i) & 0xFF;
            s[idx] = (byte)((s[idx] + k) & 0xFF);
        }
        return output;
    }

    public static byte[] TokenCipher(ReadOnlySpan<byte> key2, ReadOnlySpan<byte> data)
    {
        byte[] s = new byte[256];
        for (int x = 0; x < 256; x++) s[x] = (byte)x;

        int j = 0;
        for (int x = 0; x < 256; x++)
        {
            j = (j + s[x] + Key[x % 32] + key2[x % key2.Length]) & 0xFF;
            (s[x], s[j]) = (s[j], s[x]);
        }

        byte[] output = new byte[data.Length];
        int i = 0;
        j = 0;
        byte carry = 0x5a;
        for (int n = 0; n < data.Length; n++)
        {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            (s[i], s[j]) = (s[j], s[i]);

            int t = ((s[i] + s[j]) ^ carry) & 0xFF;
            byte v = (byte)(s[t] ^ data[n]);
            v = (byte)((v << 3) | (v >> 5));
            carry = (byte)((carry + v) & 0xFF);
            output[n] = carry;
        }
        return output;
    }

    public static byte[] TokenDecipher(ReadOnlySpan<byte> key2, ReadOnlySpan<byte> ct)
    {
        byte[] s = new byte[256];
        for (int x = 0; x < 256; x++) s[x] = (byte)x;

        int j = 0;
        for (int x = 0; x < 256; x++)
        {
            j = (j + s[x] + Key[x % 32] + key2[x % key2.Length]) & 0xFF;
            (s[x], s[j]) = (s[j], s[x]);
        }

        byte[] output = new byte[ct.Length];
        int i = 0;
        j = 0;
        byte carry = 0x5a;
        for (int n = 0; n < ct.Length; n++)
        {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            (s[i], s[j]) = (s[j], s[i]);

            int t = ((s[i] + s[j]) ^ carry) & 0xFF;
            byte k = s[t];
            byte v = (byte)((ct[n] - carry) & 0xFF);
            output[n] = (byte)(k ^ ((v >> 3) | (v << 5)));
            carry = ct[n];
        }
        return output;
    }

    public static string CanonicalQuery(string query)
    {
        if (query.StartsWith('?')) query = query[1..];
        if (string.IsNullOrEmpty(query)) return string.Empty;

        var seen = new HashSet<string>();
        var pairs = new List<(string Key, string Value)>();

        foreach (var part in query.Split('&', StringSplitOptions.RemoveEmptyEntries))
        {
            int eq = part.IndexOf('=');
            string k = eq == -1 ? part : part[..eq];
            string v = eq == -1 ? string.Empty : part[(eq + 1)..];
            if (seen.Add(k))
            {
                pairs.Add((k, v));
            }
        }

        pairs.Sort((a, b) => string.Compare(a.Key, b.Key, StringComparison.Ordinal));
        return string.Join("&", pairs.Select(p => $"{p.Key}={p.Value}"));
    }

    public static BnitOutput Build(
        string method,
        string path,
        string query = "",
        byte[]? body = null,
        long timestamp = 0,
        string? bootId = null,
        string? inode = null,
        long? nowMs = null,
        uint[]? randoms = null)
    {
        body ??= Array.Empty<byte>();

        uint[] rnd = randoms ?? new uint[15];
        if (randoms == null)
        {
            byte[] rndBytes = new byte[60];
            RandomNumberGenerator.Fill(rndBytes);
            for (int r = 0; r < 15; r++)
            {
                rnd[r] = BinaryPrimitives.ReadUInt32LittleEndian(rndBytes.AsSpan(r * 4, 4));
            }
        }

        long now = nowMs ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        string bId = bootId ?? Guid.NewGuid().ToString();
        string inod = inode ?? DefaultInode;

        byte[] nonce = new byte[16];
        for (int r = 0; r < 4; r++)
        {
            BinaryPrimitives.WriteUInt32LittleEndian(nonce.AsSpan(r * 4, 4), rnd[r]);
        }
        string nonceHex = Convert.ToHexString(nonce).ToLowerInvariant();
        string ts = (now + timestamp).ToString();

        // field: RC4(Key + nonceHex + ts, body)
        var rc4Key = new List<byte>(Key.Length + nonceHex.Length + ts.Length);
        rc4Key.AddRange(Key);
        rc4Key.AddRange(Encoding.Latin1.GetBytes(nonceHex));
        rc4Key.AddRange(Encoding.Latin1.GetBytes(ts));
        byte[] payload = Rc4(rc4Key.ToArray(), body);

        // msg1 = method:path:query:ts:nonce:field:boot_id:inode
        var msg1 = new List<byte>();
        msg1.AddRange(Encoding.Latin1.GetBytes(method));
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(path));
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(query));
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(ts));
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(nonceHex));
        msg1.Add((byte)':');
        msg1.AddRange(payload);
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(bId));
        msg1.Add((byte)':');
        msg1.AddRange(Encoding.Latin1.GetBytes(inod));

        byte[] hmacDigest = HMACSHA256.HashData(Key, msg1.ToArray());
        string hmacHex = Convert.ToHexString(hmacDigest).ToLowerInvariant();

        char flag = body.Length > 0 ? '1' : '0';
        string envelope = $"{hmacHex}|{ts}|{nonceHex}|{bId}|{inod}|{flag}";

        byte[] key2 = new byte[4];
        BinaryPrimitives.WriteUInt32BigEndian(key2, rnd[4]);

        byte[] encryptedEnvelope = TokenCipher(key2, Encoding.Latin1.GetBytes(envelope));
        byte[] tokenBlob = new byte[4 + encryptedEnvelope.Length];
        Buffer.BlockCopy(key2, 0, tokenBlob, 0, 4);
        Buffer.BlockCopy(encryptedEnvelope, 0, tokenBlob, 4, encryptedEnvelope.Length);

        string coreToken = CustomBase64Encode(tokenBlob);

        // Trace ID: UUID from 4 u32 little endian
        var sbTrace = new StringBuilder(32);
        for (int r = 0; r < 4; r++)
        {
            byte[] b = new byte[4];
            BinaryPrimitives.WriteUInt32LittleEndian(b, rnd[5 + r]);
            sbTrace.Append(Convert.ToHexString(b).ToLowerInvariant());
        }
        string u = sbTrace.ToString();
        string traceId = $"{u[0..8]}-{u[8..12]}-{u[12..16]}-{u[16..20]}-{u[20..32]}";

        // Client meta: base64 of 6 u32 little-endian
        byte[] meta = new byte[24];
        for (int r = 0; r < 6; r++)
        {
            BinaryPrimitives.WriteUInt32LittleEndian(meta.AsSpan(r * 4, 4), rnd[9 + r]);
        }
        string clientMeta = CustomBase64Encode(meta);

        return new BnitOutput(coreToken, traceId, clientMeta, payload);
    }

    public static string RandomDeviceId()
    {
        byte[] bytes = new byte[16];
        RandomNumberGenerator.Fill(bytes);
        return "lanemusic_" + Convert.ToHexString(bytes).ToLowerInvariant();
    }
}
