package com.umlpilot.umlpilot_api.application.generation;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendGenerationValidatorTest {

    private final BackendGenerationValidator validator = new BackendGenerationValidator();

    @Test
    void acceptsAssociationsAndAssociationClasses() {
        assertDoesNotThrow(() -> validator.validate(SnapshotFixtures.threeClassesWithRelations()));
        assertDoesNotThrow(() -> validator.validate(SnapshotFixtures.associationClass()));
    }

    @Test
    void rejectsGeneralization() {
        DomainException error = assertThrows(DomainException.class,
                () -> validator.validate(SnapshotFixtures.withInheritance()));
        assertTrue(error.getMessage().contains("generalization"), error.getMessage());
        assertTrue(error.getMessage().contains("Animal"), error.getMessage());
    }

    @Test
    void rejectsEveryUnsupportedRelationshipType() {
        for (String type : new String[] {"realization", "dependency", "template_binding", "aggregation", "composition"}) {
            DomainException error = assertThrows(DomainException.class,
                    () -> validator.validate(SnapshotFixtures.withRelationshipType(type)),
                    "debería rechazar " + type);
            assertTrue(error.getMessage().contains(type), error.getMessage());
        }
    }

    @Test
    void normalizesBlankTypeToAssociation() {
        assertDoesNotThrow(() -> validator.validate(SnapshotFixtures.withRelationshipType(null)));
    }
}
