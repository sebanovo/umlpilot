package com.umlpilot.umlpilot_api.application.dto;

public record ParameterResult(String id, String methodId, String name, String dataType, Integer orderIndex, Boolean isVarargs, Boolean isFinal, String defaultValue) {}
