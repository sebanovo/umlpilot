package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.application.dto.ChatAiRequest;
import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;

public interface ChatAiPort {

    ChatAiResult reply(ChatAiRequest request);
}
