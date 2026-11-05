import '../entities/project.dart';

abstract interface class ProjectsRepository {
  Future<List<Project>> list();

  Future<Project> get(String id);

  Future<Project> create({required String name, String? description});

  Future<Project> update(String id, {String? name, String? description});

  Future<void> delete(String id);
}
