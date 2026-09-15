package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response;

import java.util.List;
import java.util.Map;

public record UmlAiChatResponse(String reply, List<Map<String, Object>> actions) {
}
