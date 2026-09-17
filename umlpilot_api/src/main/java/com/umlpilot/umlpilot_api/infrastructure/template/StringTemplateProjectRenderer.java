package com.umlpilot.umlpilot_api.infrastructure.template;

import com.umlpilot.umlpilot_api.application.generation.BackendModel;
import com.umlpilot.umlpilot_api.application.port.out.BackendProjectRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renderiza el proyecto Spring Boot con StringTemplate (ST4). Las plantillas viven en
 * {@code classpath:templates/springboot/**} y se cargan de forma segura incluso desde un
 * empaquetado en jar (se resuelve el recurso por su URL y se declaran sus argumentos).
 */
@Component
public class StringTemplateProjectRenderer implements BackendProjectRenderer {

    private static final String TEMPLATE_ROOT = "classpath:templates/springboot/";
    private static final String MODEL_ARG = "model";
    private static final String ENTITY_ARG = "model,entity";
    private static final char DELIMITER = '$';

    private final ResourceLoader resourceLoader;

    public StringTemplateProjectRenderer(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Map<String, String> render(BackendModel model) {
        Map<String, String> files = new LinkedHashMap<>();
        Map<String, Object> modelAttributes = Map.of(MODEL_ARG, model);

        files.put("pom.xml", render("project/pom", MODEL_ARG, modelAttributes));
        files.put("README.md", render("project/readme", MODEL_ARG, modelAttributes));
        files.put(".gitignore", render("project/gitignore", MODEL_ARG, modelAttributes));
        files.put("src/main/resources/application.yml", render("project/application_yml", MODEL_ARG, modelAttributes));
        files.put("src/main/resources/application-postgres.yml",
                render("project/application_postgres_yml", MODEL_ARG, modelAttributes));

        String javaRoot = "src/main/java/" + model.basePath();
        files.put(javaRoot + "/GeneratedApplication.java", render("java/application", MODEL_ARG, modelAttributes));
        files.put(javaRoot + "/config/OpenApiConfig.java",
                render("java/openapi_config", MODEL_ARG, modelAttributes));
        files.put(javaRoot + "/exception/ResourceNotFoundException.java",
                render("java/not_found_exception", MODEL_ARG, modelAttributes));
        files.put(javaRoot + "/exception/GlobalExceptionHandler.java",
                render("java/global_exception_handler", MODEL_ARG, modelAttributes));

        for (BackendModel.Entity entity : model.entities()) {
            Map<String, Object> attributes = Map.of(MODEL_ARG, model, "entity", entity);
            files.put(javaRoot + "/model/" + entity.className() + ".java",
                    render("java/entity", ENTITY_ARG, attributes));
            files.put(javaRoot + "/repository/" + entity.className() + "Repository.java",
                    render("java/repository", ENTITY_ARG, attributes));
            files.put(javaRoot + "/service/" + entity.className() + "Service.java",
                    render("java/service", ENTITY_ARG, attributes));
            files.put(javaRoot + "/controller/" + entity.className() + "Controller.java",
                    render("java/controller", ENTITY_ARG, attributes));
            files.put(javaRoot + "/dto/" + entity.className() + "Request.java",
                    render("java/dto_request", ENTITY_ARG, attributes));
            files.put(javaRoot + "/dto/" + entity.className() + "Response.java",
                    render("java/dto_response", ENTITY_ARG, attributes));
        }

        return files;
    }

    private String render(String templatePath, String arguments, Map<String, Object> attributes) {
        ST template = load(templatePath, arguments);
        attributes.forEach(template::add);
        return template.render();
    }

    private ST load(String templatePath, String arguments) {
        Resource resource = resourceLoader.getResource(TEMPLATE_ROOT + templatePath + ".st");
        if (!resource.exists()) {
            throw new IllegalStateException("Plantilla no encontrada: " + templatePath);
        }
        try {
            String source = resource.getContentAsString(StandardCharsets.UTF_8);
            String name = baseName(templatePath);
            STGroup group = new STGroup(DELIMITER, DELIMITER);
            group.defineTemplate(name, arguments, source);
            ST template = group.getInstanceOf(name);
            if (template == null) {
                throw new IllegalStateException("Plantilla no registrada: " + templatePath);
            }
            return template;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la plantilla: " + templatePath, e);
        }
    }

    private static String baseName(String templatePath) {
        int slash = templatePath.lastIndexOf('/');
        return slash < 0 ? templatePath : templatePath.substring(slash + 1);
    }
}
