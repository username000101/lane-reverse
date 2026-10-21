import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';
import '../player_controller.dart';
import 'full_player_sheet.dart';

class MiniPlayer extends StatelessWidget {
  final PlayerController controller;

  const MiniPlayer({Key? key, required this.controller}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final track = controller.currentTrack;
        if (track == null) return const SizedBox.shrink();

        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          decoration: BoxDecoration(
            color: AppColors.surfaceElevated.withOpacity(0.95),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.borderStrong),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.5),
                blurRadius: 20,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () {
                showModalBottomSheet(
                  context: context,
                  isScrollControlled: true,
                  backgroundColor: Colors.transparent,
                  builder: (_) => FullPlayerSheet(controller: controller),
                );
              },
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 8),
                    child: Row(
                      children: [
                        // Cover Art
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: track.coverUrl != null
                              ? CachedNetworkImage(
                                  imageUrl: track.coverUrl!,
                                  width: 44,
                                  height: 44,
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
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                track.title,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.textMain,
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

                        // Play/Pause Button
                        IconButton(
                          icon: controller.isLoading
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: AppColors.cyan,
                                  ),
                                )
                              : Icon(
                                  controller.isPlaying
                                      ? Icons.pause_rounded
                                      : Icons.play_arrow_rounded,
                                  size: 28,
                                  color: AppColors.textMain,
                                ),
                          onPressed: () => controller.togglePlayPause(),
                        ),

                        // Next Button
                        IconButton(
                          icon: const Icon(
                            Icons.skip_next_rounded,
                            size: 24,
                            color: AppColors.textSecondary,
                          ),
                          onPressed: () => controller.playNext(),
                        ),
                      ],
                    ),
                  ),

                  // Thin Progress Bar Line
                  StreamBuilder<Duration>(
                    stream: controller.positionStream,
                    builder: (context, snapshot) {
                      final pos = snapshot.data ?? Duration.zero;
                      final dur = controller.audioPlayer.duration ??
                          const Duration(seconds: 1);
                      final progress = (pos.inMilliseconds /
                              dur.inMilliseconds.clamp(1, double.infinity))
                          .clamp(0.0, 1.0);

                      return LinearProgressIndicator(
                        value: progress,
                        minHeight: 2,
                        backgroundColor: Colors.transparent,
                        valueColor: const AlwaysStoppedAnimation<Color>(
                            AppColors.cyan),
                      );
                    },
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _defaultCover() {
    return Container(
      width: 44,
      height: 44,
      color: const Color(0xFF1E293B),
      child: const Icon(Icons.music_note_rounded,
          size: 20, color: AppColors.textMuted),
    );
  }
}
