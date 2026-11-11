package com.umlpilot.umlpilot_api.application.dto;

/**
 * Solicitud de construcción de diagrama a partir de una imagen enviada al chat grupal.
 */
public record ChatAiImageRequest(
        String projectId,
        String diagramId,
        String userId,
        String authorName,
        String authorRole,
        byte[] image,
        String contentType,
        String instructions,
        boolean allowMutations
) {
}
