package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response;

public record CollaboratorResponse(String userId, String email, String firstName, String lastName, String role, String invitationStatus) {}
