package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta el bucle de conversación con function calling sobre el diagrama indicado,
 * usando las tools de {@link UmlTools}, el contexto de la conversación y rotación entre
 * proveedores (OpenRouter/Gemini) cuando uno se queda sin créditos o falla.
 */
@Service
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            Eres un asistente experto en modelado UML integrado en un editor de diagramas de clases.
            Ayudas a crear, consultar, editar y eliminar elementos UML (clases, interfaces, enumeraciones, notas),
            sus atributos, métodos y parámetros, y las relaciones entre ellos (asociación, agregación, composición,
            generalización, realización, dependencia, clase asociación y template binding).

            Reglas:
            - Trabajas SIEMPRE sobre el diagrama actual de la conversación; nunca operes sobre otro diagrama.
            - Antes de modificar algo, usa las tools de lectura (list_elements, list_attributes, list_methods,
              list_relationships o get_diagram_state) para resolver nombres a ids.
            - Usa la tool adecuada para cada acción; no inventes ids ni datos.
            - Si falta información imprescindible, pídesela al usuario en lugar de asumir.
            - Responde en el mismo idioma del usuario, de forma breve y clara.
            - Al terminar, resume en una o dos frases qué acciones ejecutaste.
            """;

    private final AiChatProviderInfo providerInfo;
    private final AiChatProviderRegistry registry;
    private final List<Provider> providers;

    public AiChatService(UmlTools umlTools, AiChatProviderInfo providerInfo, AiChatProviderRegistry registry) {
        this.providerInfo = providerInfo;
        this.registry = registry;
        this.providers = registry.entries().stream()
                .map(entry -> new Provider(entry, ChatClient.builder(entry.model())
                        .defaultSystem(SYSTEM_PROMPT)
                        .defaultTools(umlTools)
                        .build()))
                .toList();
    }

    public AiChatOutcome chat(String projectId, String diagramId, String userId, String message,
                              List<Map<String, String>> history) {
        if (!providerInfo.configured() || providers.isEmpty()) {
            throw new LlmException(
                    "El asistente de IA no está configurado (define OPENROUTER_API_KEYS o GEMINI_API_KEYS y sus modelos)",
                    false);
        }

        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = new HashMap<>();
        context.put(UmlTools.CTX_DIAGRAM_ID, diagramId);
        context.put(UmlTools.CTX_PROJECT_ID, projectId);
        context.put(UmlTools.CTX_USER_ID, userId);
        context.put(UmlTools.CTX_ACTIONS, recorder);

        List<Message> messages = new ArrayList<>();
        if (history != null) {
            for (Map<String, String> turn : history) {
                if (turn == null || turn.get("content") == null) continue;
                String role = turn.getOrDefault("role", "user");
                String content = turn.get("content");
                if ("assistant".equalsIgnoreCase(role)) {
                    messages.add(new AssistantMessage(content));
                } else if ("system".equalsIgnoreCase(role)) {
                    messages.add(new SystemMessage(content));
                } else {
                    messages.add(new UserMessage(content));
                }
            }
        }

        int size = providers.size();
        int start = registry.cursor();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int index = (start + i) % size;
            Provider provider = providers.get(index);
            if (registry.isCoolingDown(provider.entry.cooldownKey())) continue;

            try {
                String reply = provider.client.prompt()
                        .messages(messages)
                        .user(message)
                        .toolContext(context)
                        .call()
                        .content();
                registry.moveCursorTo(index);
                return new AiChatOutcome(reply == null ? "" : reply, recorder.actions());
            } catch (Exception e) {
                if (QuotaErrors.isQuotaRelated(e)) {
                    // Créditos/cuota agotados: se enfría todo el proveedor y se rota al otro.
                    registry.coolDownProvider(provider.entry.id(), provider.entry.quotaCooldownSeconds());
                } else {
                    // Modelo concreto no disponible: se enfría solo ese modelo y se prueba el siguiente.
                    registry.coolDownEntry(provider.entry.cooldownKey(), provider.entry.errorCooldownSeconds());
                }
                errors.add(provider.entry.id() + " [" + provider.entry.modelName() + "]: " + e.getMessage());
            }
        }

        String detail = errors.isEmpty()
                ? "todos en enfriamiento (" + registry.remainingCooldownSeconds() + "s)"
                : String.join(" | ", errors);
        throw new LlmException("Ningún proveedor de IA disponible: " + detail, true);
    }

    private record Provider(AiChatProviderRegistry.Entry entry, ChatClient client) {
    }
}
