package com.umlpilot.umlpilot_api.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMentionsTest {

    @Test
    void detectsAssistantMention() {
        assertTrue(ChatMentions.mentionsAssistant("@asistente crea una clase Cliente"));
        assertTrue(ChatMentions.mentionsAssistant("Hola @Asistente, revisa el diagrama"));
        assertTrue(ChatMentions.mentionsAssistant("@ai ayúdame"));
        assertTrue(ChatMentions.mentionsAssistant("@ia resume la conversación"));
    }

    @Test
    void ignoresMemberMentionsAndPlainMessages() {
        assertFalse(ChatMentions.mentionsAssistant("@Luis revisa esto"));
        assertFalse(ChatMentions.mentionsAssistant("Hola equipo, ¿cómo va?"));
        assertFalse(ChatMentions.mentionsAssistant(null));
    }
}
