package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Relationship {
    private final RelationshipId id;
    private final DiagramId diagramId;
    private final ElementId sourceElementId;
    private final ElementId targetElementId;
    private String relationshipType;
    private String name;
    private String direction;
    private Boolean isTemplateBinding;
    private String templateClass;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Relationship(RelationshipId id, DiagramId diagramId, ElementId sourceElementId, ElementId targetElementId, String relationshipType, String name, String direction, Boolean isTemplateBinding, String templateClass, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.diagramId = diagramId; this.sourceElementId = sourceElementId; this.targetElementId = targetElementId;
        this.relationshipType = relationshipType; this.name = name; this.direction = direction;
        this.isTemplateBinding = isTemplateBinding; this.templateClass = templateClass;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static Relationship create(String diagramId, String sourceElementId, String targetElementId, String type) {
        if (type == null || type.isBlank()) throw new DomainException("Relationship type cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new Relationship(RelationshipId.generate(), DiagramId.from(diagramId), ElementId.from(sourceElementId), ElementId.from(targetElementId), type, null, "bidirectional", false, null, now, now);
    }

    public static Relationship reconstitute(RelationshipId id, DiagramId diagramId, ElementId sourceElementId, ElementId targetElementId, String relationshipType, String name, String direction, Boolean isTemplateBinding, String templateClass, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Relationship(id, diagramId, sourceElementId, targetElementId, relationshipType, name, direction, isTemplateBinding, templateClass, createdAt, updatedAt);
    }

    public void updateName(String name) { if (name != null) this.name = name; this.updatedAt = LocalDateTime.now(); }
    public void updateType(String type) { if (type != null) this.relationshipType = type; this.updatedAt = LocalDateTime.now(); }
    public void updateDirection(String direction) { if (direction != null) this.direction = direction; this.updatedAt = LocalDateTime.now(); }

    public RelationshipId getId() { return id; }
    public DiagramId getDiagramId() { return diagramId; }
    public ElementId getSourceElementId() { return sourceElementId; }
    public ElementId getTargetElementId() { return targetElementId; }
    public String getRelationshipType() { return relationshipType; }
    public String getName() { return name; }
    public String getDirection() { return direction; }
    public Boolean getIsTemplateBinding() { return isTemplateBinding; }
    public String getTemplateClass() { return templateClass; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
