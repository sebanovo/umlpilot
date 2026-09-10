package com.umlpilot.umlpilot_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteCollaboratorRequest(
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String email,
        @NotBlank(message = "El rol es requerido")
        String role
) {}
