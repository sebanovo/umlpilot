import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/session/session_controller.dart';
import '../../../collaborators/domain/entities/collaborator.dart';
import '../../../collaborators/domain/entities/user_search_result.dart';
import '../../../collaborators/domain/usecases/collaborators_usecases.dart';
import '../../../diagrams/domain/entities/diagram.dart';
import '../../../diagrams/domain/usecases/diagrams_usecases.dart';
import '../../domain/entities/project.dart';
import '../../domain/usecases/projects_usecases.dart';

class ProjectDetailViewModel extends ChangeNotifier {
  ProjectDetailViewModel({
    required GetProjectUseCase getProject,
    required UpdateProjectUseCase updateProject,
    required DeleteProjectUseCase deleteProject,
    required GetDiagramsUseCase getDiagrams,
    required CreateDiagramUseCase createDiagram,
    required DeleteDiagramUseCase deleteDiagram,
    required LockDiagramUseCase lockDiagram,
    required UnlockDiagramUseCase unlockDiagram,
    required GetCollaboratorsUseCase getCollaborators,
    required InviteCollaboratorUseCase inviteCollaborator,
    required ChangeCollaboratorRoleUseCase changeRole,
    required RemoveCollaboratorUseCase removeCollaborator,
    required SearchUsersUseCase searchUsers,
    required SessionController session,
  })  : _getProject = getProject,
        _updateProject = updateProject,
        _deleteProject = deleteProject,
        _getDiagrams = getDiagrams,
        _createDiagram = createDiagram,
        _deleteDiagram = deleteDiagram,
        _lockDiagram = lockDiagram,
        _unlockDiagram = unlockDiagram,
        _getCollaborators = getCollaborators,
        _inviteCollaborator = inviteCollaborator,
        _changeRole = changeRole,
        _removeCollaborator = removeCollaborator,
        _searchUsers = searchUsers,
        _session = session;

  final GetProjectUseCase _getProject;
  final UpdateProjectUseCase _updateProject;
  final DeleteProjectUseCase _deleteProject;
  final GetDiagramsUseCase _getDiagrams;
  final CreateDiagramUseCase _createDiagram;
  final DeleteDiagramUseCase _deleteDiagram;
  final LockDiagramUseCase _lockDiagram;
  final UnlockDiagramUseCase _unlockDiagram;
  final GetCollaboratorsUseCase _getCollaborators;
  final InviteCollaboratorUseCase _inviteCollaborator;
  final ChangeCollaboratorRoleUseCase _changeRole;
  final RemoveCollaboratorUseCase _removeCollaborator;
  final SearchUsersUseCase _searchUsers;
  final SessionController _session;

  Project? _project;
  List<Diagram> _diagrams = const [];
  List<Collaborator> _collaborators = const [];
  bool _loading = true;
  bool _busy = false;
  String? _error;

  Project? get project => _project;
  List<Diagram> get diagrams => _diagrams;
  List<Collaborator> get collaborators => _collaborators;
  bool get loading => _loading;
  bool get busy => _busy;
  String? get error => _error;

  String get currentUserId => (_session.user?['id'] ?? '').toString();

  Collaborator? get currentCollaborator {
    for (final collaborator in _collaborators) {
      if (collaborator.userId == currentUserId) return collaborator;
    }
    return null;
  }

  String? get currentRole => currentCollaborator?.role;
  bool get isOwner => currentRole == 'owner';
  bool get canEdit => isOwner || currentRole == 'editor';
  bool get canInvite => canEdit;

  Future<void> load(String projectId) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      final project = await _getProject(projectId);
      final diagrams = await _getDiagrams(projectId);
      final collaborators = await _getCollaborators(projectId);
      _project = project;
      _diagrams = diagrams;
      _collaborators = collaborators;
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<bool> saveProject({String? name, String? description}) async {
    final project = _project;
    if (project == null) return false;
    _busy = true;
    notifyListeners();
    try {
      _project = await _updateProject(project.id, name: name, description: description);
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<bool> deleteProject(String projectId) async {
    _busy = true;
    notifyListeners();
    try {
      await _deleteProject(projectId);
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<Diagram?> createDiagram({required String name, required String type, String? description}) async {
    final project = _project;
    if (project == null) return null;
    _busy = true;
    notifyListeners();
    try {
      final diagram = await _createDiagram(project.id, name: name, type: type, description: description);
      _diagrams = [..._diagrams, diagram];
      return diagram;
    } catch (e) {
      _error = messageForError(e);
      return null;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<bool> deleteDiagram(String diagramId) async {
    final project = _project;
    if (project == null) return false;
    try {
      await _deleteDiagram(project.id, diagramId);
      _diagrams = _diagrams.where((diagram) => diagram.id != diagramId).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> toggleLock(String diagramId) async {
    final project = _project;
    if (project == null) return false;
    final matches = _diagrams.where((item) => item.id == diagramId);
    if (matches.isEmpty) return false;
    final diagram = matches.first;
    try {
      final updated = diagram.isLocked
          ? await _unlockDiagram(project.id, diagramId)
          : await _lockDiagram(project.id, diagramId);
      _diagrams = _diagrams.map((item) => item.id == diagramId ? updated : item).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> invite({required String email, required String role}) async {
    final project = _project;
    if (project == null) return false;
    _busy = true;
    notifyListeners();
    try {
      final collaborator = await _inviteCollaborator(project.id, email: email, role: role);
      _collaborators = [
        ..._collaborators.where((item) => item.userId != collaborator.userId),
        collaborator,
      ];
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<bool> changeCollaboratorRole(String userId, String role) async {
    final project = _project;
    if (project == null) return false;
    try {
      await _changeRole(project.id, userId, role);
      _collaborators = _collaborators
          .map((item) => item.userId == userId
              ? Collaborator(
                  userId: item.userId,
                  email: item.email,
                  firstName: item.firstName,
                  lastName: item.lastName,
                  role: role,
                  invitationStatus: item.invitationStatus,
                )
              : item)
          .toList();
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> removeCollaborator(String userId) async {
    final project = _project;
    if (project == null) return false;
    try {
      await _removeCollaborator(project.id, userId);
      _collaborators = _collaborators.where((item) => item.userId != userId).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  Future<List<UserSearchResult>> searchUsers(String query) async {
    try {
      return await _searchUsers(query);
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return const [];
    }
  }

  void clearError() {
    if (_error == null) return;
    _error = null;
    notifyListeners();
  }
}
