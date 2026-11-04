package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.SpringBootGenerationService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba del endpoint a través de Spring MVC. Cubre específicamente que el streaming
 * ({@code StreamingResponseBody}) se resuelva como descarga zip y no caiga en los
 * conversores de mensajes JSON.
 */
@WebMvcTest(controllers = BackendGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
class BackendGenerationControllerTest {

    private static final String URL = "/api/v1/projects/p1/diagrams/d1/generate/backend";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpringBootGenerationService generationService;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void stubZipWriter() throws Exception {
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(1);
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                zip.putNextEntry(new ZipEntry("pom.xml"));
                zip.write("<project/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return null;
        }).when(generationService).writeZip(any(), any());
    }

    @Test
    void streamsZipWithDownloadHeaders() throws Exception {
        when(generationService.generateFiles("d1")).thenReturn(Map.of("pom.xml", "<project/>"));

        MvcResult result = mockMvc.perform(get(URL))
                .andExpect(request().asyncStarted())
                .andReturn();

        byte[] body = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("generated-backend.zip")))
                .andExpect(content().contentTypeCompatibleWith("application/zip"))
                .andReturn().getResponse().getContentAsByteArray();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(body))) {
            ZipEntry entry = zip.getNextEntry();
            assertNotNull(entry, "el cuerpo debe ser un zip con entradas");
            assertEquals("pom.xml", entry.getName());
        }
    }

    @Test
    void invalidDiagramReturns400() throws Exception {
        when(generationService.generateFiles("d1")).thenThrow(new DomainException("relaciones no soportadas"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("relaciones no soportadas")));
    }

    @Test
    void missingDiagramReturns404() throws Exception {
        when(diagramService.getDiagram("p1", "d1", null)).thenThrow(new DomainException("Diagrama no encontrado"));

        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Diagrama no encontrado")));
    }
}
