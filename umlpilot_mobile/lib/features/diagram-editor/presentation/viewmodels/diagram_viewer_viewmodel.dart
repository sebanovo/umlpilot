import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../../../core/realtime/realtime_change.dart';
import '../../../../core/realtime/realtime_service.dart';
import '../../../../core/session/session_controller.dart';
import '../../../collaborators/domain/entities/collaborator.dart';
import '../../../collaborators/domain/usecases/collaborators_usecases.dart';
import '../../../diagrams/domain/entities/diagram.dart';
import '../../../diagrams/domain/usecases/diagrams_usecases.dart';
import '../../domain/entities/uml_models.dart';
import '../../domain/repositories/diagram_editor_repository.dart';

class DiagramViewerViewModel extends ChangeNotifier {
  DiagramViewerViewModel({
    required GetDiagramUseCase getDiagram,
    required DiagramEditorRepository repository,
    required GetCollaboratorsUseCase getCollaborators,
    required SessionController session,
    required RealtimeService realtime,
  })  : _getDiagram = getDiagram,
        _repository = repository,
        _getCollaborators = getCollaborators,
        _session = session,
        _realtime = realtime;

  final GetDiagramUseCase _getDiagram;
  final DiagramEditorRepository _repository;
  final GetCollaboratorsUseCase _getCollaborators;
  final SessionController _session;
  final RealtimeService _realtime;

  String? _projectId;
  String? _diagramId;
  Diagram? _diagram;
  List<UmlElement> _elements = const [];
  List<UmlRelationship> _relationships = const [];
  Map<String, List<UmlAttribute>> _attributes = {};
  Map<String, List<UmlMethod>> _methods = {};
  Map<String, List<UmlLiteral>> _literals = {};
  Map<String, List<UmlMultiplicity>> _multiplicities = {};
  List<Collaborator> _collaborators = const [];
  List<String> _presenceIds = const [];

  bool _loading = true;
  bool _connected = false;
  bool _disposed = false;
  String? _error;
  int _reloadToken = 0;

  Timer? _refreshDebounce;
  StreamSubscription<RealtimeChange>? _changesSubscription;
  StreamSubscription<List<String>>? _presenceSubscription;
  StreamSubscription<bool>? _connectionSubscription;

  Diagram? get diagram => _diagram;
  List<UmlElement> get elements => _elements;
  List<UmlRelationship> get relationships => _relationships;
  Map<String, List<UmlAttribute>> get attributes => _attributes;
  Map<String, List<UmlMethod>> get methods => _methods;
  Map<String, List<UmlLiteral>> get literals => _literals;
  Map<String, List<UmlMultiplicity>> get multiplicities => _multiplicities;
  bool get loading => _loading;
  bool get connected => _connected;
  String? get error => _error;
  String? get projectId => _projectId;
  String? get diagramId => _diagramId;

  String get currentUserId => (_session.user?['id'] ?? '').toString();

  Collaborator? get currentCollaborator {
    for (final collaborator in _collaborators) {
      if (collaborator.userId == currentUserId) return collaborator;
    }
    return null;
  }

  String? get currentRole => currentCollaborator?.role;
  bool get canManageShare => currentRole == 'owner' || currentRole == 'editor';

