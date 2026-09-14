package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.List;

public interface LlmProvider {

    String id();

    String complete(String apiKey, String model, List<LlmMessage> messages);
}
