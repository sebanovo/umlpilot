package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {}
