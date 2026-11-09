import '../entities/collaborator.dart';
import '../entities/user_search_result.dart';

abstract interface class CollaboratorsRepository {
  Future<List<Collaborator>> list(String projectId);

  Future<Collaborator> invite(String projectId, {required String email, required String role});

  Future<void> changeRole(String projectId, String userId, String role);

  Future<void> remove(String projectId, String userId);

  Future<List<UserSearchResult>> searchUsers(String query);
}
