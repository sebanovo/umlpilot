package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String email,
        @NotBlank(message = "El password es requerido")
        String password
) {}
