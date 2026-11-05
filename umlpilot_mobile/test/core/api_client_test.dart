import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/errors/exceptions.dart';
import 'package:umlpilot_mobile/core/network/api_client.dart';
import 'package:umlpilot_mobile/core/session/session_controller.dart';
import 'package:umlpilot_mobile/core/storage/token_storage.dart';

class _MockAdapter implements HttpClientAdapter {
  _MockAdapter(this.handler);

  final Future<ResponseBody> Function(RequestOptions options) handler;
  final List<RequestOptions> requests = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    requests.add(options);
    return handler(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody _json(Object data, int status) {
  return ResponseBody.fromString(
    jsonEncode(data),
    status,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

Future<SessionController> _session() async {
  final session = SessionController(MemoryTokenStorage());
  await session.start(
    user: {'id': 'u1'},
    accessToken: 'access-old',
    refreshToken: 'refresh-old',
  );
  return session;
}

ApiClient _client(_MockAdapter adapter, SessionController session) {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
  dio.httpClientAdapter = adapter;
  return ApiClient(baseUrl: 'http://localhost:8080', session: session, dio: dio);
}

void main() {
  test('agrega el Bearer token a las peticiones', () async {
    final session = await _session();
    final adapter = _MockAdapter((_) async => _json({'ok': true}, 200));
    final client = _client(adapter, session);

    await client.get('/api/v1/projects');

    expect(adapter.requests.single.headers['Authorization'], 'Bearer access-old');
  });

  test('traduce el mensaje de error del backend', () async {
    final session = await _session();
    final adapter = _MockAdapter((_) async => _json({'error': 'Nombre inválido'}, 400));
    final client = _client(adapter, session);

    await expectLater(
      client.post('/api/v1/projects', data: {'name': ''}),
      throwsA(isA<ServerException>().having((error) => error.message, 'message', 'Nombre inválido')),
    );
  });

  test('ante 401 refresca una vez y reintenta con el nuevo token', () async {
    final session = await _session();
    var dataCalls = 0;
    final adapter = _MockAdapter((options) async {
      if (options.path == '/api/v1/auth/refresh') {
        return _json({'accessToken': 'access-new', 'refreshToken': 'refresh-new'}, 200);
      }
      dataCalls++;
      if (dataCalls == 1) return _json({'error': 'Sesión expirada'}, 401);
      return _json({'ok': true}, 200);
    });
    final client = _client(adapter, session);

    final result = await client.get('/api/v1/projects');

    expect(result, {'ok': true});
    expect(dataCalls, 2);
    expect(session.accessToken, 'access-new');
    expect(session.refreshToken, 'refresh-new');
    final refreshRequests = adapter.requests.where((request) => request.path == '/api/v1/auth/refresh');
    expect(refreshRequests, hasLength(1));
    expect(adapter.requests.last.headers['Authorization'], 'Bearer access-new');
  });

  test('si el refresh falla cierra la sesión y lanza UnauthorizedException', () async {
    final session = await _session();
    final adapter = _MockAdapter((options) async {
      if (options.path == '/api/v1/auth/refresh') return _json({'error': 'refresh inválido'}, 401);
      return _json({'error': 'Sesión expirada'}, 401);
    });
    final client = _client(adapter, session);

    await expectLater(client.get('/api/v1/projects'), throwsA(isA<UnauthorizedException>()));
    expect(session.isAuthenticated, isFalse);
  });
}
