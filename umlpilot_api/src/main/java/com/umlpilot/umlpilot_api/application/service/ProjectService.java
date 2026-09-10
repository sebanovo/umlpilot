package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CollaboratorResult;
import com.umlpilot.umlpilot_api.application.dto.CreateProjectCommand;
import com.umlpilot.umlpilot_api.application.dto.ProjectResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, CollaboratorRepository collaboratorRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResult createProject(CreateProjectCommand command) {
        Project project = Project.create(command.name(), command.description(), command.creatorId());
        Project saved = projectRepository.save(project);

        Collaborator collaborator = Collaborator.createAccepted(saved.getId().value(), command.creatorId(), "owner");
        collaboratorRepository.save(collaborator);

        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResult> getUserProjects(String userId) {
        return projectRepository.findByUserId(userId).stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResult getProject(String projectId, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));
        return toResult(project);
    }

    @Transactional
    public ProjectResult updateProject(String projectId, String name, String description, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Sin permisos para actualizar"));

        if (!collaborator.getRole().equals("owner") && !collaborator.getRole().equals("editor")) {
            throw new DomainException("Sin permisos para actualizar");
        }

        project.updateName(name);
        project.updateDescription(description);

        return toResult(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(String projectId, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Solo el owner puede eliminar el proyecto"));

        if (!collaborator.getRole().equals("owner")) {
            throw new DomainException("Solo el owner puede eliminar el proyecto");
        }

        projectRepository.delete(project.getId());
    }

    @Transactional(readOnly = true)
    public List<CollaboratorResult> getCollaborators(String projectId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        return collaboratorRepository.findByProjectId(projectId).stream()
                .filter(c -> c.getInvitationStatus().equals("accepted"))
                .map(c -> {
                    var user = userRepository.findById(UserId.from(c.getUserId())).orElse(null);
                    if (user == null) return null;
                    return new CollaboratorResult(user.getId().value(), user.getEmail(), user.getFirstName(), user.getLastName(), c.getRole(), c.getInvitationStatus());
                })
                .filter(c -> c != null)
                .toList();
    }

    private ProjectResult toResult(Project project) {
        return new ProjectResult(
                project.getId().value(),
                project.getName(),
                project.getDescription(),
                project.getCreatorId().value(),
                project.getStatus(),
                project.getCreatedAt() != null ? project.getCreatedAt().toString() : null,
                project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null
        );
    }
}
