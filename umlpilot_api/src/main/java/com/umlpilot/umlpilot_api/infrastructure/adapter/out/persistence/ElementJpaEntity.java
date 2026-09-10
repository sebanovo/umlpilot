package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "element")
public class ElementJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "element_id") private String id;
    @Column(name = "diagram_id", nullable = false) private String diagramId;
    @Column(name = "parent_id") private String parentId;
    @Column(name = "creator_id", nullable = false) private String creatorId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String visibility;
    private String stereotype;
    @Column(name = "element_type", nullable = false) private String elementType;
    @Column(name = "position_x") private Integer positionX;
    @Column(name = "position_y") private Integer positionY;
    private Integer width;
    private Integer height;
    @Column(name = "properties") private String properties;
    @Column(name = "creation_date", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "update_date", nullable = false) private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public ElementJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDiagramId() { return diagramId; }
    public void setDiagramId(String diagramId) { this.diagramId = diagramId; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getStereotype() { return stereotype; }
    public void setStereotype(String stereotype) { this.stereotype = stereotype; }
    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }
    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }
    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
