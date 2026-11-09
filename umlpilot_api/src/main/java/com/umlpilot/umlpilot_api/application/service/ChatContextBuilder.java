package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiMessage;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Construye el contexto compartido que recibe la IA: reglas del proyecto, resumen,
 * mensajes antiguos recuperados y los mensajes recientes con autoría visible.
 */
@Component
public class ChatContextBuilder {

    private static final int MAX_MESSAGE_CHARS = 6000;

    private final ChatMongoProperties properties;

    public ChatContextBuilder(ChatMongoProperties properties) {
        this.properties = properties;
    }

    public List<ChatAiMessage> build(GroupConversation conversation,
                                     ConversationMessage current,
                                     List<ConversationMessage> recent,
                                     List<ConversationMessage> retrieved,
                                     boolean canMutate) {
        List<ChatAiMessage> messages = new ArrayList<>();
        messages.add(new ChatAiMessage("system", systemPrompt(conversation, canMutate)));

        if (conversation.summaryText() != null && !conversation.summaryText().isBlank()) {
            messages.add(new ChatAiMessage("system",
                    "Resumen de la conversación hasta ahora:\n" + conversation.summaryText()));
        }

        if (retrieved != null && !retrieved.isEmpty()) {
            StringBuilder block = new StringBuilder(
                    "Mensajes antiguos relevantes recuperados para esta consulta. "
                            + "Si los usas, cita el mensaje con [msg:<id>]:\n");
            for (ConversationMessage message : retrieved) {
                block.append("[msg:").append(message.id()).append("] [")
                        .append(message.author().name()).append(" (")
                        .append(message.author().role()).append(")]: ")
                        .append(truncate(message.content())).append('\n');
            }
            messages.add(new ChatAiMessage("system", block.toString()));
        }

        if (recent != null) {
            for (ConversationMessage message : recent) {
                if (current != null && Objects.equals(message.id(), current.id())) continue;
                messages.add(toAiMessage(message));
            }
        }

        if (current != null) {
            messages.add(toAiMessage(current));
        }
        return messages;
    }

    private ChatAiMessage toAiMessage(ConversationMessage message) {
        String content = truncate(message.content());
        return switch (message.authorType()) {
            case ASSISTANT -> new ChatAiMessage("assistant", content);
            case SYSTEM, TOOL -> new ChatAiMessage("system", content);
            case USER -> new ChatAiMessage("user",
                    "[" + message.author().name() + " (" + message.author().role() + ")]: " + content);
        };
    }

    private String systemPrompt(GroupConversation conversation, boolean canMutate) {
        String mutationRule = canMutate
                ? "Puedes ejecutar las tools de lectura y mutación sobre el diagrama seleccionado."
                : "No puedes crear, editar ni eliminar elementos: el usuario no tiene permisos de edición. "
                + "Puedes usar solo las tools de lectura.";
        return """
                Eres el asistente de IA del proyecto %s, integrado en un chat grupal y colaborativo.
                Participan varios integrantes; cada mensaje de usuario incluye [Nombre (rol)].
                Trabaja siempre sobre el diagrama seleccionado de este proyecto y no inventes ids.
                %s
                Cuando uses un mensaje antiguo recuperado, cita su id con [msg:<id>].
                No inventes citas. Responde en el idioma del usuario, de forma breve y clara.
                Si una petición es ambigua, pide la información que falte.
                """.formatted(conversation.projectId(), mutationRule);
    }

    private static String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_MESSAGE_CHARS) return content;
        return content.substring(0, MAX_MESSAGE_CHARS) + "…";
    }

    public int recentLimit() {
        return properties.getRecentMessages();
    }

    public int retrievalLimit() {
        return properties.getRetrievalLimit();
    }
}
