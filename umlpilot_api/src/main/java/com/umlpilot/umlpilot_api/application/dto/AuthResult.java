package com.umlpilot.umlpilot_api.application.dto;

public record AuthResult(String accessToken, String refreshToken, String tokenType, UserResult user) {
    public record UserResult(String id, String email, String firstName, String lastName, String role) {}
}
