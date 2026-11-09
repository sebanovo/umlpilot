import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../theme/app_colors.dart';
import '../viewmodels/notifications_viewmodel.dart';
import 'notifications_sheet.dart';

class NotificationBell extends StatefulWidget {
  const NotificationBell({super.key, this.onInvitationAccepted});

  final VoidCallback? onInvitationAccepted;

  @override
  State<NotificationBell> createState() => _NotificationBellState();
}

class _NotificationBellState extends State<NotificationBell> {
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final viewModel = context.read<NotificationsViewModel>();
      viewModel.refreshCount();
      _timer = Timer.periodic(
        AppConstants.notificationsPollInterval,
        (_) => viewModel.refreshCount(),
      );
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final unread = context.select<NotificationsViewModel, int>((viewModel) => viewModel.unread);

    return Badge(
      isLabelVisible: unread > 0,
      backgroundColor: AppColors.primary,
      label: Text(unread > 9 ? '9+' : '$unread'),
      child: IconButton(
        tooltip: 'Notificaciones',
        icon: const Icon(Icons.notifications_outlined),
        onPressed: () {
          final viewModel = context.read<NotificationsViewModel>();
          showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            useSafeArea: true,
            backgroundColor: AppColors.surface,
            builder: (_) => ChangeNotifierProvider<NotificationsViewModel>.value(
              value: viewModel,
              child: NotificationsSheet(onInvitationAccepted: widget.onInvitationAccepted),
            ),
          );
        },
      ),
    );
  }
}
