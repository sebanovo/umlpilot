import '../entities/user.dart';

abstract interface class AuthRepository {
  Future<User> login({required String email, required String password});

  Future<User> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  });

  Future<User?> restoreSession();

  Future<void> logout();
}
