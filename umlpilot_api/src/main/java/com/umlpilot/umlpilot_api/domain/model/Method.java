package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import java.util.ArrayList;
import java.util.List;

public class Method {
    private final MethodId id;
    private final ElementId elementId;
    private String name;
    private String returnType;
    private String visibility;
    private Boolean isStatic;
    private Boolean isAbstract;
    private Boolean isFinal;
    private Boolean isConstructor;
    private Boolean isSynchronized;
    private Boolean isNative;
    private String body;
    private Integer orderIndex;
    private final List<Parameter> parameters;

    private Method(MethodId id, ElementId elementId, String name, String returnType, String visibility, Boolean isStatic, Boolean isAbstract, Boolean isFinal, Boolean isConstructor, Boolean isSynchronized, Boolean isNative, String body, Integer orderIndex, List<Parameter> parameters) {
        this.id = id; this.elementId = elementId; this.name = name; this.returnType = returnType;
        this.visibility = visibility; this.isStatic = isStatic; this.isAbstract = isAbstract;
        this.isFinal = isFinal; this.isConstructor = isConstructor; this.isSynchronized = isSynchronized;
        this.isNative = isNative; this.body = body; this.orderIndex = orderIndex;
        this.parameters = new ArrayList<>(parameters);
    }

    public static Method create(String elementId, String name, String returnType, String visibility, Integer orderIndex) {
        if (name == null || name.isBlank()) throw new DomainException("Method name cannot be empty");
        return new Method(MethodId.generate(), ElementId.from(elementId), name, returnType, visibility, false, false, false, false, false, false, null, orderIndex, new ArrayList<>());
    }

    public static Method reconstitute(MethodId id, ElementId elementId, String name, String returnType, String visibility, Boolean isStatic, Boolean isAbstract, Boolean isFinal, Boolean isConstructor, Boolean isSynchronized, Boolean isNative, String body, Integer orderIndex, List<Parameter> parameters) {
        return new Method(id, elementId, name, returnType, visibility, isStatic, isAbstract, isFinal, isConstructor, isSynchronized, isNative, body, orderIndex, parameters);
    }

    public void addParameter(Parameter parameter) { parameters.add(parameter); }
    public void removeParameter(ParameterId paramId) { parameters.removeIf(p -> p.getId().equals(paramId)); }
    public List<Parameter> getParameters() { return List.copyOf(parameters); }

    public void updateName(String name) { if (name != null) this.name = name; }
    public void updateReturnType(String returnType) { if (returnType != null) this.returnType = returnType; }
    public void updateVisibility(String visibility) { if (visibility != null) this.visibility = visibility; }
    public void updateBody(String body) { this.body = body; }

    public MethodId getId() { return id; }
    public ElementId getElementId() { return elementId; }
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public String getVisibility() { return visibility; }
    public Boolean getIsStatic() { return isStatic; }
    public Boolean getIsAbstract() { return isAbstract; }
    public Boolean getIsFinal() { return isFinal; }
    public Boolean getIsConstructor() { return isConstructor; }
    public Boolean getIsSynchronized() { return isSynchronized; }
    public Boolean getIsNative() { return isNative; }
    public String getBody() { return body; }
    public Integer getOrderIndex() { return orderIndex; }
}
