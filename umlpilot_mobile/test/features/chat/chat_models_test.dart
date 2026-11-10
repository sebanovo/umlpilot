import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/features/chat/domain/entities/chat_models.dart';

void main() {
  group('ChatConversation', () {
    test('parsea la conversación y el permiso de mutación', () {
      final conversation = ChatConversation.fromJson({
        'conversationId': 'c1',
        'projectId': 'p1',
        'title': 'Chat del proyecto',
        'summary': 'Resumen',
        'focusDiagramId': 'd1',
        'canMutate': true,
        'unsummarizedCount': 3,
      });

      expect(conversation.conversationId, 'c1');
      expect(conversation.summary, 'Resumen');
      expect(conversation.focusDiagramId, 'd1');
      expect(conversation.canMutate, isTrue);
      expect(conversation.unsummarizedCount, 3);
    });
  });

  group('ChatMessage', () {
    test('parsea autoría, estado, acciones y citas', () {
      final message = ChatMessage.fromJson({
        'id': 'm1',
        'conversationId': 'c1',
        'projectId': 'p1',
        'diagramId': 'd1',
        'authorType': 'assistant',
        'authorName': 'Asistente IA',
        'authorRole': 'assistant',
        'content': 'Usamos herencia',
        'aiStatus': 'completed',
        'provider': 'gemini',
        'model': 'gemini-3.8-flash',
        'citations': [
          {
            'messageId': 'm0',
            'snippet': 'Decisión anterior',
            'authorName': 'Ana',
          },
        ],
        'actions': [
          {
            'action': 'create',
            'entity': 'class Cliente',
            'id': 'e1',
            'result': 'ok',
          },
        ],
      });

      expect(message.isAssistant, isTrue);
      expect(message.aiStatus, ChatAiStatus.completed);
      expect(message.provider, 'gemini');
      expect(message.citations.single.messageId, 'm0');
      expect(message.actions.single.entity, 'class Cliente');
    });

    test('copyWith conserva el contenido y cambia el estado', () {
      final message = ChatMessage.fromJson({
        'id': 'm1',
        'conversationId': 'c1',
        'projectId': 'p1',
        'authorType': 'user',
        'authorName': 'Ana',
        'authorRole': 'editor',
        'content': 'Hola',
        'aiStatus': 'never',
      });

      final failed = message.copyWith(failed: true, aiStatus: ChatAiStatus.failed);

      expect(failed.content, 'Hola');
      expect(failed.failed, isTrue);
      expect(failed.aiStatus, ChatAiStatus.failed);
    });
  });

  test('parsea página y resultado de envío', () {
    final page = ChatPage.fromJson({
      'messages': [
        {
          'id': 'm1',
          'conversationId': 'c1',
          'projectId': 'p1',
          'authorType': 'user',
          'authorName': 'Ana',
          'authorRole': 'editor',
          'content': 'Hola',
        },
      ],
      'nextCursor': 'cursor',
      'hasMore': true,
    });
    final result = ChatSendResult.fromJson({
      'message': {
        'id': 'm1',
        'conversationId': 'c1',
        'projectId': 'p1',
        'authorType': 'user',
        'authorName': 'Ana',
        'authorRole': 'editor',
        'content': 'Hola',
      },
      'duplicate': true,
    });

    expect(page.messages, hasLength(1));
    expect(page.nextCursor, 'cursor');
    expect(page.hasMore, isTrue);
    expect(result.duplicate, isTrue);
    expect(result.message.id, 'm1');
  });

  test('detecta la mención a la IA', () {
    expect(mentionsAssistant('@asistente revisa esto'), isTrue);
    expect(mentionsAssistant('Hola @Asistente, ayuda'), isTrue);
    expect(mentionsAssistant('@AI genera el diagrama'), isTrue);
    expect(mentionsAssistant('@Luis revisa esto'), isFalse);
    expect(mentionsAssistant('Hola equipo'), isFalse);
  });
}
