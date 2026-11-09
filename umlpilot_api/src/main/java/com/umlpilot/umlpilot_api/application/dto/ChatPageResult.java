package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

public record ChatPageResult(List<ChatMessageResult> messages, String nextCursor, boolean hasMore) {
}
