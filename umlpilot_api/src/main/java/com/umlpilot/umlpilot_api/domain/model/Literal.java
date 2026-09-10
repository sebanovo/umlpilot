package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

public class Literal {
    private final LiteralId id;
    private final ElementId elementId;
    private String name;
    private String value;
    private Integer orderIndex;

    private Literal(LiteralId id, ElementId elementId, String name, String value, Integer orderIndex) {
        this.id = id; this.elementId = elementId; this.name = name; this.value = value; this.orderIndex = orderIndex;
    }

    public static Literal create(String elementId, String name, String value, Integer orderIndex) {
        if (name == null || name.isBlank()) throw new DomainException("Literal name cannot be empty");
        return new Literal(LiteralId.generate(), ElementId.from(elementId), name, value, orderIndex);
    }

    public static Literal reconstitute(LiteralId id, ElementId elementId, String name, String value, Integer orderIndex) {
        return new Literal(id, elementId, name, value, orderIndex);
    }

    public void updateValue(String value) { this.value = value; }

    public LiteralId getId() { return id; }
    public ElementId getElementId() { return elementId; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public Integer getOrderIndex() { return orderIndex; }
}
