import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/realtime/chat_realtime_service.dart';
import '../../../../core/session/session_controller.dart';
import '../../../collaborators/domain/entities/collaborator.dart';
import '../../../collaborators/domain/usecases/collaborators_usecases.dart';
import '../../../diagrams/domain/entities/diagram.dart';
import '../../../diagrams/domain/usecases/diagrams_usecases.dart';
import '../../../projects/domain/entities/project.dart';
import '../../../projects/domain/usecases/projects_usecases.dart';
import '../../domain/entities/chat_models.dart';
import '../../domain/usecases/chat_usecases.dart';
import '../models/chat_mention_option.dart';

class ChatViewModel extends ChangeNotifier {
  ChatViewModel({
    required GetProjectUseCase getProject,
    required GetChatConversationUseCase getConversation,
    required GetChatMessagesUseCase getMessages,
    required SendChatMessageUseCase sendMessage,
    required SendChatImageUseCase sendImage,
    required GetChatMessageImageUseCase getMessageImage,
    required RetryChatMessageUseCase retryMessage,
    required TranscribeChatAudioUseCase transcribeAudio,
    required GetDiagramsUseCase getDiagrams,
    required GetCollaboratorsUseCase getCollaborators,
    required SessionController session,
    required ChatRealtimeService realtime,
  })  : _getProject = getProject,
        _getConversation = getConversation,
        _getMessages = getMessages,
        _sendMessage = sendMessage,
        _sendImage = sendImage,
        _getMessageImage = getMessageImage,
        _retryMessage = retryMessage,
        _transcribeAudio = transcribeAudio,
        _getDiagrams = getDiagrams,
        _getCollaborators = getCollaborators,
        _session = session,
        _realtime = realtime {
    _eventsSubscription = _realtime.events.listen(_handleEvent);
    _connectionSubscription = _realtime.connection.listen(_handleConnection);
  }

  final GetProjectUseCase _getProject;
  final GetChatConversationUseCase _getConversation;
  final GetChatMessagesUseCase _getMessages;
  final SendChatMessageUseCase _sendMessage;
  final SendChatImageUseCase _sendImage;
  final GetChatMessageImageUseCase _getMessageImage;
  final RetryChatMessageUseCase _retryMessage;
  final TranscribeChatAudioUseCase _transcribeAudio;
  final GetDiagramsUseCase _getDiagrams;
  final GetCollaboratorsUseCase _getCollaborators;
  final SessionController _session;
  final ChatRealtimeService _realtime;
  final Random _random = Random();

  String? _projectId;
  Project? _project;
  ChatConversation? _conversation;
  List<ChatMessage> _messages = const [];
  List<Diagram> _diagrams = const [];
  List<Collaborator> _collaborators = const [];
  List<String> _presenceIds = const [];
  String? _selectedDiagramId;
  String? _nextCursor;
  bool _hasMore = false;
  bool _loading = true;
  bool _loadingMore = false;
  bool _sending = false;
  bool _connected = false;
  bool _loaded = false;
  bool _disposed = false;
  String? _error;
  final Set<String> _loadingImages = {};

  final Map<String, String> _typingUsers = {};
  final Map<String, Timer> _typingTimers = {};
  StreamSubscription<Map<String, dynamic>>? _eventsSubscription;
  StreamSubscription<bool>? _connectionSubscription;
  Timer? _refreshTimer;

  Project? get project => _project;
  ChatConversation? get conversation => _conversation;
  List<ChatMessage> get messages => _messages;
  List<Diagram> get diagrams => _diagrams;
  List<Collaborator> get collaborators => _collaborators;
  String? get selectedDiagramId => _selectedDiagramId;
  Diagram? get selectedDiagram {
    for (final diagram in _diagrams) {
      if (diagram.id == _selectedDiagramId) return diagram;
    }
    return null;
  }

  bool get loading => _loading;
  bool get loadingMore => _loadingMore;
  bool get sending => _sending;
  bool get connected => _connected;
  bool get hasMore => _hasMore;
  String? get error => _error;

  String get currentUserId => (_session.user?['id'] ?? '').toString();
  String get currentUserName {
    final user = _session.user;
    if (user == null) return 'Usuario';
    final name = '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'.trim();
    return name.isEmpty ? (user['email'] ?? 'Usuario').toString() : name;
  }

  Collaborator? get currentCollaborator {
    for (final collaborator in _collaborators) {
      if (collaborator.userId == currentUserId) return collaborator;
    }
    return null;
  }

