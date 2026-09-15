package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.application.service.RelationshipService;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class UmlToolsTest {

    @Test
    void exposesCrudToolsToTheModel() {
        UmlTools tools = new UmlTools(
                mock(ElementService.class),
                mock(RelationshipService.class),
                mock(DiagramEventPublisher.class));

        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        Set<String> names = Arrays.stream(callbacks)
                .map(c -> c.getToolDefinition().name())
                .collect(Collectors.toSet());

        List<String> expected = List.of(
                "list_elements", "create_element", "update_element", "delete_element",
                "list_attributes", "add_attribute", "update_attribute", "delete_attribute",
                "list_methods", "add_method", "update_method", "delete_method",
                "add_parameter", "delete_parameter",
                "list_relationships", "create_relationship", "update_relationship", "delete_relationship",
                "set_multiplicity", "get_diagram_state");

        assertTrue(names.containsAll(expected), "Faltan tools expuestas: " + expected + " pero se encontraron " + names);
    }
}
