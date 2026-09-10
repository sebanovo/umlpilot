package com.umlpilot.umlpilot_api.application.dto;

public record AttributeResult(String id, String elementId, String name, String dataType, String visibility, String defaultValue, Boolean isStatic, Boolean isFinal, Integer orderIndex) {}
