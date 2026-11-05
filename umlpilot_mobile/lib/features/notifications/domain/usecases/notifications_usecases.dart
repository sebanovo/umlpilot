import '../entities/app_notification.dart';
import '../repositories/notifications_repository.dart';

class GetNotificationsUseCase {
  const GetNotificationsUseCase(this._repository);

  final NotificationsRepository _repository;

  Future<List<AppNotification>> call() => _repository.list();
}

class GetUnreadCountUseCase {
  const GetUnreadCountUseCase(this._repository);

  final NotificationsRepository _repository;

  Future<int> call() => _repository.unreadCount();
}

class MarkNotificationReadUseCase {
  const MarkNotificationReadUseCase(this._repository);

  final NotificationsRepository _repository;

  Future<void> call(String id) => _repository.markRead(id);
}

class MarkAllNotificationsReadUseCase {
  const MarkAllNotificationsReadUseCase(this._repository);

  final NotificationsRepository _repository;

  Future<void> call() => _repository.markAllRead();
}

class RespondNotificationUseCase {
  const RespondNotificationUseCase(this._repository);

  final NotificationsRepository _repository;

  Future<void> call(String id, {required bool accept}) => _repository.respond(id, accept: accept);
}
