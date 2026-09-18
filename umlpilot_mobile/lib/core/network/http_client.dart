import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../errors/exceptions.dart';

/// Cliente HTTP compartido: centraliza timeout, JSON y traducción de errores.
class AppHttpClient {
  AppHttpClient(this._client, {this.timeout = const Duration(minutes: 3)});

  final http.Client _client;
  final Duration timeout;

  Future<Map<String, dynamic>> postJson(
    String url,
    Map<String, dynamic> body,
  ) async {
    try {
      final response = await _client
          .post(
            Uri.parse(url),
            headers: const {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(timeout);

      final decoded = _decode(response);

      if (response.statusCode >= 200 && response.statusCode < 300) {
        if (decoded is Map<String, dynamic>) return decoded;
        throw const ServerException('Respuesta inesperada del servidor');
      }

      throw ServerException(
        _errorMessage(decoded, response),
        statusCode: response.statusCode,
      );
    } on ServerException {
      rethrow;
    } on TimeoutException {
      throw const NetworkException('La solicitud tardó demasiado');
    } on FormatException {
      throw const ServerException('Respuesta inválida del servidor');
    } on http.ClientException catch (error) {
      throw NetworkException('No se pudo conectar con Ollama: ${error.message}');
    } catch (error) {
      throw NetworkException('Error de red: $error');
    }
  }

  Object? _decode(http.Response response) {
    if (response.bodyBytes.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }

  String _errorMessage(Object? decoded, http.Response response) {
    if (decoded is Map<String, dynamic>) {
      final error = decoded['error'];
      if (error is String && error.isNotEmpty) return error;
      final message = decoded['message'];
      if (message is String && message.isNotEmpty) return message;
    }
    if (response.body.isNotEmpty) return response.body;
    return 'Error ${response.statusCode} del servidor';
  }
}
