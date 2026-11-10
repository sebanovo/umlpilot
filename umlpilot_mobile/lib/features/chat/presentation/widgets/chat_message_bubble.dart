import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../domain/entities/chat_models.dart';

class ChatMessageBubble extends StatelessWidget {
  const ChatMessageBubble({
    super.key,
    required this.message,
    required this.isOwn,
    required this.mentionNames,
    required this.onRetry,
  });

  final ChatMessage message;
  final bool isOwn;
  final List<String> mentionNames;
  final ValueChanged<String> onRetry;

  String _time() {
    final date = DateTime.tryParse(message.createdAt ?? '');
    return date == null ? '' : DateFormat('HH:mm').format(date.toLocal());
  }

  String? _providerLabel() {
    if (!message.isAssistant || message.provider == null) return null;
    return message.model == null ? message.provider : '${message.provider} · ${message.model}';
  }

  String _actionLabel(String action) {
    switch (action) {
      case 'create':
        return 'creó';
      case 'update':
        return 'actualizó';
      case 'delete':
        return 'eliminó';
      case 'read':
        return 'leyó';
      default:
        return action;
    }
  }

  List<InlineSpan> _contentSpans() {
    final labels = mentionNames.where((name) => name.trim().isNotEmpty).toList()
      ..sort((a, b) => b.length.compareTo(a.length));
    if (labels.isEmpty) {
      return [TextSpan(text: message.content)];
    }
    final escaped = labels.map(RegExp.escape).join('|');
    final expression = RegExp('@(?:$escaped|Asistente IA|Asistente|AI|IA)\\b', caseSensitive: false);
    final spans = <InlineSpan>[];
    var last = 0;
    for (final match in expression.allMatches(message.content)) {
      if (match.start > last) {
        spans.add(TextSpan(text: message.content.substring(last, match.start)));
      }
      spans.add(TextSpan(
        text: match.group(0),
        style: TextStyle(
          color: isOwn ? Colors.white : AppColors.primaryLight,
          fontWeight: FontWeight.w600,
          backgroundColor: (isOwn ? Colors.white : AppColors.primaryLight).withValues(alpha: 0.12),
        ),
      ));
      last = match.end;
    }
    if (last < message.content.length) {
      spans.add(TextSpan(text: message.content.substring(last)));
    }
    return spans;
  }

  @override
  Widget build(BuildContext context) {
    if (message.isSystem) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.xs),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(AppRadius.full),
            ),
            child: Text(
              message.content,
              style: const TextStyle(color: AppColors.textMuted, fontSize: 11),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      );
    }

    final textColor = isOwn ? Colors.white : AppColors.textPrimary;
    final provider = _providerLabel();
    return Align(
      alignment: isOwn ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: MediaQuery.sizeOf(context).width * 0.82),
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
          padding: const EdgeInsets.fromLTRB(AppSpacing.md, AppSpacing.sm, AppSpacing.md, AppSpacing.sm),
          decoration: BoxDecoration(
            color: isOwn ? AppColors.primary : AppColors.surfaceAlt,
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(AppRadius.xl),
              topRight: const Radius.circular(AppRadius.xl),
              bottomLeft: Radius.circular(isOwn ? AppRadius.xl : AppRadius.sm),
              bottomRight: Radius.circular(isOwn ? AppRadius.sm : AppRadius.xl),
            ),
            border: isOwn ? null : Border.all(color: AppColors.border),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    message.isAssistant ? Icons.smart_toy_outlined : Icons.person_outline,
                    size: 12,
                    color: isOwn ? Colors.white70 : AppColors.textSecondary,
                  ),
                  const SizedBox(width: 4),
                  Flexible(
                    child: Text(
                      message.authorName,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: isOwn ? Colors.white70 : AppColors.textSecondary,
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    message.authorRole,
                    style: TextStyle(
                      color: isOwn ? Colors.white54 : AppColors.textMuted,
                      fontSize: 9,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _time(),
                    style: TextStyle(
                      color: isOwn ? Colors.white54 : AppColors.textMuted,
                      fontSize: 9,
                    ),
                  ),
                  if (provider != null) ...[
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        provider,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: isOwn ? Colors.white54 : AppColors.textMuted,
                          fontSize: 9,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 3),
              Text.rich(
                TextSpan(
                  style: TextStyle(color: textColor, fontSize: 14, height: 1.35),
                  children: _contentSpans(),
                ),
              ),
              if (message.citations.isNotEmpty) ...[
                const SizedBox(height: AppSpacing.sm),
                Wrap(
                  spacing: AppSpacing.xs,
                  runSpacing: AppSpacing.xs,
                  children: message.citations.map((citation) {
                    return Tooltip(
                      message: citation.snippet,
                      child: Chip(
                        visualDensity: VisualDensity.compact,
                        avatar: const Icon(Icons.format_quote, size: 13),
                        label: Text(
                          citation.authorName,
                          style: TextStyle(
                            fontSize: 10,
                            color: isOwn ? Colors.white : AppColors.textSecondary,
                          ),
                        ),
                        backgroundColor: isOwn ? Colors.white.withValues(alpha: 0.12) : AppColors.surface,
                        side: BorderSide(color: isOwn ? Colors.white24 : AppColors.border),
                      ),
                    );
                  }).toList(),
                ),
              ],
              if (message.actions.isNotEmpty) ...[
                const SizedBox(height: AppSpacing.sm),
                ...message.actions.where((action) => action.action != 'read').map(
                      (action) => Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.check_circle_outline, size: 12, color: isOwn ? Colors.white70 : AppColors.success),
                            const SizedBox(width: 4),
                            Flexible(
                              child: Text(
                                '${_actionLabel(action.action)} ${action.entity}',
                                style: TextStyle(
                                  color: isOwn ? Colors.white70 : AppColors.success,
                                  fontSize: 10,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
              ],
              if (!message.isAssistant && message.aiStatus != ChatAiStatus.none) ...[
                const SizedBox(height: AppSpacing.xs),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (message.aiStatus == ChatAiStatus.generating || message.pending)
                      const Padding(
                        padding: EdgeInsets.only(right: 4),
                        child: SizedBox(
                          width: 10,
                          height: 10,
                          child: CircularProgressIndicator(strokeWidth: 1.5, color: Colors.white70),
                        ),
                      ),
                    Text(
                      message.aiStatus == ChatAiStatus.failed
                          ? 'No se pudo responder'
                          : message.aiStatus == ChatAiStatus.completed
                              ? 'Respondido'
                              : 'IA generando…',
                      style: TextStyle(
                        color: isOwn ? Colors.white70 : AppColors.textMuted,
                        fontSize: 10,
                      ),
                    ),
                    if (message.aiStatus == ChatAiStatus.failed) ...[
                      const SizedBox(width: AppSpacing.xs),
                      TextButton(
                        onPressed: () => onRetry(message.id),
                        style: TextButton.styleFrom(
                          minimumSize: Size.zero,
                          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          foregroundColor: isOwn ? Colors.white : AppColors.primaryLight,
                        ),
                        child: const Text('Reintentar', style: TextStyle(fontSize: 10)),
                      ),
                    ],
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
