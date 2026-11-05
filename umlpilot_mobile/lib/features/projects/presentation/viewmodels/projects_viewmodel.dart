import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../domain/entities/project.dart';
import '../../domain/usecases/projects_usecases.dart';

class ProjectsViewModel extends ChangeNotifier {
  ProjectsViewModel({
    required GetProjectsUseCase getProjects,
    required CreateProjectUseCase createProject,
    required DeleteProjectUseCase deleteProject,
  })  : _getProjects = getProjects,
        _createProject = createProject,
        _deleteProject = deleteProject;

  final GetProjectsUseCase _getProjects;
  final CreateProjectUseCase _createProject;
  final DeleteProjectUseCase _deleteProject;

  List<Project> _projects = const [];
  bool _loading = true;
  String? _error;

  List<Project> get projects => _projects;
  bool get loading => _loading;
  String? get error => _error;
  bool get isEmpty => !_loading && _error == null && _projects.isEmpty;

  Future<void> load() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _projects = await _getProjects();
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<Project?> create({required String name, String? description}) async {
    try {
      final project = await _createProject(name: name, description: description);
      _projects = [project, ..._projects];
      notifyListeners();
      return project;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return null;
    }
  }

  Future<bool> delete(String id) async {
    try {
      await _deleteProject(id);
      _projects = _projects.where((project) => project.id != id).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  void clearError() {
    if (_error == null) return;
    _error = null;
    notifyListeners();
  }
}
