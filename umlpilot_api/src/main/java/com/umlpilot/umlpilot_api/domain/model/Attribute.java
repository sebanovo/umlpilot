package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

public class Attribute {
    private final AttributeId id;
    private final ElementId elementId;
    private String name;
    private String dataType;
    private String visibility;
    private String defaultValue;
    private Boolean isStatic;
    private Boolean isFinal;
    private Boolean isTransient;
    private Boolean isVolatile;
    private String multiplicity;
    private Integer orderIndex;

    private Attribute(AttributeId id, ElementId elementId, String name, String dataType, String visibility, String defaultValue, Boolean isStatic, Boolean isFinal, Boolean isTransient, Boolean isVolatile, String multiplicity, Integer orderIndex) {
        this.id = id; this.elementId = elementId; this.name = name; this.dataType = dataType;
        this.visibility = visibility; this.defaultValue = defaultValue; this.isStatic = isStatic;
        this.isFinal = isFinal; this.isTransient = isTransient; this.isVolatile = isVolatile;
        this.multiplicity = multiplicity; this.orderIndex = orderIndex;
    }

    public static Attribute create(String elementId, String name, String dataType, String visibility, Integer orderIndex) {
        if (name == null || name.isBlank()) throw new DomainException("Attribute name cannot be empty");
        return new Attribute(AttributeId.generate(), ElementId.from(elementId), name, dataType, visibility, null, false, false, false, false, null, orderIndex);
    }

    public static Attribute reconstitute(AttributeId id, ElementId elementId, String name, String dataType, String visibility, String defaultValue, Boolean isStatic, Boolean isFinal, Boolean isTransient, Boolean isVolatile, String multiplicity, Integer orderIndex) {
        return new Attribute(id, elementId, name, dataType, visibility, defaultValue, isStatic, isFinal, isTransient, isVolatile, multiplicity, orderIndex);
    }

    public void updateName(String name) { if (name != null) this.name = name; }
    public void updateDataType(String dataType) { if (dataType != null) this.dataType = dataType; }
    public void updateVisibility(String visibility) { if (visibility != null) this.visibility = visibility; }
    public void updateDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public void updateStatic(Boolean isStatic) { this.isStatic = isStatic; }
    public void updateFinal(Boolean isFinal) { this.isFinal = isFinal; }

    public AttributeId getId() { return id; }
    public ElementId getElementId() { return elementId; }
    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public String getVisibility() { return visibility; }
    public String getDefaultValue() { return defaultValue; }
    public Boolean getIsStatic() { return isStatic; }
    public Boolean getIsFinal() { return isFinal; }
    public Boolean getIsTransient() { return isTransient; }
    public Boolean getIsVolatile() { return isVolatile; }
    public String getMultiplicity() { return multiplicity; }
    public Integer getOrderIndex() { return orderIndex; }
}
