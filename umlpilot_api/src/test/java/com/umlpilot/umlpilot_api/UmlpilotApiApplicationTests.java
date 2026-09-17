package com.umlpilot.umlpilot_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Carga el contexto completo con el perfil `h2` para no depender de un PostgreSQL
 * en el entorno de pruebas (el perfil por defecto de la app es PostgreSQL).
 */
@SpringBootTest
@ActiveProfiles("h2")
class UmlpilotApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
