import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../storage/token_storage.dart';

/// Estado de sesión compartido por la app, el cliente HTTP y el router.
class SessionController extends ChangeNotifier {
  SessionController(this._storage);

  final TokenStorage _storage;

  Map<String, dynamic>? _user;
  String? _accessToken;
  String? _refreshToken;
  bool _authenticated = false;
  bool _restored = false;

  Map<String, dynamic>? get user => _user;
  String? get accessToken => _accessToken;
  String? get refreshToken => _refreshToken;
  bool get isAuthenticated => _authenticated;
  bool get isRestored => _restored;

  Future<void> restore() async {
    final stored = await _storage.read();
    if (stored != null) {
      try {
        _user = (jsonDecode(stored.userJson) as Map).cast<String, dynamic>();
        _accessToken = stored.accessToken;
        _refreshToken = stored.refreshToken;
        _authenticated = true;
      } catch (_) {
        await _storage.clear();
        _authenticated = false;
        _user = null;
      }
    }
    _restored = true;
    notifyListeners();
  }

  Future<void> start({
    required Map<String, dynamic> user,
    required String accessToken,
    required String refreshToken,
  }) async {
    _user = user;
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _authenticated = true;
    _restored = true;
    await _storage.save(
      accessToken: accessToken,
      refreshToken: refreshToken,
      userJson: jsonEncode(user),
    );
    notifyListeners();
  }

  Future<void> updateTokens({required String accessToken, required String refreshToken}) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    await _storage.saveTokens(accessToken: accessToken, refreshToken: refreshToken);
  }

  Future<void> logout() async {
    _user = null;
    _accessToken = null;
    _refreshToken = null;
    _authenticated = false;
    _restored = true;
    await _storage.clear();
    notifyListeners();
  }

  /// La llama el cliente HTTP cuando el refresh falla.
  Future<void> expire() => logout();
}
