package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteCollaboratorRequest(@NotBlank @Email String email, @NotBlank String role) {}
