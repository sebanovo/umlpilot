package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CollaboratorResult;
import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CollaboratorResult inviteCollaborator(InviteCollaboratorCommand command) {
        projectRepository.findById(com.umlpilot.umlpilot_api.domain.model.ProjectId.from(command.projectId()))
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        var inviter = collaboratorRepository.findAcceptedCollaborator(command.projectId(), command.inviterId())
                .orElseThrow(() -> new DomainException("Sin permisos para invitar"));

        if (!inviter.getRole().equals("owner") && !inviter.getRole().equals("editor")) {
            throw new DomainException("Sin permisos para invitar");
        }

        User userToInvite = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new DomainException("Usuario no encontrado"));

        if (userToInvite.getId().value().equals(command.inviterId())) {
            throw new DomainException("No puedes invitarte a ti mismo");
        }

        if (collaboratorRepository.findByProjectIdAndUserId(command.projectId(), userToInvite.getId().value()).isPresent()) {
            throw new DomainException("El usuario ya es colaborador");
        }

        Collaborator collaborator = Collaborator.create(command.projectId(), userToInvite.getId().value(), command.role());
        collaboratorRepository.save(collaborator);

        return new CollaboratorResult(userToInvite.getId().value(), userToInvite.getEmail(), userToInvite.getFirstName(), userToInvite.getLastName(), collaborator.getRole(), collaborator.getInvitationStatus());
    }

    @Transactional(readOnly = true)
    public List<CollaboratorResult> getCollaborators(String projectId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        return collaboratorRepository.findByProjectId(projectId).stream()
                .map(c -> {
                    var user = userRepository.findById(UserId.from(c.getUserId())).orElse(null);
                    if (user == null) return null;
                    return new CollaboratorResult(user.getId().value(), user.getEmail(), user.getFirstName(), user.getLastName(), c.getRole(), c.getInvitationStatus());
                })
                .filter(c -> c != null)
                .toList();
    }

    @Transactional
    public void acceptInvitation(String projectId, String userId) {
        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Invitación no encontrada"));
        if (!collaborator.getInvitationStatus().equals("pending")) throw new DomainException("Invitación no encontrada");
        collaborator.accept();
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void rejectInvitation(String projectId, String userId) {
        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Invitación no encontrada"));
        if (!collaborator.getInvitationStatus().equals("pending")) throw new DomainException("Invitación no encontrada");
        collaborator.reject();
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void changeRole(String projectId, String targetUserId, String newRole, String userId) {
        Collaborator owner = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Solo el owner puede cambiar roles"));
        if (!owner.getRole().equals("owner")) throw new DomainException("Solo el owner puede cambiar roles");

        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new DomainException("Colaborador no encontrado"));
        collaborator.changeRole(newRole);
        collaboratorRepository.save(collaborator);
    }

    @Transactional
    public void removeCollaborator(String projectId, String targetUserId, String userId) {
        Collaborator requester = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Sin permisos"));
        if (!requester.getRole().equals("owner") && !targetUserId.equals(userId)) {
            throw new DomainException("Sin permisos para eliminar a otro colaborador");
        }
        if (requester.getRole().equals("owner") && targetUserId.equals(userId)) {
            throw new DomainException("El owner no puede eliminarse a sí mismo");
        }
        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new DomainException("Colaborador no encontrado"));
        collaboratorRepository.delete(new com.umlpilot.umlpilot_api.domain.model.CollaboratorId(projectId, targetUserId));
    }
}
