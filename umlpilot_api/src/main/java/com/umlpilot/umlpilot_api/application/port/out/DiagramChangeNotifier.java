package com.umlpilot.umlpilot_api.application.port.out;

/**
 * Puerto de salida: notifica a los colaboradores de un diagrama que hubo un cambio masivo
 * (construcción con IA, importación, etc.) para que refresquen todo el diagrama.
 */
public interface DiagramChangeNotifier {

    void bulkChanged(String diagramId, String userId, String origin);
}
