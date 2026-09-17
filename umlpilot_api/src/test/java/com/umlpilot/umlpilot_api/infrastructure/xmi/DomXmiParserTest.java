package com.umlpilot.umlpilot_api.infrastructure.xmi;

import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DomXmiParserTest {

    private final DomXmiParser parser = new DomXmiParser();

    private static final String HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.5.1"
                     xmlns:xmi="http://www.omg.org/spec/XMI/20131001"
                     xmlns:uml="http://www.omg.org/spec/UML/20131001"
                     xmlns:umldi="http://www.omg.org/spec/UML/20131001/UMLDI"
                     xmlns:dc="http://www.omg.org/spec/UML/20131001/UMLDC">
            """;

    private static String document(String inner) {
        return HEADER + inner + "\n</xmi:XMI>";
    }

    private static String model(String body) {
        return "<uml:Model xmi:type=\"uml:Model\" name=\"M\">"
                + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"pkg\" name=\"P\">"
                + body
                + "</packagedElement></uml:Model>";
    }

    private ImportedDiagram parse(String xml) {
        return parser.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    // 1.2 XML inválido / XXE

    @Test
    void rejectsMalformedXml() {
        DomainException error = assertThrows(DomainException.class,
                () -> parse("<xmi:XMI this is not xml"));
        assertTrue(error.getMessage().contains("no es válido"), error.getMessage());
    }

    @Test
    void rejectsXxeAndDoctype() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <xmi:XMI xmi:version="2.5.1" xmlns:xmi="http://www.omg.org/spec/XMI/20131001"
                         xmlns:uml="http://www.omg.org/spec/UML/20131001">
                  <uml:Model xmi:type="uml:Model" name="&xxe;"/>
                </xmi:XMI>
                """;
        assertThrows(DomainException.class, () -> parse(xxe));
    }

    // 1.3 versión y namespace

    @Test
    void rejectsUnsupportedVersions() {
        for (String version : new String[] {"2.1", "2.4"}) {
            String xml = """
                    <?xml version="1.0"?>
                    <xmi:XMI xmi:version="%s" xmlns:xmi="http://www.omg.org/spec/XMI/20131001"
                             xmlns:uml="http://www.omg.org/spec/UML/20131001">
                      <uml:Model xmi:type="uml:Model" name="M"/>
                    </xmi:XMI>
                    """.formatted(version);
            DomainException error = assertThrows(DomainException.class, () -> parse(xml));
            assertEquals("Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: " + version,
                    error.getMessage());
        }
    }

    @Test
    void rejectsWrongXmiNamespace() {
        String xml = """
                <?xml version="1.0"?>
                <xmi:XMI xmi:version="2.5.1" xmlns:xmi="http://example.com/wrong"
                         xmlns:uml="http://www.omg.org/spec/UML/20131001">
                  <uml:Model xmi:type="uml:Model" name="M"/>
                </xmi:XMI>
                """;
        DomainException error = assertThrows(DomainException.class, () -> parse(xml));
        assertTrue(error.getMessage().startsWith("Versión XMI no soportada"), error.getMessage());
    }

    // 1.4 allow-list

    @Test
    void rejectsNonClassDiagrams() {
        for (String tag : new String[] {"Actor", "UseCase", "Lifeline"}) {
            String xml = document(model(
                    "<packagedElement xmi:type=\"uml:" + tag + "\" xmi:id=\"x1\" name=\"X\"/>"));
            DomainException error = assertThrows(DomainException.class, () -> parse(xml), tag);
            assertEquals("Tipo de diagrama no soportado. Solo se aceptan diagramas de clases.", error.getMessage());
        }
    }

    @Test
    void rejectsUnsupportedElements() {
        for (String tag : new String[] {"Component", "Artifact"}) {
            String xml = document(model(
                    "<packagedElement xmi:type=\"uml:" + tag + "\" xmi:id=\"x1\" name=\"X\"/>"));
            DomainException error = assertThrows(DomainException.class, () -> parse(xml), tag);
            assertTrue(error.getMessage().startsWith("Elemento no soportado: " + tag + "."), error.getMessage());
        }
    }

    @Test
    void ignoresDiagramInterchangeAndExtensions() {
        String xml = document(model(
                "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c1\" name=\"Cliente\"/>")
                + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"d1\" modelElement=\"pkg\">"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s1\" modelElement=\"c1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"5\" y=\"6\" width=\"100\" height=\"50\"/>"
                + "</ownedElement></umldi:Diagram>"
                + "<xmi:Extension extender=\"Enterprise Architect\"><elements>"
                + "<element xmi:idref=\"c1\"><properties sType=\"Class\"/></element>"
                + "</elements></xmi:Extension>");

        ImportedDiagram imported = parse(xml);

        assertEquals(1, imported.elements().size());
        assertEquals("Cliente", imported.elements().get(0).name());
        assertEquals(5, imported.elements().get(0).positionX());
        assertEquals(6, imported.elements().get(0).positionY());
        assertEquals(100, imported.elements().get(0).width());
        assertEquals(50, imported.elements().get(0).height());
    }

    // 1.5 elementos, atributos, métodos, parámetros, bounds

    @Test
    void mapsElementsAttributesMethodsParametersAndBounds() {
        String xml = document(model("""
                <packagedElement xmi:type="uml:Class" xmi:id="c1" name="Cliente" visibility="public">
                  <ownedAttribute xmi:type="uml:Property" xmi:id="a1" name="nombre" visibility="private">
                    <type xmi:idref="EAJava_string"/>
                  </ownedAttribute>
                  <ownedAttribute xmi:type="uml:Property" xmi:id="a2" name="saldo" visibility="protected">
                    <type xmi:idref="EAJava_bigdecimal"/>
                  </ownedAttribute>
                  <ownedOperation xmi:id="m1" name="saludar">
                    <ownedParameter xmi:id="p1" name="veces" type="EAJava_int"/>
                    <ownedParameter xmi:id="m1_rt" name="return" direction="return" type="EAJava_string"/>
                  </ownedOperation>
                </packagedElement>
                <packagedElement xmi:type="uml:Interface" xmi:id="i1" name="Saludable"/>
                <packagedElement xmi:type="uml:Enumeration" xmi:id="e1" name="Estado">
                  <ownedLiteral xmi:id="l1" name="ACTIVO" value="1"/>
                </packagedElement>
                <packagedElement xmi:type="uml:Comment" xmi:id="n1" body="Nota de prueba"/>
                """)
                + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"d1\" modelElement=\"pkg\">"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s1\" modelElement=\"c1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"120\" y=\"60\" width=\"200\" height=\"120\"/>"
                + "</ownedElement>"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s2\" modelElement=\"i1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"400\" y=\"60\" width=\"100\" height=\"80\"/>"
                + "</ownedElement>"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s3\" modelElement=\"e1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"400\" y=\"200\" width=\"100\" height=\"80\"/>"
                + "</ownedElement>"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s4\" modelElement=\"n1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"600\" y=\"60\" width=\"160\" height=\"60\"/>"
                + "</ownedElement></umldi:Diagram>");

        ImportedDiagram imported = parse(xml);

        assertEquals(4, imported.elements().size());
        ImportedDiagram.ElementSpec cliente = imported.elements().get(0);
        assertEquals("class", cliente.elementType());
        assertEquals(120, cliente.positionX());
        assertEquals(200, cliente.width());
        assertEquals(2, cliente.attributes().size());
        assertEquals("String", cliente.attributes().get(0).dataType());
        assertEquals("private", cliente.attributes().get(0).visibility());
        assertEquals("BigDecimal", cliente.attributes().get(1).dataType());
        assertEquals(1, cliente.methods().size());
        assertEquals("String", cliente.methods().get(0).returnType());
        assertEquals(1, cliente.methods().get(0).parameters().size());
        assertEquals("veces", cliente.methods().get(0).parameters().get(0).name());

        assertEquals("interface", imported.elements().get(1).elementType());
        assertEquals("enumeration", imported.elements().get(2).elementType());
        assertEquals(1, imported.elements().get(2).literals().size());
        assertEquals("note", imported.elements().get(3).elementType());
        assertEquals("Nota de prueba", imported.elements().get(3).name());
    }

    @Test
    void truncatesLongCommentBodiesWithWarning() {
        String longBody = "x".repeat(300);
        String xml = document(model(
                "<packagedElement xmi:type=\"uml:Comment\" xmi:id=\"n1\" body=\"" + longBody + "\"/>"));

        ImportedDiagram imported = parse(xml);

        assertEquals(255, imported.elements().get(0).name().length());
        assertTrue(imported.warnings().stream().anyMatch(w -> w.contains("truncó")));
    }

    // 1.6 relaciones

    @Test
    void mapsAssociationsAggregationAndComposition() {
        String xml = document(model("""
                <packagedElement xmi:type="uml:Class" xmi:id="c1" name="Cliente"/>
                <packagedElement xmi:type="uml:Class" xmi:id="c2" name="Pedido"/>
                <packagedElement xmi:type="uml:Association" xmi:id="r1" name="compra">
                  <ownedEnd xmi:type="uml:Property" xmi:id="r1_src" association="r1">
                    <type xmi:idref="c1"/>
                    <lowerValue xmi:type="uml:LiteralInteger" value="1"/>
                    <upperValue xmi:type="uml:LiteralUnlimitedNatural" value="1"/>
                  </ownedEnd>
                  <ownedEnd xmi:type="uml:Property" xmi:id="r1_dst" association="r1">
                    <type xmi:idref="c2"/>
                    <lowerValue xmi:type="uml:LiteralInteger" value="0"/>
                    <upperValue xmi:type="uml:LiteralUnlimitedNatural" value="*"/>
                  </ownedEnd>
                </packagedElement>
                <packagedElement xmi:type="uml:Association" xmi:id="r2">
                  <ownedEnd xmi:type="uml:Property" xmi:id="r2_src" association="r2" aggregation="shared">
                    <type xmi:idref="c1"/>
                  </ownedEnd>
                  <ownedEnd xmi:type="uml:Property" xmi:id="r2_dst" association="r2">
                    <type xmi:idref="c2"/>
                  </ownedEnd>
                </packagedElement>
                <packagedElement xmi:type="uml:Association" xmi:id="r3">
                  <ownedEnd xmi:type="uml:Property" xmi:id="r3_src" association="r3" aggregation="composite">
                    <type xmi:idref="c1"/>
                  </ownedEnd>
                  <ownedEnd xmi:type="uml:Property" xmi:id="r3_dst" association="r3">
                    <type xmi:idref="c2"/>
                  </ownedEnd>
                </packagedElement>
                """)
                + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"d1\" modelElement=\"pkg\"/>");

        ImportedDiagram imported = parse(xml);

        assertEquals(3, imported.relationships().size());
        ImportedDiagram.RelationshipSpec association = imported.relationships().get(0);
        assertEquals("association", association.type());
        assertEquals("c1", association.sourceKey());
        assertEquals("c2", association.targetKey());
        assertEquals("1", association.sourceMultiplicity());
        assertEquals("0..*", association.targetMultiplicity());
        assertEquals("aggregation", imported.relationships().get(1).type());
        assertEquals("composition", imported.relationships().get(2).type());
    }

    @Test
    void mapsGeneralizationRealizationAndAssociationClass() {
        String xml = document(model("""
                <packagedElement xmi:type="uml:Class" xmi:id="c1" name="Animal"/>
                <packagedElement xmi:type="uml:Interface" xmi:id="i1" name="Volador"/>
                <packagedElement xmi:type="uml:Class" xmi:id="c2" name="Perro">
                  <generalization xmi:type="uml:Generalization" xmi:id="g1" general="c1"/>
                  <interfaceRealization xmi:type="uml:InterfaceRealization" xmi:id="z1" client="c2" supplier="i1" contract="i1"/>
                </packagedElement>
                <packagedElement xmi:type="uml:AssociationClass" xmi:id="ac1" name="Matricula">
                  <ownedAttribute xmi:type="uml:Property" xmi:id="aa1" name="fecha" visibility="private">
                    <type xmi:idref="EAJava_localdate"/>
                  </ownedAttribute>
                  <ownedEnd xmi:type="uml:Property" xmi:id="ac1_src" association="ac1"><type xmi:idref="c1"/></ownedEnd>
                  <ownedEnd xmi:type="uml:Property" xmi:id="ac1_dst" association="ac1"><type xmi:idref="c2"/></ownedEnd>
                </packagedElement>
                """)
                + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"d1\" modelElement=\"pkg\"/>");

        ImportedDiagram imported = parse(xml);

        assertTrue(imported.relationships().stream().anyMatch(r ->
                r.type().equals("generalization") && "c2".equals(r.sourceKey()) && "c1".equals(r.targetKey())));
        assertTrue(imported.relationships().stream().anyMatch(r ->
                r.type().equals("realization") && "c2".equals(r.sourceKey()) && "i1".equals(r.targetKey())));

        ImportedDiagram.RelationshipSpec associationClass = imported.relationships().stream()
                .filter(r -> r.type().equals("association_class")).findFirst().orElseThrow();
        assertEquals("ac1", associationClass.associationClassKey());

        ImportedDiagram.ElementSpec classElement = imported.elements().stream()
                .filter(e -> "Matricula".equals(e.name())).findFirst().orElseThrow();
        assertEquals("class", classElement.elementType());
        assertEquals("LocalDate", classElement.attributes().get(0).dataType());
    }

    @Test
    void omitsRelationWithDanglingReferenceAndWarns() {
        String xml = document(model("""
                <packagedElement xmi:type="uml:Class" xmi:id="c1" name="Cliente"/>
                <packagedElement xmi:type="uml:Association" xmi:id="r1">
                  <ownedEnd xmi:type="uml:Property" xmi:id="r1_src" association="r1"><type xmi:idref="c1"/></ownedEnd>
                  <ownedEnd xmi:type="uml:Property" xmi:id="r1_dst" association="r1"><type xmi:idref="noExiste"/></ownedEnd>
                </packagedElement>
                """));

        ImportedDiagram imported = parse(xml);

        assertTrue(imported.relationships().isEmpty());
        assertTrue(imported.warnings().stream().anyMatch(w -> w.contains("extremos")));
    }

    // 1.4bis XMI reales de EA: envoltorios, perfiles y primitivos se ignoran

    @Test
    void importsEaExportIgnoringToolWrappersProfilesAndPrimitives() {
        String xml = document(
                "<uml:Model xmi:type=\"uml:Model\" name=\"EA_Model\">"
                        + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"pkg\" name=\"Package2\">"
                        + "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c1\" name=\"Class1\"/>"
                        + "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c2\" name=\"CLIENTE\"/>"
                        + "<packagedElement xmi:type=\"uml:Association\" xmi:id=\"r1\" name=\"hace\">"
                        + "<memberEnd xmi:idref=\"r1_dst\"/><memberEnd xmi:idref=\"r1_src\"/>"
                        + "<ownedEnd xmi:type=\"uml:Property\" xmi:id=\"r1_src\" association=\"r1\">"
                        + "<type xmi:idref=\"c3\"/>"
                        + "<lowerValue xmi:type=\"uml:LiteralInteger\" value=\"1\"/>"
                        + "<upperValue xmi:type=\"uml:LiteralUnlimitedNatural\" value=\"*\"/>"
                        + "</ownedEnd>"
                        + "<ownedEnd xmi:type=\"uml:Property\" xmi:id=\"r1_dst\" association=\"r1\">"
                        + "<type xmi:idref=\"c2\"/>"
                        + "<lowerValue xmi:type=\"uml:LiteralInteger\" value=\"1\"/>"
                        + "<upperValue xmi:type=\"uml:LiteralUnlimitedNatural\" value=\"1\"/>"
                        + "</ownedEnd>"
                        + "</packagedElement>"
                        + "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c3\" name=\"PEDIDO\"/>"
                        + "</packagedElement>"
                        + "<profileApplication xmi:type=\"uml:ProfileApplication\" xmi:id=\"profileap\">"
                        + "<appliedProfile xmi:type=\"uml:Profile\" href=\"http://x/thecustomprofile\"/>"
                        + "</profileApplication>"
                        + "<primitivetypes><packagedElement xmi:type=\"uml:Package\" xmi:id=\"EAPrimitiveTypesPackage\">"
                        + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"EAJavaTypesPackage\">"
                        + "<packagedElement xmi:type=\"uml:PrimitiveType\" xmi:id=\"EAJava_int\" name=\"int\">"
                        + "<generalization xmi:type=\"uml:Generalization\" xmi:id=\"g\"/>"
                        + "</packagedElement>"
                        + "<packagedElement xmi:type=\"uml:PrimitiveType\" xmi:id=\"EAJava_string\" name=\"string\"/>"
                        + "</packagedElement></packagedElement></primitivetypes>"
                        + "<profiles><uml:Profile xmi:id=\"thecustomprofile\">"
                        + "<ownedComment xmi:type=\"uml:Comment\" xmi:id=\"comment01\"><body>Version:1.0</body></ownedComment>"
                        + "<packagedElement xmi:type=\"uml:Stereotype\" xmi:id=\"PK\" name=\"PK\"/>"
                        + "<packagedElement xmi:type=\"uml:Extension\" xmi:id=\"Property_PK\" name=\"A_Property_PK\"/>"
                        + "</uml:Profile></profiles>"
                        + "<diagrams/>"
                        + "<xmi:Extension extender=\"Enterprise Architect\" extenderID=\"6.5\"><elements/></xmi:Extension>"
                        + "</uml:Model>");

        ImportedDiagram imported = parse(xml);

        assertEquals(3, imported.elements().size());
        assertTrue(imported.warnings().isEmpty(), () -> "warnings: " + imported.warnings());
        assertEquals(1, imported.relationships().size());
        assertEquals("association", imported.relationships().get(0).type());
        assertEquals("1..*", imported.relationships().get(0).sourceMultiplicity());
        assertEquals("1", imported.relationships().get(0).targetMultiplicity());
    }

    @Test
    void treatsDataTypeAsClass() {
        String xml = document(model(
                "<packagedElement xmi:type=\"uml:DataType\" xmi:id=\"dt\" name=\"Dinero\">"
                        + "<ownedAttribute xmi:type=\"uml:Property\" xmi:id=\"a1\" name=\"monto\" visibility=\"private\">"
                        + "<type xmi:idref=\"EAJava_bigdecimal\"/>"
                        + "</ownedAttribute>"
                        + "</packagedElement>"));

        ImportedDiagram imported = parse(xml);

        assertEquals(1, imported.elements().size());
        assertEquals("class", imported.elements().get(0).elementType());
        assertEquals("Dinero", imported.elements().get(0).name());
        assertEquals("BigDecimal", imported.elements().get(0).attributes().get(0).dataType());
    }

    @Test
    void skipsModelElementsThatAreNotDrawnInTheDiagram() {
        String xml = document(model(
                "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c1\" name=\"CLIENTE\"/>"
                        + "<packagedElement xmi:type=\"uml:Class\" xmi:id=\"c2\" name=\"NoDibujada\"/>")
                + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"d1\" modelElement=\"pkg\">"
                + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"s1\" modelElement=\"c1\">"
                + "<bounds xmi:type=\"dc:bounds\" x=\"10\" y=\"10\" width=\"100\" height=\"80\"/>"
                + "</ownedElement></umldi:Diagram>");

        ImportedDiagram imported = parse(xml);

        assertEquals(1, imported.elements().size());
        assertEquals("CLIENTE", imported.elements().get(0).name());
        assertTrue(imported.warnings().stream()
                        .anyMatch(w -> w.contains("NoDibujada") && w.contains("no está en el diagrama")),
                () -> "warnings: " + imported.warnings());
    }
}
