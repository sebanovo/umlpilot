package com.umlpilot.umlpilot_api.infrastructure.xmi;

import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;
import com.umlpilot.umlpilot_api.application.port.out.XmiDocumentParser;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parser XMI 2.5.1 de diagramas de clases. Usa JAXP/DOM en modo seguro (sin DTD ni
 * entidades externas) para evitar XXE, valida versión/namespace y la allow-list de
 * elementos, y produce un {@link ImportedDiagram} aditivo.
 */
@Component
public class DomXmiParser implements XmiDocumentParser {

    static final String NS_XMI = "http://www.omg.org/spec/XMI/20131001";
    static final String NS_UML = "http://www.omg.org/spec/UML/20131001";
    static final String XMI_VERSION = "2.5.1";

    private static final Set<String> ELEMENT_TAGS = Set.of("Class", "Interface", "Enumeration", "Comment", "DataType");
    private static final Set<String> RELATION_TAGS =
            Set.of("Association", "AssociationClass", "Generalization", "Realization", "InterfaceRealization");
    private static final Set<String> CONTAINER_TAGS = Set.of("Package", "Model");
    private static final String UNSUPPORTED_ELEMENTS_MESSAGE =
            "Solo se permiten Clase, Interfaz, Enum, Nota y las relaciones "
                    + "Asociación, Generalización, Composición, Agregación, Clase asociación y Realización.";

    /** Elementos de otros diagramas que sí se rechazan de forma explícita. */
    private static final Set<String> UNSUPPORTED_ELEMENT_TAGS = Set.of(
            "Component", "Node", "Device", "ExecutionEnvironment", "Artifact",
            "DeploymentSpecification", "Deployment", "Manifestation", "InformationItem");

    private static final Set<String> OTHER_DIAGRAM_TAGS = Set.of(
            "Actor", "UseCase", "Lifeline", "Interaction", "InteractionFragment", "Message",
            "OccurrenceSpecification", "Activity", "ActivityPartition", "ActivityNode", "Action",
            "InitialNode", "ActivityFinalNode", "FlowFinalNode", "DecisionNode", "MergeNode",
            "ForkNode", "JoinNode", "StateMachine", "State", "Region", "Pseudostate", "FinalState",
            "Transition", "ProtocolStateMachine", "Collaboration", "CollaborationUse",
            "InteractionUse", "CombinedFragment", "ExecutionSpecification");

    private static final Map<String, String> CANONICAL_PRIMITIVES = Map.ofEntries(
            Map.entry("string", "String"), Map.entry("int", "int"), Map.entry("integer", "Integer"),
            Map.entry("long", "long"), Map.entry("double", "double"), Map.entry("float", "float"),
            Map.entry("boolean", "boolean"), Map.entry("bool", "boolean"), Map.entry("char", "char"),
            Map.entry("date", "Date"), Map.entry("localdate", "LocalDate"),
            Map.entry("localdatetime", "LocalDateTime"), Map.entry("timestamp", "LocalDateTime"),
            Map.entry("bigdecimal", "BigDecimal"), Map.entry("decimal", "BigDecimal"),
            Map.entry("uuid", "UUID"), Map.entry("void", "void"));

    private static final int MAX_NAME_LENGTH = 255;

    @Override
    public ImportedDiagram parse(byte[] xmi) {
        if (xmi == null || xmi.length == 0) {
            throw new DomainException("El archivo XMI está vacío");
        }
        Document document = parseSecurely(xmi);
        Element root = document.getDocumentElement();
        validateVersion(root);

        Element model = findModel(root);
        if (model == null) {
            throw new DomainException("El archivo XMI no contiene un modelo UML (uml:Model)");
        }

        Map<String, String> namesById = new HashMap<>();
        indexElementNames(model, namesById);

        Map<String, ImportedDiagram.Bounds> bounds = new HashMap<>();
        collectBounds(root, bounds);

        List<String> warnings = new ArrayList<>();
        List<ImportedDiagram.ElementSpec> elements = new ArrayList<>();
        List<ImportedDiagram.RelationshipSpec> relationships = new ArrayList<>();
        Set<String> unsupportedTypes = new LinkedHashSet<>();
        processNode(model, namesById, bounds, warnings, elements, relationships, unsupportedTypes);
        if (elements.isEmpty() && relationships.isEmpty()) {
            if (!unsupportedTypes.isEmpty()) {
                String type = unsupportedTypes.iterator().next();
                throw new DomainException("Elemento no soportado: " + type + ". " + UNSUPPORTED_ELEMENTS_MESSAGE);
            }
            throw new DomainException("Tipo de diagrama no soportado. Solo se aceptan diagramas de clases.");
        }
        return new ImportedDiagram(elements, relationships, warnings);
    }

