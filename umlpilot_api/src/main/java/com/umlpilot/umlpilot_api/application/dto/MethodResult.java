package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

public record MethodResult(String id, String elementId, String name, String returnType, String visibility, String alias, String defaultValue, Boolean isStatic, Boolean isAbstract, Boolean isFinal, Boolean isConstructor, Integer orderIndex, List<ParameterResult> parameters) {}
