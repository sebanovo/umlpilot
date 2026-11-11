package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.application.dto.ChatAiImageRequest;
import com.umlpilot.umlpilot_api.application.dto.ChatAiRequest;
import com.umlpilot.umlpilot_api.application.dto.ChatAiResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatAiPort;
import org.springframework.stereotype.Component;

@Component
public class AiChatServiceAdapter implements ChatAiPort {

    private final AiChatService aiChatService;

    public AiChatServiceAdapter(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Override
    public ChatAiResult reply(ChatAiRequest request) {
        boolean toolsEnabled = request.toolsEnabled() && request.diagramId() != null;
        AiChatResult result = aiChatService.chatWithContext(
                request.projectId(),
                request.diagramId(),
                request.userId(),
                request.messages().stream()
                        .map(message -> new LlmMessage(message.role(), message.content()))
                        .toList(),
                request.allowMutations(),
                toolsEnabled);

        return new ChatAiResult(result.reply(), result.actions(), result.provider(), result.model());
    }

    @Override
    public ChatAiResult replyWithImage(ChatAiImageRequest request) {
        AiChatOutcome outcome = aiChatService.diagramFromImage(
                request.projectId(),
                request.diagramId(),
                request.userId(),
                request.image(),
                request.contentType(),
                request.instructions());
        return new ChatAiResult(outcome.reply(), outcome.actions(), null, null);
    }
}
