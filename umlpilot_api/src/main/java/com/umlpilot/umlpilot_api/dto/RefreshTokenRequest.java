package com.umlpilot.umlpilot_api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken
) {}
