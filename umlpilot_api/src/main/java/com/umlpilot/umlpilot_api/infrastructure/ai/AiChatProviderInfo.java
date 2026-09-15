package com.umlpilot.umlpilot_api.infrastructure.ai;

/**
 * Proveedor resuelto para el asistente de IA (id, modelo y si está configurado con una
 * API key y un modelo). Se construye en {@code AiChatConfiguration}.
 */
public record AiChatProviderInfo(String provider, String model, boolean configured) {
}
