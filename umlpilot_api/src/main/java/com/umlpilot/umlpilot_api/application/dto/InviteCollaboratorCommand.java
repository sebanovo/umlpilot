package com.umlpilot.umlpilot_api.application.dto;

public record InviteCollaboratorCommand(String projectId, String email, String role, String inviterId) {}