  String? get currentRole => currentCollaborator?.role;
  bool get canMutate => _conversation?.canMutate ?? (currentRole == 'owner' || currentRole == 'editor');
  bool get isViewer => currentRole == 'viewer';

  List<ChatMentionOption> get mentionOptions {
    return [
      const ChatMentionOption(label: 'Asistente IA', role: 'IA', isAssistant: true),
      ..._collaborators.map(
        (collaborator) => ChatMentionOption(
          label: collaborator.fullName,
          role: collaborator.role,
          userId: collaborator.userId,
        ),
      ),
    ];
  }

  List<String> get mentionNames => mentionOptions.map((option) => option.label).toList();

  List<String> get typingNames {
    return _typingUsers.entries
        .where((entry) => entry.key != currentUserId)
        .map((entry) => entry.value)
        .toSet()
        .toList();
  }

  List<String> get presenceIds => List.unmodifiable(_presenceIds);

  List<String> get presenceNames {
    return _presenceIds
        .map((id) {
          for (final collaborator in _collaborators) {
            if (collaborator.userId == id) return collaborator.fullName;
          }
          return 'Colaborador';
        })
        .toSet()
        .toList();
  }

  String? get projectName => _project?.name;

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  Future<void> load(String projectId) async {
    _projectId = projectId;
    _loading = true;
    _error = null;
    _notify();
    try {
      final results = await Future.wait<Object>([
        _getProject(projectId),
        _getConversation(projectId),
        _getMessages(projectId),
        _getDiagrams(projectId),
        _getCollaborators(projectId),
      ]);
      _project = results[0] as Project;
      _conversation = results[1] as ChatConversation;
      _applyInitialPage(results[2] as ChatPage);
      _diagrams = results[3] as List<Diagram>;
      _collaborators = results[4] as List<Collaborator>;
      _selectedDiagramId = _resolveInitialDiagram();
      _loaded = true;
      _realtime.join(projectId);
      unawaited(_hydrateMessageImages(projectId));
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      _notify();
    }
  }

  void _applyInitialPage(ChatPage page) {
    _messages = _sorted(page.messages);
    _nextCursor = page.nextCursor;
    _hasMore = page.hasMore;
  }

  String? _resolveInitialDiagram() {
    final focus = _conversation?.focusDiagramId;
    if (focus != null && _diagrams.any((diagram) => diagram.id == focus)) return focus;
    return _diagrams.isEmpty ? null : _diagrams.first.id;
  }

  void setSelectedDiagram(String? diagramId) {
    if (_selectedDiagramId == diagramId) return;
    _selectedDiagramId = diagramId;
    _notify();
  }

  Future<void> loadOlder() async {
    final projectId = _projectId;
    final cursor = _nextCursor;
    if (projectId == null || cursor == null || !_hasMore || _loadingMore) return;
    _loadingMore = true;
    _notify();
    try {
      final page = await _getMessages(projectId, before: cursor);
      _messages = _sorted([...page.messages, ..._messages]);
      _nextCursor = page.nextCursor;
      _hasMore = page.hasMore;
      unawaited(_hydrateMessageImages(projectId));
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loadingMore = false;
      _notify();
    }
  }

  Future<void> sendMessage(String content) async {
    final projectId = _projectId;
    final trimmed = content.trim();
    if (projectId == null || trimmed.isEmpty || _sending) return;

    final clientMessageId = _newClientMessageId();
    final optimistic = ChatMessage(
      id: 'pending-$clientMessageId',
      conversationId: _conversation?.conversationId ?? '',
      projectId: projectId,
      diagramId: _selectedDiagramId,
      authorType: 'user',
      authorId: currentUserId,
      authorName: currentUserName,
      authorRole: currentRole ?? 'viewer',
      content: trimmed,
      clientMessageId: clientMessageId,
      aiStatus: mentionsAssistant(trimmed) ? ChatAiStatus.generating : ChatAiStatus.none,
      createdAt: DateTime.now().toUtc().toIso8601String(),
      pending: true,
    );

    _messages = _sorted([..._messages, optimistic]);
    _sending = true;
    _error = null;
    _notify();
    try {
      final result = await _sendMessage(
        projectId,
        clientMessageId: clientMessageId,
        content: trimmed,
        diagramId: _selectedDiagramId,
      );
      _mergeMessage(result.message);
    } catch (e) {
      _messages = _messages
          .map((message) => message.clientMessageId == clientMessageId
              ? message.copyWith(pending: false, failed: true, aiStatus: ChatAiStatus.failed)
              : message)
          .toList();
      _error = messageForError(e);
    } finally {
      _sending = false;
      _notify();
    }
  }

