package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.NotificationId;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    public NotificationPersistenceAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity(
                notification.getId().value(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getProjectId(),
                notification.getRole(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<Notification> findByUserId(String userId, boolean unreadOnly) {
        List<NotificationJpaEntity> entities = unreadOnly
                ? jpa.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                : jpa.findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Notification> findByIdAndUserId(NotificationId id, String userId) {
        return jpa.findByIdAndUserId(id.value(), userId).map(this::toDomain);
    }

    @Override
    public long countUnread(String userId) {
        return jpa.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<NotificationJpaEntity> unread = jpa.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        if (unread.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(entity -> entity.setReadAt(now));
        jpa.saveAll(unread);
    }

    @Override
    public void markInvitationAsRead(String userId, String projectId) {
        jpa.findFirstByUserIdAndProjectIdAndTypeOrderByCreatedAtDesc(
                        userId, projectId, Notification.TYPE_PROJECT_INVITATION)
                .filter(entity -> entity.getReadAt() == null)
                .ifPresent(entity -> {
                    entity.setReadAt(LocalDateTime.now());
                    jpa.save(entity);
                });
    }

    @Override
    public void deleteInvitation(String userId, String projectId) {
        jpa.deleteByUserIdAndProjectIdAndType(userId, projectId, Notification.TYPE_PROJECT_INVITATION);
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstitute(
                NotificationId.from(entity.getId()),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getProjectId(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }
}
