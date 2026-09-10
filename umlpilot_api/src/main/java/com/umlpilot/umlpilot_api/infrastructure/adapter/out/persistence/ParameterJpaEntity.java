package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "parameter")
public class ParameterJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "parameter_id") private String id;
    @Column(name = "method_id", nullable = false) private String methodId;
    @Column(nullable = false) private String name;
    @Column(name = "data_type", nullable = false) private String dataType;
    @Column(name = "order_index") private Integer orderIndex;
    @Column(name = "is_varargs") private Boolean isVarargs;
    @Column(name = "is_final") private Boolean isFinal;
    @Column(name = "default_value") private String defaultValue;

    public ParameterJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMethodId() { return methodId; }
    public void setMethodId(String methodId) { this.methodId = methodId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public Boolean getIsVarargs() { return isVarargs; }
    public void setIsVarargs(Boolean isVarargs) { this.isVarargs = isVarargs; }
    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}