    // ------------------------------------------------------------------
    // Documento y validación de versión
    // ------------------------------------------------------------------

    private Document parseSecurely(byte[] xmi) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmi));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new DomainException("El archivo XMI no es válido: " + e.getMessage());
        }
    }

    private void validateVersion(Element root) {
        String version = root.getAttributeNS(NS_XMI, "version");
        String namespace = root.getNamespaceURI();
        boolean isXmiRoot = root.getLocalName() != null && "XMI".equals(root.getLocalName());
        boolean correctNamespace = NS_XMI.equals(namespace);

        if (correctNamespace && isXmiRoot && (version == null || version.isBlank() || XMI_VERSION.equals(version))) {
            return;
        }
        String detected = version == null || version.isBlank() ? "desconocida" : version;
        if (!correctNamespace && (version == null || version.isBlank())) {
            detected = "desconocida";
        }
        throw new DomainException(
                "Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: " + detected);
    }

    private Element findModel(Element root) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "Model".equals(localName(node))) {
                return (Element) node;
            }
        }
        return null;
    }

    /** Índice xmi:id → nombre de los elementos de modelo (para resolver tipos por referencia). */
    private void indexElementNames(Element node, Map<String, String> namesById) {
        for (Element child : elementChildren(node)) {
            if (!"packagedElement".equals(localName(child))) {
                continue;
            }
            String type = umlType(child);
            String id = xmiAttr(child, "id");
            if (id != null && (ELEMENT_TAGS.contains(type) || "AssociationClass".equals(type))) {
                namesById.put(id, displayName(child, type));
            }
            if (CONTAINER_TAGS.contains(type)) {
                indexElementNames(child, namesById);
            }
        }
    }

    // ------------------------------------------------------------------
    // Recorrido del modelo
    // ------------------------------------------------------------------

    private void processNode(Element node, Map<String, String> namesById, Map<String, ImportedDiagram.Bounds> bounds,
                             List<String> warnings, List<ImportedDiagram.ElementSpec> elements,
                             List<ImportedDiagram.RelationshipSpec> relationships, Set<String> unsupportedTypes) {
        for (Element child : elementChildren(node)) {
            // Solo el contenido del diagrama vive en `packagedElement`. El resto son
            // envoltorios/construcciones de la herramienta de origen (primitivetypes,
            // profiles, diagrams, profileApplication, xmi:Extension…) que se ignoran.
            if (!"packagedElement".equals(localName(child))) {
                continue;
            }
            String type = umlType(child);
            if (CONTAINER_TAGS.contains(type)) {
                processNode(child, namesById, bounds, warnings, elements, relationships, unsupportedTypes);
            } else if (ELEMENT_TAGS.contains(type)) {
                String id = xmiAttr(child, "id");
                if (!isDrawn(id, bounds)) {
                    warnings.add("Se omitió '" + displayName(child, type) + "' porque no está en el diagrama");
                    continue;
                }
                elements.add(elementSpec(child, type, bounds, namesById, warnings));
                processNestedRelations(child, warnings, namesById, relationships);
            } else if ("Association".equals(type) || "AssociationClass".equals(type)) {
                processAssociation(child, type, namesById, bounds, warnings, elements, relationships);
            } else if ("Generalization".equals(type)) {
                relationship(child, "generalization", attr(child, "specific"), attr(child, "general"),
                        null, null, null, warnings, namesById, relationships);
            } else if ("Realization".equals(type) || "InterfaceRealization".equals(type)) {
                relationship(child, "realization", attr(child, "client"), supplierOf(child),
                        null, null, null, warnings, namesById, relationships);
            } else if (OTHER_DIAGRAM_TAGS.contains(type)) {
                // Un archivo XMI de EA puede contener varios diagramas (actividad, estados,
                // casos de uso...). Se ignoran sus elementos y se importa la parte de clases.
            } else if (UNSUPPORTED_ELEMENT_TAGS.contains(type)) {
                unsupportedTypes.add(type);
            }
            // Cualquier otro tipo (perfil, estereotipo, extensión, primitivo…) se ignora.
        }
    }

    /** Generalizaciones y realizaciones vienen anidadas dentro del elemento que las origina. */
    private void processNestedRelations(Element owner, List<String> warnings, Map<String, String> namesById,
                                        List<ImportedDiagram.RelationshipSpec> relationships) {
        String ownerId = xmiAttr(owner, "id");
        for (Element child : elementChildren(owner)) {
            String type = umlType(child);
            if ("Generalization".equals(type)) {
                relationship(child, "generalization", ownerId, attr(child, "general"),
                        null, null, null, warnings, namesById, relationships);
            } else if ("Realization".equals(type) || "InterfaceRealization".equals(type)) {
                relationship(child, "realization", ownerId, supplierOf(child),
                        null, null, null, warnings, namesById, relationships);
            }
        }
    }

    private void processAssociation(Element association, String type, Map<String, String> namesById,
                                    Map<String, ImportedDiagram.Bounds> bounds, List<String> warnings,
                                    List<ImportedDiagram.ElementSpec> elements,
                                    List<ImportedDiagram.RelationshipSpec> relationships) {
        String associationId = xmiAttr(association, "id");
        boolean associationClass = "AssociationClass".equals(type);

        if (associationClass) {
            if (!isDrawn(associationId, bounds)) {
                warnings.add("Se omitió la clase asociación '" + displayName(association, type)
                        + "' porque no está en el diagrama");
                return;
            }
            // La clase asociación es, a la vez, un elemento (con atributos) y una relación.
            elements.add(elementSpec(association, "Class", bounds, namesById, warnings));
        }

        List<Element> ends = new ArrayList<>();
        for (Element child : elementChildren(association)) {
            if ("ownedEnd".equals(localName(child)) || attr(child, "association") != null) {
                ends.add(child);
            }
        }
        if (ends.size() < 2) {
            warnings.add("Se omitió una relación sin dos extremos: " + displayName(association, type));
            return;
        }

        Element first = ends.get(0);
        Element second = ends.get(1);
        String relationshipType = associationClass ? "association_class" : aggregationType(ends);

        relationship(association, relationshipType, typeIdOf(first), typeIdOf(second),
                multiplicity(first), multiplicity(second), associationClass ? associationId : null,
                warnings, namesById, relationships);
    }

    private void relationship(Element node, String type, String sourceId, String targetId,
                              String sourceMultiplicity, String targetMultiplicity, String associationClassId,
                              List<String> warnings, Map<String, String> namesById,
                              List<ImportedDiagram.RelationshipSpec> relationships) {
        boolean known = sourceId != null && targetId != null
                && namesById.containsKey(sourceId) && namesById.containsKey(targetId);
        if (!known) {
            warnings.add("Se omitió una relación con extremos no reconocidos: " + displayName(node, type));
            return;
        }
        relationships.add(new ImportedDiagram.RelationshipSpec(
                type, sourceId, targetId, attr(node, "name"),
                sourceMultiplicity, targetMultiplicity, associationClassId));
    }

    private ImportedDiagram.ElementSpec elementSpec(Element node, String type,
                                                    Map<String, ImportedDiagram.Bounds> bounds,
                                                    Map<String, String> namesById,
                                                    List<String> warnings) {
        String id = xmiAttr(node, "id");
        String rawName = displayName(node, type);
        String name = rawName;
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
            warnings.add("El texto de un elemento se truncó a " + MAX_NAME_LENGTH + " caracteres");
        }
        String elementType = switch (type) {
            case "Interface" -> "interface";
            case "Enumeration" -> "enumeration";
            case "Comment" -> "note";
            default -> "class"; // Class y DataType se importan como clase
        };

        ImportedDiagram.Bounds b = id == null ? null : bounds.get(id);
        return new ImportedDiagram.ElementSpec(
                id, elementType, name == null || name.isBlank() ? "Sin nombre" : name,
                normalizeVisibility(attr(node, "visibility")),
                b == null ? null : b.x(), b == null ? null : b.y(),
                b == null ? null : b.width(), b == null ? null : b.height(),
                attributeSpecs(node, namesById),
                methodSpecs(node, namesById),
                literalSpecs(node));
    }

    private List<ImportedDiagram.AttributeSpec> attributeSpecs(Element owner, Map<String, String> namesById) {
        List<ImportedDiagram.AttributeSpec> attributes = new ArrayList<>();
        for (Element child : elementChildren(owner)) {
            if (!"ownedAttribute".equals(localName(child)) || attr(child, "association") != null) {
                continue;
            }
            String name = attr(child, "name");
            if (name == null || name.isBlank()) {
                continue;
            }
            Element typeNode = firstChild(child, "type");
            String dataType = resolveType(typeNode == null ? null : xmiAttr(typeNode, "idref"), namesById);
            attributes.add(new ImportedDiagram.AttributeSpec(
                    name, dataType, normalizeVisibility(attr(child, "visibility")), attr(child, "defaultValue")));
        }
        return attributes;
    }

    private List<ImportedDiagram.MethodSpec> methodSpecs(Element owner, Map<String, String> namesById) {
        List<ImportedDiagram.MethodSpec> methods = new ArrayList<>();
        for (Element child : elementChildren(owner)) {
            if (!"ownedOperation".equals(localName(child))) {
                continue;
            }
            String name = attr(child, "name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String returnType = "void";
            List<ImportedDiagram.ParameterSpec> parameters = new ArrayList<>();
            for (Element parameter : elementChildren(child)) {
                if (!"ownedParameter".equals(localName(parameter))) {
                    continue;
                }
                String direction = attr(parameter, "direction");
                String parameterType = resolveType(attr(parameter, "type"), namesById);
                if ("return".equals(direction) || "return".equals(attr(parameter, "name"))) {
                    returnType = orDefault(parameterType, "void");
                } else {
                    parameters.add(new ImportedDiagram.ParameterSpec(
                            orDefault(attr(parameter, "name"), "param"),
                            parameterType,
                            direction == null ? "in" : direction));
                }
            }
            methods.add(new ImportedDiagram.MethodSpec(
                    name, returnType, normalizeVisibility(attr(child, "visibility")), parameters));
        }
        return methods;
    }

    private List<ImportedDiagram.LiteralSpec> literalSpecs(Element owner) {
        List<ImportedDiagram.LiteralSpec> literals = new ArrayList<>();
        for (Element child : elementChildren(owner)) {
            if (!"ownedLiteral".equals(localName(child))) {
                continue;
            }
            String name = attr(child, "name");
            if (name != null && !name.isBlank()) {
                literals.add(new ImportedDiagram.LiteralSpec(name, attr(child, "value")));
            }
        }
        return literals;
    }

    // ------------------------------------------------------------------
    // Bounds (dc:bounds de umldi)
    // ------------------------------------------------------------------

    private void collectBounds(Element node, Map<String, ImportedDiagram.Bounds> bounds) {
        if ("bounds".equals(localName(node)) && "dc:bounds".equals(xmiAttr(node, "type"))) {
            Node parent = node.getParentNode();
            if (parent instanceof Element parentElement) {
                String modelElement = attr(parentElement, "modelElement");
                if (modelElement != null) {
                    Integer x = intOrDefault(attr(node, "x"), 0);
                    Integer y = intOrDefault(attr(node, "y"), 0);
                    Integer width = intOrDefault(attr(node, "width"), null);
                    Integer height = intOrDefault(attr(node, "height"), null);
                    bounds.putIfAbsent(modelElement, new ImportedDiagram.Bounds(x, y, width, height));
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collectBounds((Element) child, bounds);
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Indica si un elemento forma parte del diagrama. Si el archivo trae formas con
     * `dc:bounds` (diagram interchange), solo se importan los elementos dibujados;
     * si no trae información de diagrama, se importan todos y se colocan en cuadrícula.
     */
    private boolean isDrawn(String id, Map<String, ImportedDiagram.Bounds> bounds) {
        return bounds.isEmpty() || (id != null && bounds.containsKey(id));
    }

    private String resolveType(String idref, Map<String, String> namesById) {
        if (idref == null || idref.isBlank()) {
            return null;
        }
        String referencedName = namesById.get(idref);
        if (referencedName != null && !referencedName.isBlank()) {
            return referencedName;
        }
        if (idref.startsWith("EAJava_")) {
            String suffix = idref.substring("EAJava_".length()).toLowerCase(Locale.ROOT);
            return CANONICAL_PRIMITIVES.getOrDefault(suffix, suffix);
        }
        return idref;
    }

    private String aggregationType(List<Element> ends) {
        for (Element end : ends) {
            String aggregation = attr(end, "aggregation");
            if ("composite".equals(aggregation)) {
                return "composition";
            }
            if ("shared".equals(aggregation)) {
                return "aggregation";
            }
        }
        return "association";
    }

    private String multiplicity(Element end) {
        Element lowerNode = firstChild(end, "lowerValue");
        Element upperNode = firstChild(end, "upperValue");
        Integer lower = lowerNode == null ? null : intOrDefault(attr(lowerNode, "value"), null);
        String upperRaw = upperNode == null ? null : attr(upperNode, "value");
        if (lower == null && upperRaw == null) {
            return null;
        }
        Integer upper = upperRaw == null ? null : ("*".equals(upperRaw) ? -1 : intOrDefault(upperRaw, -1));
        if (upper != null && upper == -1) {
            return (lower == null ? 0 : lower) + "..*";
        }
        if (lower != null && lower.equals(upper)) {
            return String.valueOf(lower);
        }
        return (lower == null ? 0 : lower) + ".." + (upper == null ? "*" : upper);
    }

    private String typeIdOf(Element end) {
        Element typeNode = firstChild(end, "type");
        return typeNode == null ? null : xmiAttr(typeNode, "idref");
    }

    private String supplierOf(Element node) {
        String supplier = attr(node, "supplier");
        return supplier != null ? supplier : attr(node, "contract");
    }

    private String displayName(Element node, String type) {
        if ("Comment".equals(type)) {
            String body = attr(node, "body");
            if (body != null && !body.isBlank()) {
                return body;
            }
        }
        return attr(node, "name");
    }

    private static String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "public";
        }
        return switch (visibility.trim().toLowerCase(Locale.ROOT)) {
            case "private", "privado" -> "private";
            case "protected", "protegido" -> "protected";
            case "package", "paquete" -> "package";
            default -> "public";
        };
    }

    private static List<Element> elementChildren(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static Element firstChild(Element parent, String name) {
        for (Element child : elementChildren(parent)) {
            if (name.equals(localName(child))) {
                return child;
            }
        }
        return null;
    }

    private static String umlType(Element node) {
        String type = xmiAttr(node, "type");
        if (type != null && !type.isBlank()) {
            int colon = type.indexOf(':');
            return colon >= 0 ? type.substring(colon + 1) : type;
        }
        return localName(node);
    }

    private static String localName(Node node) {
        String local = node.getLocalName();
        return local != null ? local : node.getNodeName();
    }

    private static String xmiAttr(Element node, String local) {
        String value = node.getAttributeNS(NS_XMI, local);
        return value == null || value.isEmpty() ? null : value;
    }

    private static String attr(Element node, String name) {
        String value = node.getAttribute(name);
        return value == null || value.isEmpty() ? null : value;
    }

    private static Integer intOrDefault(String value, Integer fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
