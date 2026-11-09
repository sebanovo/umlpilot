package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umlpilot.umlpilot_api.application.dto.DiagramBuildResult;
import com.umlpilot.umlpilot_api.application.dto.DiagramSpec;
import com.umlpilot.umlpilot_api.application.service.DiagramBuildService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ejecuta el chat con function calling sobre el diagrama indicado, rota entre proveedores
 * (OpenRouter/Gemini) cuando uno falla o se queda sin cuota, y construye diagramas
 * completos (desde imagen o texto) a partir de un JSON ({@link DiagramSpec}) aplicado por
 * {@link DiagramBuildService}. Si el JSON falla tras un reintento, recurre a las tools.
 */
@Service
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            Eres un asistente experto en modelado UML integrado en un editor de diagramas de clases.
            Ayudas a crear, consultar, editar y eliminar elementos UML (clases, interfaces, enumeraciones, notas),
            sus atributos, métodos y parámetros, y las relaciones entre ellos (asociación, agregación, composición,
            generalización, realización y clase asociación).

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
              realization) y nombre si aparece en la imagen.
            - SOLO puedes crear y leer; nunca edites ni elimines elementos existentes.
            - Antes de crear, consulta el estado actual (get_diagram_state o list_elements) para no duplicar.
            - Distribuye los elementos con posiciones razonables (por ejemplo, en cuadrícula).
            - Si la imagen no es un diagrama UML, explícalo y no crees nada.
            - Al terminar, resume en una o dos frases qué construiste.
            - Responde en el idioma del usuario.
            """;

    private static final String JSON_SYSTEM_PROMPT = """
            Eres un modelador UML. Devuelve EXCLUSIVAMENTE un objeto JSON válido (sin texto adicional, sin ```),
            con este esquema exacto:
            {
              "title": "string",
              "elements": [
                {
                  "key": "identificador_local_unico",
                  "elementType": "class|interface|enumeration|note",
                  "name": "string",
                  "attributes": [ { "name": "string", "dataType": "string", "visibility": "public|private|protected", "alias": "string|null", "stereotype": "string|null", "defaultValue": "string|null" } ],
                  "methods": [ { "name": "string", "returnType": "string", "visibility": "public|private|protected", "alias": "string|null", "stereotype": "string|null", "defaultValue": "string|null", "parameters": [ { "name": "string", "dataType": "string", "direction": "in|out|inout", "notes": "string|null" } ] } ]
                }
              ],
              "relationships": [
                { "type": "association|aggregation|composition|generalization|realization|association_class", "sourceKey": "key", "targetKey": "key", "name": "string|null", "sourceMultiplicity": "string|null", "targetMultiplicity": "string|null" }
              ]
            }
            Reglas:
            - "key" es un identificador local único por elemento (puede ser el nombre).
            - Cada relación referencia sus extremos con "sourceKey"/"targetKey".
            - Las multiplicidades son texto: "1", "0..1", "1..*", "0..*".
            - No incluyas coordenadas ni posiciones.
            - Si un elemento no tiene atributos o métodos, usa [].
            - No inventes elementos que no aparezcan en la fuente.
            - Devuelve SOLO el JSON.
            """;

    private static final String IMAGE_SPEC_PROMPT =
            "Analiza la imagen (un diagrama UML de clases) y devuelve el JSON del diagrama que representa.";

    private static final String TEXT_SPEC_PROMPT =
            "Construye un diagrama UML de clases a partir de la siguiente descripción y devuelve el JSON.";

    private static final String BUILD_FROM_IMAGE_PROMPT = """
            Analiza la imagen adjunta y construye el diagrama de clases que representa en el diagrama actual.
            Crea todos los elementos, atributos, métodos y relaciones que puedas identificar.
            """;

    /** Tools permitidas para el flujo "construir desde imagen" (solo lectura y creación). */
    private static final Set<String> BUILD_TOOLS = Set.of(
            "list_elements", "list_attributes", "list_methods", "list_relationships", "get_diagram_state",
            "create_element", "add_attribute", "add_method", "add_parameter", "create_relationship",
            "set_multiplicity");

    /** Tools de solo lectura para viewers y contextos sin permiso de mutación. */
    private static final Set<String> READ_ONLY_TOOLS = Set.of(
            "list_elements", "list_attributes", "list_methods", "list_relationships", "get_diagram_state");

    private static final int SPEC_ATTEMPTS = 2;

    private final AiChatProviderInfo providerInfo;
    private final AiChatProviderRegistry registry;
    private final AiChatProviderRegistry visionRegistry;
    private final DiagramBuildService diagramBuildService;
    private final DiagramEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final List<Provider> providers;
    private final List<Provider> readOnlyProviders;
    private final List<Provider> plainProviders;
    private final List<Provider> specProviders;
    private final List<Provider> imageProviders;
    private final List<Provider> visionSpecProviders;

    public AiChatService(UmlTools umlTools, AiChatProviderInfo providerInfo,
                         @Qualifier("aiChatProviderRegistry") AiChatProviderRegistry registry,
                         @Qualifier("aiChatVisionRegistry") AiChatProviderRegistry visionRegistry,
                         DiagramBuildService diagramBuildService, DiagramEventPublisher eventPublisher) {
        this.providerInfo = providerInfo;
        this.registry = registry;
        this.visionRegistry = visionRegistry;
        this.diagramBuildService = diagramBuildService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ToolCallback[] allCallbacks = ToolCallbacks.from(umlTools);
        ToolCallback[] buildCallbacks = Arrays.stream(allCallbacks)
                .filter(callback -> BUILD_TOOLS.contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        ToolCallback[] readOnlyCallbacks = Arrays.stream(allCallbacks)
                .filter(callback -> READ_ONLY_TOOLS.contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);

        this.providers = registry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), SYSTEM_PROMPT, allCallbacks)))
                .toList();
        this.readOnlyProviders = registry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), SYSTEM_PROMPT, readOnlyCallbacks)))
                .toList();
        this.plainProviders = registry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), "Eres un asistente útil y conciso.", null)))
                .toList();
        this.specProviders = registry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), JSON_SYSTEM_PROMPT, null)))
                .toList();
        this.imageProviders = visionRegistry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), IMAGE_SYSTEM_PROMPT, buildCallbacks)))
                .toList();
        this.visionSpecProviders = visionRegistry.entries().stream()
                .map(entry -> new Provider(entry, client(entry.model(), JSON_SYSTEM_PROMPT, null)))
                .toList();
    }

    private static ChatClient client(OpenAiChatModel model, String system, ToolCallback[] tools) {
        ChatClient.Builder builder = ChatClient.builder(model).defaultSystem(system);
        if (tools != null && tools.length > 0) {
            builder = builder.defaultTools(tools);
        }
        return builder.build();
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

        return execute(registry, providers, messages, context, recorder);
    }

    /**
     * Ejecuta el asistente con un historial ya construido por el chat grupal. Permite
     * deshabilitar las mutaciones para viewers y las tools para tareas de resumen.
     */
    public AiChatResult chatWithContext(String projectId, String diagramId, String userId,
                                        List<LlmMessage> history, boolean allowMutations,
                                        boolean toolsEnabled) {
        ensureConfigured();
        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = context(projectId, diagramId, userId, recorder);
        List<Message> messages = history == null ? new ArrayList<>()
                : history.stream().map(AiChatService::toMessage).toList();

        List<Provider> selected;
        if (!toolsEnabled) {
            selected = plainProviders;
        } else {
            selected = allowMutations ? providers : readOnlyProviders;
        }
        Execution execution = executeContent(registry, selected, messages, context);
        return new AiChatResult(execution.content() == null ? "" : execution.content(),
                recorder.actions(), execution.provider(), execution.model());
    }

    static Set<String> readOnlyToolNames() {
        return READ_ONLY_TOOLS;
    }

    private static Message toMessage(LlmMessage message) {
        if (message == null || message.content() == null) {
            return new UserMessage("");
        }
        return switch (message.role() == null ? "user" : message.role().toLowerCase()) {
            case "assistant" -> new AssistantMessage(message.content());
            case "system" -> new SystemMessage(message.content());
            default -> new UserMessage(message.content());
        };
    }

    /** Construye el diagrama completo desde una descripción de texto. */
    public AiChatOutcome diagramFromText(String projectId, String diagramId, String userId, String prompt) {
        if (isBlank(prompt)) {
            throw new LlmException("La descripción es requerida", false);
        }
        AiChatOutcome viaSpec = tryBuildFromSpec(projectId, diagramId, userId, null, null, prompt);
        if (viaSpec != null) return viaSpec;
        // Respaldo: usar las tools del chat normal.
        return chat(projectId, diagramId, userId, prompt, List.of());
    }

    /** Construye el diagrama completo desde una imagen. */
    public AiChatOutcome diagramFromImage(String projectId, String diagramId, String userId,
                                          byte[] image, String contentType, String instructions) {
        AiChatOutcome viaSpec = tryBuildFromSpec(projectId, diagramId, userId, image, contentType, instructions);
        if (viaSpec != null) return viaSpec;
        return imageViaTools(projectId, diagramId, userId, image, contentType, instructions);
    }

    /**
     * Intenta generar un {@link DiagramSpec} (JSON), validarlo y construirlo. Reintenta una
     * vez con el error anterior. Devuelve {@code null} si no lo consigue (para el respaldo).
     */
    private AiChatOutcome tryBuildFromSpec(String projectId, String diagramId, String userId,
                                           byte[] image, String contentType, String input) {
        AiChatProviderRegistry specRegistry = image != null ? visionRegistry : registry;
        List<Provider> specList = image != null ? visionSpecProviders : specProviders;
        if (specList.isEmpty()) return null;

        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = context(projectId, diagramId, userId, recorder);

        String previousError = null;
        for (int attempt = 0; attempt < SPEC_ATTEMPTS; attempt++) {
            try {
                List<Message> messages = specMessages(image, contentType, input, previousError);
                String content = executeContent(specRegistry, specList, messages, context).content();
                DiagramSpec spec = parseSpec(content);
                DiagramBuildResult result = diagramBuildService.build(diagramId, userId, spec);
                eventPublisher.diagramChanged(diagramId, userId);
                return new AiChatOutcome(summaryText(result), summaryActions(result));
            } catch (Exception e) {
                previousError = e.getMessage();
            }
        }
        return null;
    }

    private List<Message> specMessages(byte[] image, String contentType, String input, String previousError) {
        StringBuilder text = new StringBuilder();
        if (image != null) {
            text.append(IMAGE_SPEC_PROMPT);
            if (!isBlank(input)) text.append("\nInstrucciones adicionales: ").append(input.trim());
        } else {
            text.append(TEXT_SPEC_PROMPT);
            if (!isBlank(input)) text.append("\nDescripción: ").append(input.trim());
        }
        if (previousError != null) {
            text.append("\n\nEl intento anterior falló con: ").append(previousError)
                    .append("\nCorrige y devuelve SOLO el JSON válido, sin texto adicional.");
        }

        if (image != null) {
            String mime = isBlank(contentType) ? "image/png" : contentType;
            Media media = Media.builder().mimeType(MimeType.valueOf(mime)).data(image).build();
            return List.of(UserMessage.builder().text(text.toString()).media(media).build());
        }
        return List.of(new UserMessage(text.toString()));
    }

    private DiagramSpec parseSpec(String content) {
        if (isBlank(content)) {
            throw new DomainException("El modelo no devolvió contenido");
        }
        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, DiagramSpec.class);
        } catch (Exception e) {
            throw new DomainException("JSON inválido: " + e.getMessage());
        }
    }

    private static String extractJson(String content) {
        String s = content.trim();
        if (s.startsWith("```")) {
            int newline = s.indexOf('\n');
            if (newline > 0) s = s.substring(newline + 1);
            int fence = s.lastIndexOf("```");
            if (fence >= 0) s = s.substring(0, fence);
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new DomainException("No se encontró un objeto JSON en la respuesta del modelo");
        }
        return s.substring(start, end + 1);
    }

    private static String summaryText(DiagramBuildResult result) {
        return "Construí el diagrama: " + result.summary() + ".";
    }

    private static List<Map<String, Object>> summaryActions(DiagramBuildResult result) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("action", "create");
        action.put("entity", result.summary());
        action.put("id", "");
        action.put("result", "ok");
        return List.of(action);
    }

    private AiChatOutcome imageViaTools(String projectId, String diagramId, String userId,
                                        byte[] image, String contentType, String instructions) {
        if (imageProviders.isEmpty()) {
            throw new LlmException(
                    "No hay modelos con visión configurados (define OPENROUTER_VISION_MODELS o GEMINI_VISION_MODELS)",
                    false);
        }

        ActionRecorder recorder = new ActionRecorder();
        Map<String, Object> context = context(projectId, diagramId, userId, recorder);

        String mime = isBlank(contentType) ? "image/png" : contentType;
        Media media = Media.builder().mimeType(MimeType.valueOf(mime)).data(image).build();

        StringBuilder prompt = new StringBuilder(BUILD_FROM_IMAGE_PROMPT);
        if (!isBlank(instructions)) {
            prompt.append("\nInstrucciones adicionales: ").append(instructions.trim());
        }
        UserMessage userMessage = UserMessage.builder().text(prompt.toString()).media(media).build();

        return execute(visionRegistry, imageProviders, List.of(userMessage), context, recorder);
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

    private AiChatOutcome execute(AiChatProviderRegistry providerRegistry, List<Provider> providerList,
                                  List<Message> messages, Map<String, Object> context, ActionRecorder recorder) {
        Execution execution = executeContent(providerRegistry, providerList, messages, context);
        return new AiChatOutcome(execution.content() == null ? "" : execution.content(), recorder.actions());
    }

    private Execution executeContent(AiChatProviderRegistry providerRegistry, List<Provider> providerList,
                                     List<Message> messages, Map<String, Object> context) {
        int size = providerList.size();
        int start = providerRegistry.cursor();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int index = (start + i) % size;
            Provider provider = providerList.get(index);
            if (providerRegistry.isCoolingDown(provider.entry.cooldownKey())) continue;

            try {
                String reply = provider.client.prompt()
                        .messages(messages)
                        .toolContext(context)
                        .call()
                        .content();
                providerRegistry.moveCursorTo(index);
                return new Execution(reply, provider.entry.id(), provider.entry.modelName());
            } catch (Exception e) {
                if (QuotaErrors.isProviderWideQuota(e)) {
                    providerRegistry.coolDownProvider(provider.entry.id(), provider.entry.quotaCooldownSeconds());
                } else if (QuotaErrors.isQuotaRelated(e)) {
                    providerRegistry.coolDownEntry(provider.entry.cooldownKey(), provider.entry.quotaCooldownSeconds());
                } else {
                    providerRegistry.coolDownEntry(provider.entry.cooldownKey(), provider.entry.errorCooldownSeconds());
                }
                errors.add(provider.entry.id() + " [" + provider.entry.modelName() + "]: " + e.getMessage());
            }
        }

        String detail = errors.isEmpty()
                ? "todos en enfriamiento (" + providerRegistry.remainingCooldownSeconds() + "s)"
                : String.join(" | ", errors);
        throw new LlmException("Ningún proveedor de IA disponible: " + detail, true);
    }

    private record Execution(String content, String provider, String model) {
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Provider(AiChatProviderRegistry.Entry entry, ChatClient client) {
    }
}
