package com.umlpilot.umlpilot_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDiagramRequest(
        @NotBlank(message = "El nombre es requerido")
        String name,
        @NotBlank(message = "El tipo es requerido")
        String type,
        String description
) {}
