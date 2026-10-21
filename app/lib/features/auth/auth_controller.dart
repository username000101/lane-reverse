import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../core/constants/api_constants.dart';
import '../../core/crypto/bnit.dart';
import '../../core/models/lane_user.dart';
import '../../core/network/lane_api_client.dart';
import '../../core/storage/session_storage.dart';

enum AuthStatus {
  initial,
  loading,
  polling,
  authenticated,
  error,
}

class AuthController extends ChangeNotifier {
  final LaneApiClient _apiClient;

  AuthStatus _status = AuthStatus.initial;
  String _deviceId = '';
  String _botUrl = '';
  int _attempts = 0;
  int _secondsLeft = 1200; // 20 minutes
  String? _errorMessage;
  LaneUser? _currentUser;
  Timer? _pollTimer;
  Timer? _countdownTimer;

  AuthController({required LaneApiClient apiClient}) : _apiClient = apiClient {
    _checkExistingSession();
  }

  AuthStatus get status => _status;
  String get deviceId => _deviceId;
  String get botUrl => _botUrl;
  int get attempts => _attempts;
  int get secondsLeft => _secondsLeft;
  String? get errorMessage => _errorMessage;
  LaneUser? get currentUser => _currentUser;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  Future<void> _checkExistingSession() async {
    _status = AuthStatus.loading;
    notifyListeners();

    final session = await SessionStorage.loadSession();
    if (session != null) {
      _deviceId = session.deviceId;
      _currentUser = session.user;
      _apiClient.token = session.token;
      _apiClient.deviceId = session.deviceId;
      _status = AuthStatus.authenticated;
    } else {
      _status = AuthStatus.initial;
    }
    notifyListeners();
  }

  void startAuthFlow() {
    _cancelTimers();
    _deviceId = Bnit.randomDeviceId(32);
    _botUrl = 'https://t.me/${ApiConstants.botUsername}?start=auth$_deviceId';
    _attempts = 0;
    _secondsLeft = 1200;
    _status = AuthStatus.polling;
    _errorMessage = null;
    notifyListeners();

    // Start countdown
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsLeft > 0) {
        _secondsLeft--;
        notifyListeners();
      } else {
        _cancelTimers();
        _status = AuthStatus.error;
        _errorMessage = 'Время действия сессии истекло. Сгенерируйте новый ключ.';
        notifyListeners();
      }
    });

    // Start polling loop every 2.5s
    _pollTimer = Timer.periodic(const Duration(milliseconds: 2500), (_) async {
      await _pollOnce();
    });

    _pollOnce();
  }

  Future<void> _pollOnce() async {
    if (_status != AuthStatus.polling) return;
    _attempts++;
    notifyListeners();

    try {
      final res = await _apiClient.pollAuthToken(_deviceId);
      if (res != null && res['token'] is String) {
        final token = res['token'] as String;
        LaneUser? user;
        if (res['user'] is Map<String, dynamic>) {
          user = LaneUser.fromJson(res['user'] as Map<String, dynamic>);
        }

        await SessionStorage.saveSession(
          deviceId: _deviceId,
          token: token,
          user: user,
        );

        _cancelTimers();
        _apiClient.token = token;
        _apiClient.deviceId = _deviceId;
        _currentUser = user;
        _status = AuthStatus.authenticated;
        notifyListeners();
      }
    } catch (e) {
      // Ignore transient network errors during polling
    }
  }

  Future<void> openTelegramBot() async {
    if (_botUrl.isEmpty) return;
    final uri = Uri.parse(_botUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  Future<void> logout() async {
    _cancelTimers();
    await SessionStorage.clearSession();
    _apiClient.token = null;
    _currentUser = null;
    _status = AuthStatus.initial;
    notifyListeners();
  }

  void _cancelTimers() {
    _pollTimer?.cancel();
    _pollTimer = null;
    _countdownTimer?.cancel();
    _countdownTimer = null;
  }

  @override
  void dispose() {
    _cancelTimers();
    super.dispose();
  }
}
