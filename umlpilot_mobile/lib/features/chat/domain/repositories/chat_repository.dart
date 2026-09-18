import '../entities/chat_message.dart';

/// Contrato del repositorio de chat (capa de dominio).
abstract interface class ChatRepository {
  /// Envía la conversación completa y devuelve la respuesta del asistente.
  Future<ChatMessage> sendMessages(List<ChatMessage> conversation);
}
