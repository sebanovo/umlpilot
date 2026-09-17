package com.umlpilot.umlpilot_api.infrastructure.template;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;
import com.umlpilot.umlpilot_api.application.generation.BackendModelBuilder;
import com.umlpilot.umlpilot_api.application.generation.SnapshotFixtures;
import com.umlpilot.umlpilot_api.infrastructure.config.CodeGenerationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que el proyecto generado compila de verdad. Requiere Maven y acceso al
 * repositorio de dependencias, por eso es opt-in: se ejecuta con
 * {@code GENERATED_BACKEND_COMPILE_TEST=true}.
 */
@EnabledIfEnvironmentVariable(named = "GENERATED_BACKEND_COMPILE_TEST", matches = "true")
class GeneratedBackendCompilationTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedProjectCompiles() throws Exception {
        compile(SnapshotFixtures.threeClassesWithRelations(), tempDir.resolve("three-classes"));
    }

    @Test
    void reportedDiagramCompiles() throws Exception {
        compile(SnapshotFixtures.reportedDiagram(), tempDir.resolve("reported"));
    }

    @Test
    void associationClassDiagramCompiles() throws Exception {
        compile(SnapshotFixtures.associationClass(), tempDir.resolve("association-class"));
    }

    private void compile(DiagramSnapshot snapshot, Path projectDir) throws Exception {
        var model = new BackendModelBuilder(new CodeGenerationProperties()).build(snapshot);
        Map<String, String> files = new StringTemplateProjectRenderer(new DefaultResourceLoader()).render(model);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path target = projectDir.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue());
        }

        ProcessBuilder builder = new ProcessBuilder(mavenCommand(), "-q", "-DskipTests", "compile");
        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        assertEquals(0, exitCode, () -> "La compilación del backend generado falló:\n" + output);
    }

    private static String mavenCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    }
}
