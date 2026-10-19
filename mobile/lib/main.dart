import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/theme/app_theme.dart';
import 'features/kitchen/screens/kitchen_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    const ProviderScope(
      child: QrOrderKitchenApp(),
    ),
  );
}

class QrOrderKitchenApp extends StatelessWidget { 
  const QrOrderKitchenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'QR Order - Kitchen Display',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      home: const KitchenScreen(),
    );
  }
}
