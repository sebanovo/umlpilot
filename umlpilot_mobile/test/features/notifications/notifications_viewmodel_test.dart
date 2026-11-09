import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/features/notifications/domain/entities/app_notification.dart';
import 'package:umlpilot_mobile/features/notifications/domain/repositories/notifications_repository.dart';
import 'package:umlpilot_mobile/features/notifications/domain/usecases/notifications_usecases.dart';
import 'package:umlpilot_mobile/features/notifications/presentation/viewmodels/notifications_viewmodel.dart';

class _FakeNotificationsRepository implements NotificationsRepository {
  final List<AppNotification> items = [
    const AppNotification(
      id: 'n1',
      type: 'project_invitation',
      title: 'Invitación',
      message: 'Te invitaron',
      read: false,
      createdAt: '2026-01-01T10:00:00',
      projectId: 'p1',
      role: 'editor',
      invitationStatus: 'pending',
    ),
  ];
  int unread = 1;

  @override
  Future<List<AppNotification>> list() async => List.of(items);

  @override
  Future<int> unreadCount() async => unread;

  @override
  Future<void> markRead(String id) async => unread = 0;

  @override
  Future<void> markAllRead() async {
    unread = 0;
    for (var i = 0; i < items.length; i++) {
      final item = items[i];
      items[i] = AppNotification(
        id: item.id,
        type: item.type,
        title: item.title,
        message: item.message,
        read: true,
        createdAt: item.createdAt,
        projectId: item.projectId,
        role: item.role,
        invitationStatus: item.invitationStatus,
      );
    }
  }

  @override
  Future<void> respond(String id, {required bool accept}) async {
    items[0] = AppNotification(
      id: items[0].id,
      type: items[0].type,
      title: items[0].title,
      message: items[0].message,
      read: true,
      createdAt: items[0].createdAt,
      projectId: items[0].projectId,
      role: items[0].role,
      invitationStatus: accept ? 'accepted' : 'rejected',
    );
  }
}

NotificationsViewModel _viewModel(NotificationsRepository repository) {
  return NotificationsViewModel(
    getNotifications: GetNotificationsUseCase(repository),
    getUnreadCount: GetUnreadCountUseCase(repository),
    markRead: MarkNotificationReadUseCase(repository),
    markAllRead: MarkAllNotificationsReadUseCase(repository),
    respond: RespondNotificationUseCase(repository),
  );
}

void main() {
  test('carga notificaciones y calcula no leídas', () async {
    final viewModel = _viewModel(_FakeNotificationsRepository());

    await viewModel.load();

    expect(viewModel.notifications, hasLength(1));
    expect(viewModel.unread, 1);
  });

  test('respuesta de invitación actualiza el estado', () async {
    final viewModel = _viewModel(_FakeNotificationsRepository());
    await viewModel.load();

    final ok = await viewModel.respond('n1', accept: true);

    expect(ok, isTrue);
    expect(viewModel.notifications.first.invitationStatus, 'accepted');
    expect(viewModel.unread, 0);
  });

  test('marcar todas deja el contador en cero', () async {
    final viewModel = _viewModel(_FakeNotificationsRepository());
    await viewModel.load();

    await viewModel.markAllRead();

    expect(viewModel.unread, 0);
  });
}
