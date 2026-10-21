import 'package:flutter/material.dart';

class AppColors {
  static const Color background = Color(0xFF08080A);
  static const Color surface = Color(0xFF12131A);
  static const Color surfaceElevated = Color(0xFF181924);
  static const Color surfaceActive = Color(0x1FFFFFFF);

  static const Color borderSubtle = Color(0x14FFFFFF);
  static const Color borderStrong = Color(0x26FFFFFF);

  static const Color textMain = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color textMuted = Color(0xFF64748B);

  static const Color cyan = Color(0xFF38BDF8);
  static const Color mint = Color(0xFF10B981);
  static const Color telegram = Color(0xFF229ED9);
  static const Color rose = Color(0xFFFB7185);
}

class AppTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.background,
      colorScheme: const ColorScheme.dark(
        surface: AppColors.surface,
        primary: AppColors.textMain,
        secondary: AppColors.cyan,
        tertiary: AppColors.mint,
      ),
      fontFamily: 'Inter',
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: AppColors.textMain,
          fontSize: 18,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.02,
        ),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.borderSubtle),
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.surface,
        modalBackgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
      ),
      sliderTheme: const SliderThemeData(
        activeTrackColor: AppColors.cyan,
        inactiveTrackColor: AppColors.borderStrong,
        thumbColor: Colors.white,
        thumbShape: RoundSliderThumbShape(enabledThumbRadius: 6),
        overlayShape: RoundSliderOverlayShape(overlayRadius: 12),
        trackHeight: 3.5,
      ),
    );
  }
}
