import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../constants/api_constants.dart';
import '../session/session_controller.dart';
import 'realtime_change.dart';

/// Cliente STOMP del backend UMLPilot. Solo lectura: presencia y cambios remotos.
class RealtimeService {
  RealtimeService(this._session);

  final SessionController _session;

  final _presence = StreamController<List<String>>.broadcast();
  final _changes = StreamController<RealtimeChange>.broadcast();
  final _connection = StreamController<bool>.broadcast();

  StompClient? _client;
  StompUnsubscribe? _presenceUnsubscribe;
  StompUnsubscribe? _changesUnsubscribe;
  String? _diagramId;
  bool _disposed = false;
  bool _wasConnected = false;

  Stream<List<String>> get presence => _presence.stream;
  Stream<RealtimeChange> get changes => _changes.stream;
  Stream<bool> get connection => _connection.stream;
  bool get isConnected => _client?.connected ?? false;

  void connect() {
    if (_disposed || _client != null) return;
    final token = _session.accessToken;
    final headers = <String, String>{
      if (token != null) 'Authorization': 'Bearer $token',
    };
    final client = StompClient(
      config: StompConfig(
        url: ApiConstants.wsUrl,
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        reconnectDelay: const Duration(seconds: 3),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (_) {
          if (_disposed) return;
          _emitConnection(true);
          final diagramId = _diagramId;
          if (diagramId != null) _join(diagramId);
        },
        onDisconnect: (_) {
          if (_disposed) return;
          _emitConnection(false);
          _cancelSubscriptions();
        },
        onStompError: (_) {
          if (_disposed) return;
          _emitConnection(false);
        },
        onWebSocketError: (_) {
          if (_disposed) return;
          _emitConnection(false);
        },
      ),
    );
    _client = client;
    client.activate();
  }

  void _emitConnection(bool connected) {
    if (_disposed || _wasConnected == connected) return;
    _wasConnected = connected;
    _connection.add(connected);
  }

  void join(String diagramId) {
    if (_disposed) return;
    _diagramId = diagramId;
    connect();
    if (_client?.connected == true) _join(diagramId);
  }

  void _join(String diagramId) {
    if (_disposed) return;
    _client?.send(
      destination: '/app/joinDiagram',
      body: jsonEncode({'diagramId': diagramId}),
    );
    _subscribe(diagramId);
  }

  void _subscribe(String diagramId) {
    if (_disposed) return;
    _cancelSubscriptions();
    final client = _client;
    if (client == null) return;
    _presenceUnsubscribe = client.subscribe(
      destination: '/topic/diagram/$diagramId/presence',
      callback: (frame) {
        if (_disposed) return;
        try {
          final data = jsonDecode(frame.body ?? '{}') as Map;
          final users = (data['users'] as List? ?? const []).map((item) => item.toString()).toList();
          _presence.add(users);
        } catch (_) {
          // Ignorar mensajes mal formados.
        }
      },
    );
    _changesUnsubscribe = client.subscribe(
      destination: '/topic/diagram/$diagramId/changes',
      callback: (frame) {
        if (_disposed) return;
        try {
          final payload = jsonDecode(frame.body ?? '{}') as Map<String, dynamic>;
          final type = (payload['type'] ?? '').toString();
          // Los cursores y selecciones se emiten continuamente y no cambian el diagrama.
          if (type.isEmpty || type == 'user_cursor' || type == 'user_selection') return;
          final data = (payload['data'] as Map?)?.cast<String, dynamic>() ?? const <String, dynamic>{};
          _changes.add(RealtimeChange(type, data));
        } catch (_) {
          // Ignorar mensajes mal formados.
        }
      },
    );
  }

  void leave() {
    final diagramId = _diagramId;
    if (diagramId != null && _client?.connected == true) {
      _client!.send(
        destination: '/app/leaveDiagram',
        body: jsonEncode({'diagramId': diagramId}),
      );
    }
    _cancelSubscriptions();
    _diagramId = null;
  }

  void _cancelSubscriptions() {
    _presenceUnsubscribe?.call();
    _changesUnsubscribe?.call();
    _presenceUnsubscribe = null;
    _changesUnsubscribe = null;
  }

  void dispose() {
    if (_disposed) return;
    _disposed = true;
    leave();
    _client?.deactivate();
    _client = null;
    _presence.close();
    _changes.close();
    _connection.close();
  }
}
