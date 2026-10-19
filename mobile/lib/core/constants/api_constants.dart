import 'dart:io';
import 'package:flutter/foundation.dart';

class ApiConstants {
  /// Base URL endpoint for backend REST API
  /// - Android Emulator uses 10.0.2.2 to access host machine localhost
  /// - iOS Simulator / Web / Desktop uses localhost
  static String get baseUrl {
    if (kIsWeb) {
      return 'http://localhost:8080/api/v1';
    }
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8080/api/v1';
    }
    return 'http://localhost:8080/api/v1';
  }

  static const int defaultOutletId = 1;
}
