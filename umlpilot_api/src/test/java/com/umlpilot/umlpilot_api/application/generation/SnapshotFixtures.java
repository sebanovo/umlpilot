package com.umlpilot.umlpilot_api.application.generation;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;

import java.util.List;

/** Fixtures de {@link DiagramSnapshot} reutilizables en los tests de generación. */
public final class SnapshotFixtures {

    private SnapshotFixtures() {
    }

    public static DiagramSnapshot.AttributeSnapshot attribute(String id, String name, String dataType) {
        return new DiagramSnapshot.AttributeSnapshot(id, name, dataType, "private", null, null);
    }

    public static DiagramSnapshot.ParameterSnapshot parameter(String id, String name, String dataType) {
        return new DiagramSnapshot.ParameterSnapshot(id, name, dataType, "in");
    }

    public static DiagramSnapshot.MethodSnapshot method(String id, String name, String returnType,
                                                        List<DiagramSnapshot.ParameterSnapshot> parameters) {
        return new DiagramSnapshot.MethodSnapshot(id, name, returnType, "public", parameters);
    }

    public static DiagramSnapshot.ElementSnapshot element(String id, String name, String elementType,
                                                          List<DiagramSnapshot.AttributeSnapshot> attributes,
                                                          List<DiagramSnapshot.MethodSnapshot> methods) {
        return new DiagramSnapshot.ElementSnapshot(id, name, elementType, null, "public", attributes, methods);
    }

    public static DiagramSnapshot.RelationshipSnapshot relationship(String id, String type,
                                                                    String sourceId, String targetId,
                                                                    String sourceMultiplicity, String targetMultiplicity) {
        return new DiagramSnapshot.RelationshipSnapshot(
                id, type, null, sourceId, targetId, sourceMultiplicity, targetMultiplicity, null);
    }

    /**
     * Tres clases: Cliente 1—0..* Pedido (asociación) y Pedido 0..*—0..* Producto (muchos a muchos).
     */
    public static DiagramSnapshot threeClassesWithRelations() {
        DiagramSnapshot.ElementSnapshot cliente = element("e1", "Cliente", "class",
                List.of(attribute("a1", "nombre", "String"), attribute("a2", "edad", "int")),
                List.of());
        DiagramSnapshot.ElementSnapshot pedido = element("e2", "Pedido", "class",
                List.of(attribute("a3", "fecha", "LocalDate"), attribute("a4", "total", "BigDecimal")),
                List.of(method("m1", "calcularTotal", "double", List.of())));
        DiagramSnapshot.ElementSnapshot producto = element("e3", "Producto", "class",
                List.of(attribute("a5", "nombre", "String"), attribute("a6", "precio", "double")),
                List.of());
        return new DiagramSnapshot(
                List.of(cliente, pedido, producto),
                List.of(
                        relationship("r1", "association", "e1", "e2", "1", "0..*"),
                        relationship("r2", "association", "e2", "e3", "0..*", "0..*")));
    }

    /** Dos clases con asociación 1—1. */
    public static DiagramSnapshot oneToOne() {
        DiagramSnapshot.ElementSnapshot persona = element("p1", "Persona", "class",
                List.of(attribute("pa", "nombre", "String")), List.of());
        DiagramSnapshot.ElementSnapshot pasaporte = element("p2", "Pasaporte", "class",
                List.of(attribute("pb", "numero", "String")), List.of());
        return new DiagramSnapshot(
                List.of(persona, pasaporte),
                List.of(relationship("r1", "association", "p1", "p2", "1", "1")));
    }

    /** Diagrama con una relación de herencia (no soportada). */
    public static DiagramSnapshot withInheritance() {
        DiagramSnapshot.ElementSnapshot base = element("b1", "Animal", "class", List.of(), List.of());
        DiagramSnapshot.ElementSnapshot child = element("b2", "Perro", "class", List.of(), List.of());
        return new DiagramSnapshot(
                List.of(base, child),
                List.of(relationship("r1", "generalization", "b2", "b1", null, null)));
    }

