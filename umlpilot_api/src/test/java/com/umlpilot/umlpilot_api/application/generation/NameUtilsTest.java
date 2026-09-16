package com.umlpilot.umlpilot_api.application.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameUtilsTest {

    @Test
    void buildsValidClassNames() {
        assertEquals("MiClasePedido", NameUtils.className("mi-clase Pedido"));
        assertEquals("Cliente", NameUtils.className("cliente"));
        assertEquals("Anadir", NameUtils.className("Añadir"));
        assertEquals("ClassModel", NameUtils.className("class"));
        assertTrue(Character.isJavaIdentifierStart(NameUtils.className("3d").charAt(0)));
    }

    @Test
    void buildsValidFieldNamesAndAvoidsKeywords() {
        assertEquals("nombreCompleto", NameUtils.camelCase("Nombre Completo"));
        assertEquals("cliente", NameUtils.fieldName("Cliente"));
        assertEquals("class_", NameUtils.fieldName("class"));
        assertEquals("if_", NameUtils.fieldName("if"));
    }

    @Test
    void buildsSnakeCaseAndPlural() {
        assertEquals("pedido_item", NameUtils.snakeCase("PedidoItem"));
        assertEquals("clientes", NameUtils.pluralize("cliente"));
        assertEquals("categories", NameUtils.pluralize("category"));
        assertEquals("boxes", NameUtils.pluralize("box"));
        assertEquals("dishes", NameUtils.pluralize("dish"));
        assertEquals("pedidos", NameUtils.pluralize("pedido"));
    }

    @Test
    void buildsAccessorNames() {
        assertEquals("getNombre", NameUtils.getterName("nombre"));
        assertEquals("setNombre", NameUtils.setterName("nombre"));
    }
}
