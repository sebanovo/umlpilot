import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/session/session_controller.dart';
import 'package:umlpilot_mobile/core/storage/token_storage.dart';
import 'package:umlpilot_mobile/features/auth/domain/entities/user.dart';
import 'package:umlpilot_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:umlpilot_mobile/features/auth/domain/usecases/auth_usecases.dart';
import 'package:umlpilot_mobile/features/auth/presentation/viewmodels/auth_viewmodel.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/entities/collaborator.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/entities/user_search_result.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/repositories/collaborators_repository.dart';
import 'package:umlpilot_mobile/features/collaborators/domain/usecases/collaborators_usecases.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/entities/diagram.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/repositories/diagrams_repository.dart';
import 'package:umlpilot_mobile/features/diagrams/domain/usecases/diagrams_usecases.dart';
import 'package:umlpilot_mobile/features/projects/domain/entities/project.dart';
import 'package:umlpilot_mobile/features/projects/domain/repositories/projects_repository.dart';
import 'package:umlpilot_mobile/features/projects/domain/usecases/projects_usecases.dart';
import 'package:umlpilot_mobile/features/projects/presentation/viewmodels/project_detail_viewmodel.dart';
import 'package:umlpilot_mobile/features/projects/presentation/viewmodels/projects_viewmodel.dart';
import 'package:umlpilot_mobile/features/sharing/domain/entities/public_diagram_view.dart';
import 'package:umlpilot_mobile/features/sharing/domain/entities/share_link_state.dart';
import 'package:umlpilot_mobile/features/sharing/domain/repositories/sharing_repository.dart';
import 'package:umlpilot_mobile/features/sharing/presentation/viewmodels/share_diagram_viewmodel.dart';

class _FakeAuthRepository implements AuthRepository {
  @override
  Future<User> login({required String email, required String password}) async =>
      User(id: 'u1', email: email, firstName: 'Ada', lastName: 'Lovelace', role: 'USER');

  @override
  Future<User> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async =>
      User(id: 'u1', email: email, firstName: firstName, lastName: lastName, role: 'USER');

  @override
  Future<User?> restoreSession() async => null;

  @override
  Future<void> logout() async {}
}

class _FakeProjectsRepository implements ProjectsRepository {
  final List<Project> projects = [];

  @override
  Future<List<Project>> list() async => List.of(projects);

  @override
  Future<Project> create({required String name, String? description}) async {
    final project = Project(
      id: 'p${projects.length + 1}',
      name: name,
      description: description,
      creatorId: 'u1',
      status: 'active',
    );
    projects.add(project);
    return project;
  }

  @override
  Future<void> delete(String id) async => projects.removeWhere((project) => project.id == id);

  @override
  Future<Project> get(String id) async => projects.firstWhere((project) => project.id == id);

  @override
  Future<Project> update(String id, {String? name, String? description}) async =>
      projects.firstWhere((project) => project.id == id);
}

class _FakeCollaboratorsRepository implements CollaboratorsRepository {
  @override
  Future<void> changeRole(String projectId, String userId, String role) async {}

  @override
  Future<Collaborator> invite(String projectId, {required String email, required String role}) async =>
      Collaborator(
        userId: 'invited',
        email: email,
        firstName: 'Invitado',
        lastName: '',
        role: role,
        invitationStatus: 'pending',
      );

  @override
  Future<List<Collaborator>> list(String projectId) async => const [
        Collaborator(
          userId: 'u1',
          email: 'ada@x.com',
          firstName: 'Ada',
          lastName: 'Lovelace',
          role: 'owner',
          invitationStatus: 'accepted',
        ),
      ];

  @override
  Future<void> remove(String projectId, String userId) async {}

  @override
  Future<List<UserSearchResult>> searchUsers(String query) async => const [];
}

class _FakeDiagramsRepository implements DiagramsRepository {
  final List<Diagram> diagrams = [];

  @override
  Future<Diagram> create(
    String projectId, {
    required String name,
    required String type,
    String? description,
  }) async {
    final diagram = Diagram(
      id: 'd${diagrams.length + 1}',
      projectId: projectId,
      name: name,
      type: type,
      description: description,
      version: 1,
      isLocked: false,
    );
    diagrams.add(diagram);
    return diagram;
  }

  @override
  Future<void> delete(String projectId, String diagramId) async =>
      diagrams.removeWhere((diagram) => diagram.id == diagramId);

  @override
  Future<Diagram> get(String projectId, String diagramId) async =>
      diagrams.firstWhere((diagram) => diagram.id == diagramId);

  @override
  Future<List<Diagram>> list(String projectId) async => List.of(diagrams);

