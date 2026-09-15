package com.umlpilot.umlpilot_api.application.dto;

public record AttributeResult(String id, String elementId, String name, String dataType, String visibility, String alias, String stereotype, String defaultValue, Boolean isStatic, Boolean isFinal, Integer orderIndex) {}
