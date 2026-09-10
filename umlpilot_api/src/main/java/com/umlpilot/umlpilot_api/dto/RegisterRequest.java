package com.umlpilot.umlpilot_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String email,

        @NotBlank(message = "El password es requerido")
        @Size(min = 8, message = "El password debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "El nombre es requerido")
        String firstName,

        @NotBlank(message = "El apellido es requerido")
        String lastName
) {}
