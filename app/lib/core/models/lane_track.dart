class LaneTrack {
  final String songId;
  final String title;
  final String artistsDisplayedName;
  final String? coverUrl;
  final String duration;
  final String? album;
  final bool explicit;

  const LaneTrack({
    required this.songId,
    required this.title,
    required this.artistsDisplayedName,
    this.coverUrl,
    required this.duration,
    this.album,
    this.explicit = false,
  });

  factory LaneTrack.fromJson(Map<String, dynamic> json) {
    String dur = '0:00';
    if (json['duration'] != null) {
      if (json['duration'] is int) {
        final total = json['duration'] as int;
        final m = total ~/ 60;
        final s = total % 60;
        dur = '$m:${s < 10 ? '0' : ''}$s';
      } else {
        dur = json['duration'].toString();
      }
    }

    String? alb;
    if (json['spData'] is Map && json['spData']['album'] != null) {
      alb = json['spData']['album'].toString();
    }

    return LaneTrack(
      songId: json['songId']?.toString() ?? '',
      title: json['title']?.toString() ?? 'Без названия',
      artistsDisplayedName:
          json['artistsDisplayedName']?.toString() ?? 'Неизвестный исполнитель',
      coverUrl: json['coverUrl']?.toString(),
      duration: dur,
      album: alb,
      explicit: json['explicit'] == true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'songId': songId,
      'title': title,
      'artistsDisplayedName': artistsDisplayedName,
      'coverUrl': coverUrl,
      'duration': duration,
      'album': album,
      'explicit': explicit,
    };
  }
}
