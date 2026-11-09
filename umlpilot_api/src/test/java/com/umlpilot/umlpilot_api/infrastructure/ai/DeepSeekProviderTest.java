package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeepSeekProviderTest {

    @Test
    void exposesDeepSeekId() {
        assertEquals("deepseek", new DeepSeekProvider("https://api.deepseek.com").id());
    }
}
