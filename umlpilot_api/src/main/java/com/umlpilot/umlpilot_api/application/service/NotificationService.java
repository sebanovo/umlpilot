package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.NotificationResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.NotificationId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorService collaboratorService;

    public NotificationService(NotificationRepository notificationRepository,
                               CollaboratorRepository collaboratorRepository,
                               CollaboratorService collaboratorService) {
        this.notificationRepository = notificationRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorService = collaboratorService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResult> getNotifications(String userId, boolean unreadOnly) {
        return notificationRepository.findByUserId(userId, unreadOnly).stream()
                .map(notification -> toResult(notification, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countUnread(userId);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = findOwned(notificationId, userId);
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void respondToInvitation(String notificationId, String userId, String action) {
        Notification notification = findOwned(notificationId, userId);
        if (!notification.isProjectInvitation()) {
            throw new DomainException("La notificación no admite respuesta");
        }

        String normalized = action == null ? "" : action.trim().toLowerCase();
        switch (normalized) {
            case "accept" -> collaboratorService.acceptInvitation(notification.getProjectId(), userId);
            case "reject" -> collaboratorService.rejectInvitation(notification.getProjectId(), userId);
            default -> throw new DomainException("Acción no válida. Usa 'accept' o 'reject'");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    private Notification findOwned(String notificationId, String userId) {
        return notificationRepository.findByIdAndUserId(NotificationId.from(notificationId), userId)
                .orElseThrow(() -> new DomainException("Notificación no encontrada"));
    }

    private NotificationResult toResult(Notification notification, String userId) {
        String invitationStatus = null;
        if (notification.isProjectInvitation() && notification.getProjectId() != null) {
            invitationStatus = collaboratorRepository
                    .findByProjectIdAndUserId(notification.getProjectId(), userId)
                    .map(Collaborator::getInvitationStatus)
                    .orElse(null);
        }
        return new NotificationResult(
                notification.getId().value(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getProjectId(),
                notification.getRole(),
                notification.isRead(),
                toIso(notification.getReadAt()),
                toIso(notification.getCreatedAt()),
                invitationStatus
        );
    }

    private static String toIso(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }
}
