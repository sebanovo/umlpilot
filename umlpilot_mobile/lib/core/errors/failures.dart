import 'exceptions.dart';

/// Fallos que la capa de dominio expone a la presentación.
class Failure implements Exception {
  const Failure(this.message);

  final String message;

  @override
  String toString() => message;
}

class ServerFailure extends Failure {
  const ServerFailure(super.message);
}

class NetworkFailure extends Failure {
  const NetworkFailure([super.message = 'No se pudo conectar con el servidor']);
}

class AuthFailure extends Failure {
  const AuthFailure([super.message = 'Tu sesión expiró. Inicia sesión de nuevo.']);
}

class PermissionFailure extends Failure {
  const PermissionFailure([super.message = 'No tienes permisos para realizar esta acción']);
}

/// Convierte cualquier error en un fallo de dominio.
Failure failureFromError(Object error) {
  if (error is Failure) return error;
  if (error is UnauthorizedException) return AuthFailure(error.message);
  if (error is ForbiddenException) return PermissionFailure(error.message);
  if (error is AppException) return ServerFailure(error.message);
  return const ServerFailure('Ocurrió un error inesperado. Intenta de nuevo.');
}

/// Convierte cualquier error en un mensaje legible para la UI.
String messageForError(Object error) {
  if (error is Failure) return error.message;
  if (error is AppException) return error.message;
  return 'Ocurrió un error inesperado. Intenta de nuevo.';
}
