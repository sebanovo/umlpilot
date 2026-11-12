package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "relationship")
public class RelationshipJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "relationship_id") private String id;
    @Column(name = "diagram_id", nullable = false) private String diagramId;
    @Column(name = "source_element_id", nullable = false) private String sourceElementId;
    @Column(name = "target_element_id", nullable = false) private String targetElementId;
    @Column(name = "relationship_type", nullable = false) private String relationshipType;
    private String name;
    private String direction;
    @Column(name = "association_class_element_id") private String associationClassElementId;
    @Column(name = "source_handle") private String sourceHandle;
    @Column(name = "target_handle") private String targetHandle;
    @Column(name = "waypoints", columnDefinition = "TEXT") private String waypoints;
    @Column(name = "creation_date", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "update_date", nullable = false) private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public RelationshipJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDiagramId() { return diagramId; }
    public void setDiagramId(String diagramId) { this.diagramId = diagramId; }
    public String getSourceElementId() { return sourceElementId; }
    public void setSourceElementId(String sourceElementId) { this.sourceElementId = sourceElementId; }
    public String getTargetElementId() { return targetElementId; }
    public void setTargetElementId(String targetElementId) { this.targetElementId = targetElementId; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getAssociationClassElementId() { return associationClassElementId; }
    public void setAssociationClassElementId(String associationClassElementId) { this.associationClassElementId = associationClassElementId; }
    public String getSourceHandle() { return sourceHandle; }
    public void setSourceHandle(String sourceHandle) { this.sourceHandle = sourceHandle; }
    public String getTargetHandle() { return targetHandle; }
    public void setTargetHandle(String targetHandle) { this.targetHandle = targetHandle; }
    public String getWaypoints() { return waypoints; }
    public void setWaypoints(String waypoints) { this.waypoints = waypoints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
