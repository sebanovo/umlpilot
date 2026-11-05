import 'package:flutter/foundation.dart';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/errors/failures.dart';
import '../../domain/entities/share_link_state.dart';
import '../../domain/repositories/sharing_repository.dart';

class ShareDiagramViewModel extends ChangeNotifier {
  ShareDiagramViewModel(this._repository);

  final SharingRepository _repository;

  String? _projectId;
  String? _diagramId;
  ShareLinkState? _state;
  bool _loading = true;
  bool _busy = false;
  String? _error;

  bool get loading => _loading;
  bool get busy => _busy;
  String? get error => _error;
  ShareLinkState? get state => _state;

  String get absoluteUrl {
    final url = _state?.url;
    if (url == null || url.isEmpty) return '';
    return '${ApiConstants.webBaseUrl}$url';
  }

  Future<void> load(String projectId, String diagramId) async {
    _projectId = projectId;
    _diagramId = diagramId;
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _state = await _repository.getStatus(projectId, diagramId);
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<bool> enable() async {
    final projectId = _projectId;
    final diagramId = _diagramId;
    if (projectId == null || diagramId == null) return false;
    _busy = true;
    _error = null;
    notifyListeners();
    try {
      _state = await _repository.enable(projectId, diagramId, _state?.role ?? 'viewer');
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<bool> disable() async {
    final projectId = _projectId;
    final diagramId = _diagramId;
    if (projectId == null || diagramId == null) return false;
    _busy = true;
    _error = null;
    notifyListeners();
    try {
      await _repository.disable(projectId, diagramId);
      _state = ShareLinkState(enabled: false, role: _state?.role ?? 'viewer');
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<bool> changeRole(String role) async {
    final projectId = _projectId;
    final diagramId = _diagramId;
    if (projectId == null || diagramId == null) return false;

    // Si el enlace está desactivado, solo se guarda la preferencia local;
    // no debe activarse al cambiar el rol.
    if (_state?.enabled != true) {
      _state = ShareLinkState(enabled: false, role: role);
      notifyListeners();
      return true;
    }

    _busy = true;
    _error = null;
    notifyListeners();
    try {
      _state = await _repository.enable(projectId, diagramId, role);
      return true;
    } catch (e) {
      _error = messageForError(e);
      return false;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }
}
