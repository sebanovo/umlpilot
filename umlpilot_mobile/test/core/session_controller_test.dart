import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/session/session_controller.dart';
import 'package:umlpilot_mobile/core/storage/token_storage.dart';

void main() {
  test('restaura una sesión vacía como no autenticada', () async {
    final session = SessionController(MemoryTokenStorage());
    await session.restore();

    expect(session.isRestored, isTrue);
    expect(session.isAuthenticated, isFalse);
    expect(session.user, isNull);
  });

  test('guarda la sesión y la restaura', () async {
    final storage = MemoryTokenStorage();
    final session = SessionController(storage);

    await session.start(
      user: {'id': 'u1', 'email': 'user@x.com', 'firstName': 'Ada', 'lastName': 'Lovelace'},
      accessToken: 'access',
      refreshToken: 'refresh',
    );

    expect(session.isAuthenticated, isTrue);
    expect(session.accessToken, 'access');

    final restored = SessionController(storage);
    await restored.restore();
    expect(restored.isAuthenticated, isTrue);
    expect(restored.user?['id'], 'u1');
  });

  test('logout limpia la sesión persistida', () async {
    final storage = MemoryTokenStorage();
    final session = SessionController(storage);
    await session.start(
      user: {'id': 'u1'},
      accessToken: 'access',
      refreshToken: 'refresh',
    );

    await session.logout();

    expect(session.isAuthenticated, isFalse);
    expect(await storage.read(), isNull);
  });
}