  Future<void> sendImage({
    required Uint8List image,
    required String filename,
    String? instructions,
  }) async {
    final projectId = _projectId;
    final diagramId = _selectedDiagramId;
    if (projectId == null || image.isEmpty || _sending) return;
    if (!canMutate) {
      _error = 'Solo owner/editor pueden construir desde una imagen';
      _notify();
      return;
    }
    if (diagramId == null) {
      _error = 'Selecciona un diagrama de contexto para construir desde la imagen';
      _notify();
      return;
    }

    final clientMessageId = _newClientMessageId();
    final prompt = (instructions ?? '').trim().isEmpty
        ? 'Construye el diagrama a partir de la imagen'
        : instructions!.trim();
    final optimistic = ChatMessage(
      id: 'pending-$clientMessageId',
      conversationId: _conversation?.conversationId ?? '',
      projectId: projectId,
      diagramId: diagramId,
      authorType: 'user',
      authorId: currentUserId,
      authorName: currentUserName,
      authorRole: currentRole ?? 'viewer',
      content: '[Imagen: $filename] $prompt',
      clientMessageId: clientMessageId,
      aiStatus: ChatAiStatus.generating,
      createdAt: DateTime.now().toUtc().toIso8601String(),
      previewBytes: image,
      pending: true,
    );

    _messages = _sorted([..._messages, optimistic]);
    _sending = true;
    _error = null;
    _notify();
    try {
      final result = await _sendImage(
        projectId,
        clientMessageId: clientMessageId,
        image: image,
        filename: filename,
        diagramId: diagramId,
        instructions: prompt,
      );
      _mergeMessage(result.message);
    } catch (e) {
      _messages = _messages
          .map((message) => message.clientMessageId == clientMessageId
              ? message.copyWith(pending: false, failed: true, aiStatus: ChatAiStatus.failed)
              : message)
          .toList();
      _error = messageForError(e);
    } finally {
      _sending = false;
      _notify();
    }
  }

  Future<void> retryMessage(String messageId) async {
    final projectId = _projectId;
    if (projectId == null) return;

    // Si el envío REST falló antes de persistirse, se reintenta el envío completo.
    ChatMessage? pending;
    for (final message in _messages) {
      if (message.id == messageId && message.id.startsWith('pending-')) {
        pending = message;
        break;
      }
    }
    if (pending != null) {
      if (pending.previewBytes != null) {
        _error = 'Vuelve a adjuntar la imagen para reintentar';
        _notify();
        return;
      }
      _messages = _messages.where((message) => message.id != messageId).toList();
      _notify();
      await sendMessage(pending.content);
      return;
    }

    _messages = _messages
        .map((message) => message.id == messageId
            ? message.copyWith(failed: false, aiStatus: ChatAiStatus.generating)
            : message)
        .toList();
    _notify();
    try {
      final result = await _retryMessage(projectId, messageId);
      _mergeMessage(result.message);
    } catch (e) {
      _messages = _messages
          .map((message) => message.id == messageId
              ? message.copyWith(failed: true, aiStatus: ChatAiStatus.failed)
              : message)
          .toList();
      _error = messageForError(e);
      _notify();
    }
  }

  void sendTyping(bool typing) {
    _realtime.sendTyping(typing);
  }

  Future<String> transcribeAudio(Uint8List audio) async {
    final projectId = _projectId;
    if (projectId == null) throw StateError('Proyecto no disponible');
    return _transcribeAudio(projectId, audio);
  }

