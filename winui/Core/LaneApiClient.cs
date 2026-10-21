using System.Net.Http.Headers;
using System.Text.Json;

namespace LaneWinUI.Core;

public class LaneApiClient
{
    private const string BaseUrl = "https://api.lanemusic.app/v1";
    private const string AppVersion = "207";
    private const string Platform = "android";
    private const string UserAgent = "okhttp/4.12.0";

    private readonly HttpClient _http;
    public string? Token { get; set; }
    public string? DeviceId { get; set; }

    public LaneApiClient(HttpClient? httpClient = null)
    {
        _http = httpClient ?? new HttpClient();
    }

    public async Task<JsonElement> RequestAsync(
        string method,
        string path,
        string query = "",
        byte[]? body = null,
        bool requiresAuth = true)
    {
        string canonQ = Bnit.CanonicalQuery(query);
        var signed = Bnit.Build(
            method.ToUpperInvariant(),
            path,
            canonQ,
            body
        );

        string url = $"{BaseUrl}{path}{(string.IsNullOrEmpty(canonQ) ? "" : $"?{canonQ}")}";
        using var request = new HttpRequestMessage(new HttpMethod(method.ToUpperInvariant()), url);

        request.Headers.Add("x-accept-red", "0");
        request.Headers.Add("x-app-version", AppVersion);
        request.Headers.Add("x-platform", Platform);
        request.Headers.Add("x-theme", "dark");
        request.Headers.Add("accept-language", "ru");
        request.Headers.Add("tz", "Europe/Moscow");
        request.Headers.Add("User-Agent", UserAgent);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

        request.Headers.Add("x-core-token", signed.CoreToken);
        request.Headers.Add("x-request-trace-id", signed.TraceId);
        request.Headers.Add("x-client-meta", signed.ClientMeta);

        if (!string.IsNullOrEmpty(DeviceId))
        {
            request.Headers.Add("ldi", DeviceId);
        }

        if (requiresAuth && !string.IsNullOrEmpty(Token))
        {
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", Token);
        }

        if (body != null && body.Length > 0)
        {
            request.Content = new ByteArrayContent(signed.Payload);
            request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json") { CharSet = "utf-8" };
        }

        var response = await _http.SendAsync(request);
        string json = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
        {
            throw new Exception($"API Error ({response.StatusCode}): {json}");
        }

        using var doc = JsonDocument.Parse(json);
        return doc.RootElement.Clone();
    }

    public async Task<(bool Success, string? Token, LaneUser? User)> PollAuthTokenAsync(string deviceId)
    {
        try
        {
            DeviceId = deviceId;
            var root = await RequestAsync("GET", $"/auth/{deviceId}", requiresAuth: false);

            if (root.TryGetProperty("token", out var tokenProp) && tokenProp.GetString() is string token && !string.IsNullOrEmpty(token))
            {
                Token = token;
                LaneUser? user = null;
                if (root.TryGetProperty("user", out var userProp))
                {
                    user = JsonSerializer.Deserialize<LaneUser>(userProp.GetRawText());
                }
                return (true, token, user);
            }
        }
        catch
        {
            // Pending or network retry
        }
        return (false, null, null);
    }

    public async Task<LaneUser?> GetProfileAsync()
    {
        var root = await RequestAsync("GET", "/user/profile");
        if (root.TryGetProperty("data", out var dataProp))
        {
            return JsonSerializer.Deserialize<LaneUser>(dataProp.GetRawText());
        }
        return null;
    }

    public async Task<List<LanePlaylist>> GetPlaylistsAsync()
    {
        var root = await RequestAsync("GET", "/user/playlists");
        var list = new List<LanePlaylist>();

        if (root.TryGetProperty("data", out var dataProp))
        {
            if (dataProp.ValueKind == JsonValueKind.Array)
            {
                foreach (var item in dataProp.EnumerateArray())
                {
                    var pl = JsonSerializer.Deserialize<LanePlaylist>(item.GetRawText());
                    if (pl != null) list.Add(pl);
                }
            }
            else if (dataProp.TryGetProperty("playlists", out var plArr) && plArr.ValueKind == JsonValueKind.Array)
            {
                foreach (var item in plArr.EnumerateArray())
                {
                    var pl = JsonSerializer.Deserialize<LanePlaylist>(item.GetRawText());
                    if (pl != null) list.Add(pl);
                }
            }
        }

        return list;
    }

    public async Task<List<LaneTrack>> GetPlaylistTracksAsync(string playlistId)
    {
        var root = await RequestAsync("GET", $"/user/playlists/{playlistId}/tracks", "page=1&pageSize=100");
        var list = new List<LaneTrack>();

        if (root.TryGetProperty("data", out var dataProp))
        {
            JsonElement trackArray = dataProp;
            if (dataProp.TryGetProperty("tracks", out var arr))
            {
                trackArray = arr;
            }

            if (trackArray.ValueKind == JsonValueKind.Array)
            {
                foreach (var item in trackArray.EnumerateArray())
                {
                    var track = JsonSerializer.Deserialize<LaneTrack>(item.GetRawText());
                    if (track != null) list.Add(track);
                }
            }
        }

        return list;
    }

    public async Task<string?> GetTrackStreamUrlAsync(string songId, string quality = "ULTRA")
    {
        var root = await RequestAsync("GET", $"/tracks/{songId}/stream", $"streamQuality={quality}&prefetch=0");
        if (root.TryGetProperty("data", out var dataProp) && dataProp.TryGetProperty("url", out var urlProp))
        {
            return urlProp.GetString();
        }
        return null;
    }
}
