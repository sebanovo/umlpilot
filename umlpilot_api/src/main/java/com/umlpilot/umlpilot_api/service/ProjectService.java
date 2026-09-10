package com.umlpilot.umlpilot_api.service;

import com.umlpilot.umlpilot_api.dto.CollaboratorResponse;
import com.umlpilot.umlpilot_api.dto.CreateProjectRequest;
import com.umlpilot.umlpilot_api.dto.ProjectResponse;
import com.umlpilot.umlpilot_api.model.Collaborator;
import com.umlpilot.umlpilot_api.model.Project;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public ProjectResponse createProject(CreateProjectRequest request, String userId) {
        Project project = new Project(request.name(), request.description(), userId);
        Project saved = projectRepository.save(project);

        Collaborator collaborator = new Collaborator(saved.getId(), userId, "owner");
        collaborator.setInvitationStatus("accepted");
        collaboratorRepository.save(collaborator);

        return toResponse(saved);
    }

    public List<ProjectResponse> getUserProjects(String userId) {
        return projectRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProject(String projectId, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId);
        if (project == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(String projectId, String name, String description, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId);
        if (project == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (collaborator == null || (!collaborator.getRole().equals("owner") && !collaborator.getRole().equals("editor"))) {
            throw new IllegalArgumentException("Sin permisos para actualizar");
        }

        if (name != null) project.setName(name);
        if (description != null) project.setDescription(description);

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(String projectId, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId);
        if (project == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (collaborator == null || !collaborator.getRole().equals("owner")) {
            throw new IllegalArgumentException("Solo el owner puede eliminar el proyecto");
        }

        projectRepository.delete(project);
    }

    public List<CollaboratorResponse> getCollaborators(String projectId, String userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId);
        if (project == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        return collaboratorRepository.findByProjectId(projectId).stream()
                .filter(c -> c.getInvitationStatus().equals("accepted"))
                .map(c -> {
                    User user = userRepository.findById(c.getUserId()).orElse(null);
                    if (user == null) return null;
                    return new CollaboratorResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), c.getRole(), c.getInvitationStatus());
                })
                .filter(c -> c != null)
                .toList();
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatorId(),
                project.getStatus(),
                project.getCreatedAt() != null ? project.getCreatedAt().toString() : null,
                project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null
        );
    }
}
