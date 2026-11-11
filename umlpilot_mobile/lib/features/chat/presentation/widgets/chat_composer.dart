import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../models/chat_mention_option.dart';

class ChatComposer extends StatefulWidget {
  const ChatComposer({
    super.key,
    required this.sending,
    required this.canMutate,
    required this.mentionOptions,
    required this.onSend,
    required this.onTyping,
    required this.onTranscribe,
    this.onSendImage,
  });

  final bool sending;
  final bool canMutate;
  final List<ChatMentionOption> mentionOptions;
  final ValueChanged<String> onSend;
  final ValueChanged<bool> onTyping;
  final Future<String> Function(Uint8List audio) onTranscribe;
  final void Function(Uint8List image, String filename, String? instructions)? onSendImage;

  @override
  State<ChatComposer> createState() => _ChatComposerState();
}

class _ChatComposerState extends State<ChatComposer> {
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  final AudioRecorder _recorder = AudioRecorder();
  Timer? _typingTimer;
  bool _hasText = false;
  bool _recording = false;
  final ImagePicker _imagePicker = ImagePicker();
  Uint8List? _imageBytes;
  String? _imageName;
  String? _imageError;
  bool _transcribing = false;
  String? _voiceError;

  @override
  void dispose() {
    _typingTimer?.cancel();
    unawaited(_recorder.dispose());
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _notifyTyping(bool typing) {
    widget.onTyping(typing);
    _typingTimer?.cancel();
    if (typing) {
      _typingTimer = Timer(const Duration(milliseconds: 1500), () {
        widget.onTyping(false);
      });
    }
  }

  void _send() {
    final text = _controller.text.trim();
    if (widget.sending || _transcribing) return;
    if (_imageBytes != null && widget.onSendImage != null) {
      widget.onSendImage!(_imageBytes!, _imageName ?? 'diagrama.jpg', text.isEmpty ? null : text);
      _controller.clear();
      setState(() {
        _hasText = false;
        _imageBytes = null;
        _imageName = null;
        _imageError = null;
      });
      _notifyTyping(false);
      _focusNode.requestFocus();
      return;
    }
    if (text.isEmpty) return;
    widget.onSend(text);
    _controller.clear();
    setState(() => _hasText = false);
    _notifyTyping(false);
    _focusNode.requestFocus();
  }

  Future<void> _pickImage() async {
    if (!widget.canMutate) return;
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      showDragHandle: true,
      backgroundColor: AppColors.surface,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Elegir de la galería'),
              onTap: () => Navigator.of(context).pop(ImageSource.gallery),
            ),
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Tomar una foto'),
              onTap: () => Navigator.of(context).pop(ImageSource.camera),
            ),
          ],
        ),
      ),
    );
    if (source == null || !mounted) return;
    try {
      final picked = await _imagePicker.pickImage(
        source: source,
        imageQuality: 85,
        maxWidth: 1800,
      );
      if (picked == null) return;
      final bytes = await picked.readAsBytes();
      if (!mounted) return;
      setState(() {
        _imageBytes = bytes;
        _imageName = picked.name.isEmpty ? 'diagrama.jpg' : picked.name;
        _imageError = null;
      });
    } catch (_) {
      if (mounted) setState(() => _imageError = 'No se pudo seleccionar la imagen');
    }
  }

  void _clearImage() {
    setState(() {
      _imageBytes = null;
      _imageName = null;
      _imageError = null;
    });
  }

  Future<void> _toggleRecording() async {
    if (_transcribing) return;
    if (_recording) {
      await _stopRecording();
      return;
    }
    setState(() => _voiceError = null);
    try {
      final permission = await _recorder.hasPermission();
      if (!permission) {
        if (mounted) setState(() => _voiceError = 'Permiso de micrófono denegado');
        return;
      }
      final directory = await getTemporaryDirectory();
      final path = '${directory.path}/umlpilot-voice-${DateTime.now().microsecondsSinceEpoch}.m4a';
      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.aacLc,
          sampleRate: 16000,
          numChannels: 1,
        ),
        path: path,
      );
      if (mounted) setState(() => _recording = true);
    } catch (_) {
      if (mounted) setState(() => _voiceError = 'No se pudo acceder al micrófono');
    }
  }

  Future<void> _stopRecording() async {
    String? path;
    try {
      path = await _recorder.stop();
    } catch (_) {
      path = null;
    }
    if (mounted) setState(() => _recording = false);
    if (path == null || path.isEmpty) return;

    Uint8List audio;
    try {
      audio = await File(path).readAsBytes();
    } catch (_) {
      if (mounted) setState(() => _voiceError = 'No se pudo leer el audio grabado');
      return;
    }
    if (audio.isEmpty) return;

    if (mounted) setState(() => _transcribing = true);
    try {
      final text = (await widget.onTranscribe(audio)).trim();
      if (text.isNotEmpty && mounted) {
        final value = _controller.value;
        final selection = value.selection;
        final start = selection.isValid ? selection.start : value.text.length;
        final end = selection.isValid ? selection.end : value.text.length;
        final before = value.text.substring(0, start);
        final after = value.text.substring(end);
        final needsSpace = before.isNotEmpty && !before.endsWith(' ') && !before.endsWith('\n');
        final insertion = '${needsSpace ? ' ' : ''}$text';
        _controller.value = TextEditingValue(
          text: '$before$insertion$after',
          selection: TextSelection.collapsed(offset: before.length + insertion.length),
        );
        setState(() => _hasText = true);
        _focusNode.requestFocus();
      }
    } catch (error) {
      if (mounted) {
        setState(() => _voiceError = error.toString().replaceFirst('Exception: ', ''));
      }
    } finally {
      if (mounted) setState(() => _transcribing = false);
    }
  }

  Future<void> _insertMention() async {
    if (widget.mentionOptions.isEmpty) return;
    final selected = await showModalBottomSheet<ChatMentionOption>(
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
                'Mencionar',
                style: TextStyle(color: AppColors.textSecondary, fontSize: 12, fontWeight: FontWeight.w600),
              ),
            ),
            ...widget.mentionOptions.map(
              (option) => ListTile(
                leading: CircleAvatar(
                  backgroundColor: option.isAssistant ? AppColors.primary : AppColors.surfaceAlt,
                  child: Icon(
                    option.isAssistant ? Icons.smart_toy_outlined : Icons.person_outline,
                    size: 18,
                    color: option.isAssistant ? Colors.white : AppColors.textSecondary,
                  ),
                ),
                title: Text(option.label, style: const TextStyle(color: AppColors.textPrimary)),
                subtitle: Text(option.role, style: const TextStyle(color: AppColors.textMuted, fontSize: 11)),
                trailing: option.isAssistant
                    ? const Chip(
                        visualDensity: VisualDensity.compact,
                        label: Text('IA', style: TextStyle(fontSize: 10)),
                      )
                    : null,
                onTap: () => Navigator.of(context).pop(option),
              ),
            ),
          ],
        ),
      ),
    );
    if (selected == null || !mounted) return;
    final value = _controller.value;
    final selection = value.selection;
    final start = selection.isValid ? selection.start : value.text.length;
    final end = selection.isValid ? selection.end : value.text.length;
    final before = value.text.substring(0, start);
    final after = value.text.substring(end);
    final needsSpace = before.isNotEmpty && !before.endsWith(' ') && !before.endsWith('\n');
    final insertion = '${needsSpace ? ' ' : ''}${selected.insertion}';
    final text = '$before$insertion$after';
    _controller.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: before.length + insertion.length),
    );
    _focusNode.requestFocus();
    setState(() => _hasText = true);
  }

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(AppSpacing.sm, AppSpacing.sm, AppSpacing.sm, AppSpacing.sm),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_imageBytes != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.sm, 0, AppSpacing.sm, AppSpacing.sm),
                  child: Container(
                    padding: const EdgeInsets.all(AppSpacing.sm),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Row(
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(AppRadius.md),
                          child: Image.memory(
                            _imageBytes!,
                            width: 52,
                            height: 52,
                            fit: BoxFit.cover,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.sm),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _imageName ?? 'Imagen',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(color: AppColors.textPrimary, fontSize: 12),
                              ),
                              const Text(
                                'Se construirá el diagrama seleccionado.',
                                style: TextStyle(color: AppColors.textMuted, fontSize: 10),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          tooltip: 'Quitar imagen',
                          onPressed: _clearImage,
                          icon: const Icon(Icons.close, size: 16, color: AppColors.textMuted),
                        ),
                      ],
                    ),
                  ),
                ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  IconButton(
                    tooltip: 'Mencionar a la IA o a un integrante',
                    onPressed: _insertMention,
                    icon: const Icon(Icons.alternate_email, color: AppColors.textSecondary),
                  ),
                  if (widget.onSendImage != null)
                    IconButton(
                      tooltip: widget.canMutate
                          ? 'Adjuntar imagen del diagrama'
                          : 'Solo owner/editor pueden construir desde imagen',
                      onPressed: widget.canMutate ? _pickImage : null,
                      icon: Icon(
                        Icons.image_outlined,
                        color: widget.canMutate ? AppColors.textSecondary : AppColors.textMuted,
                      ),
                    ),
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      focusNode: _focusNode,
                      minLines: 1,
                      maxLines: 5,
                      keyboardType: TextInputType.multiline,
                      textInputAction: TextInputAction.newline,
                      style: const TextStyle(color: AppColors.textPrimary),
                      onChanged: (value) {
                        setState(() => _hasText = value.trim().isNotEmpty);
                        _notifyTyping(value.trim().isNotEmpty);
                      },
                      decoration: InputDecoration(
                        hintText: 'Escribe al equipo… usa @asistente para la IA',
                        hintStyle: const TextStyle(color: AppColors.textMuted, fontSize: 13),
                        filled: true,
                        fillColor: AppColors.background,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(AppRadius.xl),
                          borderSide: const BorderSide(color: AppColors.border),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(AppRadius.xl),
                          borderSide: const BorderSide(color: AppColors.border),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(AppRadius.xl),
                          borderSide: const BorderSide(color: AppColors.primaryLight),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.xs),
                  IconButton(
                    tooltip: _recording ? 'Detener grabación' : 'Dictar por voz',
                    onPressed: widget.sending ? null : _toggleRecording,
                    icon: _transcribing
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Icon(
                            _recording ? Icons.stop_circle_outlined : Icons.mic_none,
                            color: _recording ? AppColors.danger : AppColors.textSecondary,
                          ),
                    style: IconButton.styleFrom(
                      backgroundColor: _recording
                          ? AppColors.danger.withValues(alpha: 0.12)
                          : AppColors.surfaceAlt,
                      foregroundColor: _recording ? AppColors.danger : AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.xs),
                  IconButton.filled(
                    tooltip: 'Enviar mensaje',
                    onPressed: widget.sending || _transcribing || (!_hasText && _imageBytes == null)
                        ? null
                        : _send,
                    icon: widget.sending
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Icon(Icons.send_rounded),
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.only(left: 44, right: 4),
                child: Text(
                  _recording
                      ? 'Grabando… pulsa el micrófono para detener.'
                      : _transcribing
                          ? 'Transcribiendo audio…'
                          : _imageError ??
                              _voiceError ??
                              (widget.canMutate
                                  ? 'La IA puede consultar y modificar el diagrama seleccionado.'
                                  : 'Modo lectura: puedes conversar, pero la IA no modificará el diagrama.'),
                  style: TextStyle(
                    color: _imageError != null || _voiceError != null || _recording
                        ? AppColors.danger
                        : _transcribing
                            ? AppColors.primaryLight
                            : AppColors.textMuted,
                    fontSize: 10,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
