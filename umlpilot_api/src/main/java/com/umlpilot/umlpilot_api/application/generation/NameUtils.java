package com.umlpilot.umlpilot_api.application.generation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utilidades para producir identificadores Java válidos y nombres de tabla a partir
 * de nombres UML arbitrarios.
 */
public final class NameUtils {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "record", "yield", "sealed", "permits");

    private NameUtils() {
    }

    /** "mi-clase Pedido" -> "MiClasePedido". */
    public static String pascalCase(String raw) {
        List<String> words = splitWords(raw);
        if (words.isEmpty()) {
            return "Generated";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
        }
        return ensureValidStart(sb.toString());
    }

    /** "Mi Clase" -> "miClase". */
    public static String camelCase(String raw) {
        String pascal = pascalCase(raw);
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /** "PedidoItem" -> "pedido_item". */
    public static String snakeCase(String raw) {
        List<String> words = splitWords(raw);
        if (words.isEmpty()) {
            return "generated";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(word.toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /** Pluraliza un nombre en snake_case ("cliente" -> "clientes", "category" -> "categories"). */
    public static String pluralize(String snake) {
        if (snake == null || snake.isBlank()) {
            return "items";
        }
        if (snake.endsWith("y") && snake.length() > 1 && !isVowel(snake.charAt(snake.length() - 2))) {
            return snake.substring(0, snake.length() - 1) + "ies";
        }
        if (snake.endsWith("s") || snake.endsWith("x") || snake.endsWith("z")
                || snake.endsWith("ch") || snake.endsWith("sh")) {
            return snake + "es";
        }
        return snake + "s";
    }

    /** Nombre de clase Java válido. */
    public static String className(String raw) {
        String name = pascalCase(raw);
        if (JAVA_KEYWORDS.contains(name.toLowerCase(Locale.ROOT))) {
            name = name + "Model";
        }
        return name;
    }

    /** Nombre de campo Java válido (evita palabras reservadas). */
    public static String fieldName(String raw) {
        String name = camelCase(raw);
        if (JAVA_KEYWORDS.contains(name)) {
            name = name + "_";
        }
        return name;
    }

    /** "get" + "cliente" -> "getCliente". */
    public static String getterName(String fieldName) {
        return "get" + capitalize(fieldName);
    }

    /** "set" + "cliente" -> "setCliente". */
    public static String setterName(String fieldName) {
        return "set" + capitalize(fieldName);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(Character.toLowerCase(c)) >= 0;
    }

    private static String ensureValidStart(String name) {
        if (name.isEmpty()) {
            return "Generated";
        }
        char first = name.charAt(0);
        if (Character.isJavaIdentifierStart(first)) {
            return name;
        }
        return "_" + name;
    }

    private static List<String> splitWords(String raw) {
        List<String> words = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return words;
        }
        String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        for (String token : ascii.split("[^A-Za-z0-9]+")) {
            if (token.isEmpty()) {
                continue;
            }
            StringBuilder current = new StringBuilder();
            char previous = 0;
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                boolean boundary = current.length() > 0
                        && Character.isUpperCase(c)
                        && (Character.isLowerCase(previous) || Character.isDigit(previous));
                if (boundary) {
                    words.add(current.toString());
                    current.setLength(0);
                }
                current.append(c);
                previous = c;
            }
            if (current.length() > 0) {
                words.add(current.toString());
            }
        }
        return words;
    }
}
