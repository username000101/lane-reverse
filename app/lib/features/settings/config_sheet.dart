import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core/theme/app_theme.dart';
import '../auth/auth_controller.dart';

class ConfigSheet extends StatelessWidget {
  final AuthController authController;

  const ConfigSheet({super.key, required this.authController});

  @override
  Widget build(BuildContext context) {
    final user = authController.currentUser;
    final devId = authController.deviceId;

    final lanedConfig = {
      'deviceId': devId,
      'inode': '668035',
      'token': '*** (JWT Bearer Token)',
      'user': user?.toJson(),
    };
    final prettyJson = const JsonEncoder.withIndent('  ').convert(lanedConfig);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Row(
                  children: [
                    Icon(Icons.terminal_rounded, size: 20, color: AppColors.cyan),
                    SizedBox(width: 8),
                    Text(
                      'Конфиг сессии (laned.json)',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textMain,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(Icons.close_rounded),
                  color: AppColors.textMuted,
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text(
              'Используйте эти данные в консольных клиентах (crates/laned-cli, crates/laned-tui):',
              style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
            ),
            const SizedBox(height: 16),

            // Code Preview Box
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFF09090B),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Text(
                  prettyJson,
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 12,
                    color: AppColors.cyan,
                    height: 1.5,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Copy Action
            SizedBox(
              width: double.infinity,
              height: 44,
              child: OutlinedButton.icon(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: prettyJson));
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Конфиг скопирован в буфер обмена'),
                      duration: Duration(seconds: 2),
                    ),
                  );
                },
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textMain,
                  side: const BorderSide(color: AppColors.borderStrong),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                icon: const Icon(Icons.copy_rounded, size: 16),
                label: const Text('Скопировать laned.json'),
              ),
            ),
            const SizedBox(height: 16),

            // Logout Action
            SizedBox(
              width: double.infinity,
              height: 44,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  authController.logout();
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.rose.withValues(alpha: 0.12),
                  foregroundColor: AppColors.rose,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                    side: BorderSide(color: AppColors.rose.withValues(alpha: 0.25)),
                  ),
                ),
                icon: const Icon(Icons.logout_rounded, size: 16),
                label: const Text('Выйти из аккаунта'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
