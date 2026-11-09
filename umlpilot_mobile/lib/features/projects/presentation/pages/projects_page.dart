import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/constants/api_constants.dart';
import '../../../../core/widgets/app_card.dart';
import '../../../../core/widgets/app_snackbar.dart';
import '../../../../core/widgets/empty_state.dart';
import '../../../../core/widgets/error_state.dart';
import '../../../../core/widgets/loading_state.dart';
import '../../../../features/auth/presentation/viewmodels/auth_viewmodel.dart';
import '../../../../features/notifications/presentation/widgets/notification_bell.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../viewmodels/projects_viewmodel.dart';
import '../widgets/project_form_sheet.dart';

class ProjectsPage extends StatefulWidget {
  const ProjectsPage({super.key});

  @override
  State<ProjectsPage> createState() => _ProjectsPageState();
}

class _ProjectsPageState extends State<ProjectsPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<ProjectsViewModel>().load();
    });
  }

  Future<void> _openCreate() async {
    final data = await showModalBottomSheet<ProjectFormData>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const ProjectFormSheet(),
    );
    if (data == null || !mounted) return;
    final viewModel = context.read<ProjectsViewModel>();
    final project = await viewModel.create(name: data.name, description: data.description);
    if (!mounted) return;
    if (project != null) {
      showAppSnackBar(context, 'Proyecto creado', isSuccess: true);
      context.push('/projects/${project.id}');
    } else {
      showAppSnackBar(context, viewModel.error ?? 'No se pudo crear el proyecto', isError: true);
    }
  }

  Future<void> _logout() async {
    await context.read<AuthViewModel>().logout();
    if (mounted) context.go('/login');
  }

  String _date(String? value) {
    final parsed = DateTime.tryParse(value ?? '');
    if (parsed == null) return '';
    return DateFormat('dd/MM/yyyy').format(parsed.toLocal());
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ProjectsViewModel>();

    return Scaffold(
      appBar: AppBar(
        title: Text(ApiConstants.appName),
        actions: [
          NotificationBell(onInvitationAccepted: viewModel.load),
          IconButton(
            tooltip: 'Salir',
            icon: const Icon(Icons.logout),
            onPressed: _logout,
          ),
        ],
      ),
      body: viewModel.loading
          ? const LoadingState(message: 'Cargando proyectos...')
          : viewModel.error != null
              ? ErrorState(message: viewModel.error!, onRetry: viewModel.load)
              : viewModel.projects.isEmpty
                  ? EmptyState(
                      title: 'No tienes proyectos',
                      message: 'Crea tu primer proyecto para empezar a modelar.',
                      icon: Icons.folder_open_outlined,
                      actionLabel: 'Crear primer proyecto',
                      onAction: _openCreate,
                    )
                  : RefreshIndicator(
                      onRefresh: viewModel.load,
                      child: ListView.separated(
                        padding: const EdgeInsets.all(AppSpacing.lg),
                        itemCount: viewModel.projects.length,
                        separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.md),
                        itemBuilder: (context, index) {
                          final project = viewModel.projects[index];
                          return AppCard(
                            onTap: () => context.push('/projects/${project.id}'),
                            child: Row(
                              children: [
                                Container(
                                  width: 44,
                                  height: 44,
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(AppRadius.lg),
                                  ),
                                  child: const Icon(Icons.folder_outlined, color: AppColors.primaryLight),
                                ),
                                const SizedBox(width: AppSpacing.md),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(project.name, style: Theme.of(context).textTheme.titleMedium),
                                      if (project.description != null && project.description!.isNotEmpty) ...[
                                        const SizedBox(height: 2),
                                        Text(
                                          project.description!,
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(color: AppColors.textSecondary, fontSize: 13),
                                        ),
                                      ],
                                      const SizedBox(height: AppSpacing.sm),
                                      Text(
                                        _date(project.createdAt),
                                        style: const TextStyle(color: AppColors.textMuted, fontSize: 11),
                                      ),
                                    ],
                                  ),
                                ),
                                const Icon(Icons.chevron_right, color: AppColors.textMuted),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openCreate,
        icon: const Icon(Icons.add),
        label: const Text('Nuevo proyecto'),
      ),
    );
  }
}
