package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, UserDto user) {
    public record UserDto(String id, String email, String firstName, String lastName, String role) {}
}
