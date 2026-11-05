import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_snackbar.dart';
import '../../../../core/widgets/empty_state.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../domain/entities/app_notification.dart';
import '../viewmodels/notifications_viewmodel.dart';

class NotificationsSheet extends StatefulWidget {
  const NotificationsSheet({super.key, this.onInvitationAccepted});

  final VoidCallback? onInvitationAccepted;

  @override
  State<NotificationsSheet> createState() => _NotificationsSheetState();
}

class _NotificationsSheetState extends State<NotificationsSheet> {
  String? _busyId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<NotificationsViewModel>().load();
    });
  }

  String _date(String value) {
    final parsed = DateTime.tryParse(value)?.toLocal();
    if (parsed == null) return '';
    return DateFormat('dd/MM HH:mm').format(parsed);
  }

  Future<void> _respond(AppNotification notification, bool accept) async {
    setState(() => _busyId = notification.id);
    final viewModel = context.read<NotificationsViewModel>();
    final ok = await viewModel.respond(notification.id, accept: accept);
    if (!mounted) return;
    setState(() => _busyId = null);
    if (ok) {
      showAppSnackBar(
        context,
        accept ? 'Invitación aceptada' : 'Invitación rechazada',
        isSuccess: accept,
      );
      if (accept) widget.onInvitationAccepted?.call();
    } else if (viewModel.error != null) {
      showAppSnackBar(context, viewModel.error!, isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<NotificationsViewModel>();

    return FractionallySizedBox(
      heightFactor: 0.85,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.sm, AppSpacing.sm),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Notificaciones', style: Theme.of(context).textTheme.titleLarge),
                      Text(
                        viewModel.unread > 0 ? '${viewModel.unread} sin leer' : 'Todo al día',
                        style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                if (viewModel.unread > 0)
                  TextButton(
                    onPressed: viewModel.markAllRead,
                    child: const Text('Marcar todas'),
                  ),
                IconButton(
                  tooltip: 'Cerrar',
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: viewModel.loading
                ? const Center(child: CircularProgressIndicator())
                : viewModel.notifications.isEmpty
                    ? const EmptyState(
                        title: 'No tienes notificaciones',
                        message: 'Aquí llegarán las invitaciones a proyectos.',
                        icon: Icons.notifications_none,
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.all(AppSpacing.lg),
                        itemCount: viewModel.notifications.length,
                        separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
                        itemBuilder: (context, index) {
                          final notification = viewModel.notifications[index];
                          return _NotificationTile(
                            notification: notification,
                            date: _date(notification.createdAt),
                            busy: _busyId == notification.id,
                            onAccept: () => _respond(notification, true),
                            onReject: () => _respond(notification, false),
                            onMarkRead: notification.read ? null : () => viewModel.markRead(notification.id),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({
    required this.notification,
    required this.date,
    required this.busy,
    required this.onAccept,
    required this.onReject,
    required this.onMarkRead,
  });

  final AppNotification notification;
  final String date;
  final bool busy;
  final VoidCallback onAccept;
  final VoidCallback onReject;
  final VoidCallback? onMarkRead;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: notification.read ? AppColors.surfaceAlt : AppColors.primary.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                margin: const EdgeInsets.only(top: 6),
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: notification.read ? AppColors.borderLight : AppColors.primaryLight,
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(notification.title, style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 2),
                    Text(
                      notification.message,
                      style: const TextStyle(color: AppColors.textSecondary, fontSize: 13),
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Wrap(
                      spacing: AppSpacing.sm,
                      runSpacing: AppSpacing.xs,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        if (notification.role != null)
                          _Chip(label: notification.role!),
                        if (notification.invitationStatus == 'accepted') _Chip(label: 'Aceptada', color: AppColors.success),
                        if (notification.invitationStatus == 'rejected') _Chip(label: 'Rechazada', color: AppColors.danger),
                        if (onMarkRead != null)
                          TextButton(
                            onPressed: onMarkRead,
                            style: TextButton.styleFrom(
                              minimumSize: const Size(0, 36),
                              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                            ),
                            child: const Text('Marcar leída', style: TextStyle(fontSize: 12)),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              Text(date, style: const TextStyle(color: AppColors.textMuted, fontSize: 11)),
            ],
          ),
          if (notification.isPendingInvitation) ...[
            const SizedBox(height: AppSpacing.sm),
            Row(
              children: [
                Expanded(
                  child: AppButton(
                    label: 'Aceptar',
                    icon: Icons.check,
                    loading: busy,
                    onPressed: onAccept,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  child: AppButton(
                    label: 'Rechazar',
                    variant: AppButtonVariant.secondary,
                    icon: Icons.close,
                    onPressed: busy ? null : onReject,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.label, this.color = AppColors.textSecondary});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.full),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(label, style: TextStyle(color: color, fontSize: 11)),
    );
  }
}
