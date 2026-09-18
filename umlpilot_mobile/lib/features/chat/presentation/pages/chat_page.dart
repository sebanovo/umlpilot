import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/widgets/loading_widget.dart';
import '../viewmodels/chat_viewmodel.dart';
import '../widgets/chat_bubble.dart';
import '../widgets/chat_input.dart';

/// Pantalla del asistente: lista de mensajes + entrada de texto.
class ChatPage extends StatelessWidget {
  const ChatPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Asistente'),
        actions: [
          IconButton(
            tooltip: 'Limpiar conversación',
            onPressed: context.read<ChatViewModel>().clearConversation,
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
      body: const SafeArea(
        child: Column(
          children: [
            Expanded(child: _MessageList()),
            _ErrorBanner(),
            ChatInput(),
          ],
        ),
      ),
    );
  }
}

class _MessageList extends StatelessWidget {
  const _MessageList();

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ChatViewModel>();
    final messages = viewModel.messages;
    final showLoader = viewModel.isSending;

    return ListView.builder(
      reverse: true,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      itemCount: messages.length + (showLoader ? 1 : 0),
      itemBuilder: (context, index) {
        if (showLoader && index == 0) {
          return const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Align(
              alignment: Alignment.centerLeft,
              child: LoadingWidget(label: 'Escribiendo…'),
            ),
          );
        }
        final position = showLoader ? index - 1 : index;
        return ChatBubble(message: messages[messages.length - 1 - position]);
      },
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner();

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<ChatViewModel>();
    final message = viewModel.errorMessage;
    if (message == null) return const SizedBox.shrink();

    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      color: scheme.errorContainer,
      padding: const EdgeInsets.fromLTRB(16, 8, 4, 8),
      child: Row(
        children: [
          Icon(Icons.error_outline, size: 20, color: scheme.onErrorContainer),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(color: scheme.onErrorContainer),
            ),
          ),
          IconButton(
            tooltip: 'Cerrar',
            onPressed: viewModel.clearError,
            icon: Icon(Icons.close, size: 20, color: scheme.onErrorContainer),
          ),
        ],
      ),
    );
  }
}
