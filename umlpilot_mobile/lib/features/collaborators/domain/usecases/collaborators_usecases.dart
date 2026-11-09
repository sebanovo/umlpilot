import '../entities/collaborator.dart';
import '../entities/user_search_result.dart';
import '../repositories/collaborators_repository.dart';

class GetCollaboratorsUseCase {
  const GetCollaboratorsUseCase(this._repository);

  final CollaboratorsRepository _repository;

  Future<List<Collaborator>> call(String projectId) => _repository.list(projectId);
}

class InviteCollaboratorUseCase {
  const InviteCollaboratorUseCase(this._repository);

  final CollaboratorsRepository _repository;

  Future<Collaborator> call(String projectId, {required String email, required String role}) =>
      _repository.invite(projectId, email: email, role: role);
}

class ChangeCollaboratorRoleUseCase {
  const ChangeCollaboratorRoleUseCase(this._repository);

  final CollaboratorsRepository _repository;

  Future<void> call(String projectId, String userId, String role) =>
      _repository.changeRole(projectId, userId, role);
}

class RemoveCollaboratorUseCase {
  const RemoveCollaboratorUseCase(this._repository);

  final CollaboratorsRepository _repository;

  Future<void> call(String projectId, String userId) => _repository.remove(projectId, userId);
}

class SearchUsersUseCase {
  const SearchUsersUseCase(this._repository);

  final CollaboratorsRepository _repository;

  Future<List<UserSearchResult>> call(String query) => _repository.searchUsers(query);
}
