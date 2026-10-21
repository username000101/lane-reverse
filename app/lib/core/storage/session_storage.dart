import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/lane_user.dart';

class SessionData {
  final String deviceId;
  final String token;
  final LaneUser? user;

  const SessionData({
    required this.deviceId,
    required this.token,
    this.user,
  });

  Map<String, dynamic> toLanedJson() {
    return {
      'deviceId': deviceId,
      'inode': '668035',
      'token': token,
      'user': user?.toJson(),
    };
  }
}

class SessionStorage {
  static const String _keyDeviceId = 'lane_device_id';
  static const String _keyToken = 'lane_jwt_token';
  static const String _keyUser = 'lane_user_profile';
  static const String _keyQuality = 'lane_stream_quality';

  static Future<void> saveSession({
    required String deviceId,
    required String token,
    LaneUser? user,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyDeviceId, deviceId);
    await prefs.setString(_keyToken, token);
    if (user != null) {
      await prefs.setString(_keyUser, jsonEncode(user.toJson()));
    } else {
      await prefs.remove(_keyUser);
    }
  }

  static Future<SessionData?> loadSession() async {
    final prefs = await SharedPreferences.getInstance();
    final deviceId = prefs.getString(_keyDeviceId);
    final token = prefs.getString(_keyToken);

    if (deviceId == null || token == null || token.isEmpty) {
      return null;
    }

    LaneUser? user;
    final userJson = prefs.getString(_keyUser);
    if (userJson != null) {
      try {
        user = LaneUser.fromJson(jsonDecode(userJson) as Map<String, dynamic>);
      } catch (_) {}
    }

    return SessionData(
      deviceId: deviceId,
      token: token,
      user: user,
    );
  }

  static Future<void> clearSession() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyToken);
    await prefs.remove(_keyUser);
    // Note: deviceId is kept to maintain device identity unless explicitly reset
  }

  static Future<String> getStreamQuality() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_keyQuality) ?? 'HIGH';
  }

  static Future<void> setStreamQuality(String quality) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyQuality, quality);
  }
}
