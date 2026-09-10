package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "literal")
public class LiteralJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "literal_id") private String id;
    @Column(name = "element_id", nullable = false) private String elementId;
    @Column(nullable = false) private String name;
    private String value;
    @Column(name = "order_index") private Integer orderIndex;

    public LiteralJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
