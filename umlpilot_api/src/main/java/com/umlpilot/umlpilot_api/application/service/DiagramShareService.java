package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.AttributeResult;
import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.LiteralResult;
import com.umlpilot.umlpilot_api.application.dto.MethodResult;
import com.umlpilot.umlpilot_api.application.dto.MultiplicityResult;
import com.umlpilot.umlpilot_api.application.dto.PublicDiagramView;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.application.dto.ShareJoinResult;
import com.umlpilot.umlpilot_api.application.dto.ShareLinkResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import com.umlpilot.umlpilot_api.domain.model.DiagramShareLink;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.domain.repository.DiagramShareLinkRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compartir como en Figma: un enlace que permite previsualizar y unirse al proyecto
 * con un rol (lector/editor), o invitaciones por email con notificación para aceptar.
 */
@Service
public class DiagramShareService {

    private final DiagramShareLinkRepository shareLinkRepository;
    private final ProjectRepository projectRepository;
    private final DiagramRepository diagramRepository;
    private final ProjectAccessService accessService;
    private final CollaboratorService collaboratorService;
    private final ElementService elementService;
    private final RelationshipService relationshipService;

    public DiagramShareService(DiagramShareLinkRepository shareLinkRepository,
                               ProjectRepository projectRepository,
                               DiagramRepository diagramRepository,
                               ProjectAccessService accessService,
                               CollaboratorService collaboratorService,
                               ElementService elementService,
                               RelationshipService relationshipService) {
        this.shareLinkRepository = shareLinkRepository;
        this.projectRepository = projectRepository;
        this.diagramRepository = diagramRepository;
        this.accessService = accessService;
        this.collaboratorService = collaboratorService;
        this.elementService = elementService;
        this.relationshipService = relationshipService;
    }

    @Transactional(readOnly = true)
    public ShareLinkResult getStatus(String projectId, String diagramId, String userId) {
        accessService.requireDiagramEditor(projectId, diagramId, userId);
        return shareLinkRepository.findByDiagramId(diagramId)
                .map(this::toResult)
                .orElseGet(() -> new ShareLinkResult(false, null, null, DiagramShareLink.ROLE_VIEWER));
    }

    /**
     * Activa (o reactiva) el enlace. {@code role} es opcional: al crear se usa lector;
     * si se envía, también actualiza el rol del enlace existente.
     */
    @Transactional
    public ShareLinkResult enable(String projectId, String diagramId, String userId, String role) {
        accessService.requireDiagramEditor(projectId, diagramId, userId);
        DiagramShareLink link = shareLinkRepository.findByDiagramId(diagramId)
                .map(existing -> {
                    if (role != null && !role.isBlank()) {
                        existing.changeRole(role);
                    }
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> DiagramShareLink.create(projectId, diagramId, userId, role));
        return toResult(shareLinkRepository.save(link));
    }

    @Transactional
    public void disable(String projectId, String diagramId, String userId) {
        accessService.requireDiagramEditor(projectId, diagramId, userId);
        shareLinkRepository.findByDiagramId(diagramId).ifPresent(link -> {
            link.revoke();
            shareLinkRepository.save(link);
        });
    }

    /** El usuario autenticado se une al proyecto con el rol del enlace. Idempotente. */
    @Transactional
    public ShareJoinResult join(String token, String userId) {
        DiagramShareLink link = shareLinkRepository.findActiveByToken(token)
                .orElseThrow(() -> new DomainException("El enlace no es válido o fue desactivado"));
        String role = collaboratorService.joinByLink(link.getProjectId(), userId, link.getLinkRole());
        return new ShareJoinResult(link.getProjectId(), link.getDiagramId(), role);
    }

    /** Vista de solo lectura por token. No requiere autenticación. */
    @Transactional(readOnly = true)
    public PublicDiagramView getPublicView(String token) {
        DiagramShareLink link = shareLinkRepository.findActiveByToken(token)
                .orElseThrow(() -> new DomainException("El enlace no es válido o fue desactivado"));

        Project project = projectRepository.findById(ProjectId.from(link.getProjectId()))
                .orElseThrow(() -> new DomainException("El enlace no es válido"));
        Diagram diagram = diagramRepository.findById(DiagramId.from(link.getDiagramId()))
                .orElseThrow(() -> new DomainException("El enlace no es válido"));

        String diagramId = diagram.getId().value();
        List<ElementResult> elements = elementService.getElements(diagramId);
        List<RelationshipResult> relationships = relationshipService.getRelationships(diagramId);

        Map<String, List<AttributeResult>> attributes = new LinkedHashMap<>();
        Map<String, List<MethodResult>> methods = new LinkedHashMap<>();
        Map<String, List<LiteralResult>> literals = new LinkedHashMap<>();
        for (ElementResult element : elements) {
            attributes.put(element.id(), elementService.getAttributes(element.id()));
            methods.put(element.id(), elementService.getMethods(element.id()));
            literals.put(element.id(), elementService.getLiterals(element.id()));
        }

        Map<String, List<MultiplicityResult>> multiplicities = new LinkedHashMap<>();
        for (RelationshipResult relationship : relationships) {
            multiplicities.put(relationship.id(), relationshipService.getMultiplicities(relationship.id()));
        }

        return new PublicDiagramView(
                link.getProjectId(),
                link.getDiagramId(),
                link.getLinkRole(),
                project.getName(),
                diagram.getName(),
                diagram.getType(),
                elements,
                relationships,
                attributes,
                methods,
                literals,
                multiplicities
        );
    }

    private ShareLinkResult toResult(DiagramShareLink link) {
        if (!link.isActive()) {
            return new ShareLinkResult(false, null, null, link.getLinkRole());
        }
        return new ShareLinkResult(true, link.getToken(), "/share/" + link.getToken(), link.getLinkRole());
    }
}
