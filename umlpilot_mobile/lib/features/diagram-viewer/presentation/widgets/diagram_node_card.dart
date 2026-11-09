import 'package:flutter/material.dart';

import '../../../../theme/app_colors.dart';
import '../../../diagram-editor/domain/entities/uml_models.dart';

class DiagramNodeCard extends StatelessWidget {
  const DiagramNodeCard({
    super.key,
    required this.element,
    required this.attributes,
    required this.methods,
    required this.literals,
    this.selected = false,
  });

  final UmlElement element;
  final List<UmlAttribute> attributes;
  final List<UmlMethod> methods;
  final List<UmlLiteral> literals;
  final bool selected;

  static const _mono = TextStyle(fontFamily: 'monospace', fontSize: 10.5, height: 1.3);

  String _visibility(String value) {
    return switch (value) {
      'private' => '-',
      'protected' => '#',
      'package' => '~',
      _ => '+',
    };
  }

  String get _stereotype {
    if (element.elementType == 'interface') return 'interface';
    if (element.elementType == 'enumeration') return 'enumeration';
    return element.stereotype ?? '';
  }

  Widget _row(String text, {Color color = AppColors.textPrimary}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      child: Text(
        text,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: _mono.copyWith(color: color),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isNote = element.elementType == 'note';
    final isEnum = element.elementType == 'enumeration';

    return Container(
      decoration: BoxDecoration(
        color: isNote ? AppColors.noteBackground : AppColors.surface,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: selected ? AppColors.primary : AppColors.borderLight,
          width: selected ? 2 : 1,
        ),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(5),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isNote)
              Padding(
                padding: const EdgeInsets.all(8),
                child: Text(
                  element.name,
                  maxLines: 6,
                  overflow: TextOverflow.ellipsis,
                  style: _mono.copyWith(color: AppColors.noteText),
                ),
              )
            else ...[
              if (_stereotype.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    '«$_stereotype»',
                    textAlign: TextAlign.center,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _mono.copyWith(color: AppColors.textSecondary, fontStyle: FontStyle.italic),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
                child: Text(
                  element.name,
                  textAlign: TextAlign.center,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: _mono.copyWith(color: AppColors.textPrimary, fontWeight: FontWeight.w700),
                ),
              ),
              if (attributes.isNotEmpty || methods.isNotEmpty || literals.isNotEmpty)
                const Divider(height: 1, color: AppColors.border),
              if (isEnum)
                ...literals.map((literal) => _row(literal.name))
              else ...[
                ...attributes.map(
                  (attribute) => _row(
                    '${_visibility(attribute.visibility)} ${attribute.name}: ${attribute.dataType}',
                  ),
                ),
                if (attributes.isNotEmpty && methods.isNotEmpty)
                  const Divider(height: 1, color: AppColors.border),
                ...methods.map(
                  (method) => _row(
                    '${_visibility(method.visibility)} ${method.name}(${method.parameters.map((p) => p.name).join(', ')}): ${method.returnType}',
                  ),
                ),
              ],
            ],
          ],
        ),
      ),
    );
  }
}
