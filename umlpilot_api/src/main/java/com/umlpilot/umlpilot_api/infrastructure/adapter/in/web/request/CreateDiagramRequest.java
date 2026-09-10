package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDiagramRequest(@NotBlank String name, @NotBlank String type, String description) {}
