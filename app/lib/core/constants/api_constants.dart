class ApiConstants {
  static const String baseUrl = 'https://ru.laneapi.com';
  static const String botUsername = 'lane_music_bot';
  static const String appVersion = '207';
  static const String platform = 'android';
  static const String userAgent = 'LaneMusic/1.0 (Android; Mobile)';

  // Endpoints
  static String authEndpoint(String deviceId) => '/auth/$deviceId';
  static const String userPlaylists = '/user/playlists';
  static const String userRecent = '/user/recent';
  static String playlistMeta(String id) => '/playlist/$id';
  static String playlistTracks(String id, {int page = 1, int pageSize = 50}) =>
      '/playlist/$id/tracks?page=$page&pageSize=$pageSize';
  static String trackStream({
    required String trackId,
    required String refId,
    String quality = 'HIGH',
  }) =>
      '/track/stream?trackId=$trackId&refId=$refId&streamQuality=$quality';
}
