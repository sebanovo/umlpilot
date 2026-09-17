package com.umlpilot.umlpilot_api.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI/Swagger de UmlPilot, accesible en {@code /swagger-ui.html}.
 * Incluye el esquema Bearer (JWT) para poder autorizar las peticiones desde Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI umlPilotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("UMLPilot API")
                        .description("API del editor UML colaborativo con IA: proyectos, diagramas, "
                                + "elementos/relaciones, colaboración en tiempo real, asistente de IA y "
                                + "generación de backend Spring Boot.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
