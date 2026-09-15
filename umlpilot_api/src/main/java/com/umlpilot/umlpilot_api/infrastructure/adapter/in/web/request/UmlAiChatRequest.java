package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

import java.util.List;

public record UmlAiChatRequest(String message, List<Turn> history) {
    public record Turn(String role, String content) {}
}
