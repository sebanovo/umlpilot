package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.SpringBootGenerationService;
import com.umlpilot.umlpilot_api.infrastructure.config.SecurityConfig;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la seguridad del endpoint de generación: sin token responde 401 (JSON) y, con
 * usuario autenticado, el despacho ASYNC del streaming no se re-autoriza (si se re-autorizara
 * con contexto anónimo daría {@code AuthorizationDeniedException} con la respuesta ya comprometida).
 */
@WebMvcTest(controllers = BackendGenerationController.class)
@Import(SecurityConfig.class)
class BackendGenerationSecurityTest {

    private static final String URL = "/api/v1/projects/p1/diagrams/d1/generate/backend";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpringBootGenerationService generationService;

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
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void asyncDispatchOfStreamingResponseIsAllowed() throws Exception {
        when(generationService.generateFiles("d1")).thenReturn(Map.of("pom.xml", "<project/>"));

        MvcResult result = mockMvc.perform(get(URL))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/zip"));
    }
}
