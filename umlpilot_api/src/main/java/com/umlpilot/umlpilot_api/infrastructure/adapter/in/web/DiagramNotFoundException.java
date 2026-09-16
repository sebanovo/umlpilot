package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

/**
 * Se lanza cuando el diagrama solicitado no existe o no pertenece al proyecto indicado.
 */
public class DiagramNotFoundException extends RuntimeException {

    public DiagramNotFoundException(String message) {
        super(message);
    }
}
