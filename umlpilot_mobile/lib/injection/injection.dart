import 'package:get_it/get_it.dart';

import '../core/constants/api_constants.dart';
import '../core/network/api_client.dart';
import '../core/realtime/chat_realtime_service.dart';
import '../core/realtime/realtime_service.dart';
import '../core/session/session_controller.dart';
import '../core/storage/token_storage.dart';
import '../features/auth/data/datasources/auth_remote_datasource.dart';
import '../features/auth/data/repositories/auth_repository_impl.dart';
import '../features/auth/domain/repositories/auth_repository.dart';
import '../features/auth/domain/usecases/auth_usecases.dart';
import '../features/auth/presentation/viewmodels/auth_viewmodel.dart';
import '../features/collaborators/data/datasources/collaborators_remote_datasource.dart';
import '../features/collaborators/domain/repositories/collaborators_repository.dart';
import '../features/collaborators/domain/usecases/collaborators_usecases.dart';
import '../features/chat/data/datasources/chat_remote_datasource.dart';
import '../features/chat/data/repositories/chat_repository_impl.dart';
import '../features/chat/domain/repositories/chat_repository.dart';
import '../features/chat/domain/usecases/chat_usecases.dart';
import '../features/chat/presentation/viewmodels/chat_viewmodel.dart';
import '../features/diagram-editor/data/datasources/diagram_editor_remote_datasource.dart';
import '../features/diagram-editor/domain/repositories/diagram_editor_repository.dart';
import '../features/diagram-editor/presentation/viewmodels/diagram_viewer_viewmodel.dart';
import '../features/diagrams/data/datasources/diagrams_remote_datasource.dart';
import '../features/diagrams/domain/repositories/diagrams_repository.dart';
import '../features/diagrams/domain/usecases/diagrams_usecases.dart';
import '../features/notifications/data/datasources/notifications_remote_datasource.dart';
import '../features/notifications/domain/repositories/notifications_repository.dart';
import '../features/notifications/domain/usecases/notifications_usecases.dart';
import '../features/notifications/presentation/viewmodels/notifications_viewmodel.dart';
import '../features/projects/data/datasources/projects_remote_datasource.dart';
import '../features/projects/domain/repositories/projects_repository.dart';
import '../features/projects/domain/usecases/projects_usecases.dart';
import '../features/projects/presentation/viewmodels/project_detail_viewmodel.dart';
import '../features/projects/presentation/viewmodels/projects_viewmodel.dart';
import '../features/sharing/data/datasources/sharing_remote_datasource.dart';
import '../features/sharing/domain/repositories/sharing_repository.dart';
import '../features/sharing/presentation/viewmodels/public_diagram_viewmodel.dart';
import '../features/sharing/presentation/viewmodels/share_diagram_viewmodel.dart';
import '../routes/app_router.dart';

final GetIt getIt = GetIt.instance;

