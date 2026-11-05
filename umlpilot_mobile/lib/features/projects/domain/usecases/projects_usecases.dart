import '../entities/project.dart';
import '../repositories/projects_repository.dart';

class GetProjectsUseCase {
  const GetProjectsUseCase(this._repository);

  final ProjectsRepository _repository;

  Future<List<Project>> call() => _repository.list();
}

class GetProjectUseCase {
  const GetProjectUseCase(this._repository);

  final ProjectsRepository _repository;

  Future<Project> call(String id) => _repository.get(id);
}

class CreateProjectUseCase {
  const CreateProjectUseCase(this._repository);

  final ProjectsRepository _repository;

  Future<Project> call({required String name, String? description}) =>
      _repository.create(name: name, description: description);
}

class UpdateProjectUseCase {
  const UpdateProjectUseCase(this._repository);

  final ProjectsRepository _repository;

  Future<Project> call(String id, {String? name, String? description}) =>
      _repository.update(id, name: name, description: description);
}

class DeleteProjectUseCase {
  const DeleteProjectUseCase(this._repository);

  final ProjectsRepository _repository;

  Future<void> call(String id) => _repository.delete(id);
}
