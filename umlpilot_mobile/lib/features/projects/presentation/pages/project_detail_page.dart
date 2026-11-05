import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/widgets/app_card.dart';
import '../../../../core/widgets/app_snackbar.dart';
import '../../../../core/widgets/confirm_dialog.dart';
import '../../../../core/widgets/error_state.dart';
import '../../../../core/widgets/loading_state.dart';
import '../../../collaborators/domain/entities/collaborator.dart';
import '../../../collaborators/presentation/widgets/invite_collaborator_sheet.dart';
import '../../../diagrams/domain/entities/diagram.dart';
import '../../../notifications/presentation/widgets/notification_bell.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../viewmodels/project_detail_viewmodel.dart';
import '../widgets/diagram_form_sheet.dart';
import '../widgets/project_form_sheet.dart';

class ProjectDetailPage extends StatefulWidget {
  const ProjectDetailPage({super.key});

  @override
  State<ProjectDetailPage> createState() => _ProjectDetailPageState();
}

class _ProjectDetailPageState extends State<ProjectDetailPage> {
  String get _projectId => GoRouterState.of(context).pathParameters['projectId'] ?? '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<ProjectDetailViewModel>().load(_projectId);
    });
  }

  Future<void> _refresh() => context.read<ProjectDetailViewModel>().load(_projectId);

  Future<void> _openCreateDiagram() async {
    final data = await showModalBottomSheet<DiagramFormData>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const DiagramFormSheet(),
    );
    if (data == null || !mounted) return;
    final viewModel = context.read<ProjectDetailViewModel>();
    final diagram = await viewModel.createDiagram(name: data.name, type: data.type);
    if (!mounted) return;
    if (diagram != null) {
      showAppSnackBar(context, 'Diagrama creado', isSuccess: true);
    } else {
      showAppSnackBar(context, viewModel.error ?? 'No se pudo crear el diagrama', isError: true);
    }
  }

  Future<void> _deleteDiagram(Diagram diagram) async {
    final confirmed = await showConfirmDialog(
      context,
      title: 'Eliminar diagrama',
      message: 'Se eliminará "${diagram.name}" y todo su contenido. Esta acción no se puede deshacer.',
      confirmLabel: 'Eliminar',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.deleteDiagram(diagram.id);
    if (!mounted) return;
    showAppSnackBar(
      context,
      ok ? 'Diagrama eliminado' : (viewModel.error ?? 'No se pudo eliminar'),
      isSuccess: ok,
      isError: !ok,
    );
  }

  Future<void> _toggleLock(Diagram diagram) async {
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.toggleLock(diagram.id);
    if (!mounted) return;
    if (ok) {
      showAppSnackBar(context, diagram.isLocked ? 'Diagrama desbloqueado' : 'Diagrama bloqueado', isSuccess: true);
    } else {
      showAppSnackBar(context, viewModel.error ?? 'No se pudo cambiar el bloqueo', isError: true);
    }
  }

  Future<void> _openEditProject() async {
    final project = context.read<ProjectDetailViewModel>().project;
    if (project == null) return;
    final data = await showModalBottomSheet<ProjectFormData>(
      context: context,
      isScrollControlled: true,
      builder: (_) => ProjectFormSheet(
        title: 'Editar proyecto',
        initialName: project.name,
        initialDescription: project.description ?? '',
        submitLabel: 'Guardar',
      ),
    );
    if (data == null || !mounted) return;
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.saveProject(name: data.name, description: data.description);
    if (!mounted) return;
    showAppSnackBar(
      context,
      ok ? 'Proyecto actualizado' : (viewModel.error ?? 'No se pudo actualizar'),
      isSuccess: ok,
      isError: !ok,
    );
  }

  Future<void> _deleteProject() async {
    final confirmed = await showConfirmDialog(
      context,
      title: 'Eliminar proyecto',
      message: 'Se eliminará el proyecto y todos sus diagramas. Esta acción no se puede deshacer.',
      confirmLabel: 'Eliminar',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.deleteProject(_projectId);
    if (!mounted) return;
    if (ok) {
      showAppSnackBar(context, 'Proyecto eliminado', isSuccess: true);
      context.go('/projects');
    } else {
      showAppSnackBar(context, viewModel.error ?? 'No se pudo eliminar', isError: true);
    }
  }

  Future<void> _openInvite() async {
    final viewModel = context.read<ProjectDetailViewModel>();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => InviteCollaboratorSheet(
        searchUsers: viewModel.searchUsers,
        invite: (email, role) async {
          final ok = await viewModel.invite(email: email, role: role);
          return ok ? null : (viewModel.error ?? 'No se pudo invitar');
        },
      ),
    );
  }

  Future<void> _changeRole(Collaborator collaborator, String role) async {
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.changeCollaboratorRole(collaborator.userId, role);
    if (!mounted) return;
    showAppSnackBar(
      context,
      ok ? 'Rol actualizado' : (viewModel.error ?? 'No se pudo cambiar el rol'),
      isSuccess: ok,
      isError: !ok,
    );
  }

  Future<void> _removeCollaborator(Collaborator collaborator) async {
    final confirmed = await showConfirmDialog(
      context,
      title: 'Eliminar colaborador',
      message: '${collaborator.fullName} perderá el acceso al proyecto de inmediato.',
      confirmLabel: 'Eliminar',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    final viewModel = context.read<ProjectDetailViewModel>();
    final ok = await viewModel.removeCollaborator(collaborator.userId);
    if (!mounted) return;
    showAppSnackBar(
      context,
      ok ? 'Colaborador eliminado' : (viewModel.error ?? 'No se pudo eliminar'),
      isSuccess: ok,
      isError: !ok,
    );
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ProjectDetailViewModel>();
    final project = viewModel.project;

    if (viewModel.loading) {
      return const Scaffold(body: LoadingState(message: 'Cargando proyecto...'));
    }
    if (viewModel.error != null && project == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Proyecto')),
        body: ErrorState(message: viewModel.error!, onRetry: _refresh),
      );
    }
    if (project == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Proyecto')),
        body: ErrorState(message: 'Proyecto no encontrado', onRetry: _refresh),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(project.name, overflow: TextOverflow.ellipsis),
        actions: [
          NotificationBell(onInvitationAccepted: _refresh),
          if (viewModel.canEdit)
            IconButton(tooltip: 'Editar proyecto', icon: const Icon(Icons.edit_outlined), onPressed: _openEditProject),
          if (viewModel.isOwner)
            IconButton(tooltip: 'Eliminar proyecto', icon: const Icon(Icons.delete_outline), onPressed: _deleteProject),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            _ProjectHeader(viewModel: viewModel),
            const SizedBox(height: AppSpacing.xl),
            _SectionHeader(
              title: 'Diagramas (${viewModel.diagrams.length})',
              actionLabel: viewModel.canEdit ? 'Nuevo' : null,
              onAction: viewModel.canEdit ? _openCreateDiagram : null,
            ),
            const SizedBox(height: AppSpacing.md),
            if (viewModel.diagrams.isEmpty)
              const _InlineEmpty(message: 'Este proyecto aún no tiene diagramas.')
            else
              ...viewModel.diagrams.map(
                (diagram) => Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                  child: _DiagramTile(
                    diagram: diagram,
                    canEdit: viewModel.canEdit,
                    onOpen: () => context.push('/projects/${project.id}/diagrams/${diagram.id}'),
                    onLock: () => _toggleLock(diagram),
                    onDelete: () => _deleteDiagram(diagram),
                  ),
                ),
              ),
            const SizedBox(height: AppSpacing.xl),
            _SectionHeader(
              title: 'Colaboradores (${viewModel.collaborators.length})',
              actionLabel: viewModel.canInvite ? 'Invitar' : null,
              onAction: viewModel.canInvite ? _openInvite : null,
            ),
            const SizedBox(height: AppSpacing.md),
            ...viewModel.collaborators.map(
              (collaborator) => Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: _CollaboratorTile(
                  collaborator: collaborator,
                  isOwner: viewModel.isOwner,
                  onChangeRole: (role) => _changeRole(collaborator, role),
                  onRemove: () => _removeCollaborator(collaborator),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ProjectHeader extends StatelessWidget {
  const _ProjectHeader({required this.viewModel});

  final ProjectDetailViewModel viewModel;

  @override
  Widget build(BuildContext context) {
    final project = viewModel.project!;
    final role = viewModel.currentRole;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(project.name, style: Theme.of(context).textTheme.headlineMedium)),
              if (role != null) _Badge(label: role, color: role == 'owner' ? AppColors.warning : AppColors.primaryLight),
            ],
          ),
          if (project.description != null && project.description!.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(project.description!, style: const TextStyle(color: AppColors.textSecondary)),
          ],
          const SizedBox(height: AppSpacing.md),
          Row(
            children: [
              const Icon(Icons.folder_outlined, size: 16, color: AppColors.textMuted),
              const SizedBox(width: AppSpacing.xs),
              Text(
                '${viewModel.diagrams.length} diagrama${viewModel.diagrams.length == 1 ? '' : 's'}',
                style: const TextStyle(color: AppColors.textMuted, fontSize: 12),
              ),
              const SizedBox(width: AppSpacing.md),
              const Icon(Icons.group_outlined, size: 16, color: AppColors.textMuted),
              const SizedBox(width: AppSpacing.xs),
              Text(
                '${viewModel.collaborators.length} colaborador${viewModel.collaborators.length == 1 ? '' : 'es'}',
                style: const TextStyle(color: AppColors.textMuted, fontSize: 12),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title, this.actionLabel, this.onAction});

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: Text(title, style: Theme.of(context).textTheme.titleMedium)),
        if (actionLabel != null)
          TextButton.icon(
            onPressed: onAction,
            icon: const Icon(Icons.add, size: 18),
            label: Text(actionLabel!),
          ),
      ],
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
      child: Text(message, style: const TextStyle(color: AppColors.textMuted)),
    );
  }
}

