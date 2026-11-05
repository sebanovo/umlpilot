import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/errors/failures.dart';
import 'package:umlpilot_mobile/features/auth/domain/entities/user.dart';
import 'package:umlpilot_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:umlpilot_mobile/features/auth/domain/usecases/auth_usecases.dart';
import 'package:umlpilot_mobile/features/auth/presentation/viewmodels/auth_viewmodel.dart';

class _FakeAuthRepository implements AuthRepository {
  _FakeAuthRepository({this.fail = false});

  final bool fail;
  User? restored;

  @override
  Future<User> login({required String email, required String password}) async {
    if (fail) throw const AuthFailure('Credenciales inválidas');
    return User(id: 'u1', email: email, firstName: 'Ada', lastName: 'Lovelace', role: 'USER');
  }

  @override
  Future<User> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    if (fail) throw const AuthFailure('Email ya registrado');
    return User(id: 'u1', email: email, firstName: firstName, lastName: lastName, role: 'USER');
  }

  @override
  Future<User?> restoreSession() async => restored;

  @override
  Future<void> logout() async {}
}

AuthViewModel _viewModel(AuthRepository repository) {
  return AuthViewModel(
    loginUseCase: LoginUseCase(repository),
    registerUseCase: RegisterUseCase(repository),
    restoreSessionUseCase: RestoreSessionUseCase(repository),
    logoutUseCase: LogoutUseCase(repository),
  );
}

void main() {
  test('login exitoso autentica al usuario', () async {
    final viewModel = _viewModel(_FakeAuthRepository());

    final ok = await viewModel.login(email: 'ada@x.com', password: '12345678');

    expect(ok, isTrue);
    expect(viewModel.isAuthenticated, isTrue);
    expect(viewModel.user?.email, 'ada@x.com');
    expect(viewModel.error, isNull);
  });

  test('login fallido expone el error', () async {
    final viewModel = _viewModel(_FakeAuthRepository(fail: true));

    final ok = await viewModel.login(email: 'ada@x.com', password: '12345678');

    expect(ok, isFalse);
    expect(viewModel.isAuthenticated, isFalse);
    expect(viewModel.error, 'Credenciales inválidas');
  });

  test('restaura la sesión', () async {
    final repository = _FakeAuthRepository();
    repository.restored = const User(
      id: 'u1',
      email: 'ada@x.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
      role: 'USER',
    );
    final viewModel = _viewModel(repository);

    final user = await viewModel.restore();

    expect(user?.id, 'u1');
    expect(viewModel.isAuthenticated, isTrue);
  });
}
