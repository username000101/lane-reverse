using System.Text.Json;
using System.Text.Json.Serialization;

namespace LaneWinUI.Core;

public class LaneTrack
{
    [JsonPropertyName("songId")]
    public string SongId { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = "Без названия";

    [JsonPropertyName("artistsDisplayedName")]
    public string? ArtistsDisplayedNameProp { get; set; }

    [JsonPropertyName("artists")]
    public List<LaneArtist>? Artists { get; set; }

    [JsonPropertyName("duration")]
    public JsonElement DurationElement { get; set; }

    [JsonPropertyName("explicit")]
    public bool Explicit { get; set; }

    [JsonPropertyName("coverUrl")]
    public string? CoverUrl { get; set; }

    [JsonIgnore]
    public string ArtistsDisplayedName
    {
        get
        {
            if (!string.IsNullOrEmpty(ArtistsDisplayedNameProp)) return ArtistsDisplayedNameProp;
            if (Artists != null && Artists.Count > 0) return string.Join(", ", Artists.Select(a => a.Name));
            return "Неизвестный исполнитель";
        }
    }

    [JsonIgnore]
    public string DurationFormatted
    {
        get
        {
            try
            {
                if (DurationElement.ValueKind == JsonValueKind.Number)
                {
                    int totalSec = DurationElement.GetInt32();
                    return $"{totalSec / 60}:{totalSec % 60:D2}";
                }
                if (DurationElement.ValueKind == JsonValueKind.String)
                {
                    return DurationElement.GetString() ?? "0:00";
                }
            }
            catch { }
            return "0:00";
        }
    }
}

public class LaneArtist
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

public class LanePlaylist
{
    [JsonPropertyName("id")]
    public string? Id { get; set; }

    [JsonPropertyName("playlistId")]
    public string? PlaylistId { get; set; }

    [JsonIgnore]
    public string RealId => !string.IsNullOrEmpty(PlaylistId) ? PlaylistId : (Id ?? "lane_likes");

    [JsonPropertyName("title")]
    public string? Title { get; set; }

    [JsonPropertyName("playlistName")]
    public string? PlaylistName { get; set; }

    [JsonIgnore]
    public string RealTitle => !string.IsNullOrEmpty(PlaylistName) ? PlaylistName : (Title ?? "Плейлист");

    [JsonPropertyName("tracksCount")]
    public int? TracksCount { get; set; }

    [JsonPropertyName("playlistTracksIds")]
    public List<string>? PlaylistTracksIds { get; set; }

    [JsonIgnore]
    public int TotalTracks => TracksCount ?? PlaylistTracksIds?.Count ?? 0;

    [JsonPropertyName("coverUrl")]
    public string? CoverUrl { get; set; }

    [JsonPropertyName("playlistImageUrl")]
    public string? PlaylistImageUrl { get; set; }

    [JsonIgnore]
    public string? RealCoverUrl => PlaylistImageUrl ?? CoverUrl;
}

public class LaneUser
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("username")]
    public string? Username { get; set; }

    [JsonPropertyName("firstName")]
    public string? FirstName { get; set; }

    [JsonPropertyName("lastName")]
    public string? LastName { get; set; }

    [JsonPropertyName("isVip")]
    public bool IsVip { get; set; }

    [JsonPropertyName("vipExpireAt")]
    public long? VipExpireAt { get; set; }

    [JsonIgnore]
    public string DisplayName =>
        !string.IsNullOrEmpty(Username)
            ? $"@{Username}"
            : $"{FirstName} {LastName}".Trim();
}

public class AuthSession
{
    public string Token { get; set; } = string.Empty;
    public string DeviceId { get; set; } = string.Empty;
    public LaneUser? User { get; set; }
}
