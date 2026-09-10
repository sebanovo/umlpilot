package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ElementService {
    private final ElementRepository elementRepository;
    private final AttributeRepository attributeRepository;
    private final MethodRepository methodRepository;
    private final ParameterRepository parameterRepository;
    private final LiteralRepository literalRepository;

    public ElementService(ElementRepository elementRepository, AttributeRepository attributeRepository, MethodRepository methodRepository, ParameterRepository parameterRepository, LiteralRepository literalRepository) {
        this.elementRepository = elementRepository;
        this.attributeRepository = attributeRepository;
        this.methodRepository = methodRepository;
        this.parameterRepository = parameterRepository;
        this.literalRepository = literalRepository;
    }

    @Transactional
    public ElementResult createElement(CreateElementCommand command) {
        Element element = Element.create(command.diagramId(), command.creatorId(), command.name(), command.elementType(), command.positionX(), command.positionY());
        return toResult(elementRepository.save(element));
    }

    @Transactional(readOnly = true)
    public List<ElementResult> getElements(String diagramId) {
        return elementRepository.findByDiagramId(diagramId).stream().map(this::toResult).toList();
    }

    @Transactional
    public ElementResult updateElement(String diagramId, String elementId, String name, String visibility, Integer positionX, Integer positionY, Integer width, Integer height) {
        Element element = elementRepository.findById(ElementId.from(elementId))
                .orElseThrow(() -> new DomainException("Element not found"));
        if (!element.getDiagramId().value().equals(diagramId)) throw new DomainException("Element not in diagram");
        element.updateName(name);
        element.updateVisibility(visibility);
        if (positionX != null && positionY != null) element.updatePosition(positionX, positionY);
        if (width != null && height != null) element.updateSize(width, height);
        return toResult(elementRepository.save(element));
    }

    @Transactional
    public void deleteElement(String elementId) {
        ElementId id = ElementId.from(elementId);
        attributeRepository.findByElementId(elementId).forEach(a -> attributeRepository.delete(a.getId()));
        methodRepository.findByElementId(elementId).forEach(m -> {
            parameterRepository.findByMethodId(m.getId().value()).forEach(p -> parameterRepository.delete(p.getId()));
            methodRepository.delete(m.getId());
        });
        literalRepository.findByElementId(elementId).forEach(l -> literalRepository.delete(l.getId()));
        elementRepository.delete(id);
    }

    @Transactional
    public AttributeResult addAttribute(String elementId, String name, String dataType, String visibility, Integer orderIndex) {
        elementRepository.findById(ElementId.from(elementId)).orElseThrow(() -> new DomainException("Element not found"));
        Attribute attr = Attribute.create(elementId, name, dataType, visibility, orderIndex);
        Attribute saved = attributeRepository.save(attr);
        return new AttributeResult(saved.getId().value(), saved.getElementId().value(), saved.getName(), saved.getDataType(), saved.getVisibility(), saved.getDefaultValue(), saved.getIsStatic(), saved.getIsFinal(), saved.getOrderIndex());
    }

    @Transactional(readOnly = true)
    public List<AttributeResult> getAttributes(String elementId) {
        return attributeRepository.findByElementId(elementId).stream()
                .map(a -> new AttributeResult(a.getId().value(), a.getElementId().value(), a.getName(), a.getDataType(), a.getVisibility(), a.getDefaultValue(), a.getIsStatic(), a.getIsFinal(), a.getOrderIndex()))
                .toList();
    }

    @Transactional
    public AttributeResult updateAttribute(String attributeId, String name, String dataType, String visibility, String defaultValue) {
        Attribute attr = attributeRepository.findById(AttributeId.from(attributeId))
                .orElseThrow(() -> new DomainException("Attribute not found"));
        attr.updateName(name);
        attr.updateDataType(dataType);
        attr.updateVisibility(visibility);
        attr.updateDefaultValue(defaultValue);
        Attribute saved = attributeRepository.save(attr);
        return new AttributeResult(saved.getId().value(), saved.getElementId().value(), saved.getName(), saved.getDataType(), saved.getVisibility(), saved.getDefaultValue(), saved.getIsStatic(), saved.getIsFinal(), saved.getOrderIndex());
    }

    @Transactional
    public void deleteAttribute(String attributeId) {
        attributeRepository.delete(AttributeId.from(attributeId));
    }

    @Transactional
    public MethodResult addMethod(String elementId, String name, String returnType, String visibility, Integer orderIndex) {
        elementRepository.findById(ElementId.from(elementId)).orElseThrow(() -> new DomainException("Element not found"));
        Method method = Method.create(elementId, name, returnType, visibility, orderIndex);
        Method saved = methodRepository.save(method);
        return new MethodResult(saved.getId().value(), saved.getElementId().value(), saved.getName(), saved.getReturnType(), saved.getVisibility(), saved.getIsStatic(), saved.getIsAbstract(), saved.getIsFinal(), saved.getIsConstructor(), saved.getOrderIndex(), List.of());
    }

    @Transactional(readOnly = true)
    public List<MethodResult> getMethods(String elementId) {
        return methodRepository.findByElementId(elementId).stream()
                .map(m -> {
                    List<ParameterResult> params = parameterRepository.findByMethodId(m.getId().value()).stream()
                            .map(p -> new ParameterResult(p.getId().value(), p.getMethodId().value(), p.getName(), p.getDataType(), p.getOrderIndex(), p.getIsVarargs(), p.getIsFinal(), p.getDefaultValue()))
                            .toList();
                    return new MethodResult(m.getId().value(), m.getElementId().value(), m.getName(), m.getReturnType(), m.getVisibility(), m.getIsStatic(), m.getIsAbstract(), m.getIsFinal(), m.getIsConstructor(), m.getOrderIndex(), params);
                })
                .toList();
    }

    @Transactional
    public MethodResult updateMethod(String methodId, String name, String returnType, String visibility) {
        Method method = methodRepository.findById(MethodId.from(methodId))
                .orElseThrow(() -> new DomainException("Method not found"));
        method.updateName(name);
        method.updateReturnType(returnType);
        method.updateVisibility(visibility);
        Method saved = methodRepository.save(method);
        List<ParameterResult> params = parameterRepository.findByMethodId(methodId).stream()
                .map(p -> new ParameterResult(p.getId().value(), p.getMethodId().value(), p.getName(), p.getDataType(), p.getOrderIndex(), p.getIsVarargs(), p.getIsFinal(), p.getDefaultValue()))
                .toList();
        return new MethodResult(saved.getId().value(), saved.getElementId().value(), saved.getName(), saved.getReturnType(), saved.getVisibility(), saved.getIsStatic(), saved.getIsAbstract(), saved.getIsFinal(), saved.getIsConstructor(), saved.getOrderIndex(), params);
    }

    @Transactional
    public void deleteMethod(String methodId) {
        parameterRepository.findByMethodId(methodId).forEach(p -> parameterRepository.delete(p.getId()));
        methodRepository.delete(MethodId.from(methodId));
    }

    @Transactional
    public ParameterResult addParameter(String methodId, String name, String dataType, Integer orderIndex) {
        methodRepository.findById(MethodId.from(methodId)).orElseThrow(() -> new DomainException("Method not found"));
        Parameter param = Parameter.create(methodId, name, dataType, orderIndex);
        Parameter saved = parameterRepository.save(param);
        return new ParameterResult(saved.getId().value(), saved.getMethodId().value(), saved.getName(), saved.getDataType(), saved.getOrderIndex(), saved.getIsVarargs(), saved.getIsFinal(), saved.getDefaultValue());
    }

    @Transactional
    public void deleteParameter(String parameterId) {
        parameterRepository.delete(ParameterId.from(parameterId));
    }

    @Transactional
    public LiteralResult addLiteral(String elementId, String name, String value, Integer orderIndex) {
        elementRepository.findById(ElementId.from(elementId)).orElseThrow(() -> new DomainException("Element not found"));
        Literal literal = Literal.create(elementId, name, value, orderIndex);
        Literal saved = literalRepository.save(literal);
        return new LiteralResult(saved.getId().value(), saved.getElementId().value(), saved.getName(), saved.getValue(), saved.getOrderIndex());
    }

    @Transactional(readOnly = true)
    public List<LiteralResult> getLiterals(String elementId) {
        return literalRepository.findByElementId(elementId).stream()
                .map(l -> new LiteralResult(l.getId().value(), l.getElementId().value(), l.getName(), l.getValue(), l.getOrderIndex()))
                .toList();
    }

    @Transactional
    public void deleteLiteral(String literalId) {
        literalRepository.delete(LiteralId.from(literalId));
    }

    private ElementResult toResult(Element e) {
        return new ElementResult(e.getId().value(), e.getDiagramId().value(), e.getName(), e.getVisibility(), e.getStereotype(), e.getElementType(), e.getPositionX(), e.getPositionY(), e.getWidth(), e.getHeight());
    }
}
