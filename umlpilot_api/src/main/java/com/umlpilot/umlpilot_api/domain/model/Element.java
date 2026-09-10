package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.time.LocalDateTime;

public class Element {
    private final ElementId id;
    private final DiagramId diagramId;
    private String parentId;
    private final String creatorId;
    private String name;
    private String visibility;
    private String stereotype;
    private final String elementType;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private String properties;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Element(ElementId id, DiagramId diagramId, String parentId, String creatorId, String name, String visibility, String stereotype, String elementType, Integer positionX, Integer positionY, Integer width, Integer height, String properties, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.diagramId = diagramId; this.parentId = parentId; this.creatorId = creatorId;
        this.name = name; this.visibility = visibility; this.stereotype = stereotype; this.elementType = elementType;
        this.positionX = positionX; this.positionY = positionY; this.width = width; this.height = height;
        this.properties = properties; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static Element create(String diagramId, String creatorId, String name, String elementType, Integer x, Integer y) {
        if (name == null || name.isBlank()) throw new DomainException("Element name cannot be empty");
        LocalDateTime now = LocalDateTime.now();
        return new Element(ElementId.generate(), DiagramId.from(diagramId), null, creatorId, name, "public", null, elementType, x, y, 100, 80, "{}", now, now);
    }

    public static Element reconstitute(ElementId id, DiagramId diagramId, String parentId, String creatorId, String name, String visibility, String stereotype, String elementType, Integer positionX, Integer positionY, Integer width, Integer height, String properties, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Element(id, diagramId, parentId, creatorId, name, visibility, stereotype, elementType, positionX, positionY, width, height, properties, createdAt, updatedAt);
    }

    public void updatePosition(Integer x, Integer y) { this.positionX = x; this.positionY = y; this.updatedAt = LocalDateTime.now(); }
    public void updateName(String name) { if (name != null) this.name = name; this.updatedAt = LocalDateTime.now(); }
    public void updateVisibility(String visibility) { if (visibility != null) this.visibility = visibility; this.updatedAt = LocalDateTime.now(); }
    public void updateStereotype(String stereotype) { this.stereotype = stereotype; this.updatedAt = LocalDateTime.now(); }
    public void updateSize(Integer w, Integer h) { if (w != null) this.width = w; if (h != null) this.height = h; this.updatedAt = LocalDateTime.now(); }

    public ElementId getId() { return id; }
    public DiagramId getDiagramId() { return diagramId; }
    public String getParentId() { return parentId; }
    public String getCreatorId() { return creatorId; }
    public String getName() { return name; }
    public String getVisibility() { return visibility; }
    public String getStereotype() { return stereotype; }
    public String getElementType() { return elementType; }
    public Integer getPositionX() { return positionX; }
    public Integer getPositionY() { return positionY; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getProperties() { return properties; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
