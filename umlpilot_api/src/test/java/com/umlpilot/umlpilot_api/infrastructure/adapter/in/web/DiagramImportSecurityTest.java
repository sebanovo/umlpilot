package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.XmiImportService;
import com.umlpilot.umlpilot_api.infrastructure.config.SecurityConfig;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El endpoint de importación queda protegido por la configuración de seguridad: sin token
 * responde 401 y no llega al servicio.
 */
@WebMvcTest(controllers = DiagramImportController.class)
@Import(SecurityConfig.class)
class DiagramImportSecurityTest {

    private static final String URL = "/api/v1/projects/p1/diagrams/d1/import/xmi";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XmiImportService importService;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "diagram.xmi", "application/xml",
                "<xmi/>".getBytes());

        mockMvc.perform(multipart(URL).file(file))
                .andExpect(status().isUnauthorized());
    }
}
