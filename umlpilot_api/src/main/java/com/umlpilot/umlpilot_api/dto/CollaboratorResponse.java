package com.umlpilot.umlpilot_api.dto;

public record CollaboratorResponse(
        String userId,
        String email,
        String firstName,
        String lastName,
        String role,
        String invitationStatus
) {}
