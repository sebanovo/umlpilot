import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../../core/widgets/empty_state.dart';
import '../../../../core/widgets/error_state.dart';
import '../../../../core/widgets/loading_state.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../domain/entities/chat_models.dart';
import '../viewmodels/chat_viewmodel.dart';
import '../widgets/chat_composer.dart';
import '../widgets/chat_message_bubble.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final ScrollController _scrollController = ScrollController();
  bool _atBottom = true;
  int _lastMessageCount = 0;

  String get _projectId => GoRouterState.of(context).pathParameters['projectId'] ?? '';

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<ChatViewModel>().load(_projectId);
    });
  }

  @override
  void dispose() {
    _scrollController
      ..removeListener(_onScroll)
      ..dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final distance = _scrollController.position.maxScrollExtent - _scrollController.offset;
    final atBottom = distance < 80;
    if (atBottom != _atBottom) setState(() => _atBottom = atBottom);
  }

  void _scrollToBottom() {
    if (!_scrollController.hasClients) return;
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent,
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOut,
    );
  }

  void _scheduleAutoScroll(List<ChatMessage> messages) {
    final last = messages.isEmpty ? null : messages.last;
    final shouldScroll = _atBottom || (last != null && last.authorId == context.read<ChatViewModel>().currentUserId);
    if (messages.length != _lastMessageCount) {
      _lastMessageCount = messages.length;
      if (shouldScroll) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) _scrollToBottom();
        });
      }
    }
  }

  Future<void> _showParticipants(ChatViewModel viewModel) async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      backgroundColor: AppColors.surface,
      builder: (context) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.only(bottom: AppSpacing.md),
          children: [
            const Padding(
              padding: EdgeInsets.fromLTRB(AppSpacing.lg, 0, AppSpacing.lg, AppSpacing.sm),
              child: Text(
                'Participantes',
                style: TextStyle(color: AppColors.textSecondary, fontSize: 12, fontWeight: FontWeight.w600),
              ),
            ),
            ...viewModel.collaborators.map((collaborator) {
              final online = collaborator.userId == viewModel.currentUserId ||
                  viewModel.presenceIds.contains(collaborator.userId);
              return ListTile(
                leading: CircleAvatar(
                  backgroundColor: AppColors.primary.withValues(alpha: 0.2),
                  child: Text(
                    collaborator.fullName.isEmpty ? '?' : collaborator.fullName[0].toUpperCase(),
                    style: const TextStyle(color: AppColors.primaryLight, fontSize: 12),
                  ),
                ),
                title: Text(collaborator.fullName, style: const TextStyle(color: AppColors.textPrimary)),
                subtitle: Text(
                  collaborator.role,
                  style: const TextStyle(color: AppColors.textMuted, fontSize: 11),
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.circle, size: 8, color: online ? AppColors.success : AppColors.textMuted),
                    const SizedBox(width: 4),
                    Text(
                      online ? 'En línea' : 'Desconectado',
                      style: const TextStyle(color: AppColors.textMuted, fontSize: 10),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }

  String _dayKey(String? value) {
    final parsed = DateTime.tryParse(value ?? '');
    if (parsed == null) return '';
    final date = parsed.toLocal();
    return '${date.year}-${date.month}-${date.day}';
  }

  String _dayLabel(String? value) {
    final parsed = DateTime.tryParse(value ?? '');
    if (parsed == null) return '';
    final date = parsed.toLocal();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final day = DateTime(date.year, date.month, date.day);
    if (day == today) return 'Hoy';
    if (day == today.subtract(const Duration(days: 1))) return 'Ayer';
    return DateFormat('dd/MM/yyyy').format(date);
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ChatViewModel>();

    if (viewModel.loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Chat del equipo')),
        body: const LoadingState(message: 'Cargando conversación...'),
      );
    }
    if (viewModel.error != null && viewModel.project == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Chat del equipo')),
        body: ErrorState(
          message: viewModel.error!,
          onRetry: () => viewModel.load(_projectId),
        ),
      );
    }

    _scheduleAutoScroll(viewModel.messages);

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(viewModel.projectName ?? 'Chat del equipo', overflow: TextOverflow.ellipsis),
            Text(
              'Chat grupal · ${viewModel.connected ? 'en tiempo real' : 'reconectando'}',
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w400,
                color: viewModel.connected ? AppColors.success : AppColors.warning,
              ),
            ),
          ],
        ),
        actions: [
          Tooltip(
            message: viewModel.connected ? 'Conectado' : 'Sin conexión',
            child: Icon(
              viewModel.connected ? Icons.wifi : Icons.wifi_off,
              color: viewModel.connected ? AppColors.success : AppColors.warning,
            ),
          ),
          if (viewModel.diagrams.isNotEmpty)
            PopupMenuButton<String>(
              tooltip: 'Diagrama: ${viewModel.selectedDiagram?.name ?? 'ninguno'}',
              icon: const Icon(Icons.account_tree_outlined),
              onSelected: (value) {
                viewModel.setSelectedDiagram(value == '__none__' ? null : value);
              },
              itemBuilder: (_) => [
                const PopupMenuItem(value: '__none__', child: Text('Sin diagrama (solo consultas)')),
                ...viewModel.diagrams.map(
                  (diagram) => PopupMenuItem(value: diagram.id, child: Text(diagram.name)),
                ),
              ],
            ),
          IconButton(
            tooltip: 'Participantes',
            onPressed: () => _showParticipants(viewModel),
            icon: const Icon(Icons.group_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          if (viewModel.isViewer)
            Container(
              width: double.infinity,
              color: AppColors.warning.withValues(alpha: 0.1),
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
              child: const Row(
                children: [
                  Icon(Icons.visibility_outlined, size: 16, color: AppColors.warning),
                  SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      'Modo lectura: puedes conversar, pero la IA no modificará el diagrama.',
                      style: TextStyle(color: AppColors.warning, fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
          Expanded(
            child: viewModel.messages.isEmpty
                ? const EmptyState(
                    icon: Icons.forum_outlined,
                    title: 'Aún no hay mensajes',
                    message: 'Pregunta al asistente con @asistente o comparte una decisión con el equipo.',
                  )
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.fromLTRB(AppSpacing.md, AppSpacing.sm, AppSpacing.md, AppSpacing.md),
                    itemCount: viewModel.messages.length + 1,
                    itemBuilder: (context, index) {
                      if (index == 0) {
                        if (!viewModel.hasMore) return const SizedBox.shrink();
                        return Center(
                          child: TextButton(
                            onPressed: viewModel.loadingMore ? null : viewModel.loadOlder,
                            child: viewModel.loadingMore
                                ? const SizedBox(
                                    width: 16,
                                    height: 16,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  )
                                : const Text('Cargar mensajes anteriores'),
                          ),
                        );
                      }
                      final message = viewModel.messages[index - 1];
                      final previous = index > 1 ? viewModel.messages[index - 2] : null;
                      final showDay = previous == null ||
                          _dayKey(previous.createdAt) != _dayKey(message.createdAt);
                      return Column(
                        children: [
                          if (showDay)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                              child: Text(
                                _dayLabel(message.createdAt),
                                style: const TextStyle(color: AppColors.textMuted, fontSize: 10),
                              ),
                            ),
                          ChatMessageBubble(
                            message: message,
                            isOwn: message.authorId == viewModel.currentUserId,
                            mentionNames: viewModel.mentionNames,
                            onRetry: viewModel.retryMessage,
                          ),
                        ],
                      );
                    },
                  ),
          ),
          if (viewModel.typingNames.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(AppSpacing.lg, 0, AppSpacing.lg, AppSpacing.xs),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  '${viewModel.typingNames.join(', ')} ${viewModel.typingNames.length == 1 ? 'está escribiendo…' : 'están escribiendo…'}',
                  style: const TextStyle(color: AppColors.textMuted, fontSize: 10),
                ),
              ),
            ),
          if (viewModel.error != null)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.fromLTRB(AppSpacing.md, 0, AppSpacing.md, AppSpacing.xs),
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.xs),
              decoration: BoxDecoration(
                color: AppColors.danger.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(AppRadius.md),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      viewModel.error!,
                      style: const TextStyle(color: AppColors.danger, fontSize: 11),
                    ),
                  ),
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    onPressed: viewModel.clearError,
                    icon: const Icon(Icons.close, size: 14, color: AppColors.danger),
                  ),
                ],
              ),
            ),
          ChatComposer(
            sending: viewModel.sending,
            canMutate: viewModel.canMutate,
            mentionOptions: viewModel.mentionOptions,
            onSend: viewModel.sendMessage,
            onTyping: viewModel.sendTyping,
            onTranscribe: viewModel.transcribeAudio,
            onSendImage: (bytes, filename, instructions) => viewModel.sendImage(
              image: bytes,
              filename: filename,
              instructions: instructions,
            ),
          ),
        ],
      ),
    );
  }
}
