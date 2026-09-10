package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "method")
public class MethodJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "method_id") private String id;
    @Column(name = "element_id", nullable = false) private String elementId;
    @Column(nullable = false) private String name;
    @Column(name = "return_type", nullable = false) private String returnType;
    private String visibility;
    @Column(name = "is_static") private Boolean isStatic;
    @Column(name = "is_abstract") private Boolean isAbstract;
    @Column(name = "is_final") private Boolean isFinal;
    @Column(name = "is_constructor") private Boolean isConstructor;
    @Column(name = "is_synchronized") private Boolean isSynchronized;
    @Column(name = "is_native") private Boolean isNative;
    private String body;
    @Column(name = "order_index") private Integer orderIndex;

    public MethodJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Boolean getIsStatic() { return isStatic; }
    public void setIsStatic(Boolean isStatic) { this.isStatic = isStatic; }
    public Boolean getIsAbstract() { return isAbstract; }
    public void setIsAbstract(Boolean isAbstract) { this.isAbstract = isAbstract; }
    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
    public Boolean getIsConstructor() { return isConstructor; }
    public void setIsConstructor(Boolean isConstructor) { this.isConstructor = isConstructor; }
    public Boolean getIsSynchronized() { return isSynchronized; }
    public void setIsSynchronized(Boolean isSynchronized) { this.isSynchronized = isSynchronized; }
    public Boolean getIsNative() { return isNative; }
    public void setIsNative(Boolean isNative) { this.isNative = isNative; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
