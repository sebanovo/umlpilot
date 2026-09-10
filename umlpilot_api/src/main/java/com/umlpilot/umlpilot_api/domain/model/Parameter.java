package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

public class Parameter {
    private final ParameterId id;
    private final MethodId methodId;
    private String name;
    private String dataType;
    private Integer orderIndex;
    private Boolean isVarargs;
    private Boolean isFinal;
    private String defaultValue;

    private Parameter(ParameterId id, MethodId methodId, String name, String dataType, Integer orderIndex, Boolean isVarargs, Boolean isFinal, String defaultValue) {
        this.id = id; this.methodId = methodId; this.name = name; this.dataType = dataType;
        this.orderIndex = orderIndex; this.isVarargs = isVarargs; this.isFinal = isFinal; this.defaultValue = defaultValue;
    }

    public static Parameter create(String methodId, String name, String dataType, Integer orderIndex) {
        if (name == null || name.isBlank()) throw new DomainException("Parameter name cannot be empty");
        return new Parameter(ParameterId.generate(), MethodId.from(methodId), name, dataType, orderIndex, false, false, null);
    }

    public static Parameter reconstitute(ParameterId id, MethodId methodId, String name, String dataType, Integer orderIndex, Boolean isVarargs, Boolean isFinal, String defaultValue) {
        return new Parameter(id, methodId, name, dataType, orderIndex, isVarargs, isFinal, defaultValue);
    }

    public void updateName(String name) { if (name != null) this.name = name; }
    public void updateDataType(String dataType) { if (dataType != null) this.dataType = dataType; }

    public ParameterId getId() { return id; }
    public MethodId getMethodId() { return methodId; }
    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public Integer getOrderIndex() { return orderIndex; }
    public Boolean getIsVarargs() { return isVarargs; }
    public Boolean getIsFinal() { return isFinal; }
    public String getDefaultValue() { return defaultValue; }
}
