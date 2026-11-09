package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.model.ChatCitation;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae las referencias [msg:<id>] de una respuesta y las resuelve contra mensajes reales.
 */
@Component
public class ChatCitationParser {

    private static final Pattern CITATION = Pattern.compile("\\[msg:([A-Za-z0-9_-]+)]");

    public List<String> extractIds(String reply) {
        if (reply == null || reply.isBlank()) return List.of();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(reply);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return List.copyOf(ids);
    }

    public List<ChatCitation> resolve(String reply,
                                      Function<String, Optional<ConversationMessage>> lookup) {
        List<ChatCitation> citations = new ArrayList<>();
        for (String id : extractIds(reply)) {
            lookup.apply(id).ifPresent(message -> citations.add(new ChatCitation(
                    message.id(),
                    snippet(message.content()),
                    message.author().name(),
                    message.createdAt())));
        }
        return citations;
    }

    private static String snippet(String content) {
        if (content == null) return "";
        String normalized = content.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "…";
    }
}
