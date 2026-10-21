import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../crypto/bnit.dart';
import '../models/lane_playlist.dart';
import '../models/lane_track.dart';
import '../models/lane_user.dart';

class LaneApiException implements Exception {
  final int statusCode;
  final String message;
  final String? errorCode;

  LaneApiException({
    required this.statusCode,
    required this.message,
    this.errorCode,
  });

  @override
  String toString() => 'LaneApiException($statusCode, $errorCode: $message)';
}

class LaneApiClient {
  final http.Client _client;
  String? token;
  String? deviceId;

  LaneApiClient({http.Client? client, this.token, this.deviceId})
      : _client = client ?? http.Client();

  /// Подписанный HTTP-запрос к API Lane.
  Future<http.Response> request({
    required String method,
    required String path,
    String query = '',
    List<int>? body,
    bool requiresAuth = true,
  }) async {
    final ldi = deviceId ?? '';
    final signed = Bnit.build(
      method: method.toUpperCase(),
      path: path,
      query: query,
      body: body,
    );

    final canonQ = Bnit.canonicalQuery(query);
    final urlStr =
        '${ApiConstants.baseUrl}$path${canonQ.isNotEmpty ? '?$canonQ' : ''}';
    final uri = Uri.parse(urlStr);

    final headers = <String, String>{
      'x-accept-red': '0',
      'x-app-version': ApiConstants.appVersion,
      'x-platform': ApiConstants.platform,
      'x-theme': 'dark',
      'accept-language': 'ru',
      'tz': 'Europe/Moscow',
      'user-agent': ApiConstants.userAgent,
      'accept': 'application/json',
      'x-core-token': signed.coreToken,
      'x-request-trace-id': signed.traceId,
      'x-client-meta': signed.clientMeta,
    };

    if (ldi.isNotEmpty) {
      headers['ldi'] = ldi;
    }
    if (requiresAuth && token != null && token!.isNotEmpty) {
      headers['authorization'] = 'Bearer $token';
    }
    if (body != null && body.isNotEmpty) {
      headers['content-type'] = 'application/json; charset=UTF-8';
    }

    final req = http.Request(method, uri);
    req.headers.addAll(headers);
    if (body != null && body.isNotEmpty) {
      req.bodyBytes = signed.payload;
    }

    final streamedRes = await _client.send(req);
    return http.Response.fromStream(streamedRes);
  }

  /// Опрос статуса авторизации через /auth/<deviceId>.
  Future<Map<String, dynamic>?> pollAuthToken(String devId) async {
    final res = await request(
      method: 'GET',
      path: ApiConstants.authEndpoint(devId),
      requiresAuth: false,
    );

    if (res.statusCode == 200) {
      final json = jsonDecode(utf8.decode(res.bodyBytes));
      if (json is Map<String, dynamic> && json['token'] is String) {
        return json;
      }
    }
    return null; // В процессе (pending) или еще не нажал Start
  }

  /// Получение плейлистов пользователя.
  Future<List<LanePlaylist>> getUserPlaylists() async {
    final res = await request(
      method: 'GET',
      path: ApiConstants.userPlaylists,
    );

    if (res.statusCode != 200) {
      throw _parseError(res);
    }

    final data = jsonDecode(utf8.decode(res.bodyBytes));
    if (data is List) {
      return data
          .map((e) => LanePlaylist.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Получение треков плейлиста.
  Future<List<LaneTrack>> getPlaylistTracks(
    String playlistId, {
    int page = 1,
    int pageSize = 50,
  }) async {
    final res = await request(
      method: 'GET',
      path: '/playlist/$playlistId/tracks',
      query: 'page=$page&pageSize=$pageSize',
    );

    if (res.statusCode != 200) {
      throw _parseError(res);
    }

    final data = jsonDecode(utf8.decode(res.bodyBytes));
    final items = data is Map ? (data['items'] as List?) : (data as List?);
    if (items != null) {
      return items
          .map((e) => LaneTrack.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Получение прямого CDN-потока для трека.
  Future<String> getTrackStreamUrl({
    required String trackId,
    required String refId,
    String quality = 'HIGH',
  }) async {
    final res = await request(
      method: 'GET',
      path: '/track/stream',
      query: 'trackId=$trackId&refId=$refId&streamQuality=$quality',
    );

    if (res.statusCode != 200) {
      throw _parseError(res);
    }

    final data = jsonDecode(utf8.decode(res.bodyBytes));
    if (data is Map && data['url'] is String) {
      return data['url'] as String;
    }
    throw LaneApiException(
      statusCode: 502,
      message: 'Сервер не вернул ссылку на аудиопоток',
    );
  }

  LaneApiException _parseError(http.Response res) {
    String msg = 'Ошибка API: ${res.statusCode}';
    String? code = res.headers['x-error-code'];
    try {
      final json = jsonDecode(res.body);
      if (json is Map && (json['message'] != null || json['error'] != null)) {
        msg = (json['message'] ?? json['error']).toString();
      }
      if (json is Map && json['code'] != null) {
        code = json['code'].toString();
      }
    } catch (_) {}
    return LaneApiException(
      statusCode: res.statusCode,
      message: msg,
      errorCode: code,
    );
  }
}
