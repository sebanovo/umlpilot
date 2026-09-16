package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String IMAGE_SYSTEM_PROMPT = """
            Eres un asistente experto en modelado UML. Recibes la imagen de un diagrama de clases y debes
            CONSTRUIR ese diagrama en el diagrama actual del editor usando las tools disponibles.

            Reglas:
            - Crea los elementos que veas (clases, interfaces, enumeraciones) con sus atributos y métodos.
            - Crea las relaciones entre ellos con su tipo (association, aggregation, composition, generalization,
              realization, dependency) y nombre si aparece en la imagen.
            - SOLO puedes crear y leer; nunca edites ni elimines elementos existentes.
            - Antes de crear, consulta el estado actual (get_diagram_state o list_elements) para no duplicar.
            - Distribuye los elementos con posiciones razonables (por ejemplo, en cuadrícula).
            - Si la imagen no es un diagrama UML, explícalo y no crees nada.
            - Al terminar, resume en una o dos frases qué construiste.
            - Responde en el idioma del usuario.
            """;

    private static final String BUILD_FROM_IMAGE_PROMPT = """
            Analiza la imagen adjunta y construye el diagrama de clases que representa en el diagrama actual.
            Crea todos los elementos, atributos, métodos y relaciones que puedas identificar.
            """;

    /** Tools permitidas para el flujo "construir desde imagen" (solo lectura y creación). */
    private static final Set<String> BUILD_TOOLS = Set.of(
            "list_elements", "list_attributes", "list_methods", "list_relationships", "get_diagram_state",
            "create_element", "add_attribute", "add_method", "add_parameter", "create_relationship",
            "set_multiplicity");

    private final AiChatProviderInfo providerInfo;
    private final AiChatProviderRegistry registry;
    private final List<Provider> providers;
    private final List<Provider> imageProviders;

    public AiChatService(UmlTools umlTools, AiChatProviderInfo providerInfo,
                         @Qualifier("aiChatProviderRegistry") AiChatProviderRegistry registry,
                         @Qualifier("aiChatVisionRegistry") AiChatProviderRegistry visionRegistry) {
        this.providerInfo = providerInfo;
        this.registry = registry;

        ToolCallback[] allCallbacks = ToolCallbacks.from(umlTools);
        ToolCallback[] buildCallbacks = Arrays.stream(allCallbacks)
                .filter(callback -> BUILD_TOOLS.contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);

        this.providers = registry.entries().stream()
                .map(entry -> new Provider(entry, ChatClient.builder(entry.model())
                        .defaultSystem(SYSTEM_PROMPT)
                        .defaultTools(allCallbacks)
                        .build()))
                .toList();

        this.imageProviders = visionRegistry.entries().stream()
                .map(entry -> new Provider(entry, ChatClient.builder(entry.model())
                        .defaultSystem(IMAGE_SYSTEM_PROMPT)
                        .defaultTools(buildCallbacks)
                        .build()))
                .toList();
    }

    public AiChatOutcome chat(String projectId, String diagramId, String userId, String message,
                              List<Map<String, String>> history) {
        ensureConfigured();

        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = context(projectId, diagramId, userId, recorder);

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
        messages.add(new UserMessage(message));

        return execute(providers, messages, context, recorder);
    }

    /**
     * Construye el diagrama a partir de una imagen usando un modelo multimodal. Solo usa
     * tools de lectura y creación (no edita ni elimina lo existente).
     */
    public AiChatOutcome diagramFromImage(String projectId, String diagramId, String userId,
                                          byte[] image, String contentType, String instructions) {
        if (imageProviders.isEmpty()) {
            throw new LlmException(
                    "No hay modelos con visión configurados (define OPENROUTER_VISION_MODELS o GEMINI_VISION_MODELS)",
                    false);
        }

        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = context(projectId, diagramId, userId, recorder);

        String mime = (contentType == null || contentType.isBlank()) ? "image/png" : contentType;
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(mime))
                .data(image)
                .build();

        StringBuilder prompt = new StringBuilder(BUILD_FROM_IMAGE_PROMPT);
        if (instructions != null && !instructions.isBlank()) {
            prompt.append("\nInstrucciones adicionales: ").append(instructions);
        }

        UserMessage userMessage = UserMessage.builder().text(prompt.toString()).media(media).build();

        return execute(imageProviders, List.of(userMessage), context, recorder);
    }

    private void ensureConfigured() {
        if (!providerInfo.configured() || providers.isEmpty()) {
            throw new LlmException(
                    "El asistente de IA no está configurado (define OPENROUTER_API_KEYS o GEMINI_API_KEYS y sus modelos)",
                    false);
        }
    }

    private Map<String, Object> context(String projectId, String diagramId, String userId, ActionRecorder recorder) {
        Map<String, Object> context = new HashMap<>();
        context.put(UmlTools.CTX_DIAGRAM_ID, diagramId);
        context.put(UmlTools.CTX_PROJECT_ID, projectId);
        context.put(UmlTools.CTX_USER_ID, userId);
        context.put(UmlTools.CTX_ACTIONS, recorder);
        return context;
    }

    private AiChatOutcome execute(List<Provider> providerList, List<Message> messages,
                                  Map<String, Object> context, ActionRecorder recorder) {
        int size = providerList.size();
        int start = registry.cursor();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int index = (start + i) % size;
            Provider provider = providerList.get(index);
            if (registry.isCoolingDown(provider.entry.cooldownKey())) continue;

            try {
                String reply = provider.client.prompt()
                        .messages(messages)
                        .toolContext(context)
                        .call()
                        .content();
                registry.moveCursorTo(index);
                return new AiChatOutcome(reply == null ? "" : reply, recorder.actions());
            } catch (Exception e) {
                if (QuotaErrors.isProviderWideQuota(e)) {
                    // Créditos de la cuenta agotados: se enfría todo el proveedor.
                    registry.coolDownProvider(provider.entry.id(), provider.entry.quotaCooldownSeconds());
                } else if (QuotaErrors.isQuotaRelated(e)) {
                    // Cuota/límite de esa combinación (key + modelo): se enfría solo esa.
                    registry.coolDownEntry(provider.entry.cooldownKey(), provider.entry.quotaCooldownSeconds());
                } else {
                    // Fallo puntual (modelo saturado, no disponible, etc.): cooldown corto.
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
