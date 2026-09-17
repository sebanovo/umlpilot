package com.umlpilot.umlpilot_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que OpenAPI/Swagger está habilitado y es público (no requiere JWT), que es justo
 * lo que fallaba antes: /swagger-ui.html no estaba permitida ni existía springdoc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class OpenApiDocsTest {

    @Autowired
    private Environment environment;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpResponse<String> get(String path) throws Exception {
        String url = "http://localhost:" + environment.getProperty("local.server.port") + path;
        return client.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void apiDocsIsPublic() throws Exception {
        HttpResponse<String> response = get("/api-docs");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"openapi\""),
                "el JSON de OpenAPI debe estar accesible en /api-docs");
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        HttpResponse<String> response = get("/swagger-ui.html");

        assertEquals(200, response.statusCode(),
                "Swagger UI debe estar accesible en /swagger-ui.html");
        assertTrue(response.body().toLowerCase().contains("swagger"),
                "la respuesta debe ser la página de Swagger UI");
    }
}
