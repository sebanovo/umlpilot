package com.umlpilot.umlpilot_api.application.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeMapperTest {

    @Test
    void mapsSupportedTypes() {
        assertEquals("String", JavaTypeMapper.map("String").name());
        assertEquals("Integer", JavaTypeMapper.map("int").name());
        assertEquals("Long", JavaTypeMapper.map("long").name());
        assertEquals("Double", JavaTypeMapper.map("double").name());
        assertEquals("Boolean", JavaTypeMapper.map("boolean").name());
        assertEquals("Date", JavaTypeMapper.map("Date").name());
        assertEquals("LocalDate", JavaTypeMapper.map("localdate").name());
        assertEquals("LocalDateTime", JavaTypeMapper.map("LocalDateTime").name());
        assertEquals("BigDecimal", JavaTypeMapper.map("bigdecimal").name());
        assertEquals("UUID", JavaTypeMapper.map("uuid").name());
    }

    @Test
    void addsImportsForNonPrimitiveTypes() {
        assertTrue(JavaTypeMapper.map("LocalDate").needsImport());
        assertEquals("java.time.LocalDate", JavaTypeMapper.map("LocalDate").importFqcn());
        assertEquals("java.math.BigDecimal", JavaTypeMapper.map("BigDecimal").importFqcn());
        assertFalse(JavaTypeMapper.map("String").needsImport());
        assertFalse(JavaTypeMapper.map("int").needsImport());
    }

    @Test
    void fallsBackToStringForUnknownOrBlankTypes() {
        assertEquals("String", JavaTypeMapper.map("Pepito").name());
        assertEquals("String", JavaTypeMapper.map("  ").name());
        assertEquals("String", JavaTypeMapper.map(null).name());
    }
}
