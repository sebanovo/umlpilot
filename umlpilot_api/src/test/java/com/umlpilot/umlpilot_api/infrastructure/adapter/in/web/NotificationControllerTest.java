package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.NotificationResult;
import com.umlpilot.umlpilot_api.application.service.NotificationService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtAuthenticationFilter;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void listsNotifications() throws Exception {
        when(notificationService.getNotifications(isNull(), eq(false))).thenReturn(List.of(
                new NotificationResult("n1", "project_invitation", "Invitación a proyecto",
                        "Ana te invitó", "p1", "editor", false, null, "2026-01-01T10:00", "pending")));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("n1"))
                .andExpect(jsonPath("$[0].invitationStatus").value("pending"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void returnsUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(isNull())).thenReturn(4L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void respondsToInvitation() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/n1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"accept\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Respuesta registrada"));

        verify(notificationService).respondToInvitation(eq("n1"), any(), eq("accept"));
    }

    @Test
    void invalidResponseReturns400() throws Exception {
        doThrow(new DomainException("Acción no válida"))
                .when(notificationService).respondToInvitation(eq("n1"), any(), eq("maybe"));

        mockMvc.perform(post("/api/v1/notifications/n1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"maybe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Acción no válida"));
    }

    @Test
    void marksAllAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).markAllAsRead(null);
    }
}
