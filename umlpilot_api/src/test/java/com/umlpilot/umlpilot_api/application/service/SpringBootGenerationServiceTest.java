package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.generation.BackendGenerationValidator;
import com.umlpilot.umlpilot_api.application.generation.BackendModelBuilder;
import com.umlpilot.umlpilot_api.application.generation.SnapshotFixtures;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.config.CodeGenerationProperties;
import com.umlpilot.umlpilot_api.infrastructure.template.StringTemplateProjectRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringBootGenerationServiceTest {

    private final DiagramSnapshotService snapshotService = mock(DiagramSnapshotService.class);
    private final SpringBootGenerationService service = new SpringBootGenerationService(
            snapshotService,
            new BackendGenerationValidator(),
            new BackendModelBuilder(new CodeGenerationProperties()),
            new StringTemplateProjectRenderer(new DefaultResourceLoader()));

    private static List<String> entries(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    @Test
    void generatesZipWithWholeProject() throws IOException {
        when(snapshotService.snapshot("d1")).thenReturn(SnapshotFixtures.threeClassesWithRelations());

        byte[] zip = service.generateZip("d1");
        List<String> names = entries(zip);

        assertTrue(zip.length > 0);
        assertTrue(names.contains("pom.xml"));
        assertTrue(names.contains("README.md"));
        assertTrue(names.contains(".gitignore"));
        assertTrue(names.contains("src/main/resources/application.yml"));
        assertTrue(names.contains("src/main/java/com/generated/GeneratedApplication.java"));
        assertTrue(names.contains("src/main/java/com/generated/config/OpenApiConfig.java"));
        assertTrue(names.contains("src/main/java/com/generated/exception/ResourceNotFoundException.java"));
        assertTrue(names.contains("src/main/java/com/generated/exception/GlobalExceptionHandler.java"));
        for (String entity : new String[] {"Cliente", "Pedido", "Producto"}) {
            assertTrue(names.contains("src/main/java/com/generated/model/" + entity + ".java"));
            assertTrue(names.contains("src/main/java/com/generated/repository/" + entity + "Repository.java"));
            assertTrue(names.contains("src/main/java/com/generated/service/" + entity + "Service.java"));
            assertTrue(names.contains("src/main/java/com/generated/controller/" + entity + "Controller.java"));
            assertTrue(names.contains("src/main/java/com/generated/dto/" + entity + "Request.java"));
            assertTrue(names.contains("src/main/java/com/generated/dto/" + entity + "Response.java"));
        }
        assertEquals(4 + 4 + 6 * 3, names.size());
    }

    @Test
    void rejectedDiagramProducesNoZip() {
        when(snapshotService.snapshot("bad")).thenReturn(SnapshotFixtures.withInheritance());
        assertThrows(DomainException.class, () -> service.generateZip("bad"));
    }

    @Test
    void generatesWithoutLeavingTemporaryFiles() throws IOException {
        when(snapshotService.snapshot("d1")).thenReturn(SnapshotFixtures.threeClassesWithRelations());

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        List<String> before;
        try (Stream<Path> stream = Files.list(tempDir)) {
            before = stream.map(p -> p.getFileName().toString()).sorted().toList();
        }

        Map<String, String> files = service.generateFiles("d1");
        assertFalse(files.isEmpty());

        List<String> after;
        try (Stream<Path> stream = Files.list(tempDir)) {
            after = stream.map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertEquals(before, after, "la generación no debe crear archivos temporales");

        try {
            service.generateZip("d1");
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void generatesAssociationClassAsEntity() throws IOException {
        when(snapshotService.snapshot("d1")).thenReturn(SnapshotFixtures.associationClass());
        List<String> names = entries(service.generateZip("d1"));
        assertTrue(names.contains("src/main/java/com/generated/model/PedidoProducto.java"));
        assertTrue(names.contains("src/main/java/com/generated/controller/PedidoProductoController.java"));
    }
}
