package com.umlpilot.umlpilot_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank(message = "El nombre es requerido")
        String name,
        String description
) {}
