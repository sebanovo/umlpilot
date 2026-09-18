/// Rol de un mensaje dentro de la conversación.
enum ChatRole { system, user, assistant }

/// Entidad de dominio: un mensaje del chat.
class ChatMessage {
  const ChatMessage({
    required this.role,
    required this.content,
    required this.createdAt,
  });

  factory ChatMessage.system(String content) =>
      ChatMessage(role: ChatRole.system, content: content, createdAt: DateTime.now());

  factory ChatMessage.user(String content) =>
      ChatMessage(role: ChatRole.user, content: content, createdAt: DateTime.now());

  factory ChatMessage.assistant(String content) =>
      ChatMessage(role: ChatRole.assistant, content: content, createdAt: DateTime.now());

  final ChatRole role;
  final String content;
  final DateTime createdAt;

  bool get isUser => role == ChatRole.user;
}
