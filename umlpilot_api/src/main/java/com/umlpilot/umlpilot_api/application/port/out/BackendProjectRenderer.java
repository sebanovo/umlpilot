package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.application.generation.BackendModel;

import java.util.Map;

/**
 * Puerto de salida: renderiza el proyecto Spring Boot a partir del modelo de generación.
 * Devuelve un mapa ordenado de ruta relativa → contenido.
 */
public interface BackendProjectRenderer {

    Map<String, String> render(BackendModel model);
}
