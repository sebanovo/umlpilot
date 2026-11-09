package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;

import java.time.Instant;
import java.util.List;

public interface ChatContextRetriever {

    List<ConversationMessage> retrieveOlder(String conversationId, String query, Instant before, int limit);
}
