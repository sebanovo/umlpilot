package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.domain.model.ConversationSummary;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatSummaryRepository {

    Optional<ConversationSummary> findLatest(String conversationId);

    ConversationSummary save(ConversationSummary summary);

    List<GroupConversation> findDueForSummary(int threshold, int limit);

    void markSummarized(String conversationId, String text, String throughMessageId, Instant when);
}
