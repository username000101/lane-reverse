class LanePlaylist {
  final String playlistId;
  final String playlistName;
  final String? playlistImageUrl;
  final List<String> playlistTracksIds;
  final int? tracksCount;

  const LanePlaylist({
    required this.playlistId,
    required this.playlistName,
    this.playlistImageUrl,
    this.playlistTracksIds = const [],
    this.tracksCount,
  });

  int get totalTracks => tracksCount ?? playlistTracksIds.length;

  bool get isLikes => playlistId == 'lane_likes';

  factory LanePlaylist.fromJson(Map<String, dynamic> json) {
    final rawIds = json['playlistTracksIds'];
    List<String> ids = [];
    if (rawIds is List) {
      ids = rawIds.map((e) => e.toString()).toList();
    }

    return LanePlaylist(
      playlistId: json['playlistId']?.toString() ?? 'lane_likes',
      playlistName: json['playlistName']?.toString() ?? 'Любимые треки',
      playlistImageUrl: json['playlistImageUrl']?.toString(),
      playlistTracksIds: ids,
      tracksCount: json['tracksCount'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'playlistId': playlistId,
      'playlistName': playlistName,
      'playlistImageUrl': playlistImageUrl,
      'playlistTracksIds': playlistTracksIds,
      'tracksCount': tracksCount,
    };
  }
}
