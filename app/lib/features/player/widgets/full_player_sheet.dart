import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';
import '../player_controller.dart';

class FullPlayerSheet extends StatelessWidget {
  final PlayerController controller;

  const FullPlayerSheet({super.key, required this.controller});

  String _formatDuration(Duration d) {
    final m = d.inMinutes;
    final s = d.inSeconds % 60;
    return '$m:${s < 10 ? '0' : ''}$s';
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final track = controller.currentTrack;
        if (track == null) return const SizedBox.shrink();

        return Container(
          height: MediaQuery.of(context).size.height * 0.88,
          decoration: const BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
          ),
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
              child: Column(
                children: [
                  // Top Grab Bar
                  Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.borderStrong,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Header with Quality Selector
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'СЕЙЧАС ИГРАЕТ',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.05,
                          color: AppColors.textMuted,
                        ),
                      ),
                      _buildQualityPill(context),
                    ],
                  ),
                  const Spacer(),

                  // Big Cover Art
                  Center(
                    child: Container(
                      width: 280,
                      height: 280,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(20),
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.cyan.withValues(alpha: 0.12),
                            blurRadius: 50,
                            offset: const Offset(0, 14),
                          ),
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.6),
                            blurRadius: 30,
                            offset: const Offset(0, 10),
                          ),
                        ],
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(20),
                        child: track.coverUrl != null
                            ? CachedNetworkImage(
                                imageUrl: track.coverUrl!,
                                fit: BoxFit.cover,
                                errorWidget: (_, __, ___) => _defaultCover(),
                              )
                            : _defaultCover(),
                      ),
                    ),
                  ),
                  const Spacer(),

                  // Track Info
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              track.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w700,
                                letterSpacing: -0.02,
                                color: AppColors.textMain,
                              ),
                            ),
                          ),
                          if (track.explicit) ...[
                            const SizedBox(width: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 4, vertical: 1),
                              decoration: BoxDecoration(
                                color: AppColors.borderStrong,
                                borderRadius: BorderRadius.circular(3),
                              ),
                              child: const Text(
                                'E',
                                style: TextStyle(
                                  fontSize: 10,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        track.artistsDisplayedName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // Progress Scrubber
                  StreamBuilder<Duration>(
                    stream: controller.positionStream,
                    builder: (context, snapshot) {
                      final pos = snapshot.data ?? Duration.zero;
                      final dur = controller.audioPlayer.duration ??
                          const Duration(seconds: 1);
                      final clampedPos = pos > dur ? dur : pos;

                      return Column(
                        children: [
                          SliderTheme(
                            data: SliderTheme.of(context).copyWith(
                              activeTrackColor: AppColors.cyan,
                              inactiveTrackColor: AppColors.borderStrong,
                              thumbColor: Colors.white,
                              trackHeight: 4,
                            ),
                            child: Slider(
                              value: clampedPos.inMilliseconds
                                  .toDouble()
                                  .clamp(0.0, dur.inMilliseconds.toDouble()),
                              min: 0,
                              max: dur.inMilliseconds.toDouble(),
                              onChanged: (val) {
                                controller
                                    .seek(Duration(milliseconds: val.toInt()));
                              },
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  _formatDuration(clampedPos),
                                  style: const TextStyle(
                                    fontFamily: 'monospace',
                                    fontSize: 11,
                                    color: AppColors.textMuted,
                                  ),
                                ),
                                Text(
                                  _formatDuration(dur),
                                  style: const TextStyle(
                                    fontFamily: 'monospace',
                                    fontSize: 11,
                                    color: AppColors.textMuted,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                  const SizedBox(height: 12),

                  // Transport Buttons
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.skip_previous_rounded, size: 36),
                        color: AppColors.textMain,
                        onPressed: () => controller.playPrevious(),
                      ),
                      Container(
                        width: 68,
                        height: 68,
                        decoration: const BoxDecoration(
                          shape: BoxShape.circle,
                          color: AppColors.textMain,
                        ),
                        child: IconButton(
                          icon: controller.isLoading
                              ? const SizedBox(
                                  width: 24,
                                  height: 24,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2.5,
                                    color: Colors.black,
                                  ),
                                )
                              : Icon(
                                  controller.isPlaying
                                      ? Icons.pause_rounded
                                      : Icons.play_arrow_rounded,
                                  size: 38,
                                  color: Colors.black,
                                ),
                          onPressed: () => controller.togglePlayPause(),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.skip_next_rounded, size: 36),
                        color: AppColors.textMain,
                        onPressed: () => controller.playNext(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildQualityPill(BuildContext context) {
    return PopupMenuButton<String>(
      initialValue: controller.quality,
      onSelected: (val) => controller.setQuality(val),
      color: AppColors.surfaceElevated,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.borderStrong),
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: AppColors.cyan.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cyan.withValues(alpha: 0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              controller.quality,
              style: const TextStyle(
                fontFamily: 'monospace',
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: AppColors.cyan,
              ),
            ),
            const SizedBox(width: 4),
            const Icon(Icons.arrow_drop_down, size: 16, color: AppColors.cyan),
          ],
        ),
      ),
      itemBuilder: (context) => [
        const PopupMenuItem(
          value: 'BASIC',
          child: Text('BASIC · 128 kbps'),
        ),
        const PopupMenuItem(
          value: 'HIGH',
          child: Text('HIGH · 192 kbps'),
        ),
        const PopupMenuItem(
          value: 'ULTRA',
          child: Text('ULTRA · 320 kbps'),
        ),
      ],
    );
  }

  Widget _defaultCover() {
    return Container(
      color: const Color(0xFF1E293B),
      child: const Center(
        child: Icon(Icons.music_note_rounded,
            size: 80, color: AppColors.textMuted),
      ),
    );
  }
}
