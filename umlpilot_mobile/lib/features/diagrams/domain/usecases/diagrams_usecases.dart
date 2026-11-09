import '../entities/diagram.dart';
import '../repositories/diagrams_repository.dart';

class GetDiagramsUseCase {
  const GetDiagramsUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<List<Diagram>> call(String projectId) => _repository.list(projectId);
}

class GetDiagramUseCase {
  const GetDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<Diagram> call(String projectId, String diagramId) => _repository.get(projectId, diagramId);
}

class CreateDiagramUseCase {
  const CreateDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<Diagram> call(String projectId, {required String name, required String type, String? description}) =>
      _repository.create(projectId, name: name, type: type, description: description);
}

class UpdateDiagramUseCase {
  const UpdateDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<Diagram> call(String projectId, String diagramId, {String? name, String? description, String? canvasData}) =>
      _repository.update(projectId, diagramId, name: name, description: description, canvasData: canvasData);
}

class DeleteDiagramUseCase {
  const DeleteDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<void> call(String projectId, String diagramId) => _repository.delete(projectId, diagramId);
}

class LockDiagramUseCase {
  const LockDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<Diagram> call(String projectId, String diagramId) => _repository.lock(projectId, diagramId);
}

class UnlockDiagramUseCase {
  const UnlockDiagramUseCase(this._repository);

  final DiagramsRepository _repository;

  Future<Diagram> call(String projectId, String diagramId) => _repository.unlock(projectId, diagramId);
}
