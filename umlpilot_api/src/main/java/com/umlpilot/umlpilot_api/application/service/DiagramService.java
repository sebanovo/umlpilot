package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CreateDiagramCommand;
import com.umlpilot.umlpilot_api.application.dto.DiagramResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiagramService {

    private final DiagramRepository diagramRepository;
    private final ProjectAccessService accessService;

    public DiagramService(DiagramRepository diagramRepository, ProjectAccessService accessService) {
        this.diagramRepository = diagramRepository;
        this.accessService = accessService;
    }

    @Transactional
    public DiagramResult createDiagram(CreateDiagramCommand command, String userId) {
        accessService.requireEditor(command.projectId(), userId);

        Diagram diagram = Diagram.create(command.projectId(), command.name(), command.type());
        if (command.description() != null) diagram.updateDescription(command.description());

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional(readOnly = true)
    public List<DiagramResult> getDiagrams(String projectId, String userId) {
        accessService.requireMember(projectId, userId);

        return diagramRepository.findByProjectId(projectId).stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiagramResult getDiagram(String projectId, String diagramId, String userId) {
        return toResult(accessService.requireDiagramMember(projectId, diagramId, userId));
    }

    /** Valida que el usuario pueda editar el diagrama (owner/editor del proyecto). */
    @Transactional(readOnly = true)
    public void requireEditor(String projectId, String diagramId, String userId) {
        accessService.requireDiagramEditor(projectId, diagramId, userId);
    }

    @Transactional
    public DiagramResult updateDiagram(String projectId, String diagramId, String name, String description, String canvasData, String userId) {
        Diagram diagram = accessService.requireDiagramEditor(projectId, diagramId, userId);

        if (diagram.getIsLocked() && !diagram.getLockedByUserId().equals(userId)) throw new DomainException("El diagrama está bloqueado por otro usuario");

        diagram.updateName(name);
        diagram.updateDescription(description);
        diagram.updateCanvasData(canvasData);

        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResult lockDiagram(String projectId, String diagramId, String userId) {
        Diagram diagram = accessService.requireDiagramEditor(projectId, diagramId, userId);
        diagram.lock(userId);
        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public DiagramResult unlockDiagram(String projectId, String diagramId, String userId) {
        Diagram diagram = accessService.requireDiagramEditor(projectId, diagramId, userId);
        diagram.unlock(userId);
        return toResult(diagramRepository.save(diagram));
    }

    @Transactional
    public void deleteDiagram(String projectId, String diagramId, String userId) {
        Diagram diagram = accessService.requireDiagramEditor(projectId, diagramId, userId);
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
