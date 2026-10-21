import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../../core/models/lane_playlist.dart';
import '../../core/models/lane_track.dart';
import '../../core/network/lane_api_client.dart';
import '../../core/theme/app_theme.dart';
import '../auth/auth_controller.dart';
import '../player/player_controller.dart';
import '../player/widgets/mini_player.dart';
import '../settings/config_sheet.dart';

class LibraryScreen extends StatefulWidget {
  final AuthController authController;
  final PlayerController playerController;
  final LaneApiClient apiClient;

  const LibraryScreen({
    super.key,
    required this.authController,
    required this.playerController,
    required this.apiClient,
  });

  @override
  State<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends State<LibraryScreen> {
  List<LanePlaylist> _playlists = [];
  String _activePlaylistId = 'lane_likes';
  String _activePlaylistTitle = 'Мне нравится';

  List<LaneTrack> _allTracks = [];
  List<LaneTrack> _filteredTracks = [];
  bool _isLoadingTracks = true;
  String? _error;
  String _searchQuery = '';

  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadPlaylists();
    _loadTracks('lane_likes');
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadPlaylists() async {
    try {
      final list = await widget.apiClient.getUserPlaylists();
      if (mounted) {
        setState(() {
          _playlists = list;
        });
      }
    } catch (_) {}
  }

  Future<void> _loadTracks(String playlistId) async {
    setState(() {
      _isLoadingTracks = true;
      _error = null;
    });

    try {
      final tracks = await widget.apiClient.getPlaylistTracks(playlistId);
      if (mounted) {
        setState(() {
          _allTracks = tracks;
          _applySearch();
          _isLoadingTracks = false;
        });
        widget.playerController.setQueue(_allTracks, playlistId);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Не удалось загрузить треки: $e';
          _isLoadingTracks = false;
        });
      }
    }
  }

  void _applySearch() {
    final q = _searchQuery.trim().toLowerCase();
    if (q.isEmpty) {
      _filteredTracks = List.from(_allTracks);
    } else {
      _filteredTracks = _allTracks.where((t) {
        return t.title.toLowerCase().contains(q) ||
            t.artistsDisplayedName.toLowerCase().contains(q);
      }).toList();
    }
  }

  void _selectPlaylist(String id, String title) {
    if (_activePlaylistId == id) return;
    setState(() {
      _activePlaylistId = id;
      _activePlaylistTitle = title;
    });
    _loadTracks(id);
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.authController.currentUser;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // Top App Bar
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              child: Row(
                children: [
                  // User Monogram
                  Container(
                    width: 36,
                    height: 36,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: LinearGradient(
                        colors: [Color(0xFF0284C7), Color(0xFF38BDF8)],
                      ),
                    ),
                    child: Center(
                      child: Text(
                        user?.initials ?? 'L',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: Colors.black,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),

                  // Welcome
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.displayName ?? 'Пользователь Lane',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textMain,
                          ),
                        ),
                        Row(
                          children: [
                            Container(
                              width: 6,
                              height: 6,
                              decoration: const BoxDecoration(
                                shape: BoxShape.circle,
                                color: AppColors.mint,
                              ),
                            ),
                            const SizedBox(width: 5),
                            const Text(
                              'ru.laneapi.com:443',
                              style: TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 11,
                                color: AppColors.textMuted,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),

                  // Config / CLI Button
                  IconButton(
                    icon: const Icon(Icons.terminal_rounded, size: 20),
                    color: AppColors.textSecondary,
                    tooltip: 'Конфиг сессии (laned.json)',
                    onPressed: () {
                      showModalBottomSheet(
                        context: context,
                        backgroundColor: Colors.transparent,
                        builder: (_) => ConfigSheet(
                          authController: widget.authController,
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),

            // Search Bar
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
              child: Container(
                height: 42,
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.borderSubtle),
                ),
                child: TextField(
                  controller: _searchController,
                  style: const TextStyle(
                      fontSize: 13, color: AppColors.textMain),
                  decoration: InputDecoration(
                    hintText: 'Поиск по трекам или артистам...',
                    hintStyle: const TextStyle(
                        fontSize: 13, color: AppColors.textMuted),
                    prefixIcon: const Icon(Icons.search_rounded,
                        size: 18, color: AppColors.textMuted),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear_rounded, size: 16),
                            onPressed: () {
                              _searchController.clear();
                              setState(() {
                                _searchQuery = '';
                                _applySearch();
                              });
                            },
                          )
                        : null,
                    border: InputBorder.none,
                    contentPadding: const EdgeInsets.symmetric(vertical: 10),
                  ),
                  onChanged: (val) {
                    setState(() {
                      _searchQuery = val;
                      _applySearch();
                    });
                  },
                ),
              ),
            ),
            const SizedBox(height: 8),

            // Playlists Horizontal Pill Row
            SizedBox(
              height: 38,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                children: [
                  _buildPlaylistPill(
                    id: 'lane_likes',
                    name: '❤️ Мне нравится',
                    isActive: _activePlaylistId == 'lane_likes',
                  ),
                  for (final pl in _playlists)
                    if (pl.playlistId != 'lane_likes')
                      _buildPlaylistPill(
                        id: pl.playlistId,
                        name: pl.playlistName,
                        count: pl.totalTracks,
                        isActive: _activePlaylistId == pl.playlistId,
                      ),
                ],
              ),
            ),
            const SizedBox(height: 12),

            // Track List Header
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    _activePlaylistTitle,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textMain,
                    ),
                  ),
                  Text(
                    '${_filteredTracks.length} треков',
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 11,
                      color: AppColors.textMuted,
                    ),
                  ),
                ],
              ),
            ),

            // Track List
            Expanded(
              child: _isLoadingTracks
                  ? const Center(
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        color: AppColors.cyan,
                      ),
                    )
                  : _error != null
                      ? Center(
                          child: Text(
                            _error!,
                            style: const TextStyle(color: AppColors.rose),
                          ),
                        )
                      : _filteredTracks.isEmpty
                          ? const Center(
                              child: Text(
                                'Нет треков',
                                style: TextStyle(color: AppColors.textMuted),
                              ),
                            )
                          : ListView.builder(
                              padding: const EdgeInsets.only(
                                  top: 4, bottom: 90, left: 12, right: 12),
                              itemCount: _filteredTracks.length,
                              itemBuilder: (context, index) {
                                final track = _filteredTracks[index];
                                return _buildTrackTile(track, index);
                              },
                            ),
            ),

            // Floating MiniPlayer Dock
            MiniPlayer(controller: widget.playerController),
          ],
        ),
      ),
    );
  }

  Widget _buildPlaylistPill({
    required String id,
    required String name,
    int? count,
    required bool isActive,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: () => _selectPlaylist(id, name),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            color: isActive ? AppColors.textMain : AppColors.surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: isActive ? AppColors.textMain : AppColors.borderSubtle,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                name,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
                  color: isActive ? Colors.black : AppColors.textSecondary,
                ),
              ),
              if (count != null) ...[
                const SizedBox(width: 6),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                  decoration: BoxDecoration(
                    color: isActive
                        ? Colors.black.withValues(alpha: 0.12)
                        : AppColors.borderStrong,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    count.toString(),
                    style: TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 10,
                      color: isActive ? Colors.black : AppColors.textMuted,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTrackTile(LaneTrack track, int index) {
    return ListenableBuilder(
      listenable: widget.playerController,
      builder: (context, _) {
        final isCurrent = widget.playerController.currentTrack?.songId ==
            track.songId;
        final isPlaying = isCurrent && widget.playerController.isPlaying;

        return Material(
          color: isCurrent
              ? AppColors.cyan.withValues(alpha: 0.08)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
          child: InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: () {
              widget.playerController.playTrack(
                track,
                index: index,
                refId: _activePlaylistId,
              );
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              child: Row(
                children: [
                  // Track Number or Equalizer
                  SizedBox(
                    width: 24,
                    child: isPlaying
                        ? _buildPlayingEqualizer()
                        : Text(
                            '${index + 1}',
                            style: TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 11,
                              color: isCurrent
                                  ? AppColors.cyan
                                  : AppColors.textMuted,
                            ),
                          ),
                  ),
                  const SizedBox(width: 8),

                  // Cover Art
                  ClipRRect(
                    borderRadius: BorderRadius.circular(6),
                    child: track.coverUrl != null
                        ? CachedNetworkImage(
                            imageUrl: track.coverUrl!,
                            width: 38,
                            height: 38,
                            fit: BoxFit.cover,
                            errorWidget: (_, __, ___) => _defaultCover(),
                          )
                        : _defaultCover(),
                  ),
                  const SizedBox(width: 12),

                  // Title & Artist
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          track.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: isCurrent
                                ? AppColors.cyan
                                : AppColors.textMain,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          track.artistsDisplayedName,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 11,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Duration
                  Text(
                    track.duration,
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 11,
                      color: AppColors.textMuted,
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildPlayingEqualizer() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        _buildBar(6),
        const SizedBox(width: 1.5),
        _buildBar(12),
        const SizedBox(width: 1.5),
        _buildBar(8),
      ],
    );
  }

  Widget _buildBar(double height) {
    return Container(
      width: 2,
      height: height,
      decoration: BoxDecoration(
        color: AppColors.cyan,
        borderRadius: BorderRadius.circular(1),
      ),
    );
  }

  Widget _defaultCover() {
    return Container(
      width: 38,
      height: 38,
      color: const Color(0xFF1E293B),
      child: const Icon(Icons.music_note_rounded,
          size: 18, color: AppColors.textMuted),
    );
  }
}
