import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class StoredSession {
  const StoredSession({
    required this.accessToken,
    required this.refreshToken,
    required this.userJson,
  });

  final String accessToken;
  final String refreshToken;
  final String userJson;
}

abstract interface class TokenStorage {
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userJson,
  });

  Future<void> saveTokens({required String accessToken, required String refreshToken});

  Future<StoredSession?> read();

  Future<void> clear();
}

class SecureTokenStorage implements TokenStorage {
  SecureTokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  static const _accessKey = 'umlpilot_access_token';
  static const _refreshKey = 'umlpilot_refresh_token';
  static const _userKey = 'umlpilot_user_json';

  final FlutterSecureStorage _storage;

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userJson,
  }) async {
    await _storage.write(key: _accessKey, value: accessToken);
    await _storage.write(key: _refreshKey, value: refreshToken);
    await _storage.write(key: _userKey, value: userJson);
  }

  @override
  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    await _storage.write(key: _accessKey, value: accessToken);
    await _storage.write(key: _refreshKey, value: refreshToken);
  }

  @override
  Future<StoredSession?> read() async {
    final access = await _storage.read(key: _accessKey);
    final refresh = await _storage.read(key: _refreshKey);
    final user = await _storage.read(key: _userKey);
    if (access == null || refresh == null || user == null) return null;
    return StoredSession(accessToken: access, refreshToken: refresh, userJson: user);
  }

  @override
  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
    await _storage.delete(key: _userKey);
  }
}

/// Implementación en memoria para pruebas.
class MemoryTokenStorage implements TokenStorage {
  StoredSession? _session;

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userJson,
  }) async {
    _session = StoredSession(
      accessToken: accessToken,
      refreshToken: refreshToken,
      userJson: userJson,
    );
  }

  @override
  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    final current = _session;
    if (current == null) return;
    _session = StoredSession(
      accessToken: accessToken,
      refreshToken: refreshToken,
      userJson: current.userJson,
    );
  }

  @override
  Future<StoredSession?> read() async => _session;

  @override
  Future<void> clear() async => _session = null;
}
