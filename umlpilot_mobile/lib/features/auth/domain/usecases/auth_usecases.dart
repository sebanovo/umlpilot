import '../entities/user.dart';
import '../repositories/auth_repository.dart';

class LoginUseCase {
  const LoginUseCase(this._repository);

  final AuthRepository _repository;

  Future<User> call({required String email, required String password}) =>
      _repository.login(email: email, password: password);
}

class RegisterUseCase {
  const RegisterUseCase(this._repository);

  final AuthRepository _repository;

  Future<User> call({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) =>
      _repository.register(
        email: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
      );
}

class RestoreSessionUseCase {
  const RestoreSessionUseCase(this._repository);

  final AuthRepository _repository;

  Future<User?> call() => _repository.restoreSession();
}

class LogoutUseCase {
  const LogoutUseCase(this._repository);

  final AuthRepository _repository;

  Future<void> call() => _repository.logout();
}
