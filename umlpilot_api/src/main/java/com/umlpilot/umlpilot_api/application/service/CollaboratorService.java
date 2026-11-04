package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CollaboratorResult;
import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
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
    private final NotificationRepository notificationRepository;

    public CollaboratorService(CollaboratorRepository collaboratorRepository,
                               ProjectRepository projectRepository,
                               UserRepository userRepository,
                               NotificationRepository notificationRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public CollaboratorResult inviteCollaborator(InviteCollaboratorCommand command) {
        validateMemberRole(command.role());

        Project project = projectRepository.findById(ProjectId.from(command.projectId()))
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        var inviter = collaboratorRepository.findAcceptedCollaborator(command.projectId(), command.inviterId())
                .orElseThrow(() -> new DomainException("Sin permisos para invitar"));

        if (!inviter.getRole().equals("owner") && !inviter.getRole().equals("editor")) {
            throw new DomainException("Sin permisos para invitar");
        }

        String email = command.email() == null ? "" : command.email().trim();
        User userToInvite = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("El usuario no está registrado en UMLPilot"));

        if (userToInvite.getId().value().equals(command.inviterId())) {
            throw new DomainException("No puedes invitarte a ti mismo");
        }

        var existing = collaboratorRepository
                .findByProjectIdAndUserId(command.projectId(), userToInvite.getId().value());

        if (existing.isPresent()) {
            Collaborator collaborator = existing.get();
            if (collaborator.getInvitationStatus().equals("accepted")) {
                throw new DomainException("El usuario ya es colaborador");
            }
            if (collaborator.getInvitationStatus().equals("pending")) {
                throw new DomainException("El usuario ya tiene una invitación pendiente");
            }
            collaborator.reinvite(command.role());
            collaboratorRepository.save(collaborator);
            notifyInvitation(project, userToInvite, command.inviterId(), command.role());
            return toResult(userToInvite, collaborator);
        }

        Collaborator collaborator = Collaborator.create(command.projectId(), userToInvite.getId().value(), command.role());
        collaboratorRepository.save(collaborator);
        notifyInvitation(project, userToInvite, command.inviterId(), command.role());

        return toResult(userToInvite, collaborator);
    }

    private void notifyInvitation(Project project, User invitee, String inviterId, String role) {
        String inviterName = userRepository.findById(UserId.from(inviterId))
                .map(user -> (user.getFirstName() + " " + user.getLastName()).trim())
                .filter(name -> !name.isBlank())
                .orElse("Un usuario");

        notificationRepository.save(Notification.projectInvitation(
                invitee.getId().value(),
                project.getId().value(),
                project.getName(),
                inviterName,
                role
        ));
    }

    private CollaboratorResult toResult(User user, Collaborator collaborator) {
        return new CollaboratorResult(user.getId().value(), user.getEmail(),
                user.getFirstName(), user.getLastName(), collaborator.getRole(), collaborator.getInvitationStatus());
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
        notificationRepository.markInvitationAsRead(userId, projectId);
    }

    @Transactional
    public void rejectInvitation(String projectId, String userId) {
        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Invitación no encontrada"));
        if (!collaborator.getInvitationStatus().equals("pending")) throw new DomainException("Invitación no encontrada");
        collaborator.reject();
        collaboratorRepository.save(collaborator);
        notificationRepository.markInvitationAsRead(userId, projectId);
    }

    @Transactional
    public void changeRole(String projectId, String targetUserId, String newRole, String userId) {
        Collaborator owner = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Solo el owner puede cambiar roles"));
        if (!owner.getRole().equals("owner")) throw new DomainException("Solo el owner puede cambiar roles");

        Collaborator collaborator = collaboratorRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new DomainException("Colaborador no encontrado"));
        if (collaborator.getRole().equals("owner")) {
            throw new DomainException("El rol del owner no se puede modificar");
        }
        collaborator.changeRole(newRole);
        collaboratorRepository.save(collaborator);
    }

    private static void validateMemberRole(String role) {
        if (!"editor".equals(role) && !"viewer".equals(role)) {
            throw new DomainException("Rol inválido. Usa 'editor' o 'viewer'");
        }
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

        if (collaborator.getInvitationStatus().equals("pending")) {
            notificationRepository.deleteInvitation(targetUserId, projectId);
        }

        collaboratorRepository.delete(new com.umlpilot.umlpilot_api.domain.model.CollaboratorId(projectId, targetUserId));
    }
}
