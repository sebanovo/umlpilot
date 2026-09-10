package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "collaborator")
@IdClass(CollaboratorJpaEntity.CollaboratorId.class)
public class CollaboratorJpaEntity {

    @Embeddable
    public static class CollaboratorId implements Serializable {
        @Column(name = "project_id") private String projectId;
        @Column(name = "user_id") private String userId;

        public CollaboratorId() {}
        public CollaboratorId(String projectId, String userId) { this.projectId = projectId; this.userId = userId; }

        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof CollaboratorId that)) return false; return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId); }
        @Override public int hashCode() { return Objects.hash(projectId, userId); }
    }

    @Id @Column(name = "project_id") private String projectId;
    @Id @Column(name = "user_id") private String userId;
    @Column(nullable = false) private String role;
    @Column(name = "invitation_status", nullable = false) private String invitationStatus = "pending";
    @Column(name = "invitation_date", nullable = false, updatable = false) private LocalDateTime invitationDate;
    @Column(name = "acceptance_date") private LocalDateTime acceptanceDate;

    @PrePersist
    protected void onCreate() { this.invitationDate = LocalDateTime.now(); }

    public CollaboratorJpaEntity() {}
    public CollaboratorJpaEntity(String projectId, String userId, String role, String invitationStatus, LocalDateTime invitationDate, LocalDateTime acceptanceDate) {
        this.projectId = projectId; this.userId = userId; this.role = role; this.invitationStatus = invitationStatus; this.invitationDate = invitationDate; this.acceptanceDate = acceptanceDate;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(String status) { this.invitationStatus = status; }
    public LocalDateTime getInvitationDate() { return invitationDate; }
    public LocalDateTime getAcceptanceDate() { return acceptanceDate; }
    public void setAcceptanceDate(LocalDateTime date) { this.acceptanceDate = date; }
}
