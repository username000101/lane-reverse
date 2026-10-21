import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../../core/theme/app_theme.dart';
import 'auth_controller.dart';

class AuthScreen extends StatefulWidget {
  final AuthController controller;

  const AuthScreen({super.key, required this.controller});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1600),
    )..repeat(reverse: true);
    _pulseAnimation =
        Tween<double>(begin: 0.85, end: 1.25).animate(CurvedAnimation(
      parent: _pulseController,
      curve: Curves.easeInOut,
    ));
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.controller,
      builder: (context, _) {
        final ctrl = widget.controller;

        return Scaffold(
          backgroundColor: AppColors.background,
          body: SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Brand Indicator
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _buildSoundwaveBar(8),
                        const SizedBox(width: 3),
                        _buildSoundwaveBar(16),
                        const SizedBox(width: 3),
                        _buildSoundwaveBar(11),
                        const SizedBox(width: 10),
                        const Text(
                          'LANE',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w800,
                            letterSpacing: -0.02,
                            color: AppColors.textMain,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppColors.cyan.withValues(alpha: 0.12),
                            border: Border.all(
                                color: AppColors.cyan.withValues(alpha: 0.3)),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: const Text(
                            'NATIVE',
                            style: TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                              color: AppColors.cyan,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 28),

                    // Main Monolith Card
                    Container(
                      width: double.infinity,
                      constraints: const BoxConstraints(maxWidth: 420),
                      padding: const EdgeInsets.all(28),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: BorderRadius.circular(24),
                        border: Border.all(color: AppColors.borderSubtle),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.6),
                            blurRadius: 40,
                            offset: const Offset(0, 16),
                          ),
                        ],
                      ),
                      child: ctrl.status == AuthStatus.polling
                          ? _buildActivePollingView(ctrl)
                          : _buildInitialView(ctrl),
                    ),

                    const SizedBox(height: 24),
                    // Security note
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.shield_outlined,
                            size: 14, color: AppColors.textMuted),
                        SizedBox(width: 6),
                        Text(
                          'Подпись BNIT v1.4.7 • Без логина и пароля',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.textMuted,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildInitialView(AuthController ctrl) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Вход в Lane Music',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w700,
            letterSpacing: -0.02,
            color: AppColors.textMain,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Авторизуйтесь через официального Telegram-бота @lane_music_bot без ввода учётных данных.',
          style: TextStyle(
            fontSize: 13,
            color: AppColors.textSecondary,
            height: 1.5,
          ),
        ),
        const SizedBox(height: 28),

        SizedBox(
          width: double.infinity,
          height: 50,
          child: ElevatedButton(
            onPressed: () => ctrl.startAuthFlow(),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.textMain,
              foregroundColor: Colors.black,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              elevation: 0,
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.send_rounded, size: 18),
                SizedBox(width: 10),
                Text(
                  'Войти через Telegram',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),

        _buildFeatureRow(
          Icons.vpn_key_outlined,
          'CSPRNG Device ID',
          '32-символьный ключ сессии создаётся локально на устройстве.',
        ),
        const SizedBox(height: 12),
        _buildFeatureRow(
          Icons.lock_outline_rounded,
          'Защищённый JWT',
          'Токен сохраняется в локальное зашифрованное хранилище.',
        ),
      ],
    );
  }

  Widget _buildActivePollingView(AuthController ctrl) {
    final minutes = ctrl.secondsLeft ~/ 60;
    final seconds = ctrl.secondsLeft % 60;
    final timeStr =
        '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';

    return Column(
      children: [
        // QR Code Container
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.3),
                blurRadius: 20,
              ),
            ],
          ),
          child: QrImageView(
            data: ctrl.botUrl,
            version: QrVersions.auto,
            size: 190,
            backgroundColor: Colors.white,
            eyeStyle: const QrEyeStyle(
              eyeShape: QrEyeShape.square,
              color: Colors.black,
            ),
            dataModuleStyle: const QrDataModuleStyle(
              dataModuleShape: QrDataModuleShape.square,
              color: Colors.black,
            ),
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Отсканируйте камерой или перейдите в бот:',
          style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
        ),
        const SizedBox(height: 16),

        // Direct Telegram Button
        SizedBox(
          width: double.infinity,
          height: 48,
          child: ElevatedButton.icon(
            onPressed: () => ctrl.openTelegramBot(),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.telegram,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            icon: const Icon(Icons.send_rounded, size: 18),
            label: const Text(
              'Открыть @lane_music_bot',
              style: TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ),
        const SizedBox(height: 12),

        // Copy Link Action
        OutlinedButton.icon(
          onPressed: () {
            Clipboard.setData(ClipboardData(text: ctrl.botUrl));
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Ссылка скопирована в буфер обмена'),
                duration: Duration(seconds: 2),
              ),
            );
          },
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.textSecondary,
            side: const BorderSide(color: AppColors.borderStrong),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          icon: const Icon(Icons.copy_rounded, size: 16),
          label: const Text('Скопировать ссылку'),
        ),
        const SizedBox(height: 20),

        // Live Radar Pulse Widget
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: AppColors.mint.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.mint.withValues(alpha: 0.2)),
          ),
          child: Row(
            children: [
              AnimatedBuilder(
                animation: _pulseAnimation,
                builder: (context, child) {
                  return Container(
                    width: 10 * _pulseAnimation.value,
                    height: 10 * _pulseAnimation.value,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: AppColors.mint,
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.mint.withValues(alpha: 0.6),
                          blurRadius: 8,
                        ),
                      ],
                    ),
                  );
                },
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Ожидаем подтверждения в боте...',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: AppColors.mint,
                      ),
                    ),
                    Text(
                      'Попытка ${ctrl.attempts} • Истекает через: $timeStr',
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 11,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.refresh_rounded, size: 18),
                color: AppColors.textSecondary,
                onPressed: () => ctrl.startAuthFlow(),
                tooltip: 'Обновить сессию',
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildFeatureRow(IconData icon, String title, String desc) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: AppColors.cyan),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textMain,
                ),
              ),
              Text(
                desc,
                style: const TextStyle(
                  fontSize: 12,
                  color: AppColors.textMuted,
                  height: 1.35,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSoundwaveBar(double height) {
    return Container(
      width: 2.5,
      height: height,
      decoration: BoxDecoration(
        color: AppColors.cyan,
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }
}
