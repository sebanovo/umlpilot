package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.ImportXmiResult;
import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.XmiImportService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiagramImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiagramImportControllerTest {

    private static final String URL = "/api/v1/projects/p1/diagrams/d1/import/xmi";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DiagramImportController controller;

    @MockitoBean
    private XmiImportService importService;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsImportSummary() throws Exception {
        when(importService.importDiagram(anyString(), any(), any()))
                .thenReturn(new ImportXmiResult(2, 1, 1, 1, 0, 1, List.of("Se renombró 'Cliente' a 'Cliente_1'")));

        MockMultipartFile file = new MockMultipartFile("file", "diagram.xmi", "application/xml",
                "<xmi/>".getBytes());

        mockMvc.perform(multipart(URL).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements").value(2))
                .andExpect(jsonPath("$.attributes").value(1))
                .andExpect(jsonPath("$.relationships").value(1))
                .andExpect(jsonPath("$.warnings[0]", containsString("Cliente_1")));
    }

    @Test
    void missingFileReturns400() throws Exception {
        mockMvc.perform(multipart(URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validationErrorReturns400WithMessage() throws Exception {
        when(importService.importDiagram(anyString(), any(), any()))
                .thenThrow(new DomainException("Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: 2.1"));

        MockMultipartFile file = new MockMultipartFile("file", "diagram.xmi", "application/xml",
                "<xmi/>".getBytes());

        mockMvc.perform(multipart(URL).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Versión XMI no soportada")));
    }

    @Test
    void missingDiagramReturns404() throws Exception {
        when(diagramService.getDiagram(anyString(), anyString())).thenThrow(new DomainException("Diagrama no encontrado"));

        MockMultipartFile file = new MockMultipartFile("file", "diagram.xmi", "application/xml",
                "<xmi/>".getBytes());

        mockMvc.perform(multipart(URL).file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void maxUploadSizeReturns400WithLimit() {
        ResponseEntity<?> response = controller.handleMaxUploadSize(new MaxUploadSizeExceededException(10_000_000));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("10 MB"));
    }
}
