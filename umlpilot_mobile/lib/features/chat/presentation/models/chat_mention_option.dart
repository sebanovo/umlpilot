class ChatMentionOption {
  const ChatMentionOption({
    required this.label,
    required this.role,
    this.isAssistant = false,
    this.userId,
  });

  final String label;
  final String role;
  final bool isAssistant;
  final String? userId;

  String get insertion => isAssistant ? '@Asistente IA ' : '@$label ';
}
