package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatAiPort;
import com.umlpilot.umlpilot_api.application.port.out.ChatContextRetriever;
import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.application.port.out.ChatSummaryRepository;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatReplyServiceTest {

    private final GroupChatRepository chatRepository = mock(GroupChatRepository.class);
    private final ChatSummaryRepository summaryRepository = mock(ChatSummaryRepository.class);
    private final ChatContextRetriever contextRetriever = mock(ChatContextRetriever.class);
    private final ChatAiPort aiPort = mock(ChatAiPort.class);
    private final ChatEventPublisher eventPublisher = mock(ChatEventPublisher.class);
    private final ChatMongoProperties properties = new ChatMongoProperties();
    private final ChatContextBuilder contextBuilder = new ChatContextBuilder(properties);
    private final ChatCitationParser citationParser = new ChatCitationParser();
    private final ChatReplyService service = new ChatReplyService(
            chatRepository, summaryRepository, contextRetriever, aiPort, eventPublisher,
            contextBuilder, citationParser, properties);

    @Test
    void persistsAssistantMessageWithCitationsAndProvider() {
        GroupConversation conversation = GroupConversation.create("p1", "u1")
                .withId("c1").withFocusDiagram("d1");
        ConversationMessage userMessage = ConversationMessage.user(
                "c1", "p1", "d1", ChatAuthor.user("u1", "Ana", "editor"),
                "¿Qué decidimos?", "client-1").withId("m1");
        ConversationMessage old = ConversationMessage.user(
                "c1", "p1", "d1", ChatAuthor.user("u2", "Luis", "viewer"),
                "Usamos herencia", "client-old").withId("old");

        when(chatRepository.findRecentMessages("c1", properties.getRecentMessages()))
                .thenReturn(List.of(userMessage));
        when(contextRetriever.retrieveOlder(eq("c1"), any(), any(), anyInt())).thenReturn(List.of(old));
        when(aiPort.reply(any())).thenReturn(new ChatAiResult(
                "Usamos herencia [msg:old]", List.of(), "gemini", "gemini-3.8-flash"));
        when(chatRepository.findMessageById("old")).thenReturn(Optional.of(old));
        when(chatRepository.saveMessage(any())).thenAnswer(invocation ->
                ((ConversationMessage) invocation.getArgument(0)).withId("a1"));

        service.generate(conversation, userMessage, true);

        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(chatRepository).saveMessage(captor.capture());
        ConversationMessage assistant = captor.getValue();
        assertNotNull(assistant);
        assertEquals("gemini", assistant.provider());
        assertEquals("gemini-3.8-flash", assistant.model());
        assertEquals(1, assistant.citations().size());
        assertEquals("old", assistant.citations().get(0).messageId());
        verify(chatRepository).updateMessageStatus("m1", ChatMessageStatus.COMPLETED);
        verify(eventPublisher).messageCreated(eq("p1"), any());
        verify(eventPublisher).aiStatus("p1", "m1", ChatMessageStatus.COMPLETED, null);
    }

    @Test
    void marksFailedWhenAiFails() {
        GroupConversation conversation = GroupConversation.create("p1", "u1").withId("c1");
        ConversationMessage userMessage = ConversationMessage.user(
                "c1", "p1", null, ChatAuthor.user("u1", "Ana", "editor"),
                "Hola", "client-1").withId("m1");
        when(chatRepository.findRecentMessages("c1", properties.getRecentMessages()))
                .thenReturn(List.of(userMessage));
        when(aiPort.reply(any())).thenThrow(new RuntimeException("sin proveedor"));

        service.generate(conversation, userMessage, false);

        verify(chatRepository).updateMessageStatus("m1", ChatMessageStatus.FAILED);
        verify(chatRepository, never()).saveMessage(any());
        verify(eventPublisher).aiStatus("p1", "m1", ChatMessageStatus.FAILED, "sin proveedor");
    }
}