  @override
  Future<Diagram> lock(String projectId, String diagramId) async => get(projectId, diagramId);

  @override
  Future<Diagram> unlock(String projectId, String diagramId) async => get(projectId, diagramId);

  @override
  Future<Diagram> update(
    String projectId,
    String diagramId, {
    String? name,
    String? description,
    String? canvasData,
  }) async =>
      get(projectId, diagramId);
}

class _FakeSharingRepository implements SharingRepository {
  String _role = 'viewer';

  @override
  Future<void> disable(String projectId, String diagramId) async {}

  @override
  Future<ShareLinkState> enable(String projectId, String diagramId, String role) async {
    _role = role;
    return ShareLinkState(enabled: true, token: 'tok', url: '/share/tok', role: role);
  }

  @override
  Future<ShareLinkState> getStatus(String projectId, String diagramId) async =>
      ShareLinkState(enabled: false, role: _role);

  @override
  Future<ShareJoinResult> join(String token) async =>
      const ShareJoinResult(projectId: 'p1', diagramId: 'd1', role: 'viewer');

  @override
  Future<PublicDiagramView> publicView(String token) async => const PublicDiagramView(
        projectId: 'p1',
        diagramId: 'd1',
        linkRole: 'viewer',
        projectName: 'Proyecto',
        diagramName: 'Clases',
        diagramType: 'class',
        elements: [],
        relationships: [],
        attributes: {},
        methods: {},
        literals: {},
        multiplicities: {},
      );
}

void main() {
  test('flujo login → proyecto → diagrama → compartir', () async {
    final session = SessionController(MemoryTokenStorage());
    await session.start(
      user: {'id': 'u1', 'firstName': 'Ada', 'lastName': 'Lovelace'},
      accessToken: 'access',
      refreshToken: 'refresh',
    );

    final authRepository = _FakeAuthRepository();
    final authViewModel = AuthViewModel(
      loginUseCase: LoginUseCase(authRepository),
      registerUseCase: RegisterUseCase(authRepository),
      restoreSessionUseCase: RestoreSessionUseCase(authRepository),
      logoutUseCase: LogoutUseCase(authRepository),
    );
    expect(await authViewModel.login(email: 'ada@x.com', password: '12345678'), isTrue);

    final projectsRepository = _FakeProjectsRepository();
    final projectsViewModel = ProjectsViewModel(
      getProjects: GetProjectsUseCase(projectsRepository),
      createProject: CreateProjectUseCase(projectsRepository),
      deleteProject: DeleteProjectUseCase(projectsRepository),
    );
    await projectsViewModel.load();
    final project = await projectsViewModel.create(name: 'Proyecto', description: 'Demo');
    expect(project, isNotNull);

    final collaboratorsRepository = _FakeCollaboratorsRepository();
    final diagramsRepository = _FakeDiagramsRepository();
    final detailViewModel = ProjectDetailViewModel(
      getProject: GetProjectUseCase(projectsRepository),
      updateProject: UpdateProjectUseCase(projectsRepository),
      deleteProject: DeleteProjectUseCase(projectsRepository),
      getDiagrams: GetDiagramsUseCase(diagramsRepository),
      createDiagram: CreateDiagramUseCase(diagramsRepository),
      deleteDiagram: DeleteDiagramUseCase(diagramsRepository),
      lockDiagram: LockDiagramUseCase(diagramsRepository),
      unlockDiagram: UnlockDiagramUseCase(diagramsRepository),
      getCollaborators: GetCollaboratorsUseCase(collaboratorsRepository),
      inviteCollaborator: InviteCollaboratorUseCase(collaboratorsRepository),
      changeRole: ChangeCollaboratorRoleUseCase(collaboratorsRepository),
      removeCollaborator: RemoveCollaboratorUseCase(collaboratorsRepository),
      searchUsers: SearchUsersUseCase(collaboratorsRepository),
      session: session,
    );
    await detailViewModel.load(project!.id);
    expect(detailViewModel.currentRole, 'owner');

    final diagram = await detailViewModel.createDiagram(name: 'Clases', type: 'class');
    expect(diagram, isNotNull);
    expect(detailViewModel.diagrams, hasLength(1));

    final sharingViewModel = ShareDiagramViewModel(_FakeSharingRepository());
    await sharingViewModel.load(project.id, diagram!.id);
    expect(sharingViewModel.state?.enabled, isFalse);
    expect(await sharingViewModel.enable(), isTrue);
    expect(sharingViewModel.state?.enabled, isTrue);
    expect(sharingViewModel.absoluteUrl, contains('/share/tok'));
  });
}
