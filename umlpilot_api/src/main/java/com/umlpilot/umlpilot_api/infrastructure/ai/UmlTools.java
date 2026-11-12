package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.application.service.RelationshipService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso UML expuestos al modelo como tools ({@code @Tool}). Cada tool obtiene el
 * diagrama y el usuario del {@link ToolContext} de la conversación, valida el aislamiento
 * por diagrama y publica los cambios en el canal de tiempo real.
 */
@Component
public class UmlTools {

    public static final String CTX_DIAGRAM_ID = "diagramId";
    public static final String CTX_PROJECT_ID = "projectId";
    public static final String CTX_USER_ID = "userId";
    public static final String CTX_ACTIONS = "actions";

    private final ElementService elementService;
    private final RelationshipService relationshipService;
    private final DiagramEventPublisher eventPublisher;

    public UmlTools(ElementService elementService, RelationshipService relationshipService,
                    DiagramEventPublisher eventPublisher) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------
    // Context helpers
    // ------------------------------------------------------------------

    private String diagram(ToolContext ctx) {
        Object v = ctx.getContext().get(CTX_DIAGRAM_ID);
        if (v == null) throw new DomainException("Falta el contexto del diagrama en la conversación");
        return v.toString();
    }

    private String user(ToolContext ctx) {
        Object v = ctx.getContext().get(CTX_USER_ID);
        return v == null ? "ai-assistant" : v.toString();
    }

    private ActionRecorder recorder(ToolContext ctx) {
        Object v = ctx.getContext().get(CTX_ACTIONS);
        if (!(v instanceof ActionRecorder recorder)) throw new DomainException("Falta el registro de acciones");
        return recorder;
    }

    private ElementResult requireElement(String diagramId, String elementId) {
        if (elementId == null || elementId.isBlank()) throw new DomainException("Se requiere el id del elemento");
        return elementService.getElements(diagramId).stream()
                .filter(e -> e.id().equals(elementId))
                .findFirst()
                .orElseThrow(() -> new DomainException("El elemento " + elementId + " no pertenece a este diagrama"));
    }

    private String findElementOfAttribute(String diagramId, String attributeId) {
        for (ElementResult el : elementService.getElements(diagramId)) {
            if (elementService.getAttributes(el.id()).stream().anyMatch(a -> a.id().equals(attributeId))) return el.id();
        }
        throw new DomainException("El atributo no pertenece a este diagrama");
    }

    private String findElementOfMethod(String diagramId, String methodId) {
        for (ElementResult el : elementService.getElements(diagramId)) {
            if (elementService.getMethods(el.id()).stream().anyMatch(m -> m.id().equals(methodId))) return el.id();
        }
        throw new DomainException("El método no pertenece a este diagrama");
    }

    private String findMethodOfParameter(String diagramId, String parameterId) {
        for (ElementResult el : elementService.getElements(diagramId)) {
            for (MethodResult m : elementService.getMethods(el.id())) {
                if (m.parameters().stream().anyMatch(p -> p.id().equals(parameterId))) return m.id();
            }
        }
        throw new DomainException("El parámetro no pertenece a este diagrama");
    }

    private void requireRelationship(String diagramId, String relationshipId) {
        boolean found = relationshipService.getRelationships(diagramId).stream()
                .anyMatch(r -> r.id().equals(relationshipId));
        if (!found) throw new DomainException("La relación no pertenece a este diagrama");
    }

    private Map<String, Object> ok(String action, String entity, String id, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("action", action);
        map.put("entity", entity);
        map.put("id", id);
        map.put("result", message);
        return map;
    }

    // ------------------------------------------------------------------
    // Element tools
    // ------------------------------------------------------------------

    @Tool(name = "list_elements", description = "Lista todos los elementos UML del diagrama actual (clases, interfaces, enumeraciones, notas) con su id, nombre y tipo. Úsalo para resolver nombres a ids antes de operar.")
    public List<ElementResult> listElements(ToolContext ctx) {
        recorder(ctx).record("read", "elements", "", "ok");
        return elementService.getElements(diagram(ctx));
    }

