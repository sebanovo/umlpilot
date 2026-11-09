package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.umlpilot.umlpilot_api.application.port.out.ChatContextRetriever;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MongoChatContextRetriever implements ChatContextRetriever {

    private final GroupChatRepository repository;

    public MongoChatContextRetriever(GroupChatRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConversationMessage> retrieveOlder(String conversationId, String query, Instant before, int limit) {
        return repository.searchOlderMessages(conversationId, query, before, limit);
    }
}
