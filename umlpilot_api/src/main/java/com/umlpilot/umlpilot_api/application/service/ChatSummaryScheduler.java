package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiMessage;
import com.umlpilot.umlpilot_api.application.dto.ChatAiRequest;
import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatAiPort;
import com.umlpilot.umlpilot_api.application.port.out.ChatSummaryRepository;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.ConversationSummary;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatSummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChatSummaryScheduler.class);

    private final ChatSummaryRepository summaryRepository;
    private final GroupChatRepository chatRepository;
    private final ChatAiPort aiPort;
    private final ChatMongoProperties properties;

    public ChatSummaryScheduler(ChatSummaryRepository summaryRepository,
                                GroupChatRepository chatRepository,
                                ChatAiPort aiPort,
                                ChatMongoProperties properties) {
        this.summaryRepository = summaryRepository;
        this.chatRepository = chatRepository;
        this.aiPort = aiPort;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.chat.summary-interval-ms:60000}")
    public void summarizeDueConversations() {
        List<GroupConversation> due = summaryRepository.findDueForSummary(
                properties.getSummaryThreshold(), 10);
        for (GroupConversation conversation : due) {
            try {
                summarize(conversation);
            } catch (Exception e) {
                // Se reintenta en la siguiente ejecución; no se pierde ningún mensaje.
                log.warn("No se pudo resumir la conversación {}: {}", conversation.id(), e.getMessage());
            }
        }
    }

    void summarize(GroupConversation conversation) {
        Instant after = summaryRepository.findLatest(conversation.id())
                .map(ConversationSummary::createdAt)
                .orElse(conversation.createdAt());
        List<ConversationMessage> messages = chatRepository.findMessagesAfter(
                conversation.id(), after, properties.getSummaryBatchSize());
        if (messages.isEmpty()) return;

        StringBuilder transcript = new StringBuilder();
        for (ConversationMessage message : messages) {
            transcript.append('[').append(message.author().name()).append(" (")
                    .append(message.author().role()).append(")]: ")
                    .append(message.content()).append('\n');
        }

        List<ChatAiMessage> prompt = List.of(
                new ChatAiMessage("system",
                        "Resume de forma concisa la siguiente conversación de equipo. "
                                + "Conserva decisiones, acuerdos, entidades UML y preguntas abiertas. "
                                + "No inventes datos. Devuelve solo el resumen."),
                new ChatAiMessage("user", transcript.toString()));

        ChatAiResult result = aiPort.reply(new ChatAiRequest(
                conversation.projectId(), conversation.focusDiagramId(), null,
                "Sistema", "system", false, false, prompt));

        ConversationMessage first = messages.get(0);
        ConversationMessage last = messages.get(messages.size() - 1);
        summaryRepository.save(new ConversationSummary(
                null, conversation.id(), first.id(), last.id(), result.reply(),
                result.model(), Instant.now()));
        summaryRepository.markSummarized(
                conversation.id(), result.reply(), last.id(), Instant.now());
    }
}
