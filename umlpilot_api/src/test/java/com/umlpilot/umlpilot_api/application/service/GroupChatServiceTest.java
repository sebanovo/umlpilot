package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatConversationResult;
import com.umlpilot.umlpilot_api.application.dto.ChatPageResult;
import com.umlpilot.umlpilot_api.application.dto.ChatSendResult;
import com.umlpilot.umlpilot_api.application.dto.SendChatMessageCommand;
import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.Role;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupChatServiceTest {

    private ProjectAccessService accessService;
    private CollaboratorRepository collaboratorRepository;
    private UserRepository userRepository;
    private GroupChatRepository chatRepository;
    private ChatReplyService replyService;
    private ChatEventPublisher eventPublisher;
    private GroupChatService service;

    private final Project project = Project.reconstitute(
            ProjectId.from("p1"), "Proyecto", "", UserId.from("u1"), "active",
            LocalDateTime.now(), LocalDateTime.now());
    private final GroupConversation conversation = GroupConversation.create("p1", "u1").withId("c1");

    @BeforeEach
    void setUp() {
        accessService = mock(ProjectAccessService.class);
        collaboratorRepository = mock(CollaboratorRepository.class);
        userRepository = mock(UserRepository.class);
        chatRepository = mock(GroupChatRepository.class);
        replyService = mock(ChatReplyService.class);
        eventPublisher = mock(ChatEventPublisher.class);
        service = new GroupChatService(accessService, collaboratorRepository, userRepository,
                chatRepository, replyService, eventPublisher);
    }

    @Test
    void ownerCanMutate() {
        when(accessService.requireMember("p1", "u1")).thenReturn(project);
        when(chatRepository.getOrCreate("p1", "u1")).thenReturn(conversation);

        ChatConversationResult result = service.getOrCreateConversation("p1", "u1");

        assertEquals("c1", result.conversationId());
        assertTrue(result.canMutate());
    }

    @Test
    void viewerSendsWithoutMutationTools() {
        User viewer = User.reconstitute(UserId.from("u2"), "luis@example.com", "x",
                "Luis", "Perez", Role.USER, LocalDateTime.now(), LocalDateTime.now());
        when(accessService.requireMember("p1", "u2")).thenReturn(project);
        when(collaboratorRepository.findAcceptedCollaborator("p1", "u2"))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", "u2", "viewer")));
        when(chatRepository.getOrCreate("p1", "u2")).thenReturn(conversation);
        when(chatRepository.findMessageByClientId("c1", "client-1")).thenReturn(Optional.empty());
        when(userRepository.findById(UserId.from("u2"))).thenReturn(Optional.of(viewer));
        when(chatRepository.saveMessage(any())).thenAnswer(invocation ->
                ((ConversationMessage) invocation.getArgument(0)).withId("m1"));

        ChatSendResult result = service.sendMessage(
                new SendChatMessageCommand("p1", "u2", "client-1", "@asistente Hola equipo", null));

        assertFalse(result.duplicate());
        assertEquals("m1", result.message().id());
        assertEquals("viewer", result.message().authorRole());
        assertEquals("generating", result.message().aiStatus());
        verify(replyService).generate(any(), any(), eq(false));
        verify(eventPublisher).messageCreated(eq("p1"), any());
    }

    @Test
    void messageWithoutAssistantMentionDoesNotInvokeAi() {
        when(accessService.requireMember("p1", "u1")).thenReturn(project);
        when(chatRepository.getOrCreate("p1", "u1")).thenReturn(conversation);
        when(chatRepository.findMessageByClientId("c1", "client-2")).thenReturn(Optional.empty());
        when(userRepository.findById(UserId.from("u1"))).thenReturn(Optional.of(
                User.reconstitute(UserId.from("u1"), "ana@example.com", "x",
                        "Ana", "Perez", Role.USER, LocalDateTime.now(), LocalDateTime.now())));
        when(chatRepository.saveMessage(any())).thenAnswer(invocation ->
                ((ConversationMessage) invocation.getArgument(0)).withId("m2"));

        ChatSendResult result = service.sendMessage(
                new SendChatMessageCommand("p1", "u1", "client-2", "Hola equipo", null));

        assertEquals("none", result.message().aiStatus());
        verify(replyService, never()).generate(any(), any(), anyBoolean());
        verify(eventPublisher).messageCreated(eq("p1"), any());
    }

    @Test
    void imageBuildRequiresEditor() {
        when(accessService.requireMember("p1", "u2")).thenReturn(project);
        doThrow(new DomainException("No tienes permisos para editar este proyecto"))
                .when(accessService).requireDiagramEditor("p1", "d1", "u2");

        assertThrows(DomainException.class, () -> service.sendImageMessage(
                "p1", "u2", "client-image", "d1", "Construye", "diagrama.png",
                new byte[] { 1, 2 }, "image/png"));
        verify(chatRepository, never()).saveMessage(any());
    }

    @Test
    void duplicateClientMessageDoesNotGenerateAgain() {
        ConversationMessage existing = ConversationMessage.user(
                "c1", "p1", null, ChatAuthor.user("u1", "Ana", "owner"),
                "Hola", "client-1").withId("m1");
        when(accessService.requireMember("p1", "u1")).thenReturn(project);
        when(chatRepository.getOrCreate("p1", "u1")).thenReturn(conversation);
        when(chatRepository.findMessageByClientId("c1", "client-1")).thenReturn(Optional.of(existing));

        ChatSendResult result = service.sendMessage(
                new SendChatMessageCommand("p1", "u1", "client-1", "Hola", null));

        assertTrue(result.duplicate());
        verify(chatRepository, never()).saveMessage(any());
        verify(replyService, never()).generate(any(), any(), anyBoolean());
    }

    @Test
    void paginationReportsMoreAndCursor() {
        when(accessService.requireMember("p1", "u1")).thenReturn(project);
        when(chatRepository.getOrCreate("p1", "u1")).thenReturn(conversation);
        ConversationMessage m1 = ConversationMessage.assistant(
                "c1", "p1", null, "Uno", null, null, null, null, null).withId("m1");
        ConversationMessage m2 = ConversationMessage.assistant(
                "c1", "p1", null, "Dos", null, null, null, null, null).withId("m2");
        ConversationMessage m3 = ConversationMessage.assistant(
                "c1", "p1", null, "Tres", null, null, null, null, null).withId("m3");
        when(chatRepository.findMessagesBefore("c1", null, null, 3))
                .thenReturn(List.of(m1, m2, m3));

        ChatPageResult page = service.listMessages("p1", "u1", null, 2);

        assertEquals(2, page.messages().size());
        assertTrue(page.hasMore());
        assertTrue(page.nextCursor() != null && !page.nextCursor().isBlank());
    }
}
