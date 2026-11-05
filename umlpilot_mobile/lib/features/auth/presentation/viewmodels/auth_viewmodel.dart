import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../domain/entities/user.dart';
import '../../domain/usecases/auth_usecases.dart';

class AuthViewModel extends ChangeNotifier {
  AuthViewModel({
    required LoginUseCase loginUseCase,
    required RegisterUseCase registerUseCase,
    required RestoreSessionUseCase restoreSessionUseCase,
    required LogoutUseCase logoutUseCase,
  })  : _login = loginUseCase,
        _register = registerUseCase,
        _restore = restoreSessionUseCase,
        _logout = logoutUseCase;

  final LoginUseCase _login;
  final RegisterUseCase _register;
  final RestoreSessionUseCase _restore;
  final LogoutUseCase _logout;

  bool _loading = false;
  String? _error;
  User? _user;

  bool get loading => _loading;
  String? get error => _error;
  User? get user => _user;
  bool get isAuthenticated => _user != null;

  Future<User?> restore() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _user = await _restore();
      return _user;
    } catch (e) {
      _error = messageForError(e);
      return null;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<bool> login({required String email, required String password}) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _user = await _login(email: email, password: password);
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<bool> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _user = await _register(
        email: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
      );
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _logout();
    _user = null;
    _error = null;
    notifyListeners();
  }

  void clearError() {
    if (_error == null) return;
    _error = null;
    notifyListeners();
  }
}