class _DiagramTile extends StatelessWidget {
  const _DiagramTile({
    required this.diagram,
    required this.canEdit,
    required this.onOpen,
    required this.onLock,
    required this.onDelete,
  });

  final Diagram diagram;
  final bool canEdit;
  final VoidCallback onOpen;
  final VoidCallback onLock;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: onOpen,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
      child: Row(
        children: [
          const Icon(Icons.account_tree_outlined, color: AppColors.primaryLight),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(diagram.name, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 2),
                Wrap(
                  spacing: AppSpacing.sm,
                  children: [
                    Text(diagram.type, style: const TextStyle(color: AppColors.textMuted, fontSize: 12)),
                    if (diagram.isLocked)
                      const _Badge(label: 'Bloqueado', color: AppColors.warning, icon: Icons.lock_outline),
                  ],
                ),
              ],
            ),
          ),
          if (canEdit)
            PopupMenuButton<String>(
              tooltip: 'Acciones',
              onSelected: (value) {
                if (value == 'lock') onLock();
                if (value == 'delete') onDelete();
              },
              itemBuilder: (_) => [
                PopupMenuItem(
                  value: 'lock',
                  child: Text(diagram.isLocked ? 'Desbloquear' : 'Bloquear'),
                ),
                const PopupMenuItem(value: 'delete', child: Text('Eliminar')),
              ],
            )
          else
            const Icon(Icons.chevron_right, color: AppColors.textMuted),
        ],
      ),
    );
  }
}

