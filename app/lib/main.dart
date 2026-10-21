import 'package:flutter/material.dart';
import 'core/network/lane_api_client.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/auth_controller.dart';
import 'features/auth/auth_screen.dart';
import 'features/library/library_screen.dart';
import 'features/player/player_controller.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final apiClient = LaneApiClient();
  final authController = AuthController(apiClient: apiClient);
  final playerController = PlayerController(apiClient: apiClient);

  runApp(LaneApp(
    apiClient: apiClient,
    authController: authController,
    playerController: playerController,
  ));
}

class LaneApp extends StatelessWidget {
  final LaneApiClient apiClient;
  final AuthController authController;
  final PlayerController playerController;

  const LaneApp({
    Key? key,
    required this.apiClient,
    required this.authController,
    required this.playerController,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Lane Music',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: ListenableBuilder(
        listenable: authController,
        builder: (context, _) {
          if (authController.status == AuthStatus.loading) {
            return const Scaffold(
              backgroundColor: AppColors.background,
              body: Center(
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  color: AppColors.cyan,
                ),
              ),
            );
          }

          if (authController.isAuthenticated) {
            return LibraryScreen(
              authController: authController,
              playerController: playerController,
              apiClient: apiClient,
            );
          }

          return AuthScreen(controller: authController);
        },
      ),
    );
  }
}
