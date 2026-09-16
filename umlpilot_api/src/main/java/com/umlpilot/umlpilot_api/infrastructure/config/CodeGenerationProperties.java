package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la generación de backend Spring Boot a partir de un diagrama.
 */
@Component
@ConfigurationProperties(prefix = "app.code-generation")
public class CodeGenerationProperties {

    /** Tope máximo de entidades generables (CODE_GENERATION_MAX_CLASSES). */
    private int maxClasses = 100;

    /** Paquete base del proyecto generado (CODE_GENERATION_BASE_PACKAGE). */
    private String basePackage = "com.generated";

    public int getMaxClasses() { return maxClasses; }
    public void setMaxClasses(int maxClasses) { this.maxClasses = maxClasses; }

    public String getBasePackage() { return basePackage; }
    public void setBasePackage(String basePackage) { this.basePackage = basePackage; }
}
