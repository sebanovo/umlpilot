import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../injection/injection.dart';
import '../../domain/usecases/send_message_usecase.dart';
import '../pages/chat_page.dart';
import '../viewmodels/chat_viewmodel.dart';

/// Botón flotante que abre el chat del asistente.
class ChatFab extends StatelessWidget {
  const ChatFab({super.key});

  @override
  Widget build(BuildContext context) {
    return FloatingActionButton(
      tooltip: 'Abrir asistente',
      onPressed: () => Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => ChangeNotifierProvider(
            create: (_) => ChatViewModel(getIt<SendMessageUseCase>()),
            child: const ChatPage(),
          ),
        ),
      ),
      child: const Icon(Icons.forum_outlined),
    );
  }
}
