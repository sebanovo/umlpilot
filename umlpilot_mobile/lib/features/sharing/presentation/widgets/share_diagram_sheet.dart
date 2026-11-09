import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_snackbar.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../../collaborators/domain/repositories/collaborators_repository.dart';
import '../../../collaborators/presentation/widgets/invite_collaborator_sheet.dart';
import '../../../../injection/injection.dart';
import '../viewmodels/share_diagram_viewmodel.dart';

class ShareDiagramSheet extends StatefulWidget {
  const ShareDiagramSheet({super.key, required this.projectId, required this.diagramId});

  final String projectId;
  final String diagramId;

  @override
  State<ShareDiagramSheet> createState() => _ShareDiagramSheetState();
}

class _ShareDiagramSheetState extends State<ShareDiagramSheet> {
  Future<void> _copy(String url) async {
    await Clipboard.setData(ClipboardData(text: url));
    if (mounted) showAppSnackBar(context, 'Enlace copiado', isSuccess: true);
  }

  Future<void> _share(String url) async {
    await Share.share(url);
  }

  Future<void> _openInvite() async {
    final repository = getIt<CollaboratorsRepository>();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => InviteCollaboratorSheet(
        searchUsers: repository.searchUsers,
        invite: (email, role) async {
          try {
            await repository.invite(widget.projectId, email: email, role: role);
            return null;
          } catch (e) {
            return e.toString();
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ShareDiagramViewModel>();
    final state = viewModel.state;
    final url = viewModel.absoluteUrl;

    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          AppSpacing.lg,
          AppSpacing.lg,
          AppSpacing.lg,
          MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(child: Text('Compartir diagrama', style: Theme.of(context).textTheme.titleLarge)),
                IconButton(
                  tooltip: 'Cerrar',
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.sm),
            const Text(
              'Cualquiera con el enlace puede previsualizarlo y unirse al proyecto con el rol elegido.',
              style: TextStyle(color: AppColors.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: AppSpacing.lg),
            if (viewModel.loading)
              const Center(child: Padding(padding: EdgeInsets.all(AppSpacing.lg), child: CircularProgressIndicator()))
            else ...[
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      initialValue: state?.role ?? 'viewer',
                      decoration: const InputDecoration(labelText: 'Al unirse, podrá'),
                      items: const [
                        DropdownMenuItem(value: 'viewer', child: Text('Ver (lector)')),
                        DropdownMenuItem(value: 'editor', child: Text('Editar (web)')),
                      ],
                      onChanged: viewModel.busy
                          ? null
                          : (value) async {
                              if (value == null) return;
                              final ok = await viewModel.changeRole(value);
                              if (!context.mounted) return;
                              if (!ok) showAppSnackBar(context, viewModel.error ?? 'No se pudo cambiar', isError: true);
                            },
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  if (state?.enabled == true)
                    AppButton(
                      label: 'Desactivar',
                      variant: AppButtonVariant.secondary,
                      loading: viewModel.busy,
                      onPressed: () async {
                        final ok = await viewModel.disable();
                        if (!context.mounted) return;
                        showAppSnackBar(
                          context,
                          ok ? 'Enlace desactivado' : (viewModel.error ?? 'No se pudo desactivar'),
                          isSuccess: ok,
                          isError: !ok,
                        );
                      },
                    )
                  else
                    AppButton(
                      label: 'Activar enlace',
                      loading: viewModel.busy,
                      onPressed: () async {
                        final ok = await viewModel.enable();
                        if (!context.mounted) return;
                        showAppSnackBar(
                          context,
                          ok ? 'Enlace activado' : (viewModel.error ?? 'No se pudo activar'),
                          isSuccess: ok,
                          isError: !ok,
                        );
                      },
                    ),
                ],
              ),
              if (state?.enabled == true && url.isNotEmpty) ...[
                const SizedBox(height: AppSpacing.md),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceAlt,
                    borderRadius: BorderRadius.circular(AppRadius.lg),
                    border: Border.all(color: AppColors.border),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          url,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                        ),
                      ),
                      IconButton(
                        tooltip: 'Copiar',
                        icon: const Icon(Icons.copy, size: 18),
                        onPressed: () => _copy(url),
                      ),
                      IconButton(
                        tooltip: 'Compartir',
                        icon: const Icon(Icons.share, size: 18),
                        onPressed: () => _share(url),
                      ),
                    ],
                  ),
                ),
              ],
              if (viewModel.error != null) ...[
                const SizedBox(height: AppSpacing.sm),
                Text(viewModel.error!, style: const TextStyle(color: AppColors.danger, fontSize: 12)),
              ],
            ],
            const SizedBox(height: AppSpacing.xl),
            const Divider(),
            const SizedBox(height: AppSpacing.md),
            AppButton(
              label: 'Invitar por correo',
              icon: Icons.person_add_alt,
              variant: AppButtonVariant.secondary,
              expand: true,
              onPressed: _openInvite,
            ),
          ],
        ),
      ),
    );
  }
}
