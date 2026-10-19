import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color primaryOrange = Color(0xFFEA580C);
  static const Color primaryAmber = Color(0xFFF59E0B);
  static const Color darkBg = Color(0xFF1C1917);
  static const Color surfaceDark = Color(0xFF292524);
  static const Color surfaceCard = Color(0xFFFFFFFF);
  static const Color stoneBg = Color(0xFFF5F5F4);

  // Status colors
  static const Color statusPaid = Color(0xFFEA580C);       // Menunggu dimasak (Orange)
  static const Color statusPreparing = Color(0xFFD97706);  // Sedang dimasak (Amber)
  static const Color statusReady = Color(0xFF16A34A);      // Siap saji (Green)
  static const Color statusCompleted = Color(0xFF6B7280);  // Selesai (Gray)

  static ThemeData lightTheme = ThemeData(
    useMaterial3: true,
    fontFamily: GoogleFonts.plusJakartaSans().fontFamily,
    colorScheme: ColorScheme.fromSeed(
      seedColor: primaryOrange,
      primary: primaryOrange,
      secondary: primaryAmber,
      surface: surfaceCard,
    ),
    scaffoldBackgroundColor: stoneBg,
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.white,
      foregroundColor: Color(0xFF1C1917),
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w700,
        color: Color(0xFF1C1917),
      ),
    ),
    cardTheme: CardThemeData(
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      color: Colors.white,
    ),
  );
}
