package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.NotificationId;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    List<Notification> findByUserId(String userId, boolean unreadOnly);

    Optional<Notification> findByIdAndUserId(NotificationId id, String userId);

    long countUnread(String userId);

    void markAllAsRead(String userId);

    /** Marca como leída la invitación pendiente de un proyecto para un usuario. */
    void markInvitationAsRead(String userId, String projectId);

    /** Elimina las notificaciones de invitación (p. ej. al cancelar una invitación pendiente). */
    void deleteInvitation(String userId, String projectId);
}
