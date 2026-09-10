package com.umlpilot.umlpilot_api.application.dto;

public record MultiplicityResult(String id, String relationshipId, String end, Integer min, Integer max, Boolean isOrdered, Boolean isUnique) {}
