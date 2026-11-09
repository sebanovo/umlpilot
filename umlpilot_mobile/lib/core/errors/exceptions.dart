/// Excepciones lanzadas por la capa de datos.
class AppException implements Exception {
  const AppException(this.message);

  final String message;

  @override
  String toString() => message;
}

class ServerException extends AppException {
  const ServerException(super.message, {this.statusCode});

  final int? statusCode;
}

class NetworkException extends AppException {
  const NetworkException([super.message = 'No se pudo conectar con el servidor']);
}

class UnauthorizedException extends AppException {
  const UnauthorizedException([super.message = 'Tu sesión expiró. Inicia sesión de nuevo.']);
}

class ForbiddenException extends AppException {
  const ForbiddenException([super.message = 'No tienes permisos para realizar esta acción']);
}

class ValidationException extends AppException {
  const ValidationException(super.message);
}
