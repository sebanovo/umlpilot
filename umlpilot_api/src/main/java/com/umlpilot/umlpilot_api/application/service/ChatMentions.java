package com.umlpilot.umlpilot_api.application.service;

import java.util.regex.Pattern;

/**
 * Reglas de menciones del chat grupal. La IA solo responde cuando se la menciona
 * explícitamente con @asistente (o sus alias @ai / @ia).
 */
public final class ChatMentions {

    private static final Pattern ASSISTANT_MENTION =
            Pattern.compile("(?i)@(asistente|ai|ia)\\b");

    private ChatMentions() {
    }

    public static boolean mentionsAssistant(String content) {
        return content != null && ASSISTANT_MENTION.matcher(content).find();
    }
}
