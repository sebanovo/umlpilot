import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../core/widgets/app_button.dart';
import '../core/widgets/error_state.dart';
import '../core/widgets/loading_state.dart';
import '../features/auth/presentation/viewmodels/auth_viewmodel.dart';
import '../features/diagram-viewer/presentation/widgets/diagram_viewer_canvas.dart';
import '../features/sharing/presentation/viewmodels/public_diagram_viewmodel.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';

class PublicDiagramPage extends StatefulWidget {
  const PublicDiagramPage({super.key, required this.token});

  final String token;

  @override
  State<PublicDiagramPage> createState() => _PublicDiagramPageState();
}

class _PublicDiagramPageState extends State<PublicDiagramPage> {
  final GlobalKey<DiagramViewerCanvasState> _canvasKey = GlobalKey<DiagramViewerCanvasState>();
  String? _selectedElementId;
  String? _selectedRelationshipId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<PublicDiagramViewModel>().load(widget.token);
    });
  }

  Future<void> _join() async {
    final viewModel = context.read<PublicDiagramViewModel>();
    final result = await viewModel.join(widget.token);
    if (!mounted) return;
    if (result != null) {
      context.go('/projects/${result.projectId}/diagrams/${result.diagramId}');
    } else if (viewModel.error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(viewModel.error!)));
    }
  }

  void _login() {
    context.go('/login?from=${Uri.encodeComponent('/share/${widget.token}')}');
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<PublicDiagramViewModel>();
    final auth = context.watch<AuthViewModel>();
    final view = viewModel.view;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(view?.diagramName ?? 'Diagrama compartido', overflow: TextOverflow.ellipsis),
            if (view != null)
              Text(view.projectName, style: const TextStyle(color: AppColors.textMuted, fontSize: 11)),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Ajustar vista',
            icon: const Icon(Icons.fit_screen_outlined),
            onPressed: () => _canvasKey.currentState?.fitToContent(),
          ),
          if (view != null)
            Padding(
              padding: const EdgeInsets.only(right: AppSpacing.sm),
              child: Center(
                child: AppButton(
                  label: auth.isAuthenticated ? 'Unirme' : 'Iniciar sesión',
                  icon: auth.isAuthenticated ? Icons.group_add_outlined : Icons.login,
                  loading: viewModel.joining,
                  onPressed: auth.isAuthenticated ? _join : _login,
                ),
              ),
            ),
        ],
      ),
      body: viewModel.loading
          ? const LoadingState(message: 'Cargando vista previa...')
          : viewModel.error != null || view == null
              ? ErrorState(
                  title: 'Enlace no disponible',
                  message: viewModel.error ?? 'Este enlace no es válido o fue desactivado.',
                  onRetry: () => viewModel.load(widget.token),
                )
              : Column(
                  children: [
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
                      color: AppColors.surface,
                      child: Row(
                        children: [
                          const Icon(Icons.visibility_outlined, size: 16, color: AppColors.textMuted),
                          const SizedBox(width: AppSpacing.sm),
                          Text(
                            'Vista previa · al unirte serás ${view.linkRole == 'editor' ? 'editor' : 'lector'}',
                            style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: DiagramViewerCanvas(
                        key: _canvasKey,
                        elements: view.elements,
                        relationships: view.relationships,
                        attributes: view.attributes,
                        methods: view.methods,
                        literals: view.literals,
                        multiplicities: view.multiplicities,
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
                  ],
                ),
    );
  }
}
