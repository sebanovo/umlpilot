package com.umlpilot.umlpilot_api.application.generation;

import java.util.Locale;

/**
 * Traduce un tipo UML a un tipo Java. Un tipo no reconocido se mapea a {@code String}.
 */
public final class JavaTypeMapper {

    private JavaTypeMapper() {
    }

    public record JavaType(String name, String importFqcn) {
        public boolean needsImport() {
            return importFqcn != null && !importFqcn.isBlank();
        }
    }

    public static JavaType map(String umlType) {
        if (umlType == null || umlType.isBlank()) {
            return new JavaType("String", null);
        }
        String type = umlType.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2);
        }
        return switch (type) {
            case "string", "str", "text", "varchar", "char", "character" -> new JavaType("String", null);
            case "int", "integer", "short", "smallint" -> new JavaType("Integer", null);
            case "long", "biginteger" -> new JavaType("Long", null);
            case "double", "float", "decimal", "number", "real" -> new JavaType("Double", null);
            case "boolean", "bool" -> new JavaType("Boolean", null);
            case "date" -> new JavaType("Date", "java.util.Date");
            case "localdate" -> new JavaType("LocalDate", "java.time.LocalDate");
            case "localdatetime", "datetime", "timestamp", "instant" ->
                    new JavaType("LocalDateTime", "java.time.LocalDateTime");
            case "bigdecimal", "money" -> new JavaType("BigDecimal", "java.math.BigDecimal");
            case "uuid", "guid" -> new JavaType("UUID", "java.util.UUID");
            default -> new JavaType("String", null);
        };
    }
}
