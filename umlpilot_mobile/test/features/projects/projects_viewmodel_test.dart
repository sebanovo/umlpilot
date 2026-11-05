import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/errors/failures.dart';
import 'package:umlpilot_mobile/features/projects/domain/entities/project.dart';
import 'package:umlpilot_mobile/features/projects/domain/repositories/projects_repository.dart';
import 'package:umlpilot_mobile/features/projects/domain/usecases/projects_usecases.dart';
import 'package:umlpilot_mobile/features/projects/presentation/viewmodels/projects_viewmodel.dart';

class _FakeProjectsRepository implements ProjectsRepository {
  _FakeProjectsRepository({this.failing = false});

  final bool failing;
  final List<Project> projects = [];

  @override
  Future<List<Project>> list() async {
    if (failing) throw const ServerFailure('Sin conexión');
    return List.of(projects);
  }

  @override
  Future<Project> create({required String name, String? description}) async {
    if (failing) throw const ServerFailure('No se pudo crear');
    final project = Project(id: 'p${projects.length + 1}', name: name, description: description, creatorId: 'u1', status: 'active');
    projects.add(project);
    return project;
  }

  @override
  Future<void> delete(String id) async {
    projects.removeWhere((project) => project.id == id);
  }

  @override
  Future<Project> get(String id) async => projects.firstWhere((project) => project.id == id);

  @override
  Future<Project> update(String id, {String? name, String? description}) async =>
      projects.firstWhere((project) => project.id == id);
}

ProjectsViewModel _viewModel(ProjectsRepository repository) {
  return ProjectsViewModel(
    getProjects: GetProjectsUseCase(repository),
    createProject: CreateProjectUseCase(repository),
    deleteProject: DeleteProjectUseCase(repository),
  );
}

void main() {
  test('carga la lista de proyectos', () async {
    final repository = _FakeProjectsRepository();
    repository.projects.add(const Project(id: 'p1', name: 'Sistema', creatorId: 'u1', status: 'active'));
    final viewModel = _viewModel(repository);

    await viewModel.load();

    expect(viewModel.loading, isFalse);
    expect(viewModel.projects, hasLength(1));
    expect(viewModel.error, isNull);
  });

  test('expone el error de carga', () async {
    final viewModel = _viewModel(_FakeProjectsRepository(failing: true));

    await viewModel.load();

    expect(viewModel.loading, isFalse);
    expect(viewModel.error, 'Sin conexión');
  });

  test('crea y agrega el proyecto al inicio', () async {
    final viewModel = _viewModel(_FakeProjectsRepository());

    final project = await viewModel.create(name: 'Nuevo', description: 'desc');

    expect(project?.name, 'Nuevo');
    expect(viewModel.projects.first.name, 'Nuevo');
  });
}