class _CollaboratorTile extends StatelessWidget {
  const _CollaboratorTile({
    required this.collaborator,
    required this.isOwner,
    required this.onChangeRole,
    required this.onRemove,
  });

  final Collaborator collaborator;
  final bool isOwner;
  final ValueChanged<String> onChangeRole;
  final VoidCallback onRemove;

  String get _initials {
    final first = collaborator.firstName.isNotEmpty ? collaborator.firstName[0] : '';
    final last = collaborator.lastName.isNotEmpty ? collaborator.lastName[0] : '';
    return '$first$last'.toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    final canManage = isOwner && !collaborator.isOwner;
    return AppCard(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
      child: Row(
        children: [
          CircleAvatar(
            radius: 18,
            backgroundColor: AppColors.primary.withValues(alpha: 0.2),
            child: Text(
              _initials.isEmpty ? '?' : _initials,
              style: const TextStyle(color: AppColors.primaryLight, fontSize: 12),
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(collaborator.fullName, style: Theme.of(context).textTheme.titleMedium),
                Text(collaborator.email, style: const TextStyle(color: AppColors.textMuted, fontSize: 12)),
              ],
            ),
          ),
          Wrap(
            spacing: AppSpacing.xs,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              if (collaborator.isPending)
                const _Badge(label: 'Pendiente', color: AppColors.warning),
              _Badge(
                label: collaborator.role,
                color: collaborator.isOwner ? AppColors.warning : AppColors.primaryLight,
              ),
            ],
          ),
          if (canManage)
            PopupMenuButton<String>(
              tooltip: 'Gestionar',
              onSelected: (value) {
                if (value == 'remove') onRemove();
                if (value == 'editor' || value == 'viewer') onChangeRole(value);
              },
              itemBuilder: (_) => [
                if (collaborator.role != 'editor') const PopupMenuItem(value: 'editor', child: Text('Hacer editor')),
                if (collaborator.role != 'viewer') const PopupMenuItem(value: 'viewer', child: Text('Hacer lector')),
                const PopupMenuItem(value: 'remove', child: Text('Eliminar')),
              ],
            ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.label, required this.color, this.icon});

  final String label;
  final Color color;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppRadius.full),
        border: Border.all(color: color.withValues(alpha: 0.5)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 11, color: color),
            const SizedBox(width: 3),
          ],
          Text(label, style: TextStyle(color: color, fontSize: 11)),
        ],
      ),
    );
  }
}
