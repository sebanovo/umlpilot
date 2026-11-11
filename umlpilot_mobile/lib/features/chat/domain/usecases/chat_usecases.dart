import 'dart:typed_data';

import '../entities/chat_models.dart';
import '../repositories/chat_repository.dart';

class GetChatConversationUseCase {
  const GetChatConversationUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatConversation> call(String projectId) => _repository.getConversation(projectId);
}

class GetChatMessagesUseCase {
  const GetChatMessagesUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatPage> call(String projectId, {String? before, int limit = 50}) =>
      _repository.getMessages(projectId, before: before, limit: limit);
}

class SendChatMessageUseCase {
  const SendChatMessageUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatSendResult> call(
    String projectId, {
    required String clientMessageId,
    required String content,
    String? diagramId,
  }) =>
      _repository.sendMessage(
        projectId,
        clientMessageId: clientMessageId,
        content: content,
        diagramId: diagramId,
      );
}

class SendChatImageUseCase {
  const SendChatImageUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatSendResult> call(
    String projectId, {
    required String clientMessageId,
    required Uint8List image,
    required String filename,
    String? diagramId,
    String? instructions,
  }) =>
      _repository.sendImage(
        projectId,
        clientMessageId: clientMessageId,
        image: image,
        filename: filename,
        diagramId: diagramId,
        instructions: instructions,
      );
}

class RetryChatMessageUseCase {
  const RetryChatMessageUseCase(this._repository);

  final ChatRepository _repository;

  Future<ChatSendResult> call(String projectId, String messageId) =>
      _repository.retryMessage(projectId, messageId);
}

class TranscribeChatAudioUseCase {
  const TranscribeChatAudioUseCase(this._repository);

  final ChatRepository _repository;

  Future<String> call(String projectId, Uint8List audio) =>
      _repository.transcribe(projectId, audio);
}
