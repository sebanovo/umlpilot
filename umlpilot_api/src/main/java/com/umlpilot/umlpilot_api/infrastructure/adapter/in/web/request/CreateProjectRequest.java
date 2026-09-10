package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String name, String description) {}
