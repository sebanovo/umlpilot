package com.umlpilot.umlpilot_api.domain.model;

import java.time.Instant;

public record ChatCitation(String messageId, String snippet, String authorName, Instant createdAt) {
}
