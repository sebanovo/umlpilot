package com.umlpilot.umlpilot_api.application.dto;

public record RegisterCommand(String email, String password, String firstName, String lastName) {}
