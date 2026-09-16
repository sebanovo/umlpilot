package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;
import com.umlpilot.umlpilot_api.application.generation.BackendGenerationValidator;
import com.umlpilot.umlpilot_api.application.generation.BackendModel;
import com.umlpilot.umlpilot_api.application.generation.BackendModelBuilder;
import com.umlpilot.umlpilot_api.application.port.out.BackendProjectRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Caso de uso: generar un proyecto Spring Boot (4 capas + DTOs) a partir de un diagrama.
 * Valida, construye el modelo, renderiza las plantillas y empaqueta el resultado en un zip,
 * todo en memoria y sin escribir archivos temporales.
 */
@Service
public class SpringBootGenerationService {

    private final DiagramSnapshotService snapshotService;
    private final BackendGenerationValidator validator;
    private final BackendModelBuilder modelBuilder;
    private final BackendProjectRenderer renderer;

    public SpringBootGenerationService(DiagramSnapshotService snapshotService,
                                       BackendGenerationValidator validator,
                                       BackendModelBuilder modelBuilder,
                                       BackendProjectRenderer renderer) {
        this.snapshotService = snapshotService;
        this.validator = validator;
        this.modelBuilder = modelBuilder;
        this.renderer = renderer;
    }

    /**
     * Valida el diagrama y renderiza todos los archivos del proyecto en memoria.
     * Lanza {@code DomainException} (HTTP 400) si el diagrama no es apto.
     */
    @Transactional(readOnly = true)
    public Map<String, String> generateFiles(String diagramId) {
        DiagramSnapshot snapshot = snapshotService.snapshot(diagramId);
        validator.validate(snapshot);
        BackendModel model = modelBuilder.build(snapshot);
        return renderer.render(model);
    }

    /** Escribe los archivos del proyecto como un zip directamente sobre el stream indicado. */
    public void writeZip(Map<String, String> files, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    /** Variante síncrona para pruebas: devuelve el zip en memoria. */
    public byte[] generateZip(String diagramId) throws IOException {
        Map<String, String> files = generateFiles(diagramId);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writeZip(files, buffer);
        return buffer.toByteArray();
    }
}
