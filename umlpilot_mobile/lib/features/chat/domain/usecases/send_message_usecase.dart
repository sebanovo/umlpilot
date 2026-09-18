import '../entities/chat_message.dart';
import '../repositories/chat_repository.dart';

/// Caso de uso: enviar un mensaje al asistente.
class SendMessageUseCase {
  const SendMessageUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatMessage> call(List<ChatMessage> conversation) =>
      _repository.sendMessages(conversation);
}
