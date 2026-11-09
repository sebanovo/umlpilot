package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

public record ChatAuthor(String userId, String name, String role) {

    public ChatAuthor {
        if (name == null || name.isBlank()) {
            throw new DomainException("El autor del mensaje es requerido");
        }
        if (role == null || role.isBlank()) {
            role = "viewer";
        }
    }

    public static ChatAuthor user(String userId, String name, String role) {
        return new ChatAuthor(userId, name, role);
    }

    public static ChatAuthor assistant() {
        return new ChatAuthor(null, "Asistente IA", "assistant");
    }

    public static ChatAuthor system() {
        return new ChatAuthor(null, "Sistema", "system");
    }
}
