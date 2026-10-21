import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import '../../core/models/lane_track.dart';
import '../../core/network/lane_api_client.dart';
import '../../core/storage/session_storage.dart';

class PlayerController extends ChangeNotifier {
  final LaneApiClient _apiClient;
  final AudioPlayer _audioPlayer = AudioPlayer();

  List<LaneTrack> _queue = [];
  int _currentIndex = -1;
  String _activeRefId = 'lane_likes';
  String _quality = 'HIGH';
  bool _isLoading = false;
  String? _error;

  PlayerController({required LaneApiClient apiClient})
      : _apiClient = apiClient {
    _init();
  }

  void _init() async {
    _quality = await SessionStorage.getStreamQuality();

    _audioPlayer.playerStateStream.listen((state) {
      if (state.processingState == ProcessingState.completed) {
        playNext();
      }
      notifyListeners();
    });
  }

  AudioPlayer get audioPlayer => _audioPlayer;
  LaneTrack? get currentTrack =>
      _currentIndex >= 0 && _currentIndex < _queue.length
          ? _queue[_currentIndex]
          : null;
  bool get isPlaying => _audioPlayer.playing;
  bool get isLoading => _isLoading;
  String get quality => _quality;
  String? get error => _error;
  List<LaneTrack> get queue => _queue;

  Stream<Duration> get positionStream => _audioPlayer.positionStream;
  Stream<Duration?> get durationStream => _audioPlayer.durationStream;

  void setQueue(List<LaneTrack> tracks, String refId) {
    _queue = List.from(tracks);
    _activeRefId = refId;
    notifyListeners();
  }

  Future<void> playTrack(LaneTrack track, {int? index, String? refId}) async {
    if (refId != null) _activeRefId = refId;
    if (index != null) {
      _currentIndex = index;
    } else {
      _currentIndex = _queue.indexWhere((t) => t.songId == track.songId);
      if (_currentIndex == -1) {
        _queue.add(track);
        _currentIndex = _queue.length - 1;
      }
    }

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final streamUrl = await _apiClient.getTrackStreamUrl(
        trackId: track.songId,
        refId: _activeRefId,
        quality: _quality,
      );

      await _audioPlayer.setUrl(streamUrl);
      await _audioPlayer.play();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _error = 'Не удалось загрузить аудио: $e';
      notifyListeners();
    }
  }

  Future<void> togglePlayPause() async {
    if (_audioPlayer.playing) {
      await _audioPlayer.pause();
    } else {
      if (_audioPlayer.duration != null) {
        await _audioPlayer.play();
      } else if (currentTrack != null) {
        await playTrack(currentTrack!);
      }
    }
    notifyListeners();
  }

  Future<void> playNext() async {
    if (_queue.isEmpty) return;
    int next = _currentIndex + 1;
    if (next >= _queue.length) next = 0;
    await playTrack(_queue[next], index: next);
  }

  Future<void> playPrevious() async {
    if (_queue.isEmpty) return;
    int prev = _currentIndex - 1;
    if (prev < 0) prev = _queue.length - 1;
    await playTrack(_queue[prev], index: prev);
  }

  Future<void> setQuality(String newQuality) async {
    if (_quality == newQuality) return;
    _quality = newQuality;
    await SessionStorage.setStreamQuality(newQuality);

    if (currentTrack != null && _audioPlayer.playing) {
      final curPos = _audioPlayer.position;
      try {
        final newUrl = await _apiClient.getTrackStreamUrl(
          trackId: currentTrack!.songId,
          refId: _activeRefId,
          quality: _quality,
        );
        await _audioPlayer.setUrl(newUrl);
        await _audioPlayer.seek(curPos);
        await _audioPlayer.play();
      } catch (_) {}
    }
    notifyListeners();
  }

  Future<void> seek(Duration position) async {
    await _audioPlayer.seek(position);
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    super.dispose();
  }
}
