package com.umlpilot.umlpilot_api.service;

import com.umlpilot.umlpilot_api.dto.CollaboratorResponse;
import com.umlpilot.umlpilot_api.dto.InviteCollaboratorRequest;
import com.umlpilot.umlpilot_api.model.Collaborator;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public CollaboratorService(CollaboratorRepository collaboratorRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CollaboratorResponse inviteCollaborator(String projectId, InviteCollaboratorRequest request, String userId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        var inviter = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (inviter == null || (!inviter.getRole().equals("owner") && !inviter.getRole().equals("editor"))) {
            throw new IllegalArgumentException("Sin permisos para invitar");
        }

        var userToInvite = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (userToInvite.getId().equals(userId)) {
            throw new IllegalArgumentException("No puedes invitarte a ti mismo");
        }

        var existing = collaboratorRepository.findByProjectIdAndUserId(projectId, userToInvite.getId());
        if (existing != null) {
            throw new IllegalArgumentException("El usuario ya es colaborador");
        }

        if (!request.role().equals("editor") && !request.role().equals("viewer")) {
            throw new IllegalArgumentException("Rol inválido");
        }

        Collaborator collaborator = new Collaborator(projectId, userToInvite.getId(), request.role());
        collaboratorRepository.save(collaborator);

        return new CollaboratorResponse(
                userToInvite.getId(),
                userToInvite.getEmail(),
                userToInvite.getFirstName(),
                userToInvite.getLastName(),
                collaborator.getRole(),
                collaborator.getInvitationStatus()
        );
    }

    public List<CollaboratorResponse> getCollaborators(String projectId, String userId) {
        var project = projectRepository.findByIdAndUserId(projectId, userId);
        if (project == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        return collaboratorRepository.findByProjectId(projectId).stream()
                .map(c -> {
                    User user = userRepository.findById(c.getUserId()).orElse(null);
                    if (user == null) return null;
                    return new CollaboratorResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), c.getRole(), c.getInvitationStatus());
                })
                .filter(c -> c != null)
                .toList();
    }

    @Transactional
    public void acceptInvitation(String projectId, String userId) {
        var collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId);
        if (collaborator == null || !collaborator.getInvitationStatus().equals("pending")) {
            throw new IllegalArgumentException("Invitación no encontrada");
        }
        collaborator.setInvitationStatus("accepted");
        collaborator.setAcceptanceDate(LocalDateTime.now());
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void rejectInvitation(String projectId, String userId) {
        var collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId);
        if (collaborator == null || !collaborator.getInvitationStatus().equals("pending")) {
            throw new IllegalArgumentException("Invitación no encontrada");
        }
        collaborator.setInvitationStatus("rejected");
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void changeRole(String projectId, String targetUserId, String newRole, String userId) {
        var owner = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (owner == null || !owner.getRole().equals("owner")) {
            throw new IllegalArgumentException("Solo el owner puede cambiar roles");
        }

        var collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, targetUserId);
        if (collaborator == null) {
            throw new IllegalArgumentException("Colaborador no encontrado");
        }

        collaborator.setRole(newRole);
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void removeCollaborator(String projectId, String targetUserId, String userId) {
        var requester = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (requester == null) {
            throw new IllegalArgumentException("Sin permisos");
        }

        if (!requester.getRole().equals("owner") && !targetUserId.equals(userId)) {
            throw new IllegalArgumentException("Sin permisos para eliminar a otro colaborador");
        }

        if (requester.getRole().equals("owner") && targetUserId.equals(userId)) {
            throw new IllegalArgumentException("El owner no puede eliminarse a sí mismo");
        }

        var collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, targetUserId);
        if (collaborator == null) {
            throw new IllegalArgumentException("Colaborador no encontrado");
        }

        collaboratorRepository.delete(collaborator);
    }
}
