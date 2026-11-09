import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/widgets/error_state.dart';
import '../../../../core/widgets/loading_state.dart';
import '../../../../injection/injection.dart';
import '../../../notifications/presentation/widgets/notification_bell.dart';
import '../../../sharing/presentation/viewmodels/share_diagram_viewmodel.dart';
import '../../../sharing/presentation/widgets/share_diagram_sheet.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../../diagram-editor/presentation/viewmodels/diagram_viewer_viewmodel.dart';
import '../widgets/diagram_details_sheets.dart';
import '../widgets/diagram_viewer_canvas.dart';

class DiagramViewerPage extends StatefulWidget {
  const DiagramViewerPage({super.key, required this.projectId, required this.diagramId});

  final String projectId;
  final String diagramId;

  @override
  State<DiagramViewerPage> createState() => _DiagramViewerPageState();
}

class _DiagramViewerPageState extends State<DiagramViewerPage> {
  final GlobalKey<DiagramViewerCanvasState> _canvasKey = GlobalKey<DiagramViewerCanvasState>();
  DiagramViewerViewModel? _viewModel;
  bool _didFit = false;
  String? _selectedElementId;
  String? _selectedRelationshipId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final viewModel = context.read<DiagramViewerViewModel>();
      _viewModel = viewModel;
      viewModel.addListener(_onViewModelChanged);
      viewModel.load(widget.projectId, widget.diagramId);
    });
  }

  @override
  void dispose() {
    _viewModel?.removeListener(_onViewModelChanged);
    super.dispose();
  }

  void _onViewModelChanged() {
    final viewModel = _viewModel;
    if (!mounted || viewModel == null || _didFit) return;
    if (viewModel.loading || viewModel.error != null) return;
    _didFit = true;
    WidgetsBinding.instance.addPostFrameCallback((_) => _canvasKey.currentState?.fitToContent());
  }

  Future<void> _openShare() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: AppColors.surface,
      builder: (_) => ChangeNotifierProvider<ShareDiagramViewModel>(
        create: (_) => getIt<ShareDiagramViewModel>()..load(widget.projectId, widget.diagramId),
        child: ShareDiagramSheet(projectId: widget.projectId, diagramId: widget.diagramId),
      ),
    );
  }

  void _showElementDetails(DiagramViewerViewModel viewModel) {
    final element = viewModel.elements.where((item) => item.id == _selectedElementId).firstOrNull;
    if (element == null) return;
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: AppColors.surface,
      builder: (_) => ElementDetailsSheet(
        element: element,
        attributes: viewModel.attributes[element.id] ?? const [],
        methods: viewModel.methods[element.id] ?? const [],
        literals: viewModel.literals[element.id] ?? const [],
      ),
    );
  }

  void _showRelationshipDetails(DiagramViewerViewModel viewModel) {
    final relationship = viewModel.relationships.where((item) => item.id == _selectedRelationshipId).firstOrNull;
    if (relationship == null) return;
    String nameFor(String id) {
      return viewModel.elements.where((item) => item.id == id).firstOrNull?.name ?? id;
    }

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: AppColors.surface,
      builder: (_) => RelationshipDetailsSheet(
        relationship: relationship,
        multiplicities: viewModel.multiplicities[relationship.id] ?? const [],
        sourceName: nameFor(relationship.sourceElementId),
        targetName: nameFor(relationship.targetElementId),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<DiagramViewerViewModel>();
    final diagram = viewModel.diagram;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(diagram?.name ?? 'Diagrama', overflow: TextOverflow.ellipsis),
            const Text('Solo lectura', style: TextStyle(color: AppColors.textMuted, fontSize: 11)),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Ajustar vista',
            icon: const Icon(Icons.fit_screen_outlined),
            onPressed: () => _canvasKey.currentState?.fitToContent(),
          ),
          if (viewModel.canManageShare)
            IconButton(
              tooltip: 'Compartir diagrama',
              icon: const Icon(Icons.share_outlined),
              onPressed: _openShare,
            ),
          const NotificationBell(),
        ],
      ),
      body: viewModel.loading
          ? const LoadingState(message: 'Cargando diagrama...')
          : viewModel.error != null
              ? ErrorState(
                  message: viewModel.error!,
                  onRetry: () => viewModel.load(widget.projectId, widget.diagramId),
                )
              : Column(
                  children: [
                    _LiveBar(connected: viewModel.connected, people: viewModel.presenceNames),
                    Expanded(
                      child: DiagramViewerCanvas(
                        key: _canvasKey,
                        elements: viewModel.elements,
                        relationships: viewModel.relationships,
                        attributes: viewModel.attributes,
                        methods: viewModel.methods,
                        literals: viewModel.literals,
                        multiplicities: viewModel.multiplicities,
                        selectedElementId: _selectedElementId,
                        selectedRelationshipId: _selectedRelationshipId,
                        onSelectElement: (id) => setState(() {
                          _selectedElementId = id;
                          _selectedRelationshipId = null;
                        }),
                        onSelectRelationship: (id) => setState(() {
                          _selectedRelationshipId = id;
                          _selectedElementId = null;
                        }),
                        onBackgroundTap: () => setState(() {
                          _selectedElementId = null;
                          _selectedRelationshipId = null;
                        }),
                      ),
                    ),
                    if (_selectedElementId != null || _selectedRelationshipId != null)
                      _SelectionBar(
                        label: _selectedElementId != null
                            ? (viewModel.elements.where((item) => item.id == _selectedElementId).firstOrNull?.name ?? 'Elemento')
                            : (viewModel.relationships.where((item) => item.id == _selectedRelationshipId).firstOrNull?.name ?? 'Relación'),
                        onDetails: () => _selectedElementId != null
                            ? _showElementDetails(viewModel)
                            : _showRelationshipDetails(viewModel),
                        onClose: () => setState(() {
                          _selectedElementId = null;
                          _selectedRelationshipId = null;
                        }),
                      ),
                  ],
                ),
    );
  }
}

class _LiveBar extends StatelessWidget {
  const _LiveBar({required this.connected, required this.people});

  final bool connected;
  final List<String> people;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      color: AppColors.surface,
      child: Row(
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: connected ? AppColors.success : AppColors.textMuted,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          Text(
            connected ? 'En vivo' : 'Conectando…',
            style: TextStyle(color: connected ? AppColors.success : AppColors.textMuted, fontSize: 12),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: people.isEmpty
                ? const SizedBox.shrink()
                : Text(
                    people.join(' · '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                  ),
          ),
        ],
      ),
    );
  }
}

class _SelectionBar extends StatelessWidget {
  const _SelectionBar({required this.label, required this.onDetails, required this.onClose});

  final String label;
  final VoidCallback onDetails;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.sm, AppSpacing.sm),
        color: AppColors.surface,
        child: Row(
          children: [
            Expanded(
              child: Text(label, style: Theme.of(context).textTheme.titleMedium, overflow: TextOverflow.ellipsis),
            ),
            TextButton.icon(onPressed: onDetails, icon: const Icon(Icons.info_outline, size: 18), label: const Text('Detalles')),
            IconButton(tooltip: 'Cerrar', icon: const Icon(Icons.close), onPressed: onClose),
          ],
        ),
      ),
    );
  }
}

