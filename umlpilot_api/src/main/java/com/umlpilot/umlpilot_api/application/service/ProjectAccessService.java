package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de acceso a proyectos y diagramas. Es el único punto que decide si un
 * usuario puede leer o editar; cualquier adaptador (REST, WebSocket, compartir)
 * debe pasar por aquí.
 */
@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final DiagramRepository diagramRepository;

    public ProjectAccessService(ProjectRepository projectRepository,
                                CollaboratorRepository collaboratorRepository,
                                DiagramRepository diagramRepository) {
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.diagramRepository = diagramRepository;
    }

    /** El usuario debe ser el creador o un colaborador con invitación aceptada. */
    @Transactional(readOnly = true)
    public Project requireMember(String projectId, String userId) {
        if (userId == null) {
            throw new DomainException("No autenticado");
        }
        Project project = projectRepository.findById(ProjectId.from(projectId))
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));
        if (project.getCreatorId().value().equals(userId)) {
            return project;
        }
        collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("No tienes acceso a este proyecto"));
        return project;
    }

    /** Además de ser miembro, debe tener rol owner o editor. */
    @Transactional(readOnly = true)
    public void requireEditor(String projectId, String userId) {
        Project project = requireMember(projectId, userId);
        if (project.getCreatorId().value().equals(userId)) {
            return;
        }
        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("No tienes acceso a este proyecto"));
        if (!"owner".equals(collaborator.getRole()) && !"editor".equals(collaborator.getRole())) {
            throw new DomainException("No tienes permisos para editar este proyecto");
        }
    }

    /** El usuario debe ser el creador o un colaborador aceptado con rol owner. */
    @Transactional(readOnly = true)
    public void requireOwner(String projectId, String userId) {
        Project project = requireMember(projectId, userId);
        if (project.getCreatorId().value().equals(userId)) {
            return;
        }
        Collaborator collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("No tienes acceso a este proyecto"));
        if (!"owner".equals(collaborator.getRole())) {
            throw new DomainException("Solo el owner puede realizar esta acción");
        }
    }

    /** Comprueba que el diagrama existe y pertenece al proyecto. */
    @Transactional(readOnly = true)
    public Diagram requireDiagramMember(String projectId, String diagramId, String userId) {
        requireMember(projectId, userId);
        return requireDiagramInProject(projectId, diagramId);
    }

    /** Miembro con rol de edición sobre un diagrama concreto del proyecto. */
    @Transactional(readOnly = true)
    public Diagram requireDiagramEditor(String projectId, String diagramId, String userId) {
        requireEditor(projectId, userId);
        return requireDiagramInProject(projectId, diagramId);
    }

    /** Variante para canales donde solo se conoce el diagrama (WebSocket). */
    @Transactional(readOnly = true)
    public Diagram requireDiagramMemberByDiagramId(String diagramId, String userId) {
        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));
        requireMember(diagram.getProjectId().value(), userId);
        return diagram;
    }

    private Diagram requireDiagramInProject(String projectId, String diagramId) {
        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));
        if (!diagram.getProjectId().value().equals(projectId)) {
            throw new DomainException("El diagrama no pertenece al proyecto");
        }
        return diagram;
    }
}