    @Tool(name = "create_element", description = "Crea un elemento UML en el diagrama actual. elementType debe ser class, interface, enumeration o note.")
    public Map<String, Object> createElement(
            @ToolParam(description = "Tipo: class, interface, enumeration o note") String elementType,
            @ToolParam(description = "Nombre del elemento") String name,
            @ToolParam(description = "Posición X (opcional)", required = false) Integer positionX,
            @ToolParam(description = "Posición Y (opcional)", required = false) Integer positionY,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String type = (elementType == null || elementType.isBlank()) ? "class" : elementType.trim();
        ElementResult created = elementService.createElement(new CreateElementCommand(
                diagramId, user(ctx), name, type,
                positionX == null ? 0 : positionX, positionY == null ? 0 : positionY));
        recorder(ctx).record("create", "element", created.id(), "ok");
        eventPublisher.elementCreated(diagramId, user(ctx), created.id());
        return ok("create", "element", created.id(), "Elemento " + created.name() + " creado");
    }

    @Tool(name = "update_element", description = "Actualiza un elemento UML existente (nombre, visibilidad o posición). Usa list_elements para obtener el id.")
    public Map<String, Object> updateElement(
            @ToolParam(description = "Id del elemento") String elementId,
            @ToolParam(description = "Nuevo nombre (opcional)", required = false) String name,
            @ToolParam(description = "Visibilidad: public, private, protected, package (opcional)", required = false) String visibility,
            @ToolParam(description = "Posición X (opcional)", required = false) Integer positionX,
            @ToolParam(description = "Posición Y (opcional)", required = false) Integer positionY,
            @ToolParam(description = "Ancho (opcional)", required = false) Integer width,
            @ToolParam(description = "Alto (opcional)", required = false) Integer height,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireElement(diagramId, elementId);
        ElementResult updated = elementService.updateElement(diagramId, elementId, name, visibility, null, positionX, positionY, width, height);
        recorder(ctx).record("update", "element", elementId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("update", "element", elementId, "Elemento actualizado: " + updated.name());
    }

    @Tool(name = "delete_element", description = "Elimina un elemento UML y sus atributos, métodos y relaciones asociadas. Usa list_elements para obtener el id.")
    public Map<String, Object> deleteElement(
            @ToolParam(description = "Id del elemento a eliminar") String elementId,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireElement(diagramId, elementId);
        elementService.deleteElement(elementId);
        recorder(ctx).record("delete", "element", elementId, "ok");
        eventPublisher.elementDeleted(diagramId, user(ctx), elementId);
        return ok("delete", "element", elementId, "Elemento eliminado");
    }

    // ------------------------------------------------------------------
    // Attribute tools
    // ------------------------------------------------------------------

    @Tool(name = "list_attributes", description = "Lista los atributos de un elemento UML con su id, nombre, tipo y visibilidad.")
    public List<AttributeResult> listAttributes(
            @ToolParam(description = "Id del elemento") String elementId,
            ToolContext ctx) {
        requireElement(diagram(ctx), elementId);
        recorder(ctx).record("read", "attributes", elementId, "ok");
        return elementService.getAttributes(elementId);
    }

    @Tool(name = "add_attribute", description = "Agrega un atributo a un elemento UML (nombre, tipo de dato, visibilidad).")
    public Map<String, Object> addAttribute(
            @ToolParam(description = "Id del elemento") String elementId,
            @ToolParam(description = "Nombre del atributo") String name,
            @ToolParam(description = "Tipo de dato, por ejemplo String o int") String dataType,
            @ToolParam(description = "Visibilidad: public, private, protected (opcional)", required = false) String visibility,
            @ToolParam(description = "Alias (opcional)", required = false) String alias,
            @ToolParam(description = "Estereotipo (opcional)", required = false) String stereotype,
            @ToolParam(description = "Valor inicial (opcional)", required = false) String defaultValue,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireElement(diagramId, elementId);
        int order = elementService.getAttributes(elementId).size();
        AttributeResult created = elementService.addAttribute(elementId, name, dataType,
                visibility == null ? "private" : visibility, alias, stereotype, defaultValue, order);
        recorder(ctx).record("create", "attribute", created.id(), "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("create", "attribute", created.id(), "Atributo " + created.name() + " agregado");
    }

    @Tool(name = "update_attribute", description = "Actualiza un atributo existente (nombre, tipo, visibilidad, alias, estereotipo o valor inicial).")
    public Map<String, Object> updateAttribute(
            @ToolParam(description = "Id del atributo") String attributeId,
            @ToolParam(description = "Nuevo nombre (opcional)", required = false) String name,
            @ToolParam(description = "Nuevo tipo de dato (opcional)", required = false) String dataType,
            @ToolParam(description = "Nueva visibilidad (opcional)", required = false) String visibility,
            @ToolParam(description = "Nuevo alias (opcional)", required = false) String alias,
            @ToolParam(description = "Nuevo estereotipo (opcional)", required = false) String stereotype,
            @ToolParam(description = "Nuevo valor inicial (opcional)", required = false) String defaultValue,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String elementId = findElementOfAttribute(diagramId, attributeId);
        AttributeResult updated = elementService.updateAttribute(attributeId, name, dataType, visibility, alias, stereotype, defaultValue);
        recorder(ctx).record("update", "attribute", attributeId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("update", "attribute", attributeId, "Atributo actualizado: " + updated.name());
    }

    @Tool(name = "delete_attribute", description = "Elimina un atributo existente de un elemento UML.")
    public Map<String, Object> deleteAttribute(
            @ToolParam(description = "Id del atributo") String attributeId,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String elementId = findElementOfAttribute(diagramId, attributeId);
        elementService.deleteAttribute(attributeId);
        recorder(ctx).record("delete", "attribute", attributeId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("delete", "attribute", attributeId, "Atributo eliminado");
    }

    // ------------------------------------------------------------------
    // Method tools
    // ------------------------------------------------------------------

    @Tool(name = "list_methods", description = "Lista los métodos de un elemento UML con sus parámetros.")
    public List<MethodResult> listMethods(
            @ToolParam(description = "Id del elemento") String elementId,
            ToolContext ctx) {
        requireElement(diagram(ctx), elementId);
        recorder(ctx).record("read", "methods", elementId, "ok");
        return elementService.getMethods(elementId);
    }

    @Tool(name = "add_method", description = "Agrega un método a un elemento UML (nombre, tipo de retorno, visibilidad).")
    public Map<String, Object> addMethod(
            @ToolParam(description = "Id del elemento") String elementId,
            @ToolParam(description = "Nombre del método") String name,
            @ToolParam(description = "Tipo de retorno, por ejemplo void o String (opcional)", required = false) String returnType,
            @ToolParam(description = "Visibilidad: public, private, protected (opcional)", required = false) String visibility,
            @ToolParam(description = "Alias (opcional)", required = false) String alias,
            @ToolParam(description = "Estereotipo (opcional)", required = false) String stereotype,
            @ToolParam(description = "Valor por defecto (opcional)", required = false) String defaultValue,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireElement(diagramId, elementId);
        int order = elementService.getMethods(elementId).size();
        MethodResult created = elementService.addMethod(elementId, name,
                returnType == null ? "void" : returnType,
                visibility == null ? "public" : visibility, alias, stereotype, defaultValue, order);
        recorder(ctx).record("create", "method", created.id(), "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("create", "method", created.id(), "Método " + created.name() + " agregado");
    }

    @Tool(name = "update_method", description = "Actualiza un método existente (nombre, tipo de retorno, visibilidad, alias, estereotipo o valor por defecto).")
    public Map<String, Object> updateMethod(
            @ToolParam(description = "Id del método") String methodId,
            @ToolParam(description = "Nuevo nombre (opcional)", required = false) String name,
            @ToolParam(description = "Nuevo tipo de retorno (opcional)", required = false) String returnType,
            @ToolParam(description = "Nueva visibilidad (opcional)", required = false) String visibility,
            @ToolParam(description = "Nuevo alias (opcional)", required = false) String alias,
            @ToolParam(description = "Nuevo estereotipo (opcional)", required = false) String stereotype,
            @ToolParam(description = "Nuevo valor por defecto (opcional)", required = false) String defaultValue,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String elementId = findElementOfMethod(diagramId, methodId);
        MethodResult updated = elementService.updateMethod(methodId, name, returnType, visibility, alias, stereotype, defaultValue);
        recorder(ctx).record("update", "method", methodId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("update", "method", methodId, "Método actualizado: " + updated.name());
    }

    @Tool(name = "delete_method", description = "Elimina un método y sus parámetros.")
    public Map<String, Object> deleteMethod(
            @ToolParam(description = "Id del método") String methodId,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String elementId = findElementOfMethod(diagramId, methodId);
        elementService.deleteMethod(methodId);
        recorder(ctx).record("delete", "method", methodId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("delete", "method", methodId, "Método eliminado");
    }

    @Tool(name = "add_parameter", description = "Agrega un parámetro a un método existente (nombre, tipo, dirección).")
    public Map<String, Object> addParameter(
            @ToolParam(description = "Id del método") String methodId,
            @ToolParam(description = "Nombre del parámetro") String name,
            @ToolParam(description = "Tipo de dato (opcional)", required = false) String dataType,
            @ToolParam(description = "Dirección: in, out, inout (opcional)", required = false) String direction,
            @ToolParam(description = "Notas (opcional)", required = false) String notes,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String elementId = findElementOfMethod(diagramId, methodId);
        int order = elementService.getMethods(elementId).stream()
                .filter(m -> m.id().equals(methodId)).findFirst()
                .map(m -> m.parameters().size()).orElse(0);
        ParameterResult created = elementService.addParameter(methodId, name, dataType,
                direction == null ? "in" : direction, notes, order);
        recorder(ctx).record("create", "parameter", created.id(), "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("create", "parameter", created.id(), "Parámetro " + created.name() + " agregado");
    }

    @Tool(name = "delete_parameter", description = "Elimina un parámetro de un método.")
    public Map<String, Object> deleteParameter(
            @ToolParam(description = "Id del parámetro") String parameterId,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        String methodId = findMethodOfParameter(diagramId, parameterId);
        String elementId = findElementOfMethod(diagramId, methodId);
        elementService.deleteParameter(parameterId);
        recorder(ctx).record("delete", "parameter", parameterId, "ok");
        eventPublisher.elementUpdated(diagramId, user(ctx), elementId);
        return ok("delete", "parameter", parameterId, "Parámetro eliminado");
    }

    // ------------------------------------------------------------------
    // Relationship tools
    // ------------------------------------------------------------------

    @Tool(name = "list_relationships", description = "Lista las relaciones del diagrama actual (asociaciones, generalizaciones, agregaciones, composiciones, realizaciones, clases de asociación) con su id, tipo y extremos.")
    public List<RelationshipResult> listRelationships(ToolContext ctx) {
        recorder(ctx).record("read", "relationships", "", "ok");
        return relationshipService.getRelationships(diagram(ctx));
    }

    @Tool(name = "create_relationship", description = "Crea una relación entre dos elementos. relationshipType: association, aggregation, composition, generalization, realization o association_class. Usa list_elements para los ids.")
    public Map<String, Object> createRelationship(
            @ToolParam(description = "Id del elemento origen") String sourceElementId,
            @ToolParam(description = "Id del elemento destino") String targetElementId,
            @ToolParam(description = "Tipo de relación") String relationshipType,
            @ToolParam(description = "Nombre de la relación (opcional)", required = false) String name,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireElement(diagramId, sourceElementId);
        requireElement(diagramId, targetElementId);
        String type = (relationshipType == null || relationshipType.isBlank()) ? "association" : relationshipType.trim();

        String associationClassElementId = null;
        if ("association_class".equals(type)) {
            ElementResult src = requireElement(diagramId, sourceElementId);
            ElementResult tgt = requireElement(diagramId, targetElementId);
            int x = (src.positionX() + tgt.positionX()) / 2;
            int y = (src.positionY() + tgt.positionY()) / 2 + 90;
            ElementResult cls = elementService.createElement(new CreateElementCommand(
                    diagramId, user(ctx), src.name() + "_" + tgt.name(), "class", x, y));
            associationClassElementId = cls.id();
            eventPublisher.elementCreated(diagramId, user(ctx), cls.id());
        }

        RelationshipResult created = relationshipService.createRelationship(new CreateRelationshipCommand(
                diagramId, sourceElementId, targetElementId, type, associationClassElementId, null, null));
        if (name != null && !name.isBlank()) {
            created = relationshipService.updateRelationship(created.id(), name, null, null, null, null, null, null, null);
        }
        recorder(ctx).record("create", "relationship", created.id(), "ok");
        eventPublisher.relationshipCreated(diagramId, user(ctx), created.id());
        return ok("create", "relationship", created.id(), "Relación " + type + " creada");
    }

    @Tool(name = "update_relationship", description = "Actualiza una relación existente (nombre, tipo o dirección).")
    public Map<String, Object> updateRelationship(
            @ToolParam(description = "Id de la relación") String relationshipId,
            @ToolParam(description = "Nuevo nombre (opcional)", required = false) String name,
            @ToolParam(description = "Nuevo tipo (opcional)", required = false) String type,
            @ToolParam(description = "Nueva dirección (opcional)", required = false) String direction,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireRelationship(diagramId, relationshipId);
        RelationshipResult updated = relationshipService.updateRelationship(relationshipId, name, type, direction, null, null, null, null, null);
        recorder(ctx).record("update", "relationship", relationshipId, "ok");
        eventPublisher.relationshipUpdated(diagramId, user(ctx), relationshipId);
        return ok("update", "relationship", relationshipId, "Relación actualizada");
    }

    @Tool(name = "delete_relationship", description = "Elimina una relación del diagrama.")
    public Map<String, Object> deleteRelationship(
            @ToolParam(description = "Id de la relación") String relationshipId,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireRelationship(diagramId, relationshipId);
        relationshipService.deleteRelationship(relationshipId);
        recorder(ctx).record("delete", "relationship", relationshipId, "ok");
        eventPublisher.relationshipDeleted(diagramId, user(ctx), relationshipId);
        return ok("delete", "relationship", relationshipId, "Relación eliminada");
    }

    @Tool(name = "set_multiplicity", description = "Define la multiplicidad de un extremo de una relación. end: source o target. min/max son enteros; usa -1 para '*' (ilimitado). Pasa min y max nulos para dejar el extremo sin multiplicidad.")
    public Map<String, Object> setMultiplicity(
            @ToolParam(description = "Id de la relación") String relationshipId,
            @ToolParam(description = "Extremo: source o target") String end,
            @ToolParam(description = "Mínimo (opcional)", required = false) Integer min,
            @ToolParam(description = "Máximo, -1 para * (opcional)", required = false) Integer max,
            ToolContext ctx) {
        String diagramId = diagram(ctx);
        requireRelationship(diagramId, relationshipId);
        String normalizedEnd = (end == null || end.isBlank()) ? "source" : end.trim();
        MultiplicityResult mult = relationshipService.getMultiplicities(relationshipId).stream()
                .filter(m -> m.end().equals(normalizedEnd))
                .findFirst()
                .orElseThrow(() -> new DomainException("El extremo " + normalizedEnd + " no tiene multiplicidad registrada"));
        relationshipService.updateMultiplicity(mult.id(), min, max);
        recorder(ctx).record("update", "multiplicity", mult.id(), "ok");
        eventPublisher.relationshipUpdated(diagramId, user(ctx), relationshipId);
        return ok("update", "multiplicity", mult.id(), "Multiplicidad " + normalizedEnd + " actualizada");
    }

    // ------------------------------------------------------------------
    // Aggregate read tool
    // ------------------------------------------------------------------

    @Tool(name = "get_diagram_state", description = "Devuelve el estado completo del diagrama actual: elementos con sus atributos y métodos, y todas las relaciones. Úsalo para responder preguntas o resolver el contexto antes de operar.")
    public Map<String, Object> getDiagramState(ToolContext ctx) {
        String diagramId = diagram(ctx);
        List<Map<String, Object>> elements = new ArrayList<>();
        for (ElementResult el : elementService.getElements(diagramId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("element", el);
            item.put("attributes", elementService.getAttributes(el.id()));
            item.put("methods", elementService.getMethods(el.id()));
            elements.add(item);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("elements", elements);
        state.put("relationships", relationshipService.getRelationships(diagramId));
        recorder(ctx).record("read", "diagram", diagramId, "ok");
        return state;
    }
}
