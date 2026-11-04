package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Collaborator {
    private final CollaboratorId id;
    private String role;
    private String invitationStatus;
    private final LocalDateTime invitationDate;
    private LocalDateTime acceptanceDate;

    private Collaborator(CollaboratorId id, String role, String invitationStatus, LocalDateTime invitationDate, LocalDateTime acceptanceDate) {
        this.id = id;
        this.role = role;
        this.invitationStatus = invitationStatus;
        this.invitationDate = invitationDate;
        this.acceptanceDate = acceptanceDate;
    }

    public static Collaborator create(String projectId, String userId, String role) {
        if (!role.equals("owner") && !role.equals("editor") && !role.equals("viewer")) throw new DomainException("Invalid role");
        return new Collaborator(new CollaboratorId(projectId, userId), role, "pending", LocalDateTime.now(), null);
    }

    public static Collaborator createAccepted(String projectId, String userId, String role) {
        Collaborator c = create(projectId, userId, role);
        c.invitationStatus = "accepted";
        c.acceptanceDate = LocalDateTime.now();
        return c;
    }

    public static Collaborator reconstitute(CollaboratorId id, String role, String invitationStatus, LocalDateTime invitationDate, LocalDateTime acceptanceDate) {
        return new Collaborator(id, role, invitationStatus, invitationDate, acceptanceDate);
    }

    public void accept() { this.invitationStatus = "accepted"; this.acceptanceDate = LocalDateTime.now(); }
    public void reject() { this.invitationStatus = "rejected"; }
    public void changeRole(String role) { this.role = role; }

    public String getProjectId() { return id.projectId(); }
    public String getUserId() { return id.userId(); }
    public String getRole() { return role; }
    public String getInvitationStatus() { return invitationStatus; }
    public LocalDateTime getInvitationDate() { return invitationDate; }
    public LocalDateTime getAcceptanceDate() { return acceptanceDate; }
}
