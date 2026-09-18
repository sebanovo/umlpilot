import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../viewmodels/chat_viewmodel.dart';

/// Campo de texto para escribir y enviar mensajes.
class ChatInput extends StatefulWidget {
  const ChatInput({super.key});

  @override
  State<ChatInput> createState() => _ChatInputState();
}

class _ChatInputState extends State<ChatInput> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _send() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    _controller.clear();
    context.read<ChatViewModel>().send(text);
  }

  @override
  Widget build(BuildContext context) {
    final isSending = context.select<ChatViewModel, bool>(
      (viewModel) => viewModel.isSending,
    );

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 8, 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: TextField(
              controller: _controller,
              minLines: 1,
              maxLines: 5,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _send(),
              decoration: InputDecoration(
                hintText: 'Escribe un mensaje…',
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                ),
              ),
            ),
          ),
          const SizedBox(width: 4),
          IconButton.filled(
            tooltip: 'Enviar',
            onPressed: isSending ? null : _send,
            icon: const Icon(Icons.send_rounded),
          ),
        ],
      ),
    );
  }
}
