package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatAiPort;
import com.umlpilot.umlpilot_api.application.port.out.ChatSummaryRepository;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.ConversationSummary;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSummarySchedulerTest {

    private final ChatSummaryRepository summaryRepository = mock(ChatSummaryRepository.class);
    private final GroupChatRepository chatRepository = mock(GroupChatRepository.class);
    private final ChatAiPort aiPort = mock(ChatAiPort.class);
    private final ChatMongoProperties properties = new ChatMongoProperties();
    private final ChatSummaryScheduler scheduler = new ChatSummaryScheduler(
            summaryRepository, chatRepository, aiPort, properties);

    @Test
    void summarizesOldestBlockAndResetsCounter() {
        GroupConversation conversation = GroupConversation.create("p1", "u1").withId("c1");
        ConversationMessage first = message("m1", "Primera decisión");
        ConversationMessage last = message("m2", "Segunda decisión");
        when(summaryRepository.findLatest("c1")).thenReturn(Optional.empty());
        when(chatRepository.findMessagesAfter(eq("c1"), any(), anyInt()))
                .thenReturn(List.of(first, last));
        when(aiPort.reply(any())).thenReturn(new ChatAiResult(
                "Resumen del bloque", List.of(), "gemini", "gemini-3.8-flash"));

        scheduler.summarize(conversation);

        ArgumentCaptor<ConversationSummary> captor = ArgumentCaptor.forClass(ConversationSummary.class);
        verify(summaryRepository).save(captor.capture());
        assertEquals("Resumen del bloque", captor.getValue().text());
        assertEquals("m1", captor.getValue().fromMessageId());
        assertEquals("m2", captor.getValue().toMessageId());
        verify(summaryRepository).markSummarized(eq("c1"), eq("Resumen del bloque"), eq("m2"), any());
    }

    @Test
    void failureDoesNotPersistSummary() {
        GroupConversation conversation = GroupConversation.create("p1", "u1").withId("c1");
        when(summaryRepository.findLatest("c1")).thenReturn(Optional.empty());
        when(chatRepository.findMessagesAfter(eq("c1"), any(), anyInt()))
                .thenReturn(List.of(message("m1", "Contenido")));
        when(aiPort.reply(any())).thenThrow(new RuntimeException("sin proveedor"));

        assertThrows(RuntimeException.class, () -> scheduler.summarize(conversation));
        verify(summaryRepository, never()).save(any());
    }

    private ConversationMessage message(String id, String content) {
        return ConversationMessage.user("c1", "p1", null,
                ChatAuthor.user("u1", "Ana", "editor"), content, "client-" + id).withId(id);
    }
}
