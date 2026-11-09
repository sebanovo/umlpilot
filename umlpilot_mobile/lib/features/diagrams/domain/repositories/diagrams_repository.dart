import '../entities/diagram.dart';

abstract interface class DiagramsRepository {
  Future<List<Diagram>> list(String projectId);

  Future<Diagram> get(String projectId, String diagramId);

  Future<Diagram> create(String projectId, {required String name, required String type, String? description});

  Future<Diagram> update(String projectId, String diagramId, {String? name, String? description, String? canvasData});

  Future<void> delete(String projectId, String diagramId);

  Future<Diagram> lock(String projectId, String diagramId);

  Future<Diagram> unlock(String projectId, String diagramId);
}
