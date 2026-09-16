package com.umlpilot.umlpilot_api.infrastructure.template;

import com.umlpilot.umlpilot_api.application.generation.BackendModel;
import com.umlpilot.umlpilot_api.application.generation.BackendModelBuilder;
import com.umlpilot.umlpilot_api.application.generation.SnapshotFixtures;
import com.umlpilot.umlpilot_api.infrastructure.config.CodeGenerationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringTemplateProjectRendererTest {

    private final BackendModelBuilder builder = new BackendModelBuilder(new CodeGenerationProperties());
    private final StringTemplateProjectRenderer renderer =
            new StringTemplateProjectRenderer(new DefaultResourceLoader());

    @Test
    void rendersExpectedProjectStructure() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        Map<String, String> files = renderer.render(model);

        assertTrue(files.containsKey("pom.xml"), "falta pom.xml");
        assertTrue(files.containsKey("README.md"), "falta README.md");
        assertTrue(files.containsKey(".gitignore"), "falta .gitignore");
        assertTrue(files.containsKey("src/main/resources/application.yml"), "falta application.yml");

        String root = "src/main/java/com/generated";
        assertTrue(files.containsKey(root + "/GeneratedApplication.java"), "falta la clase de arranque");
        assertTrue(files.containsKey(root + "/config/OpenApiConfig.java"), "falta OpenApiConfig");
        assertTrue(files.containsKey(root + "/exception/ResourceNotFoundException.java"), "falta ResourceNotFoundException");
        assertTrue(files.containsKey(root + "/exception/GlobalExceptionHandler.java"), "falta GlobalExceptionHandler");

        for (String entity : new String[] {"Cliente", "Pedido", "Producto"}) {
            assertTrue(files.containsKey(root + "/model/" + entity + ".java"), "falta el modelo de " + entity);
            assertTrue(files.containsKey(root + "/repository/" + entity + "Repository.java"), "falta el repositorio de " + entity);
            assertTrue(files.containsKey(root + "/service/" + entity + "Service.java"), "falta el servicio de " + entity);
            assertTrue(files.containsKey(root + "/controller/" + entity + "Controller.java"), "falta el controller de " + entity);
            assertTrue(files.containsKey(root + "/dto/" + entity + "Request.java"), "falta el request de " + entity);
            assertTrue(files.containsKey(root + "/dto/" + entity + "Response.java"), "falta el response de " + entity);
        }

        assertEquals(4 + 4 + 6 * 3, files.size());
    }

    @Test
    void rendersRelationsAndEndpoints() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        Map<String, String> files = renderer.render(model);

        String pedido = files.get("src/main/java/com/generated/model/Pedido.java");
        assertTrue(pedido.contains("@ManyToOne"), "Pedido debería tener @ManyToOne");
        assertTrue(pedido.contains("@ManyToMany"), "Pedido debería tener @ManyToMany");
        assertTrue(pedido.contains("private Cliente cliente;"), "Pedido debería referenciar Cliente");
        assertTrue(pedido.contains("private List<Producto> productos;"), "Pedido debería tener la lista de Producto");

        String controller = files.get("src/main/java/com/generated/controller/ClienteController.java");
        assertTrue(controller.contains("@RequestMapping(\"/api/v1/clientes\")"), "ruta de recurso incorrecta");
        assertTrue(controller.contains("HttpStatus.CREATED"), "el POST debería devolver 201");
        assertTrue(controller.contains("noContent"), "el DELETE debería devolver 204");

        String request = files.get("src/main/java/com/generated/dto/PedidoRequest.java");
        assertTrue(request.contains("@NotNull"), "la referencia requerida debería validarse");
        assertTrue(request.contains("Long clienteId"), "la referencia debería exponerse como id");
        assertTrue(request.contains("List<Long> productosIds"), "la colección debería exponerse como ids");

        String response = files.get("src/main/java/com/generated/dto/PedidoResponse.java");
        assertTrue(response.contains("Long id"), "el response debería incluir el id");
        assertFalse(response.contains("@NotNull"), "el response no debería llevar validaciones");

        String service = files.get("src/main/java/com/generated/service/PedidoService.java");
        assertTrue(service.contains("PedidoRepository"), "el servicio debería inyectar su repositorio");
        assertTrue(service.contains("ProductoRepository"), "el servicio debería inyectar el repositorio referenciado");
        assertTrue(service.contains("ResourceNotFoundException"), "el servicio debería lanzar ResourceNotFoundException");
    }

    @Test
    void applicationYmlKeepsEnvironmentPlaceholders() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        String yml = renderer.render(model).get("src/main/resources/application.yml");
        assertTrue(yml.contains("jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:generated_db}"),
                "los placeholders de entorno deben conservarse: " + yml);
        assertFalse(yml.contains("\\$"), "no deben quedar escapes en el yml");
    }

    @Test
    void entityDoesNotExposeJpaInDtos() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        Map<String, String> files = renderer.render(model);
        String request = files.get("src/main/java/com/generated/dto/ClienteRequest.java");
        String response = files.get("src/main/java/com/generated/dto/ClienteResponse.java");
        assertFalse(request.contains("jakarta.persistence"), "el DTO no debe importar JPA");
        assertFalse(response.contains("jakarta.persistence"), "el DTO no debe importar JPA");
        assertFalse(request.contains("Cliente "), "el DTO no debe exponer la entidad");
    }

    @Test
    void pomDeclaresRequiredDependencies() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        String pom = renderer.render(model).get("pom.xml");

        assertTrue(pom.contains("<project"), "el pom debe ser un XML de proyecto");
        for (String dependency : new String[] {
                "spring-boot-starter-web",
                "spring-boot-starter-data-jpa",
                "spring-boot-starter-validation",
                "spring-boot-starter-actuator",
                "springdoc-openapi-starter-webmvc-ui",
                "postgresql",
                "lombok"}) {
            assertTrue(pom.contains(dependency), "falta la dependencia " + dependency);
        }
        assertTrue(pom.contains("${springdoc.version}"), "el placeholder de versión debe quedar literal");
        assertFalse(pom.contains("\\$"), "no deben quedar escapes en el pom");
    }

    @Test
    void rendersApplicationOpenApiAndReadme() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        Map<String, String> files = renderer.render(model);
        String javaRoot = "src/main/java/com/generated";

        assertTrue(files.get(javaRoot + "/GeneratedApplication.java").contains("@SpringBootApplication"));
        assertTrue(files.get(javaRoot + "/config/OpenApiConfig.java").contains("new OpenAPI("));
        assertTrue(files.get("README.md").contains("/swagger-ui.html"));
        assertTrue(files.get("README.md").contains("/actuator/health"));
        assertTrue(files.get("README.md").contains("/api/v1/clientes"));
    }

    @Test
    void exceptionHandlerCoversNotFoundAndValidation() {
        BackendModel model = builder.build(SnapshotFixtures.threeClassesWithRelations());
        String handler = renderer.render(model)
                .get("src/main/java/com/generated/exception/GlobalExceptionHandler.java");

        assertTrue(handler.contains("HttpStatus.NOT_FOUND"), "debe mapear 404");
        assertTrue(handler.contains("MethodArgumentNotValidException"), "debe mapear validación a 400");
        assertTrue(handler.contains("ResourceNotFoundException"), "debe manejar el recurso inexistente");
    }
}
