import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Configuración leída desde el archivo `.env`.
class ApiConstants {
  ApiConstants._();

  static const String fallbackBaseUrl = 'http://localhost:8080';
  static const String fallbackAppName = 'UML Pilot';
  static const String fallbackWebBaseUrl = 'http://localhost:5173';

  static String get baseUrl => _read('API_BASE_URL', fallbackBaseUrl).replaceAll(RegExp(r'/+$'), '');

  static String get appName => _read('APP_NAME', fallbackAppName);

  /// Origen de la web, usado para construir enlaces compartidos.
  static String get webBaseUrl =>
      _read('WEB_BASE_URL', fallbackWebBaseUrl).replaceAll(RegExp(r'/+$'), '');

  /// URL STOMP derivada del backend (`http` → `ws`, `https` → `wss`).
  static String get wsUrl {
    final base = baseUrl;
    final wsBase = base.startsWith('https://')
        ? 'wss://${base.substring('https://'.length)}'
        : base.startsWith('http://')
            ? 'ws://${base.substring('http://'.length)}'
            : 'ws://$base';
    return '$wsBase/ws';
  }

  static String _read(String key, String fallback) {
    if (!dotenv.isInitialized) return fallback;
    final value = dotenv.env[key]?.trim();
    if (value == null || value.isEmpty) return fallback;
    return value;
  }
}
