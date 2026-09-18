import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Configuración de servicios externos leída desde el archivo `.env`.
class ApiConstants {
  ApiConstants._();

  static const String defaultOllamaUrl = 'http://localhost:11434';
  static const String defaultOllamaModel = 'llama3.2';

  /// URL base del servidor Ollama (sin barra final).
  static String get ollamaBaseUrl =>
      _read('OLLAMA_URL', defaultOllamaUrl).replaceAll(RegExp(r'/+$'), '');

  /// Modelo de Ollama usado por el asistente.
  static String get ollamaModel => _read('OLLAMA_MODEL', defaultOllamaModel);

  static String _read(String key, String fallback) {
    if (!dotenv.isInitialized) return fallback;
    final value = dotenv.env[key]?.trim();
    if (value == null || value.isEmpty) return fallback;
    return value;
  }
}
