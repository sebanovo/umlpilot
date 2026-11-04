package com.umlpilot.umlpilot_api.application.dto;

/** Resultado de unirse a un proyecto a través de un enlace de compartir. */
public record ShareJoinResult(String projectId, String diagramId, String role) {}
