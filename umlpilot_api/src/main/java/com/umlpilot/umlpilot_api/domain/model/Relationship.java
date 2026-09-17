package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Relationship {
    private final RelationshipId id;
    private final DiagramId diagramId;
    private ElementId sourceElementId;
    private ElementId targetElementId;
    private String relationshipType;
    private String name;
    private String direction;
    private String associationClassElementId;
    private String sourceHandle;
    private String targetHandle;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Relationship(RelationshipId id, DiagramId diagramId, ElementId sourceElementId, ElementId targetElementId, String relationshipType, String name, String direction, String associationClassElementId, String sourceHandle, String targetHandle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.diagramId = diagramId; this.sourceElementId = sourceElementId; this.targetElementId = targetElementId;
        this.relationshipType = relationshipType; this.name = name; this.direction = direction;
        this.associationClassElementId = associationClassElementId;
        this.sourceHandle = sourceHandle; this.targetHandle = targetHandle;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static Relationship create(String diagramId, String sourceElementId, String targetElementId, String type) {
        return create(diagramId, sourceElementId, targetElementId, type, null, null, null);
    }

    public static Relationship create(String diagramId, String sourceElementId, String targetElementId, String type, String associationClassElementId, String sourceHandle, String targetHandle) {
        if (type == null || type.isBlank()) throw new DomainException("Relationship type cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new Relationship(RelationshipId.generate(), DiagramId.from(diagramId), ElementId.from(sourceElementId), ElementId.from(targetElementId), type, null, "bidirectional", associationClassElementId, sourceHandle, targetHandle, now, now);
    }

    public static Relationship reconstitute(RelationshipId id, DiagramId diagramId, ElementId sourceElementId, ElementId targetElementId, String relationshipType, String name, String direction, String associationClassElementId, String sourceHandle, String targetHandle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Relationship(id, diagramId, sourceElementId, targetElementId, relationshipType, name, direction, associationClassElementId, sourceHandle, targetHandle, createdAt, updatedAt);
    }

    public void updateName(String name) { if (name != null) this.name = name; this.updatedAt = LocalDateTime.now(); }
    public void updateType(String type) { if (type != null) this.relationshipType = type; this.updatedAt = LocalDateTime.now(); }
    public void updateDirection(String direction) { if (direction != null) this.direction = direction; this.updatedAt = LocalDateTime.now(); }
    public void updateAssociationClassElementId(String associationClassElementId) { if (associationClassElementId != null) this.associationClassElementId = associationClassElementId; this.updatedAt = LocalDateTime.now(); }
    public void updateSourceElementId(String sourceElementId) { if (sourceElementId != null) this.sourceElementId = ElementId.from(sourceElementId); this.updatedAt = LocalDateTime.now(); }
    public void updateTargetElementId(String targetElementId) { if (targetElementId != null) this.targetElementId = ElementId.from(targetElementId); this.updatedAt = LocalDateTime.now(); }
    public void updateSourceHandle(String sourceHandle) { if (sourceHandle != null) this.sourceHandle = sourceHandle; this.updatedAt = LocalDateTime.now(); }
    public void updateTargetHandle(String targetHandle) { if (targetHandle != null) this.targetHandle = targetHandle; this.updatedAt = LocalDateTime.now(); }

    public RelationshipId getId() { return id; }
    public DiagramId getDiagramId() { return diagramId; }
    public ElementId getSourceElementId() { return sourceElementId; }
    public ElementId getTargetElementId() { return targetElementId; }
    public String getRelationshipType() { return relationshipType; }
    public String getName() { return name; }
    public String getDirection() { return direction; }
    public String getAssociationClassElementId() { return associationClassElementId; }
    public String getSourceHandle() { return sourceHandle; }
    public String getTargetHandle() { return targetHandle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
