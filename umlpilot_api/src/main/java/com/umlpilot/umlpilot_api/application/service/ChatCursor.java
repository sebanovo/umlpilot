package com.umlpilot.umlpilot_api.application.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record ChatCursor(Instant createdAt, String id) {

    public String encode() {
        String raw = createdAt.toEpochMilli() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ChatCursor decode(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int separator = raw.indexOf('|');
            if (separator <= 0) return null;
            return new ChatCursor(
                    Instant.ofEpochMilli(Long.parseLong(raw.substring(0, separator))),
                    raw.substring(separator + 1));
        } catch (RuntimeException invalid) {
            return null;
        }
    }
}
