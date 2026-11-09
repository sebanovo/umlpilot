import 'package:flutter/foundation.dart';

import '../../../../core/errors/failures.dart';
import '../../domain/entities/app_notification.dart';
import '../../domain/usecases/notifications_usecases.dart';

class NotificationsViewModel extends ChangeNotifier {
  NotificationsViewModel({
    required GetNotificationsUseCase getNotifications,
    required GetUnreadCountUseCase getUnreadCount,
    required MarkNotificationReadUseCase markRead,
    required MarkAllNotificationsReadUseCase markAllRead,
    required RespondNotificationUseCase respond,
  })  : _getNotifications = getNotifications,
        _getUnreadCount = getUnreadCount,
        _markRead = markRead,
        _markAllRead = markAllRead,
        _respond = respond;

  final GetNotificationsUseCase _getNotifications;
  final GetUnreadCountUseCase _getUnreadCount;
  final MarkNotificationReadUseCase _markRead;
  final MarkAllNotificationsReadUseCase _markAllRead;
  final RespondNotificationUseCase _respond;

  List<AppNotification> _notifications = const [];
  int _unread = 0;
  bool _loading = false;
  String? _error;

  List<AppNotification> get notifications => _notifications;
  int get unread => _unread;
  bool get loading => _loading;
  String? get error => _error;

  Future<void> refreshCount() async {
    try {
      _unread = await _getUnreadCount();
      notifyListeners();
    } catch (_) {
      // El contador no debe romper la pantalla.
    }
  }

  Future<void> load() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _notifications = await _getNotifications();
      _unread = _notifications.where((item) => !item.read).length;
    } catch (e) {
      _error = messageForError(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> markRead(String id) async {
    try {
      await _markRead(id);
      _notifications = _notifications
          .map((item) => item.id == id
              ? AppNotification(
                  id: item.id,
                  type: item.type,
                  title: item.title,
                  message: item.message,
                  projectId: item.projectId,
                  role: item.role,
                  read: true,
                  readAt: item.readAt,
                  createdAt: item.createdAt,
                  invitationStatus: item.invitationStatus,
                )
              : item)
          .toList();
      _unread = _notifications.where((item) => !item.read).length;
      notifyListeners();
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
    }
  }

  Future<void> markAllRead() async {
    try {
      await _markAllRead();
      _notifications = _notifications
          .map((item) => AppNotification(
                id: item.id,
                type: item.type,
                title: item.title,
                message: item.message,
                projectId: item.projectId,
                role: item.role,
                read: true,
                readAt: item.readAt,
                createdAt: item.createdAt,
                invitationStatus: item.invitationStatus,
              ))
          .toList();
      _unread = 0;
      notifyListeners();
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
    }
  }

  Future<bool> respond(String id, {required bool accept}) async {
    try {
      await _respond(id, accept: accept);
      _notifications = _notifications
          .map((item) => item.id == id
              ? AppNotification(
                  id: item.id,
                  type: item.type,
                  title: item.title,
                  message: item.message,
                  projectId: item.projectId,
                  role: item.role,
                  read: true,
                  readAt: item.readAt,
                  createdAt: item.createdAt,
                  invitationStatus: accept ? 'accepted' : 'rejected',
                )
              : item)
          .toList();
      _unread = _notifications.where((item) => !item.read).length;
      notifyListeners();
      return true;
    } catch (e) {
      _error = messageForError(e);
      notifyListeners();
      return false;
    }
  }

  void clearError() {
    if (_error == null) return;
    _error = null;
    notifyListeners();
  }
}