    /** Dos clases con una relación del tipo indicado (para validar tipos no soportados). */
    public static DiagramSnapshot withRelationshipType(String type) {
        DiagramSnapshot.ElementSnapshot a = element("g1", "Origen", "class", List.of(), List.of());
        DiagramSnapshot.ElementSnapshot b = element("g2", "Destino", "class", List.of(), List.of());
        return new DiagramSnapshot(
                List.of(a, b),
                List.of(relationship("r1", type, "g1", "g2", null, null)));
    }

    /** Diagrama con una asociación modelada como clase de asociación. */
    public static DiagramSnapshot associationClass() {
        DiagramSnapshot.ElementSnapshot pedido = element("ac1", "Pedido", "class",
                List.of(attribute("x1", "fecha", "LocalDate")), List.of());
        DiagramSnapshot.ElementSnapshot producto = element("ac2", "Producto", "class",
                List.of(attribute("x2", "nombre", "String")), List.of());
        DiagramSnapshot.ElementSnapshot link = element("ac3", "PedidoProducto", "class",
                List.of(attribute("x3", "cantidad", "int")), List.of());
        DiagramSnapshot.RelationshipSnapshot relation = new DiagramSnapshot.RelationshipSnapshot(
                "acr", "association_class", null, "ac1", "ac2", "0..*", "0..*", "ac3");
        return new DiagramSnapshot(List.of(pedido, producto, link), List.of(relation));
    }

    /** Diagrama que declara explícitamente su atributo id (tipo UUID). */
    public static DiagramSnapshot withExplicitId() {
        DiagramSnapshot.ElementSnapshot entity = element("i1", "Cuenta", "class",
                List.of(attribute("i1a", "id", "UUID"), attribute("i1b", "saldo", "BigDecimal")),
                List.of());
        return new DiagramSnapshot(List.of(entity), List.of());
    }

    /** Diagrama con solo notas (no genera entidades). */
    public static DiagramSnapshot notesOnly() {
        return new DiagramSnapshot(
                List.of(element("n1", "Nota", "note", List.of(), List.of())),
                List.of());
    }

    /** Diagrama con {@code count} clases para probar el tope. */
    public static DiagramSnapshot manyClasses(int count) {
        List<DiagramSnapshot.ElementSnapshot> elements = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(element("e" + i, "Clase" + i, "class",
                    List.of(attribute("a" + i, "nombre", "String")), List.of()));
        }
        return new DiagramSnapshot(elements, List.of());
    }

    /** Diagrama con tipos de elemento que deben tratarse como entidad. */
    public static DiagramSnapshot mixedElementTypes() {
        return new DiagramSnapshot(List.of(
                element("t1", "Interfaz", "interface", List.of(), List.of()),
                element("t2", "Enumeracion", "enumeration", List.of(), List.of()),
                element("t3", "Tabla", "table", List.of(), List.of()),
                element("t4", "Raro", "cosa", List.of(), List.of()),
                element("t5", "Nota", "note", List.of(), List.of())), List.of());
    }

    /**
     * Diagrama reportado por el usuario: {@code Cliente} y {@code Pedido} con {@code id}
     * de tipo {@code int} (PK) y {@code id_cliente} como atributo, más una asociación 1—1..*.
     */
    public static DiagramSnapshot reportedDiagram() {
        DiagramSnapshot.ElementSnapshot cliente = element("c1", "Cliente", "class",
                List.of(attribute("c1a", "nombre", "String"), attribute("c1b", "id", "int")),
                List.of());
        DiagramSnapshot.ElementSnapshot pedido = element("p1", "Pedido", "class",
                List.of(
                        attribute("p1a", "producto", "String"),
                        attribute("p1b", "id", "int"),
                        attribute("p1c", "id_cliente", "int")),
                List.of());
        return new DiagramSnapshot(
                List.of(cliente, pedido),
                List.of(relationship("r1", "association", "c1", "p1", "1", "1..*")));
    }
}
