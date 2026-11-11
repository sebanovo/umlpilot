package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatConversationResult;
import com.umlpilot.umlpilot_api.application.dto.ChatMessageResult;
import com.umlpilot.umlpilot_api.application.dto.ChatPageResult;
import com.umlpilot.umlpilot_api.application.dto.ChatSendResult;
import com.umlpilot.umlpilot_api.application.dto.SendChatMessageCommand;
import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GroupChatService {

    private final ProjectAccessService accessService;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final GroupChatRepository chatRepository;
    private final ChatReplyService replyService;
    private final ChatEventPublisher eventPublisher;

    public GroupChatService(ProjectAccessService accessService,
                            CollaboratorRepository collaboratorRepository,
                            UserRepository userRepository,
                            GroupChatRepository chatRepository,
                            ChatReplyService replyService,
                            ChatEventPublisher eventPublisher) {
        this.accessService = accessService;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.replyService = replyService;
        this.eventPublisher = eventPublisher;
    }

    /** Valida la membresía en PostgreSQL para canales auxiliares del chat (p. ej. voz). */
    public void requireMember(String projectId, String userId) {
        accessService.requireMember(projectId, userId);
    }

    public ChatConversationResult getOrCreateConversation(String projectId, String userId) {
        Project project = accessService.requireMember(projectId, userId);
        GroupConversation conversation = chatRepository.getOrCreate(projectId, userId);
        String role = roleOf(project, userId);
        return toConversationResult(conversation, canMutate(role));
    }

    public ChatPageResult listMessages(String projectId, String userId, String beforeCursor, int limit) {
        accessService.requireMember(projectId, userId);
        GroupConversation conversation = chatRepository.getOrCreate(projectId, userId);
        int pageSize = Math.max(1, Math.min(limit, 100));
        ChatCursor cursor = ChatCursor.decode(beforeCursor);
        List<ConversationMessage> fetched = chatRepository.findMessagesBefore(
                conversation.id(),
                cursor == null ? null : cursor.createdAt(),
                cursor == null ? null : cursor.id(),
                pageSize + 1);

        boolean hasMore = fetched.size() > pageSize;
        List<ConversationMessage> page = hasMore ? fetched.subList(0, pageSize) : fetched;
        String nextCursor = page.isEmpty() ? null
                : new ChatCursor(page.get(page.size() - 1).createdAt(),
                        page.get(page.size() - 1).id()).encode();

        return new ChatPageResult(
                page.stream().map(ChatMessageResult::from).toList(),
                nextCursor,
                hasMore);
    }

    public ChatSendResult sendMessage(SendChatMessageCommand command) {
        if (command.content() == null || command.content().isBlank()) {
            throw new DomainException("El mensaje es requerido");
        }
        Project project = accessService.requireMember(command.projectId(), command.userId());
        String role = roleOf(project, command.userId());
        GroupConversation conversation = chatRepository.getOrCreate(command.projectId(), command.userId());

        String clientMessageId = command.clientMessageId();
        if (clientMessageId == null || clientMessageId.isBlank()) {
            clientMessageId = UUID.randomUUID().toString();
        }

        var existing = chatRepository.findMessageByClientId(conversation.id(), clientMessageId);
        if (existing.isPresent()) {
            return new ChatSendResult(ChatMessageResult.from(existing.get()), true);
        }

        if (command.diagramId() != null && !command.diagramId().isBlank()) {
            accessService.requireDiagramMember(command.projectId(), command.diagramId(), command.userId());
            conversation = chatRepository.save(conversation.withFocusDiagram(command.diagramId()));
        }

        boolean mentionedAssistant = ChatMentions.mentionsAssistant(command.content());
        ChatAuthor author = new ChatAuthor(command.userId(), authorName(command.userId()), role);
        ConversationMessage message = ConversationMessage.user(
                conversation.id(),
                command.projectId(),
                command.diagramId(),
                author,
                command.content(),
                clientMessageId)
                .withStatus(mentionedAssistant ? ChatMessageStatus.GENERATING : ChatMessageStatus.NONE);
        ConversationMessage saved = chatRepository.saveMessage(message);
        chatRepository.incrementUnsummarized(conversation.id());
        chatRepository.save(conversation.withLastMessage(saved.createdAt()));

        eventPublisher.messageCreated(command.projectId(), saved);
        if (mentionedAssistant) {
            eventPublisher.aiStatus(command.projectId(), saved.id(),
                    ChatMessageStatus.GENERATING, null);
            boolean allowMutations = canMutate(role);
            replyService.generate(conversation, saved, allowMutations);
        }
        return new ChatSendResult(ChatMessageResult.from(saved), false);
    }

    public ChatSendResult sendImageMessage(String projectId, String userId, String clientMessageId,
                                           String diagramId, String instructions,
                                           String filename, byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new DomainException("La imagen es requerida");
        }
        if (diagramId == null || diagramId.isBlank()) {
            throw new DomainException("Selecciona un diagrama de contexto para construir desde la imagen");
        }

        Project project = accessService.requireMember(projectId, userId);
        // Construir desde imagen muta el diagrama: solo owner/editor.
        accessService.requireDiagramEditor(projectId, diagramId, userId);
        String role = roleOf(project, userId);
        GroupConversation conversation = chatRepository.getOrCreate(projectId, userId);

        String clientId = (clientMessageId == null || clientMessageId.isBlank())
                ? UUID.randomUUID().toString()
                : clientMessageId;
        var existing = chatRepository.findMessageByClientId(conversation.id(), clientId);
        if (existing.isPresent()) {
            return new ChatSendResult(ChatMessageResult.from(existing.get()), true);
        }

        conversation = chatRepository.save(conversation.withFocusDiagram(diagramId));
        ChatAuthor author = new ChatAuthor(userId, authorName(userId), role);
        String prompt = (instructions == null || instructions.isBlank())
                ? "Construye el diagrama a partir de la imagen"
                : instructions.trim();
        String displayName = (filename == null || filename.isBlank()) ? "diagrama" : filename;
        String display = "[Imagen: " + displayName + "] " + prompt;

        ConversationMessage message = ConversationMessage.user(
                conversation.id(),
                projectId,
                diagramId,
                author,
                display,
                clientId)
                .withStatus(ChatMessageStatus.GENERATING);
        ConversationMessage saved = chatRepository.saveMessage(message);
        chatRepository.incrementUnsummarized(conversation.id());
        chatRepository.save(conversation.withLastMessage(saved.createdAt()));

        eventPublisher.messageCreated(projectId, saved);
        eventPublisher.aiStatus(projectId, saved.id(), ChatMessageStatus.GENERATING, null);
        replyService.generateFromImage(conversation, saved, image, contentType, prompt);
        return new ChatSendResult(ChatMessageResult.from(saved), false);
    }

    public ChatSendResult retryMessage(String projectId, String messageId, String userId) {
        Project project = accessService.requireMember(projectId, userId);
        ConversationMessage message = chatRepository.findMessageById(messageId)
                .orElseThrow(() -> new DomainException("Mensaje no encontrado"));
        if (!projectId.equals(message.projectId()) || !message.isUserMessage()) {
            throw new DomainException("El mensaje no pertenece al chat del proyecto");
        }
        if (message.aiStatus() != ChatMessageStatus.FAILED) {
            throw new DomainException("El mensaje no tiene una respuesta fallida");
        }
        String role = roleOf(project, userId);
        chatRepository.updateMessageStatus(messageId, ChatMessageStatus.GENERATING);
        eventPublisher.aiStatus(projectId, messageId, ChatMessageStatus.GENERATING, null);
        GroupConversation conversation = chatRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("Conversación no encontrada"));
        replyService.generate(conversation, message.withStatus(ChatMessageStatus.GENERATING), canMutate(role));
        return new ChatSendResult(ChatMessageResult.from(message.withStatus(ChatMessageStatus.GENERATING)), false);
    }

    public void archiveByProject(String projectId) {
        chatRepository.archiveByProject(projectId);
    }

    private ChatConversationResult toConversationResult(GroupConversation conversation, boolean canMutate) {
        return new ChatConversationResult(
                conversation.id(),
                conversation.projectId(),
                conversation.title(),
                conversation.summaryText(),
                conversation.focusDiagramId(),
                canMutate,
                conversation.lastMessageAt(),
                conversation.unsummarizedCount());
    }

    private String roleOf(Project project, String userId) {
        if (project.getCreatorId().value().equals(userId)) {
            return "owner";
        }
        return collaboratorRepository.findAcceptedCollaborator(project.getId().value(), userId)
                .map(Collaborator::getRole)
                .orElse("viewer");
    }

    private boolean canMutate(String role) {
        return "owner".equals(role) || "editor".equals(role);
    }

    private String authorName(String userId) {
        User user = userRepository.findById(UserId.from(userId)).orElse(null);
        if (user == null) return "Usuario";
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? user.getEmail() : name;
    }
}
