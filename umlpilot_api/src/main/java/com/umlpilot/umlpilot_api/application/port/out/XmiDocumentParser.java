package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;

/**
 * Puerto de salida: parsea y valida un archivo XMI de clases (XMI 2.5.1) y devuelve
 * el modelo intermedio listo para persistir. Lanza {@code DomainException} con un
 * mensaje descriptivo cuando el archivo no cumple las validaciones.
 */
public interface XmiDocumentParser {

    ImportedDiagram parse(byte[] xmi);
}
