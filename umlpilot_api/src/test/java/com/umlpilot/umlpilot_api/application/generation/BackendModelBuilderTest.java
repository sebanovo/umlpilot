package com.umlpilot.umlpilot_api.application.generation;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.config.CodeGenerationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackendModelBuilderTest {

    private final BackendModelBuilder builder = new BackendModelBuilder(new CodeGenerationProperties());

    private BackendModel.Entity entity(BackendModel model, String className) {
        return model.entities().stream()
                .filter(e -> e.className().equals(className))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se generó la entidad " + className));
    }

    @Test
    void producesFourLayersAndDtosForEachClass() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());

        assertEquals(3, model.entities().size());
        assertEquals(3, model.getEntityList().size());
        for (String name : new String[] {"Cliente", "Pedido", "Producto"}) {
            BackendModel.Entity e = entity(model, name);
            assertEquals(e.tableName(), e.resourcePath());
            assertFalse(e.fields().isEmpty(), name + " debería tener atributos");
        }
        assertEquals("clientes", entity(model, "Cliente").tableName());
        assertEquals("pedidos", entity(model, "Pedido").tableName());
    }

    @Test
    void autogeneratesLongIdWhenMissing() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        BackendModel.Entity cliente = entity(model, "Cliente");
        assertEquals("id", cliente.idFieldName());
        assertEquals("Long", cliente.idType());
    }

    @Test
    void respectsExplicitIdType() {
        BackendModel model = builder.build(SnapshotFixtures.withExplicitId());
        BackendModel.Entity cuenta = entity(model, "Cuenta");
        assertEquals("id", cuenta.idFieldName());
        assertEquals("UUID", cuenta.idType());
        assertTrue(cuenta.importStatements().contains("import java.util.UUID;"));
        // el atributo id no se duplica como campo normal
        assertTrue(cuenta.fields().stream().noneMatch(f -> f.name().equals("id")));
    }

    @Test
    void resolvesManyToOneAndManyToMany() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        BackendModel.Entity pedido = entity(model, "Pedido");

        assertEquals(2, pedido.references().size());

        BackendModel.Reference manyToOne = pedido.references().stream()
                .filter(BackendModel.Reference::manyToOne).findFirst().orElseThrow();
        assertEquals("Cliente", manyToOne.targetClassName());
        assertTrue(manyToOne.required(), "min 1 en el extremo origen hace requerida la referencia");
        assertFalse(manyToOne.collection());

        BackendModel.Reference manyToMany = pedido.references().stream()
                .filter(BackendModel.Reference::manyToMany).findFirst().orElseThrow();
        assertEquals("Producto", manyToMany.targetClassName());
        assertTrue(manyToMany.collection());
        assertEquals("pedidos_productos", manyToMany.joinTableName());
    }

    @Test
    void resolvesOneToOne() {
        BackendModel model = builder.build(SnapshotFixtures.oneToOne());
        BackendModel.Entity persona = entity(model, "Persona");
        assertEquals(1, persona.references().size());
        BackendModel.Reference reference = persona.references().get(0);
        assertTrue(reference.oneToOne());
        assertEquals("Pasaporte", reference.targetClassName());
        assertEquals("pasaporte_id", reference.joinColumnName());
    }

    @Test
    void treatsInterfacesEnumsTablesAndUnknownAsEntitiesIgnoringNotes() {
        BackendModel model = builder.build(SnapshotFixtures.mixedElementTypes());
        assertEquals(4, model.entities().size());
        assertTrue(model.entities().stream().noneMatch(e -> e.className().equals("Nota")));
    }

    @Test
    void rejectsDiagramWithoutEntities() {
        assertThrows(DomainException.class, () -> builder.build(SnapshotFixtures.notesOnly()));
    }

    @Test
    void rejectsDiagramOverTheLimit() {
        CodeGenerationProperties properties = new CodeGenerationProperties();
        properties.setMaxClasses(100);
        BackendModelBuilder limited = new BackendModelBuilder(properties);
        DomainException error = assertThrows(DomainException.class,
                () -> limited.build(SnapshotFixtures.manyClasses(101)));
        assertTrue(error.getMessage().contains("100"), error.getMessage());

        assertDoesNotThrow(() -> limited.build(SnapshotFixtures.manyClasses(100)));
    }

    @Test
    void generatesAccessorsAndSkipsConstructorsAndAccessorCollisions() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        BackendModel.Entity cliente = entity(model, "Cliente");
        assertTrue(cliente.accessorDeclarations().stream().anyMatch(a -> a.contains("public String getNombre()")));
        assertTrue(cliente.accessorDeclarations().stream().anyMatch(a -> a.contains("public void setNombre(String nombre)")));
        assertTrue(cliente.accessorDeclarations().stream().anyMatch(a -> a.contains("public Long getId()")));
    }
}
