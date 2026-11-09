package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiMessage;
import com.umlpilot.umlpilot_api.application.dto.ChatAiRequest;
import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatAiPort;
import com.umlpilot.umlpilot_api.application.port.out.ChatContextRetriever;
import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.application.port.out.ChatSummaryRepository;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ChatAction;
import com.umlpilot.umlpilot_api.domain.model.ChatCitation;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ChatReplyService {

    private static final Logger log = LoggerFactory.getLogger(ChatReplyService.class);

    private final GroupChatRepository chatRepository;
    private final ChatSummaryRepository summaryRepository;
    private final ChatContextRetriever contextRetriever;
    private final ChatAiPort aiPort;
    private final ChatEventPublisher eventPublisher;
    private final ChatContextBuilder contextBuilder;
    private final ChatCitationParser citationParser;
    private final ChatMongoProperties properties;

    public ChatReplyService(GroupChatRepository chatRepository,
                            ChatSummaryRepository summaryRepository,
                            ChatContextRetriever contextRetriever,
                            ChatAiPort aiPort,
                            ChatEventPublisher eventPublisher,
                            ChatContextBuilder contextBuilder,
                            ChatCitationParser citationParser,
                            ChatMongoProperties properties) {
        this.chatRepository = chatRepository;
        this.summaryRepository = summaryRepository;
        this.contextRetriever = contextRetriever;
        this.aiPort = aiPort;
        this.eventPublisher = eventPublisher;
        this.contextBuilder = contextBuilder;
        this.citationParser = citationParser;
        this.properties = properties;
    }

    @Async("chatTaskExecutor")
    public void generate(GroupConversation conversation, ConversationMessage userMessage, boolean allowMutations) {
        try {
            List<ConversationMessage> recent = chatRepository.findRecentMessages(
                    conversation.id(), properties.getRecentMessages());
            Instant before = recent.isEmpty() ? Instant.now() : recent.get(0).createdAt();
            List<ConversationMessage> retrieved = contextRetriever.retrieveOlder(
                    conversation.id(), userMessage.content(), before, properties.getRetrievalLimit());

            List<ChatAiMessage> context = contextBuilder.build(
                    conversation, userMessage, recent, retrieved, allowMutations);

            String diagramId = userMessage.diagramId() != null
                    ? userMessage.diagramId() : conversation.focusDiagramId();

            ChatAiResult result = aiPort.reply(new ChatAiRequest(
                    conversation.projectId(),
                    diagramId,
                    userMessage.author().userId(),
                    userMessage.author().name(),
                    userMessage.author().role(),
                    allowMutations,
                    diagramId != null,
                    context));

            List<ChatCitation> citations = citationParser.resolve(
                    result.reply(), id -> chatRepository.findMessageById(id));
            List<ChatAction> actions = result.actions() == null ? List.of()
                    : result.actions().stream().map(this::toAction).toList();

            ConversationMessage assistant = ConversationMessage.assistant(
                    conversation.id(),
                    conversation.projectId(),
                    diagramId,
                    result.reply(),
                    userMessage.id(),
                    citations,
                    actions,
                    result.provider(),
                    result.model());
            ConversationMessage saved = chatRepository.saveMessage(assistant);
            chatRepository.updateMessageStatus(userMessage.id(), ChatMessageStatus.COMPLETED);

            eventPublisher.messageCreated(conversation.projectId(), saved);
            eventPublisher.aiStatus(conversation.projectId(), userMessage.id(),
                    ChatMessageStatus.COMPLETED, null);
        } catch (Exception e) {
            log.warn("No se pudo generar la respuesta del chat grupal: {}", e.getMessage());
            chatRepository.updateMessageStatus(userMessage.id(), ChatMessageStatus.FAILED);
            eventPublisher.aiStatus(conversation.projectId(), userMessage.id(),
                    ChatMessageStatus.FAILED, e.getMessage());
        }
    }

    private ChatAction toAction(Map<String, Object> map) {
        return new ChatAction(
                string(map.get("action")),
                string(map.get("entity")),
                string(map.get("id")),
                string(map.get("result")));
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
