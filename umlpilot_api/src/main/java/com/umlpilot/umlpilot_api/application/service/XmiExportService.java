package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.AttributeResult;
import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.MethodResult;
import com.umlpilot.umlpilot_api.application.dto.MultiplicityResult;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.application.dto.XmiExportModel;
import com.umlpilot.umlpilot_api.application.port.out.XmiDiagramRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Caso de uso: exporta un diagrama como XMI compatible con Enterprise Architect. */
@Service
public class XmiExportService {

    private final ElementService elementService;
    private final RelationshipService relationshipService;
    private final XmiDiagramRenderer renderer;

    public XmiExportService(ElementService elementService,
                            RelationshipService relationshipService,
                            XmiDiagramRenderer renderer) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public byte[] export(String diagramId) {
        List<XmiExportModel.Element> elements = elementService.getElements(diagramId).stream()
                .map(this::toElement)
                .toList();
        List<XmiExportModel.Relationship> relationships = relationshipService.getRelationships(diagramId).stream()
                .map(this::toRelationship)
                .toList();
        return renderer.render(new XmiExportModel(diagramId, "Diagram_" + diagramId, elements, relationships));
    }

    private XmiExportModel.Element toElement(ElementResult element) {
        List<XmiExportModel.Attribute> attributes = elementService.getAttributes(element.id()).stream()
                .map(this::toAttribute)
                .toList();
        List<XmiExportModel.Method> methods = elementService.getMethods(element.id()).stream()
                .map(this::toMethod)
                .toList();
        return new XmiExportModel.Element(
                element.id(), element.name(), element.elementType(), element.stereotype(), element.visibility(),
                element.positionX() == null ? 0 : element.positionX(),
                element.positionY() == null ? 0 : element.positionY(),
                element.width() == null || element.width() <= 0 ? 160 : element.width(),
                element.height() == null || element.height() <= 0 ? 80 : element.height(),
                attributes, methods);
    }

    private XmiExportModel.Attribute toAttribute(AttributeResult attribute) {
        return new XmiExportModel.Attribute(
                attribute.id(), attribute.name(), attribute.dataType(), attribute.visibility(),
                attribute.stereotype(), attribute.defaultValue(), Boolean.TRUE.equals(attribute.isStatic()));
    }

    private XmiExportModel.Method toMethod(MethodResult method) {
        List<XmiExportModel.Parameter> parameters = method.parameters() == null
                ? List.of()
                : method.parameters().stream()
                .map(parameter -> new XmiExportModel.Parameter(
                        parameter.id(), parameter.name(), parameter.dataType(), parameter.direction(), parameter.notes()))
                .toList();
        return new XmiExportModel.Method(
                method.id(), method.name(), method.returnType(), method.visibility(),
                method.stereotype(), Boolean.TRUE.equals(method.isStatic()), parameters);
    }

    private XmiExportModel.Relationship toRelationship(RelationshipResult relationship) {
        String source = null;
        String target = null;
        for (MultiplicityResult multiplicity : relationshipService.getMultiplicities(relationship.id())) {
            String label = formatMultiplicity(multiplicity);
            if ("source".equals(multiplicity.end())) {
                source = label;
            } else if ("target".equals(multiplicity.end())) {
                target = label;
            }
        }
        return new XmiExportModel.Relationship(
                relationship.id(), relationship.relationshipType(), relationship.name(),
                relationship.sourceElementId(), relationship.targetElementId(),
                source, target, relationship.associationClassElementId(), relationship.waypoints());
    }

    private static String formatMultiplicity(MultiplicityResult multiplicity) {
        Integer min = multiplicity.min();
        Integer max = multiplicity.max();
        if (min == null && max == null) {
            return null;
        }
        if (max != null && max == -1) {
            return (min == null ? 0 : min) + "..*";
        }
        if (min != null && max != null && min.equals(max)) {
            return String.valueOf(min);
        }
        return (min == null ? 0 : min) + ".." + (max == null ? "*" : max);
    }
}