  List<String> get presenceNames {
    return _presenceIds
        .map((id) {
          for (final collaborator in _collaborators) {
            if (collaborator.userId == id) return collaborator.fullName;
          }
          return 'Colaborador';
        })
        .toSet()
        .toList();
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  Future<void> load(String projectId, String diagramId) async {
    _projectId = projectId;
    _diagramId = diagramId;
    _loading = true;
    _error = null;
    _notify();
    try {
      await _loadMetadata();
      await _loadContent();
      _connectRealtime();
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      _notify();
    }
  }

  Future<void> _loadMetadata() async {
    final projectId = _projectId!;
    final diagramId = _diagramId!;
    _diagram = await _getDiagram(projectId, diagramId);
    // Los colaboradores solo cambian fuera del flujo del diagrama; se leen al abrir.
    _collaborators = await _getCollaborators(projectId);
  }

  /// Refresco completo (cambios masivos: importación, IA, "eliminar todo").
  Future<void> refresh() async {
    if (_projectId == null || _diagramId == null) return;
    try {
      _diagram = await _getDiagram(_projectId!, _diagramId!);
      await _loadContent();
      _notify();
    } catch (e) {
      _error = messageForError(e);
      _notify();
    }
  }

  Future<void> _loadContent() async {
    final projectId = _projectId!;
    final diagramId = _diagramId!;
    final lists = await Future.wait([
      _repository.listElements(projectId, diagramId),
      _repository.listRelationships(projectId, diagramId),
    ]);
    final elements = lists[0] as List<UmlElement>;
    final relationships = lists[1] as List<UmlRelationship>;

    final attributes = <String, List<UmlAttribute>>{};
    final methods = <String, List<UmlMethod>>{};
    final literals = <String, List<UmlLiteral>>{};
    await Future.wait(elements.map((element) async {
      // Solo se piden los datos que aplican al tipo: las notas no tienen
      // hijos y las enumeraciones no tienen métodos.
      if (element.elementType == 'note') {
        attributes[element.id] = const [];
        methods[element.id] = const [];
        literals[element.id] = const [];
        return;
      }
      final isEnum = element.elementType == 'enumeration';
      final results = await Future.wait([
        _repository.listAttributes(projectId, diagramId, element.id),
        isEnum
            ? Future.value(const <UmlMethod>[])
            : _repository.listMethods(projectId, diagramId, element.id),
        isEnum
            ? _repository.listLiterals(projectId, diagramId, element.id)
            : Future.value(const <UmlLiteral>[]),
      ]);
      attributes[element.id] = results[0] as List<UmlAttribute>;
      methods[element.id] = results[1] as List<UmlMethod>;
      literals[element.id] = results[2] as List<UmlLiteral>;
    }));

    final multiplicities = <String, List<UmlMultiplicity>>{};
    await Future.wait(relationships.map((relationship) async {
      multiplicities[relationship.id] =
          await _repository.listMultiplicities(projectId, diagramId, relationship.id);
    }));

    _elements = elements;
    _relationships = relationships;
    _attributes = attributes;
    _methods = methods;
    _literals = literals;
    _multiplicities = multiplicities;
  }

  void _connectRealtime() {
    final diagramId = _diagramId;
    if (diagramId == null) return;
    _connectionSubscription ??= _realtime.connection.listen((connected) {
      _connected = connected;
      _notify();
    });
    _presenceSubscription ??= _realtime.presence.listen((users) {
      _presenceIds = users.where((id) => id != currentUserId).toList();
      _notify();
    });
    _changesSubscription ??= _realtime.changes.listen(_handleChange);
    _realtime.join(diagramId);
  }

  void _handleChange(RealtimeChange change) {
    final elementId = change.data['elementId']?.toString();
    final relationshipId = change.data['relationshipId']?.toString();

    switch (change.type) {
      case 'element_moved':
        _applyElementMoved(change.data);
        break;
      case 'element_created':
        _reloadElements(elementId, fetchChildren: false);
        break;
      case 'element_updated':
        _reloadElements(elementId, fetchChildren: true);
        break;
      case 'element_deleted':
        _removeElement(elementId);
        break;
      case 'relationship_created':
      case 'relationship_updated':
        _reloadRelationships(relationshipId);
        break;
      case 'relationship_deleted':
        _removeRelationship(relationshipId);
        break;
      case 'diagram_changed':
        _scheduleRefresh();
        break;
      default:
        // Eventos desconocidos (cursores, selección) no afectan el diagrama.
        break;
    }
  }

  void _applyElementMoved(Map<String, dynamic> data) {
    final id = data['elementId']?.toString();
    final x = data['positionX'];
    final y = data['positionY'];
    if (id == null || x is! num || y is! num) {
      _scheduleRefresh();
      return;
    }
    _elements = _elements
        .map((element) => element.id == id
            ? element.copyWith(positionX: x.toInt(), positionY: y.toInt())
            : element)
        .toList();
    _notify();
  }

  void _scheduleRefresh() {
    _refreshDebounce?.cancel();
    _refreshDebounce = Timer(const Duration(milliseconds: 120), () {
      refresh();
    });
  }

  Future<void> _reloadElements(String? elementId, {required bool fetchChildren}) async {
    final projectId = _projectId;
    final diagramId = _diagramId;
    if (projectId == null || diagramId == null) return;

    final token = ++_reloadToken;
    try {
      // Lista y datos del elemento cambiado en paralelo (2 saltos → 1).
      final elementsFuture = _repository.listElements(projectId, diagramId);
      Future<List<Object>>? childrenFuture;
      if (elementId != null && fetchChildren) {
        childrenFuture = Future.wait([
          _repository.listAttributes(projectId, diagramId, elementId),
          _repository.listMethods(projectId, diagramId, elementId),
          _repository.listLiterals(projectId, diagramId, elementId),
        ]);
      }
      final elements = await elementsFuture;
      final children = childrenFuture == null ? null : await childrenFuture;
      if (_disposed || token != _reloadToken) return;

      if (elementId != null && children != null) {
        _attributes[elementId] = children[0] as List<UmlAttribute>;
        _methods[elementId] = children[1] as List<UmlMethod>;
        _literals[elementId] = children[2] as List<UmlLiteral>;
      }

      _elements = elements;
      final ids = elements.map((element) => element.id).toSet();
      for (final element in elements) {
        _attributes.putIfAbsent(element.id, () => const []);
        _methods.putIfAbsent(element.id, () => const []);
        _literals.putIfAbsent(element.id, () => const []);
      }
      _attributes.removeWhere((id, _) => !ids.contains(id));
      _methods.removeWhere((id, _) => !ids.contains(id));
      _literals.removeWhere((id, _) => !ids.contains(id));
      _notify();
    } catch (_) {
      // Un fallo transitorio se corrige con el siguiente evento o refresco.
    }
  }

  Future<void> _reloadRelationships(String? relationshipId) async {
    final projectId = _projectId;
    final diagramId = _diagramId;
    if (projectId == null || diagramId == null) return;

    final token = ++_reloadToken;
    try {
      final relationshipsFuture = _repository.listRelationships(projectId, diagramId);
      final changedMultiplicitiesFuture = relationshipId != null
          ? _repository.listMultiplicities(projectId, diagramId, relationshipId)
          : null;
      final relationships = await relationshipsFuture;
      final changedMultiplicities =
          changedMultiplicitiesFuture == null ? null : await changedMultiplicitiesFuture;
      if (_disposed || token != _reloadToken) return;

      _relationships = relationships;
      if (relationshipId != null) {
        _multiplicities[relationshipId] = changedMultiplicities ?? const [];
      } else {
        for (final relationship in relationships) {
          _multiplicities[relationship.id] =
              await _repository.listMultiplicities(projectId, diagramId, relationship.id);
        }
      }
      if (_disposed || token != _reloadToken) return;

      final ids = relationships.map((relationship) => relationship.id).toSet();
      for (final relationship in relationships) {
        _multiplicities.putIfAbsent(relationship.id, () => const []);
      }
      _multiplicities.removeWhere((id, _) => !ids.contains(id));
      _notify();
    } catch (_) {
      // Se reintenta con el siguiente evento o refresco.
    }
  }

  void _removeElement(String? id) {
    if (id == null) {
      _scheduleRefresh();
      return;
    }
    final removedRelationshipIds = _relationships
        .where((relationship) =>
            relationship.sourceElementId == id || relationship.targetElementId == id)
        .map((relationship) => relationship.id)
        .toSet();
    _elements = _elements.where((element) => element.id != id).toList();
    _relationships = _relationships
        .where((relationship) => !removedRelationshipIds.contains(relationship.id))
        .toList();
    _attributes.remove(id);
    _methods.remove(id);
    _literals.remove(id);
    for (final relationshipId in removedRelationshipIds) {
      _multiplicities.remove(relationshipId);
    }
    _notify();
  }

  void _removeRelationship(String? id) {
    if (id == null) {
      _scheduleRefresh();
      return;
    }
    _relationships = _relationships.where((relationship) => relationship.id != id).toList();
    _multiplicities.remove(id);
    _notify();
  }

  @override
  void dispose() {
    _disposed = true;
    _refreshDebounce?.cancel();
    _changesSubscription?.cancel();
    _presenceSubscription?.cancel();
    _connectionSubscription?.cancel();
    _realtime.dispose();
    super.dispose();
  }
}
