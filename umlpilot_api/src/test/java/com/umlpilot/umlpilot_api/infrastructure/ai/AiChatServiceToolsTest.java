package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatServiceToolsTest {

    @Test
    void readOnlyToolsDoNotAllowMutations() {
        var tools = AiChatService.readOnlyToolNames();

        assertTrue(tools.contains("list_elements"));
        assertTrue(tools.contains("get_diagram_state"));
        assertFalse(tools.contains("create_element"));
        assertFalse(tools.contains("delete_element"));
        assertFalse(tools.contains("create_relationship"));
    }
}
