import 'package:flutter/foundation.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/errors/failures.dart';
import '../../domain/entities/chat_message.dart';
import '../../domain/usecases/send_message_usecase.dart';

/// Estado de la pantalla de chat (capa de presentación).
class ChatViewModel extends ChangeNotifier {
  ChatViewModel(this._sendMessage);

  static const String _welcomeMessage =
      '¡Hola! Soy el asistente de UMLPilot. ¿En qué te ayudo con tus diagramas?';

  final SendMessageUseCase _sendMessage;

  final List<ChatMessage> _messages = [
    ChatMessage.assistant(_welcomeMessage),
  ];
  bool _isSending = false;
  String? _errorMessage;

  List<ChatMessage> get messages => List.unmodifiable(_messages);
  bool get isSending => _isSending;
  String? get errorMessage => _errorMessage;

  Future<void> send(String text) async {
    final content = text.trim();
    if (content.isEmpty || _isSending) return;

    _messages.add(ChatMessage.user(content));
    _isSending = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final reply = await _sendMessage([
        ChatMessage.system(AppConstants.assistantSystemPrompt),
        ..._messages,
      ]);
      _messages.add(reply);
    } on Failure catch (failure) {
      _errorMessage = failure.message;
    } catch (_) {
      _errorMessage = 'Ocurrió un error inesperado. Intenta de nuevo.';
    } finally {
      _isSending = false;
      notifyListeners();
    }
  }

  void clearError() {
    if (_errorMessage == null) return;
    _errorMessage = null;
    notifyListeners();
  }

  void clearConversation() {
    _messages
      ..clear()
      ..add(ChatMessage.assistant(_welcomeMessage));
    _errorMessage = null;
    notifyListeners();
  }
}
