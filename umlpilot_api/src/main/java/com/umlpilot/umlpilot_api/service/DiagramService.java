package com.umlpilot.umlpilot_api.service;

import com.umlpilot.umlpilot_api.dto.CreateDiagramRequest;
import com.umlpilot.umlpilot_api.dto.DiagramResponse;
import com.umlpilot.umlpilot_api.model.Diagram;
import com.umlpilot.umlpilot_api.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiagramService {

    private final DiagramRepository diagramRepository;
    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;

    public DiagramService(DiagramRepository diagramRepository, ProjectRepository projectRepository, CollaboratorRepository collaboratorRepository) {
        this.diagramRepository = diagramRepository;
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    @Transactional
    public DiagramResponse createDiagram(String projectId, CreateDiagramRequest request, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        var collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (collaborator == null || (!collaborator.getRole().equals("owner") && !collaborator.getRole().equals("editor"))) {
            throw new IllegalArgumentException("Sin permisos para crear diagramas");
        }

        Diagram diagram = new Diagram(projectId, request.name(), request.type());
        if (request.description() != null) {
            diagram.setDescription(request.description());
        }

        return toResponse(diagramRepository.save(diagram));
    }

    public List<DiagramResponse> getDiagrams(String projectId, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        return diagramRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DiagramResponse getDiagram(String projectId, String diagramId, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));

        if (!diagram.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Diagrama no pertenece al proyecto");
        }

        return toResponse(diagram);
    }

    @Transactional
    public DiagramResponse updateDiagram(String projectId, String diagramId, String name, String description, String canvasData, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));

        if (!diagram.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Diagrama no pertenece al proyecto");
        }

        if (diagram.getIsLocked() && !diagram.getLockedByUserId().equals(userId)) {
            throw new IllegalArgumentException("El diagrama está bloqueado por otro usuario");
        }

        if (name != null) diagram.setName(name);
        if (description != null) diagram.setDescription(description);
        if (canvasData != null) {
            diagram.setCanvasData(canvasData);
            diagram.setVersion(diagram.getVersion() + 1);
        }

        return toResponse(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResponse lockDiagram(String projectId, String diagramId, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));

        if (!diagram.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Diagrama no pertenece al proyecto");
        }

        if (diagram.getIsLocked() && !diagram.getLockedByUserId().equals(userId)) {
            throw new IllegalArgumentException("El diagrama está bloqueado por otro usuario");
        }

        diagram.setIsLocked(true);
        diagram.setLockedByUserId(userId);

        return toResponse(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResponse unlockDiagram(String projectId, String diagramId, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));

        if (!diagram.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Diagrama no pertenece al proyecto");
        }

        if (!diagram.getIsLocked() || !diagram.getLockedByUserId().equals(userId)) {
            throw new IllegalArgumentException("No tienes el diagrama bloqueado");
        }

        diagram.setIsLocked(false);
        diagram.setLockedByUserId(null);

        return toResponse(diagramRepository.save(diagram));
    }

    @Transactional
    public void deleteDiagram(String projectId, String diagramId, String userId) {
        if (projectRepository.findByIdAndUserId(projectId, userId) == null) {
            throw new IllegalArgumentException("Proyecto no encontrado");
        }

        var collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId);
        if (collaborator == null || (!collaborator.getRole().equals("owner") && !collaborator.getRole().equals("editor"))) {
            throw new IllegalArgumentException("Sin permisos para eliminar diagramas");
        }

        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado"));

        if (!diagram.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Diagrama no pertenece al proyecto");
        }

        diagramRepository.delete(diagram);
    }

    private DiagramResponse toResponse(Diagram diagram) {
        return new DiagramResponse(
                diagram.getId(),
                diagram.getProjectId(),
                diagram.getName(),
                diagram.getType(),
                diagram.getDescription(),
                diagram.getCanvasData(),
                diagram.getVersion(),
                diagram.getIsLocked(),
                diagram.getLockedByUserId(),
                diagram.getCreatedAt() != null ? diagram.getCreatedAt().toString() : null,
                diagram.getUpdatedAt() != null ? diagram.getUpdatedAt().toString() : null
        );
    }
}
