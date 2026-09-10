package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attribute")
public class AttributeJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "attribute_id") private String id;
    @Column(name = "element_id", nullable = false) private String elementId;
    @Column(nullable = false) private String name;
    @Column(name = "data_type", nullable = false) private String dataType;
    private String visibility;
    @Column(name = "default_value") private String defaultValue;
    @Column(name = "is_static") private Boolean isStatic;
    @Column(name = "is_final") private Boolean isFinal;
    @Column(name = "is_transient") private Boolean isTransient;
    @Column(name = "is_volatile") private Boolean isVolatile;
    private String multiplicity;
    @Column(name = "order_index") private Integer orderIndex;

    public AttributeJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getIsStatic() { return isStatic; }
    public void setIsStatic(Boolean isStatic) { this.isStatic = isStatic; }
    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
    public Boolean getIsTransient() { return isTransient; }
    public void setIsTransient(Boolean isTransient) { this.isTransient = isTransient; }
    public Boolean getIsVolatile() { return isVolatile; }
    public void setIsVolatile(Boolean isVolatile) { this.isVolatile = isVolatile; }
    public String getMultiplicity() { return multiplicity; }
    public void setMultiplicity(String multiplicity) { this.multiplicity = multiplicity; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