  void _handleEvent(Map<String, dynamic> event) {
    final type = (event['type'] ?? '').toString();
    switch (type) {
      case 'chat_message_created':
        final raw = event['message'];
        if (raw is Map) {
          _mergeMessage(ChatMessage.fromJson(raw.cast<String, dynamic>()));
          final projectId = _projectId;
          if (projectId != null) unawaited(_hydrateMessageImages(projectId));
          _notify();
        }
        break;
      case 'chat_ai_status':
        final messageId = (event['messageId'] ?? '').toString();
        final status = ChatAiStatus.fromValue(event['status'] as String?);
        final statusError = event['error'] as String?;
        _messages = _messages
            .map((message) => message.id == messageId
                ? message.copyWith(
                    aiStatus: status,
                    failed: status == ChatAiStatus.failed,
                    pending: false,
                  )
                : message)
            .toList();
        if (status == ChatAiStatus.failed && statusError != null) _error = statusError;
        _notify();
        break;
      case 'chat_typing':
        final userId = (event['userId'] ?? '').toString();
        if (userId.isEmpty || userId == currentUserId) return;
        final name = (event['name'] ?? 'Colaborador').toString();
        final typing = event['typing'] == true;
        _typingTimers.remove(userId)?.cancel();
        if (typing) {
          _typingUsers[userId] = name;
          _typingTimers[userId] = Timer(const Duration(milliseconds: 2500), () {
            _typingUsers.remove(userId);
            _typingTimers.remove(userId);
            _notify();
          });
        } else {
          _typingUsers.remove(userId);
        }
        _notify();
        break;
      case 'chat_presence':
        final users = event['users'];
        _presenceIds = users is List ? users.map((item) => item.toString()).toList() : const [];
        _notify();
        break;
      default:
        break;
    }
  }

  void _handleConnection(bool connected) {
    _connected = connected;
    _notify();
    if (!connected) return;
    _refreshTimer?.cancel();
    _refreshTimer = Timer(const Duration(milliseconds: 250), () {
      if (_loaded && !_loading) _refreshLatest();
    });
  }

  Future<void> _refreshLatest() async {
    final projectId = _projectId;
    if (projectId == null) return;
    try {
      final results = await Future.wait<Object>([
        _getConversation(projectId),
        _getMessages(projectId),
      ]);
      _conversation = results[0] as ChatConversation;
      final page = results[1] as ChatPage;
      for (final message in page.messages) {
        _mergeMessage(message);
      }
      if (_messages.isEmpty) {
        _nextCursor = page.nextCursor;
        _hasMore = page.hasMore;
      }
      unawaited(_hydrateMessageImages(projectId));
      _notify();
    } catch (_) {
      // La reconexión no debe borrar el historial ya visible.
    }
  }

  void _mergeMessage(ChatMessage incoming) {
    final index = _messages.indexWhere((message) =>
        message.id == incoming.id ||
        (incoming.clientMessageId != null && message.clientMessageId == incoming.clientMessageId));
    final existingPreview = index >= 0 ? _messages[index].previewBytes : null;
    final resolved = incoming.copyWith(
      pending: false,
      failed: false,
      previewBytes: incoming.previewBytes ?? existingPreview,
    );
    if (index >= 0) {
      final next = [..._messages];
      next[index] = resolved;
      _messages = _sorted(next);
    } else {
      _messages = _sorted([..._messages, resolved]);
    }
  }

  Future<void> _hydrateMessageImages(String projectId) async {
    final pending = _messages.where((message) =>
        message.previewBytes == null &&
        !message.id.startsWith('pending-') &&
        message.content.startsWith('[Imagen:')).toList();
    for (final message in pending) {
      if (_loadingImages.contains(message.id)) continue;
      _loadingImages.add(message.id);
      unawaited(_loadMessageImage(projectId, message.id));
    }
  }

  Future<void> _loadMessageImage(String projectId, String messageId) async {
    try {
      final bytes = await _getMessageImage(projectId, messageId);
      if (bytes.isEmpty || _disposed) return;
      _messages = _messages
          .map((message) => message.id == messageId ? message.copyWith(previewBytes: bytes) : message)
          .toList();
      _notify();
    } catch (_) {
      // La imagen puede no estar disponible; el mensaje sigue visible.
    } finally {
      _loadingImages.remove(messageId);
    }
  }

  List<ChatMessage> _sorted(List<ChatMessage> messages) {
    final copy = [...messages];
    copy.sort((a, b) {
      final left = a.createdAt ?? '';
      final right = b.createdAt ?? '';
      final byDate = left.compareTo(right);
      if (byDate != 0) return byDate;
      return a.id.compareTo(b.id);
    });
    return copy;
  }

  String _newClientMessageId() {
    final randomPart = _random.nextInt(1 << 32).toRadixString(16);
    return 'chat-${DateTime.now().microsecondsSinceEpoch}-$randomPart';
  }

  void clearError() {
    if (_error == null) return;
    _error = null;
    _notify();
  }

  @override
  void dispose() {
    _disposed = true;
    _refreshTimer?.cancel();
    _eventsSubscription?.cancel();
    _connectionSubscription?.cancel();
    for (final timer in _typingTimers.values) {
      timer.cancel();
    }
    _typingTimers.clear();
    _realtime.dispose();
    super.dispose();
  }
}
