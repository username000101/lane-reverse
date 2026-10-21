class LaneUser {
  final String? laneId;
  final String? displayedName;
  final String? username;
  final String? avatarUrl;

  const LaneUser({
    this.laneId,
    this.displayedName,
    this.username,
    this.avatarUrl,
  });

  String get displayName => displayedName ?? username ?? 'Пользователь';

  String get initials {
    final name = displayName.trim();
    if (name.isEmpty) return 'L';
    final parts = name.split(RegExp(r'\s+'));
    if (parts.length > 1) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, min(2, name.length)).toUpperCase();
  }

  factory LaneUser.fromJson(Map<String, dynamic> json) {
    return LaneUser(
      laneId: json['laneId'] as String?,
      displayedName: (json['displayedName'] ?? json['name']) as String?,
      username: json['username'] as String?,
      avatarUrl: (json['avatarUrl'] ?? json['avatar']) as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'laneId': laneId,
      'displayedName': displayedName,
      'username': username,
      'avatarUrl': avatarUrl,
    };
  }

  static int min(int a, int b) => a < b ? a : b;
}
