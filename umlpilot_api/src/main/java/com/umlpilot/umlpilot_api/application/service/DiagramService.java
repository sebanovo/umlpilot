package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CreateDiagramCommand;
import com.umlpilot.umlpilot_api.application.dto.DiagramResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
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
    public DiagramResult createDiagram(CreateDiagramCommand command) {
        projectRepository.findByIdAndUserId(command.projectId(), /* userId needed */ null)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Diagram diagram = Diagram.create(command.projectId(), command.name(), command.type());
        if (command.description() != null) diagram.updateDescription(command.description());

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional(readOnly = true)
    public List<DiagramResult> getDiagrams(String projectId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        return diagramRepository.findByProjectId(projectId).stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiagramResult getDiagram(String projectId, String diagramId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));

        if (!diagram.getProjectId().value().equals(projectId)) throw new DomainException("Diagrama no pertenece al proyecto");
        return toResult(diagram);
    }

    @Transactional
    public DiagramResult updateDiagram(String projectId, String diagramId, String name, String description, String canvasData, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));

        if (!diagram.getProjectId().value().equals(projectId)) throw new DomainException("Diagrama no pertenece al proyecto");
        if (diagram.getIsLocked() && !diagram.getLockedByUserId().equals(userId)) throw new DomainException("El diagrama está bloqueado por otro usuario");

        diagram.updateName(name);
        diagram.updateDescription(description);
        diagram.updateCanvasData(canvasData);

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResult lockDiagram(String projectId, String diagramId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));

        if (!diagram.getProjectId().value().equals(projectId)) throw new DomainException("Diagrama no pertenece al proyecto");
        diagram.lock(userId);

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResult unlockDiagram(String projectId, String diagramId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));

        if (!diagram.getProjectId().value().equals(projectId)) throw new DomainException("Diagrama no pertenece al proyecto");
        diagram.unlock(userId);

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public void deleteDiagram(String projectId, String diagramId, String userId) {
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException("Proyecto no encontrado"));

        var collaborator = collaboratorRepository.findAcceptedCollaborator(projectId, userId)
                .orElseThrow(() -> new DomainException("Sin permisos para eliminar diagramas"));

        if (!collaborator.getRole().equals("owner") && !collaborator.getRole().equals("editor")) {
            throw new DomainException("Sin permisos para eliminar diagramas");
        }

        Diagram diagram = diagramRepository.findById(DiagramId.from(diagramId))
                .orElseThrow(() -> new DomainException("Diagrama no encontrado"));

        if (!diagram.getProjectId().value().equals(projectId)) throw new DomainException("Diagrama no pertenece al proyecto");
        diagramRepository.delete(diagram.getId());
    }

    private DiagramResult toResult(Diagram diagram) {
        return new DiagramResult(
                diagram.getId().value(),
                diagram.getProjectId().value(),
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
