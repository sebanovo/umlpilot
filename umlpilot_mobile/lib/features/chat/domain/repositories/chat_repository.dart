import 'dart:typed_data';

import '../entities/chat_models.dart';

abstract interface class ChatRepository {
  Future<ChatConversation> getConversation(String projectId);

  Future<ChatPage> getMessages(String projectId, {String? before, int limit = 50});

  Future<ChatSendResult> sendMessage(
    String projectId, {
    required String clientMessageId,
    required String content,
    String? diagramId,
  });

  Future<ChatSendResult> retryMessage(String projectId, String messageId);

  Future<String> transcribe(String projectId, Uint8List audio);
}
