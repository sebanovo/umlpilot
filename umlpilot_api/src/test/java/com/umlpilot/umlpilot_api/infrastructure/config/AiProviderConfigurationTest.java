package com.umlpilot.umlpilot_api.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AiProviderConfigurationTest {

    @Test
    void deepseekProviderIsConfigurable() {
        AiProperties properties = new AiProperties();
        AiProperties.Provider deepseek = new AiProperties.Provider();
        deepseek.setApiKeys(List.of("key-1"));
        deepseek.setModels(List.of("deepseek-flash"));
        deepseek.setVisionModels(List.of("deepseek-flash"));
        properties.setDeepseek(deepseek);

        assertSame(deepseek, properties.providerConfig("deepseek"));
        assertNull(properties.providerConfig("unknown"));
    }

    @Test
    void deepseekCanLeadProviderOrder() {
        AiProperties properties = new AiProperties();
        properties.setProviderOrder(List.of("deepseek", "openrouter", "gemini"));

        assertEquals(
                List.of("deepseek", "openrouter", "gemini"),
                AiChatConfiguration.providerOrder(properties));
    }

    @Test
    void deepseekBaseUrlHasDefault() {
        assertEquals("https://api.deepseek.com", new AiChatProperties().getDeepSeekBaseUrl());
    }
}