Future<void> configureDependencies() async {
  getIt.registerLazySingleton<TokenStorage>(() => SecureTokenStorage());
  getIt.registerLazySingleton<SessionController>(() => SessionController(getIt()));
  getIt.registerLazySingleton<ApiClient>(
    () => ApiClient(baseUrl: ApiConstants.baseUrl, session: getIt()),
  );

  // Autenticación
  getIt.registerLazySingleton<AuthRemoteDataSource>(() => AuthRemoteDataSource(getIt()));
  getIt.registerLazySingleton<AuthRepository>(() => AuthRepositoryImpl(getIt(), getIt()));
  getIt.registerLazySingleton<AuthViewModel>(
    () => AuthViewModel(
      loginUseCase: LoginUseCase(getIt()),
      registerUseCase: RegisterUseCase(getIt()),
      restoreSessionUseCase: RestoreSessionUseCase(getIt()),
      logoutUseCase: LogoutUseCase(getIt()),
    ),
  );

  // Proyectos
  getIt.registerLazySingleton<ProjectsRemoteDataSource>(() => ProjectsRemoteDataSource(getIt()));
  getIt.registerLazySingleton<ProjectsRepository>(() => ProjectsRepositoryImpl(getIt()));
  getIt.registerFactory<ProjectsViewModel>(
    () => ProjectsViewModel(
      getProjects: GetProjectsUseCase(getIt()),
      createProject: CreateProjectUseCase(getIt()),
      deleteProject: DeleteProjectUseCase(getIt()),
    ),
  );

  // Colaboradores
  getIt.registerLazySingleton<CollaboratorsRemoteDataSource>(
    () => CollaboratorsRemoteDataSource(getIt()),
  );
  getIt.registerLazySingleton<CollaboratorsRepository>(
    () => CollaboratorsRepositoryImpl(getIt()),
  );

  // Diagramas
  getIt.registerLazySingleton<DiagramsRemoteDataSource>(() => DiagramsRemoteDataSource(getIt()));
  getIt.registerLazySingleton<DiagramsRepository>(() => DiagramsRepositoryImpl(getIt()));
  getIt.registerLazySingleton<DiagramEditorRemoteDataSource>(
    () => DiagramEditorRemoteDataSource(getIt()),
  );
  getIt.registerLazySingleton<DiagramEditorRepository>(
    () => DiagramEditorRepositoryImpl(getIt()),
  );

  // Notificaciones
  getIt.registerLazySingleton<NotificationsRemoteDataSource>(
    () => NotificationsRemoteDataSource(getIt()),
  );
  getIt.registerLazySingleton<NotificationsRepository>(
    () => NotificationsRepositoryImpl(getIt()),
  );
  getIt.registerLazySingleton<NotificationsViewModel>(
    () => NotificationsViewModel(
      getNotifications: GetNotificationsUseCase(getIt()),
      getUnreadCount: GetUnreadCountUseCase(getIt()),
      markRead: MarkNotificationReadUseCase(getIt()),
      markAllRead: MarkAllNotificationsReadUseCase(getIt()),
      respond: RespondNotificationUseCase(getIt()),
    ),
  );

  // Chat grupal por proyecto
  getIt.registerLazySingleton<ChatRemoteDataSource>(() => ChatRemoteDataSource(getIt()));
  getIt.registerLazySingleton<ChatRepository>(() => ChatRepositoryImpl(getIt()));
  getIt.registerFactory<ChatRealtimeService>(() => ChatRealtimeService(getIt()));
  getIt.registerFactory<ChatViewModel>(
    () => ChatViewModel(
      getProject: GetProjectUseCase(getIt()),
      getConversation: GetChatConversationUseCase(getIt()),
      getMessages: GetChatMessagesUseCase(getIt()),
      sendMessage: SendChatMessageUseCase(getIt()),
      sendImage: SendChatImageUseCase(getIt()),
      retryMessage: RetryChatMessageUseCase(getIt()),
      transcribeAudio: TranscribeChatAudioUseCase(getIt()),
      getDiagrams: GetDiagramsUseCase(getIt()),
      getCollaborators: GetCollaboratorsUseCase(getIt()),
      session: getIt(),
      realtime: getIt(),
    ),
  );

  // Tiempo real (una instancia por pantalla de diagrama)
  getIt.registerFactory<RealtimeService>(() => RealtimeService(getIt()));

  // ViewModels con estado por pantalla
  getIt.registerFactory<ProjectDetailViewModel>(
    () => ProjectDetailViewModel(
      getProject: GetProjectUseCase(getIt()),
      updateProject: UpdateProjectUseCase(getIt()),
      deleteProject: DeleteProjectUseCase(getIt()),
      getDiagrams: GetDiagramsUseCase(getIt()),
      createDiagram: CreateDiagramUseCase(getIt()),
      deleteDiagram: DeleteDiagramUseCase(getIt()),
      lockDiagram: LockDiagramUseCase(getIt()),
      unlockDiagram: UnlockDiagramUseCase(getIt()),
      getCollaborators: GetCollaboratorsUseCase(getIt()),
      inviteCollaborator: InviteCollaboratorUseCase(getIt()),
      changeRole: ChangeCollaboratorRoleUseCase(getIt()),
      removeCollaborator: RemoveCollaboratorUseCase(getIt()),
      searchUsers: SearchUsersUseCase(getIt()),
      session: getIt(),
    ),
  );

  getIt.registerFactory<DiagramViewerViewModel>(
    () => DiagramViewerViewModel(
      getDiagram: GetDiagramUseCase(getIt()),
      repository: getIt(),
      getCollaborators: GetCollaboratorsUseCase(getIt()),
      session: getIt(),
      realtime: getIt(),
    ),
  );

  // Compartir
  getIt.registerLazySingleton<SharingRemoteDataSource>(() => SharingRemoteDataSource(getIt()));
  getIt.registerLazySingleton<SharingRepository>(() => SharingRepositoryImpl(getIt()));
  getIt.registerFactory<ShareDiagramViewModel>(() => ShareDiagramViewModel(getIt()));
  getIt.registerFactory<PublicDiagramViewModel>(() => PublicDiagramViewModel(getIt()));

  getIt.registerLazySingleton<AppRouter>(() => AppRouter(getIt()));
}
