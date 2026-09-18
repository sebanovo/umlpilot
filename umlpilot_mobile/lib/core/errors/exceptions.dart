/// Excepciones lanzadas por la capa de datos.
class ServerException implements Exception {
  const ServerException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

/// Errores de conectividad (sin respuesta del servidor).
class NetworkException implements Exception {
  const NetworkException([this.message = 'No se pudo conectar con el servidor']);

  final String message;

  @override
  String toString() => message;
}
