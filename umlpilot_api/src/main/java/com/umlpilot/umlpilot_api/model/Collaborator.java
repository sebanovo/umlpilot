package com.umlpilot.umlpilot_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collaborator")
@IdClass(CollaboratorId.class)
public class Collaborator {

    @Id
    @Column(name = "project_id")
    private String projectId;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String role;

    @Column(name = "invitation_status", nullable = false)
    private String invitationStatus = "pending";

    @Column(name = "invitation_date", nullable = false, updatable = false)
    private LocalDateTime invitationDate;

    @Column(name = "acceptance_date")
    private LocalDateTime acceptanceDate;

    @PrePersist
    protected void onCreate() {
        this.invitationDate = LocalDateTime.now();
    }

    public Collaborator() {}

    public Collaborator(String projectId, String userId, String role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    public String getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(String status) { this.invitationStatus = status; }
    public LocalDateTime getInvitationDate() { return invitationDate; }
    public LocalDateTime getAcceptanceDate() { return acceptanceDate; }
    public void setAcceptanceDate(LocalDateTime date) { this.acceptanceDate = date; }
}
