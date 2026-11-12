import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:umlpilot_mobile/core/realtime/chat_realtime_service.dart';
import 'package:umlpilot_mobile/core/session/session_controller.dart';
import 'package:umlpilot_mobile/features/chat/domain/entities/chat_models.dart';
import 'package:umlpilot_mobile/features/chat/domain/repositories/chat_repository.dart';
import 'package:umlpilot_mobile/features/chat/domain/usecases/chat_usecases.dart';
import 'package:umlpilot_mobile/features/chat/presentation/viewmodels/chat_viewmodel.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/entities/collaborator.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/repositories/collaborators_repository.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/usecases/collaborators_usecases.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/entities/diagram.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/repositories/diagrams_repository.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/usecases/diagrams_usecases.dart';
import 'package:umlpilot_mobile/features/projects/domain/entities/project.dart';
import 'package:umlpilot_mobile/features/projects/domain/repositories/projects_repository.dart';
import 'package:umlpilot_mobile/features/projects/domain/usecases/projects_usecases.dart';

class _MockChatRepository extends Mock implements ChatRepository {}

class _MockProjectsRepository extends Mock implements ProjectsRepository {}

class _MockDiagramsRepository extends Mock implements DiagramsRepository {}

class _MockCollaboratorsRepository extends Mock implements CollaboratorsRepository {}

class _MockSessionController extends Mock implements SessionController {}

class _MockChatRealtimeService extends Mock implements ChatRealtimeService {}

void main() {
  late _MockChatRepository chatRepository;
  late _MockProjectsRepository projectsRepository;
  late _MockDiagramsRepository diagramsRepository;
  late _MockCollaboratorsRepository collaboratorsRepository;
  late _MockSessionController session;
  late _MockChatRealtimeService realtime;
  late ChatViewModel viewModel;

  setUp(() {
    chatRepository = _MockChatRepository();
    projectsRepository = _MockProjectsRepository();
    diagramsRepository = _MockDiagramsRepository();
    collaboratorsRepository = _MockCollaboratorsRepository();
    session = _MockSessionController();
    realtime = _MockChatRealtimeService();

    when(() => session.user).thenReturn({
      'id': 'u1',
      'firstName': 'Ana',
      'lastName': 'Perez',
      'email': 'ana@example.com',
    });
    when(() => realtime.events).thenAnswer((_) => const Stream<Map<String, dynamic>>.empty());
    when(() => realtime.connection).thenAnswer((_) => const Stream<bool>.empty());
    when(() => projectsRepository.get('p1')).thenAnswer((_) async => const Project(
          id: 'p1',
          name: 'Proyecto',
          creatorId: 'u1',
          status: 'active',
        ));
    when(() => chatRepository.getConversation('p1')).thenAnswer((_) async => const ChatConversation(
          conversationId: 'c1',
          projectId: 'p1',
          title: 'Chat del proyecto',
          canMutate: true,
        ));
    when(() => chatRepository.getMessages('p1', before: null, limit: 50)).thenAnswer((_) async => const ChatPage(
          messages: [],
          hasMore: false,
        ));
    when(() => diagramsRepository.list('p1')).thenAnswer((_) async => const <Diagram>[]);
    when(() => collaboratorsRepository.list('p1')).thenAnswer((_) async => [
          const Collaborator(
            userId: 'u1',
            email: 'ana@example.com',
            firstName: 'Ana',
            lastName: 'Perez',
            role: 'editor',
            invitationStatus: 'accepted',
          ),
        ]);

    viewModel = ChatViewModel(
      getProject: GetProjectUseCase(projectsRepository),
      getConversation: GetChatConversationUseCase(chatRepository),
      getMessages: GetChatMessagesUseCase(chatRepository),
      sendMessage: SendChatMessageUseCase(chatRepository),
      sendImage: SendChatImageUseCase(chatRepository),
      getMessageImage: GetChatMessageImageUseCase(chatRepository),
      retryMessage: RetryChatMessageUseCase(chatRepository),
      transcribeAudio: TranscribeChatAudioUseCase(chatRepository),
      getDiagrams: GetDiagramsUseCase(diagramsRepository),
      getCollaborators: GetCollaboratorsUseCase(collaboratorsRepository),
      session: session,
      realtime: realtime,
    );
  });

  tearDown(() {
    viewModel.dispose();
  });

  test('carga la conversación y permite enviar con mención', () async {
    when(() => chatRepository.sendMessage(
          'p1',
          clientMessageId: any(named: 'clientMessageId'),
          content: '@asistente hola',
          diagramId: null,
        )).thenAnswer((invocation) async {
          final clientId = invocation.namedArguments[#clientMessageId] as String;
          return ChatSendResult(
            duplicate: false,
            message: ChatMessage(
              id: 'm1',
              conversationId: 'c1',
              projectId: 'p1',
              authorType: 'user',
              authorId: 'u1',
              authorName: 'Ana Perez',
              authorRole: 'editor',
              content: '@asistente hola',
              clientMessageId: clientId,
              aiStatus: ChatAiStatus.generating,
              createdAt: DateTime.now().toUtc().toIso8601String(),
            ),
          );
        });

    await viewModel.load('p1');
    await viewModel.sendMessage('@asistente hola');

    expect(viewModel.loading, isFalse);
    expect(viewModel.projectName, 'Proyecto');
    expect(viewModel.canMutate, isTrue);
    expect(viewModel.messages, hasLength(1));
    expect(viewModel.messages.single.id, 'm1');
    expect(viewModel.messages.single.aiStatus, ChatAiStatus.generating);
    expect(viewModel.sending, isFalse);
  });

  test('un mensaje sin mención no espera respuesta de la IA', () async {
    when(() => chatRepository.sendMessage(
          'p1',
          clientMessageId: any(named: 'clientMessageId'),
          content: 'Hola equipo',
          diagramId: null,
        )).thenAnswer((invocation) async {
          final clientId = invocation.namedArguments[#clientMessageId] as String;
          return ChatSendResult(
            duplicate: false,
            message: ChatMessage(
              id: 'm2',
              conversationId: 'c1',
              projectId: 'p1',
              authorType: 'user',
              authorId: 'u1',
              authorName: 'Ana Perez',
              authorRole: 'editor',
              content: 'Hola equipo',
              clientMessageId: clientId,
              aiStatus: ChatAiStatus.none,
              createdAt: DateTime.now().toUtc().toIso8601String(),
            ),
          );
        });

    await viewModel.load('p1');
    await viewModel.sendMessage('Hola equipo');

    expect(viewModel.messages.single.aiStatus, ChatAiStatus.none);
  });
}
