package com.umlpilot.umlpilot_api.infrastructure.xmi;

import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip: un XMI con la forma exacta que exporta UmlPilot (namespace XMI 2.5.1,
 * `xmi:version`, umldi con `dc:bounds`, extensión de EA, asociaciones y clase asociación)
 * se parsea completo sin warnings.
 */
class XmiRoundTripTest {

    private static final String UMLPILOT_XMI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.5.1" xmlns:uml="http://www.omg.org/spec/UML/20131001"
                     xmlns:xmi="http://www.omg.org/spec/XMI/20131001"
                     xmlns:umldi="http://www.omg.org/spec/UML/20131001/UMLDI"
                     xmlns:dc="http://www.omg.org/spec/UML/20131001/UMLDC">
              <xmi:Documentation exporter="Enterprise Architect" exporterVersion="6.5"/>
              <uml:Model xmi:type="uml:Model" name="EA_Model">
                <packagedElement xmi:type="uml:Package" xmi:id="EAPK_1" name="Diagram_1">
                  <packagedElement xmi:type="uml:Class" xmi:id="EAID_c1" name="Cliente" visibility="public">
                    <ownedAttribute xmi:type="uml:Property" xmi:id="EAID_a1" name="nombre" visibility="private">
                      <lowerValue xmi:type="uml:LiteralInteger" xmi:id="EAID_a1_lv" value="1"/>
                      <upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="EAID_a1_uv" value="1"/>
                      <type xmi:idref="EAJava_string"/>
                    </ownedAttribute>
                    <ownedOperation xmi:id="EAID_m1" name="calcular">
                      <ownedParameter xmi:id="EAID_p1" name="factor" type="EAJava_double"/>
                      <ownedParameter xmi:id="EAID_m1_rt" name="return" direction="return" type="EAJava_double"/>
                    </ownedOperation>
                  </packagedElement>
                  <packagedElement xmi:type="uml:Class" xmi:id="EAID_c2" name="Pedido" visibility="public">
                    <generalization xmi:type="uml:Generalization" xmi:id="EAID_g1" general="EAID_c1"/>
                  </packagedElement>
                  <packagedElement xmi:type="uml:Interface" xmi:id="EAID_i1" name="Auditable"/>
                  <packagedElement xmi:type="uml:Class" xmi:id="EAID_c3" name="Factura" visibility="public">
                    <interfaceRealization xmi:type="uml:InterfaceRealization" xmi:id="EAID_z1" name="" client="EAID_c3" supplier="EAID_i1" contract="EAID_i1"/>
                  </packagedElement>
                  <packagedElement xmi:type="uml:Association" xmi:id="EAID_r1" name="compra">
                    <memberEnd xmi:idref="EAID_r1_dst"/>
                    <memberEnd xmi:idref="EAID_r1_src"/>
                    <ownedEnd xmi:type="uml:Property" xmi:id="EAID_r1_src" association="EAID_r1">
                      <type xmi:idref="EAID_c1"/>
                      <lowerValue xmi:type="uml:LiteralInteger" xmi:id="EAID_r1_slv" value="1"/>
                      <upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="EAID_r1_suv" value="1"/>
                    </ownedEnd>
                    <ownedEnd xmi:type="uml:Property" xmi:id="EAID_r1_dst" association="EAID_r1">
                      <type xmi:idref="EAID_c2"/>
                      <lowerValue xmi:type="uml:LiteralInteger" xmi:id="EAID_r1_tlv" value="0"/>
                      <upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="EAID_r1_tuv" value="*"/>
                    </ownedEnd>
                  </packagedElement>
                  <packagedElement xmi:type="uml:AssociationClass" xmi:id="EAID_ac1" name="Matricula">
                    <ownedAttribute xmi:type="uml:Property" xmi:id="EAID_ac_a1" name="fecha" visibility="private">
                      <type xmi:idref="EAJava_localdate"/>
                    </ownedAttribute>
                    <ownedEnd xmi:type="uml:Property" xmi:id="EAID_ac1_src" association="EAID_ac1"><type xmi:idref="EAID_c1"/></ownedEnd>
                    <ownedEnd xmi:type="uml:Property" xmi:id="EAID_ac1_dst" association="EAID_ac1"><type xmi:idref="EAID_c2"/></ownedEnd>
                  </packagedElement>
                </packagedElement>
              </uml:Model>
              <umldi:Diagram xmi:type="umldi:UMLClassDiagram" xmi:id="EAID_d1" isFrame="false" modelElement="EAPK_1">
                <ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="EAID_s1" modelElement="EAID_c1">
                  <bounds xmi:type="dc:bounds" xmi:id="EAID_c1_db" x="120" y="60" width="200" height="120"/>
                </ownedElement>
                <ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="EAID_s2" modelElement="EAID_c2">
                  <bounds xmi:type="dc:bounds" xmi:id="EAID_c2_db" x="420" y="60" width="200" height="120"/>
                </ownedElement>
                <ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="EAID_s3" modelElement="EAID_i1">
                  <bounds xmi:type="dc:bounds" xmi:id="EAID_i1_db" x="120" y="240" width="200" height="120"/>
                </ownedElement>
                <ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="EAID_s4" modelElement="EAID_c3">
                  <bounds xmi:type="dc:bounds" xmi:id="EAID_c3_db" x="420" y="240" width="200" height="120"/>
                </ownedElement>
                <ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="EAID_s5" modelElement="EAID_ac1">
                  <bounds xmi:type="dc:bounds" xmi:id="EAID_ac1_db" x="120" y="420" width="200" height="120"/>
                </ownedElement>
              </umldi:Diagram>
              <xmi:Extension extender="Enterprise Architect" extenderID="6.5">
                <elements>
                  <element xmi:idref="EAID_c1" xmi:type="uml:Class" name="Cliente" scope="public">
                    <properties isSpecification="false" sType="Class" nType="0" scope="public"/>
                  </element>
                </elements>
                <connectors>
                  <connector xmi:idref="EAID_r1" name="compra">
                    <source xmi:idref="EAID_c1"><model type="Class" name="Cliente"/></source>
                    <target xmi:idref="EAID_c2"><model type="Class" name="Pedido"/></target>
                    <properties ea_type="Association"/>
                  </connector>
                </connectors>
              </xmi:Extension>
            </xmi:XMI>
            """;

    @Test
    void parsesAUmlPilotExportWithoutWarnings() {
        ImportedDiagram imported = new DomXmiParser()
                .parse(UMLPILOT_XMI.getBytes(StandardCharsets.UTF_8));

        assertEquals(5, imported.elements().size());
        assertTrue(imported.warnings().isEmpty(), () -> "warnings: " + imported.warnings());

        ImportedDiagram.ElementSpec cliente = imported.elements().stream()
                .filter(e -> "Cliente".equals(e.name())).findFirst().orElseThrow();
        assertEquals("class", cliente.elementType());
        assertEquals(120, cliente.positionX());
        assertEquals(60, cliente.positionY());
        assertEquals(200, cliente.width());
        assertEquals("String", cliente.attributes().get(0).dataType());
        assertEquals("double", cliente.methods().get(0).returnType());

        assertTrue(imported.elements().stream().anyMatch(e -> "Auditable".equals(e.name())
                && "interface".equals(e.elementType())));
        assertTrue(imported.elements().stream().anyMatch(e -> "Matricula".equals(e.name())
                && e.attributes().stream().anyMatch(a -> "LocalDate".equals(a.dataType()))));

        assertEquals(4, imported.relationships().size());
        ImportedDiagram.RelationshipSpec association = imported.relationships().stream()
                .filter(r -> r.type().equals("association")).findFirst().orElseThrow();
        assertEquals("EAID_c1", association.sourceKey());
        assertEquals("EAID_c2", association.targetKey());
        assertEquals("1", association.sourceMultiplicity());
        assertEquals("0..*", association.targetMultiplicity());

        assertTrue(imported.relationships().stream().anyMatch(r ->
                r.type().equals("generalization") && "EAID_c2".equals(r.sourceKey()) && "EAID_c1".equals(r.targetKey())));
        assertTrue(imported.relationships().stream().anyMatch(r ->
                r.type().equals("realization") && "EAID_c3".equals(r.sourceKey()) && "EAID_i1".equals(r.targetKey())));
        assertTrue(imported.relationships().stream().anyMatch(r ->
                r.type().equals("association_class") && "EAID_ac1".equals(r.associationClassKey())));
    }
}
