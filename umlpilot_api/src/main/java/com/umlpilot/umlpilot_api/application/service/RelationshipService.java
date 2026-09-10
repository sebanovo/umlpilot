package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.RelationshipRepository;
import com.umlpilot.umlpilot_api.domain.repository.MultiplicityRepository;
import com.umlpilot.umlpilot_api.domain.repository.ElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RelationshipService {
    private final RelationshipRepository relationshipRepository;
    private final MultiplicityRepository multiplicityRepository;
    private final ElementRepository elementRepository;

    public RelationshipService(RelationshipRepository relationshipRepository, MultiplicityRepository multiplicityRepository, ElementRepository elementRepository) {
        this.relationshipRepository = relationshipRepository;
        this.multiplicityRepository = multiplicityRepository;
        this.elementRepository = elementRepository;
    }

    @Transactional
    public RelationshipResult createRelationship(CreateRelationshipCommand command) {
        elementRepository.findById(ElementId.from(command.sourceElementId())).orElseThrow(() -> new DomainException("Source element not found"));
        elementRepository.findById(ElementId.from(command.targetElementId())).orElseThrow(() -> new DomainException("Target element not found"));
        Relationship rel = Relationship.create(command.diagramId(), command.sourceElementId(), command.targetElementId(), command.relationshipType());
        Relationship saved = relationshipRepository.save(rel);

        Multiplicity.create(saved.getId().value(), "source", 1, 1);
        multiplicityRepository.save(Multiplicity.create(saved.getId().value(), "source", 1, 1));
        multiplicityRepository.save(Multiplicity.create(saved.getId().value(), "target", 1, 1));

        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<RelationshipResult> getRelationships(String diagramId) {
        return relationshipRepository.findByDiagramId(diagramId).stream().map(this::toResult).toList();
    }

    @Transactional
    public RelationshipResult updateRelationship(String relationshipId, String name, String type, String direction) {
        Relationship rel = relationshipRepository.findById(RelationshipId.from(relationshipId))
                .orElseThrow(() -> new DomainException("Relationship not found"));
        rel.updateName(name);
        rel.updateType(type);
        rel.updateDirection(direction);
        return toResult(relationshipRepository.save(rel));
    }

    @Transactional
    public void deleteRelationship(String relationshipId) {
        RelationshipId id = RelationshipId.from(relationshipId);
        multiplicityRepository.findByRelationshipId(relationshipId).forEach(m -> multiplicityRepository.delete(m.getId()));
        relationshipRepository.delete(id);
    }

    @Transactional
    public MultiplicityResult updateMultiplicity(String multiplicityId, Integer min, Integer max) {
        Multiplicity mult = multiplicityRepository.findById(MultiplicityId.from(multiplicityId))
                .orElseThrow(() -> new DomainException("Multiplicity not found"));
        mult.updateMin(min);
        mult.updateMax(max);
        Multiplicity saved = multiplicityRepository.save(mult);
        return new MultiplicityResult(saved.getId().value(), saved.getRelationshipId().value(), saved.getEnd(), saved.getMin(), saved.getMax(), saved.getIsOrdered(), saved.getIsUnique());
    }

    @Transactional(readOnly = true)
    public List<MultiplicityResult> getMultiplicities(String relationshipId) {
        return multiplicityRepository.findByRelationshipId(relationshipId).stream()
                .map(m -> new MultiplicityResult(m.getId().value(), m.getRelationshipId().value(), m.getEnd(), m.getMin(), m.getMax(), m.getIsOrdered(), m.getIsUnique()))
                .toList();
    }

    private RelationshipResult toResult(Relationship r) {
        return new RelationshipResult(r.getId().value(), r.getDiagramId().value(), r.getSourceElementId().value(), r.getTargetElementId().value(), r.getRelationshipType(), r.getName(), r.getDirection());
    }
}
