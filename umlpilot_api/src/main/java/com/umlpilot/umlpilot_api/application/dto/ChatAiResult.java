package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;
import java.util.Map;

public record ChatAiResult(
        String reply,
        List<Map<String, Object>> actions,
        String provider,
        String model
) {
}
