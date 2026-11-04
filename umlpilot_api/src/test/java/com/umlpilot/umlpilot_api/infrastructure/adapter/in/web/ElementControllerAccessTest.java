package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ElementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ElementControllerAccessTest {

    private static final String URL = "/api/v1/projects/p1/diagrams/d1/elements";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElementService elementService;

    @MockitoBean
    private DiagramEventPublisher eventPublisher;

    @MockitoBean
    private ProjectAccessService accessService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void removedUserCannotListElements() throws Exception {
        doThrow(new DomainException("No tienes acceso a este proyecto"))
                .when(accessService).requireDiagramMember(eq("p1"), eq("d1"), any());

        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes acceso a este proyecto"));

        verifyNoInteractions(elementService);
    }

    @Test
    void viewerCannotCreateElements() throws Exception {
        doThrow(new DomainException("No tienes permisos para editar este proyecto"))
                .when(accessService).requireDiagramEditor(eq("p1"), eq("d1"), any());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente\",\"elementType\":\"class\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permisos para editar este proyecto"));

        verifyNoInteractions(elementService);
    }

    @Test
    void memberCanListElements() throws Exception {
        when(elementService.getElements("d1")).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
