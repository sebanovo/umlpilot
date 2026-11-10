enum ChatAiStatus {
  none,
  generating,
  completed,
  failed;

  static ChatAiStatus fromValue(String? value) {
    switch ((value ?? '').toLowerCase()) {
      case 'generating':
        return ChatAiStatus.generating;
      case 'completed':
        return ChatAiStatus.completed;
      case 'failed':
        return ChatAiStatus.failed;
      default:
        return ChatAiStatus.none;
    }
  }

  String get value => name;
}

class ChatConversation {
  const ChatConversation({
    required this.conversationId,
    required this.projectId,
    required this.title,
    required this.canMutate,
    this.summary,
    this.focusDiagramId,
    this.lastMessageAt,
    this.unsummarizedCount = 0,
  });

  factory ChatConversation.fromJson(Map<String, dynamic> json) {
    return ChatConversation(
      conversationId: (json['conversationId'] ?? '').toString(),
      projectId: (json['projectId'] ?? '').toString(),
      title: (json['title'] ?? 'Chat del proyecto').toString(),
      summary: json['summary'] as String?,
      focusDiagramId: json['focusDiagramId'] as String?,
      canMutate: json['canMutate'] == true,
      lastMessageAt: json['lastMessageAt'] as String?,
      unsummarizedCount: json['unsummarizedCount'] is num
          ? (json['unsummarizedCount'] as num).toInt()
          : 0,
    );
  }

  final String conversationId;
  final String projectId;
  final String title;
  final String? summary;
  final String? focusDiagramId;
  final bool canMutate;
  final String? lastMessageAt;
  final int unsummarizedCount;
}

class ChatCitation {
  const ChatCitation({
    required this.messageId,
    required this.snippet,
    required this.authorName,
    this.createdAt,
  });

  factory ChatCitation.fromJson(Map<String, dynamic> json) {
    return ChatCitation(
      messageId: (json['messageId'] ?? '').toString(),
      snippet: (json['snippet'] ?? '').toString(),
      authorName: (json['authorName'] ?? '').toString(),
      createdAt: json['createdAt'] as String?,
    );
  }

  final String messageId;
  final String snippet;
  final String authorName;
  final String? createdAt;
}

class ChatAction {
  const ChatAction({
    required this.action,
    required this.entity,
    required this.id,
    required this.result,
  });

  factory ChatAction.fromJson(Map<String, dynamic> json) {
    return ChatAction(
      action: (json['action'] ?? '').toString(),
      entity: (json['entity'] ?? '').toString(),
      id: (json['id'] ?? '').toString(),
      result: (json['result'] ?? '').toString(),
    );
  }

  final String action;
  final String entity;
  final String id;
  final String result;
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.conversationId,
    required this.projectId,
    required this.authorType,
    required this.authorName,
    required this.authorRole,
    required this.content,
    required this.aiStatus,
    this.diagramId,
    this.authorId,
    this.clientMessageId,
    this.replyToId,
    this.citations = const [],
    this.actions = const [],
    this.provider,
    this.model,
    this.createdAt,
    this.pending = false,
    this.failed = false,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    final citations = (json['citations'] as List? ?? const [])
        .map((item) => ChatCitation.fromJson((item as Map).cast<String, dynamic>()))
        .toList();
    final actions = (json['actions'] as List? ?? const [])
        .map((item) => ChatAction.fromJson((item as Map).cast<String, dynamic>()))
        .toList();
    return ChatMessage(
      id: (json['id'] ?? '').toString(),
      conversationId: (json['conversationId'] ?? '').toString(),
      projectId: (json['projectId'] ?? '').toString(),
      diagramId: json['diagramId'] as String?,
      authorType: (json['authorType'] ?? 'user').toString(),
      authorId: json['authorId'] as String?,
      authorName: (json['authorName'] ?? 'Usuario').toString(),
      authorRole: (json['authorRole'] ?? 'viewer').toString(),
      content: (json['content'] ?? '').toString(),
      clientMessageId: json['clientMessageId'] as String?,
      replyToId: json['replyToId'] as String?,
      citations: citations,
      actions: actions,
      provider: json['provider'] as String?,
      model: json['model'] as String?,
      aiStatus: ChatAiStatus.fromValue(json['aiStatus'] as String?),
      createdAt: json['createdAt'] as String?,
    );
  }

  final String id;
  final String conversationId;
  final String projectId;
  final String? diagramId;
  final String authorType;
  final String? authorId;
  final String authorName;
  final String authorRole;
  final String content;
  final String? clientMessageId;
  final String? replyToId;
  final List<ChatCitation> citations;
  final List<ChatAction> actions;
  final String? provider;
  final String? model;
  final ChatAiStatus aiStatus;
  final String? createdAt;
  final bool pending;
  final bool failed;

  bool get isUser => authorType == 'user';
  bool get isAssistant => authorType == 'assistant';
  bool get isSystem => authorType == 'system';

  ChatMessage copyWith({
    String? id,
    ChatAiStatus? aiStatus,
    bool? pending,
    bool? failed,
  }) {
    return ChatMessage(
      id: id ?? this.id,
      conversationId: conversationId,
      projectId: projectId,
      diagramId: diagramId,
      authorType: authorType,
      authorId: authorId,
      authorName: authorName,
      authorRole: authorRole,
      content: content,
      clientMessageId: clientMessageId,
      replyToId: replyToId,
      citations: citations,
      actions: actions,
      provider: provider,
      model: model,
      aiStatus: aiStatus ?? this.aiStatus,
      createdAt: createdAt,
      pending: pending ?? this.pending,
      failed: failed ?? this.failed,
    );
  }
}

class ChatPage {
  const ChatPage({
    required this.messages,
    required this.hasMore,
    this.nextCursor,
  });

  factory ChatPage.fromJson(Map<String, dynamic> json) {
    return ChatPage(
      messages: (json['messages'] as List? ?? const [])
          .map((item) => ChatMessage.fromJson((item as Map).cast<String, dynamic>()))
          .toList(),
      nextCursor: json['nextCursor'] as String?,
      hasMore: json['hasMore'] == true,
    );
  }

  final List<ChatMessage> messages;
  final String? nextCursor;
  final bool hasMore;
}

class ChatSendResult {
  const ChatSendResult({required this.message, required this.duplicate});

  factory ChatSendResult.fromJson(Map<String, dynamic> json) {
    return ChatSendResult(
      message: ChatMessage.fromJson((json['message'] as Map).cast<String, dynamic>()),
      duplicate: json['duplicate'] == true,
    );
  }

  final ChatMessage message;
  final bool duplicate;
}

bool mentionsAssistant(String content) {
  return RegExp(r'@(asistente|ai|ia)\b', caseSensitive: false).hasMatch(content);
}
