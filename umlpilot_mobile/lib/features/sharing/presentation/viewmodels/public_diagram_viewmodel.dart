import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../domain/entities/public_diagram_view.dart';
import '../../domain/entities/share_link_state.dart';
import '../../domain/repositories/sharing_repository.dart';

class PublicDiagramViewModel extends ChangeNotifier {
  PublicDiagramViewModel(this._repository);

  final SharingRepository _repository;

  PublicDiagramView? _view;
  bool _loading = true;
  bool _joining = false;
  String? _error;

  PublicDiagramView? get view => _view;
  bool get loading => _loading;
  bool get joining => _joining;
  String? get error => _error;

  Future<void> load(String token) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _view = await _repository.publicView(token);
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<ShareJoinResult?> join(String token) async {
    _joining = true;
    _error = null;
    notifyListeners();
    try {
      return await _repository.join(token);
    } catch (e) {
      _error = messageForError(e);
      return null;
    } finally {
      _joining = false;
      notifyListeners();
    }
  }
}
