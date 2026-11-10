import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../constants/api_constants.dart';
import '../session/session_controller.dart';

/// Canal STOMP del chat grupal por proyecto.
///
/// Mantiene una única suscripción a `/topic/project/{projectId}/chat` y
/// reemite los eventos como mapas. El historial y el envío de mensajes van por REST.
class ChatRealtimeService {
  ChatRealtimeService(this._session);

  final SessionController _session;

  final _events = StreamController<Map<String, dynamic>>.broadcast();
  final _connection = StreamController<bool>.broadcast();

  StompClient? _client;
  StompUnsubscribe? _chatUnsubscribe;
  String? _projectId;
  bool _disposed = false;
  bool _wasConnected = false;

  Stream<Map<String, dynamic>> get events => _events.stream;
  Stream<bool> get connection => _connection.stream;
  bool get isConnected => _client?.connected ?? false;

  void join(String projectId) {
    if (_disposed) return;
    _projectId = projectId;
    connect();
    if (_client?.connected == true) _subscribe(projectId);
  }

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
          final projectId = _projectId;
          if (projectId != null) {
            _subscribe(projectId);
            _publishPresence(projectId);
          }
        },
        onDisconnect: (_) {
          if (_disposed) return;
          _emitConnection(false);
          _cancelSubscription();
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

  void _subscribe(String projectId) {
    if (_disposed) return;
    _cancelSubscription();
    final client = _client;
    if (client == null) return;
    _chatUnsubscribe = client.subscribe(
      destination: '/topic/project/$projectId/chat',
      callback: (frame) {
        if (_disposed) return;
        try {
          final payload = jsonDecode(frame.body ?? '{}');
          if (payload is Map) {
            _events.add(payload.cast<String, dynamic>());
          }
        } catch (_) {
          // Ignorar eventos mal formados sin romper la conversación.
        }
      },
    );
  }

  void _publishPresence(String projectId) {
    final client = _client;
    if (client == null || !client.connected) return;
    client.send(
      destination: '/app/chat/presence',
      body: jsonEncode({'projectId': projectId}),
    );
  }

  void sendTyping(bool typing) {
    final projectId = _projectId;
    final client = _client;
    if (projectId == null || client == null || !client.connected) return;
    client.send(
      destination: '/app/chat/typing',
      body: jsonEncode({'projectId': projectId, 'typing': typing}),
    );
  }

  void leave() {
    _cancelSubscription();
    _projectId = null;
  }

  void _cancelSubscription() {
    _chatUnsubscribe?.call();
    _chatUnsubscribe = null;
  }

  void dispose() {
    if (_disposed) return;
    _disposed = true;
    leave();
    _client?.deactivate();
    _client = null;
    _events.close();
    _connection.close();
  }
}
