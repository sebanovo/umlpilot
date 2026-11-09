import 'dart:ui';

import '../../../diagram-editor/domain/entities/uml_models.dart';

const double diagramNodeWidth = 200;
const double diagramNoteWidth = 220;
const double _rowHeight = 18;

Size nodeSizeFor(
  UmlElement element,
  int attributeCount,
  int methodCount,
  int literalCount,
) {
  if (element.elementType == 'note') {
    final lines = (element.name.length / 26).ceil().clamp(1, 10);
    return Size(diagramNoteWidth, 28.0 + lines * 16.0);
  }

  final hasStereotype = (element.stereotype != null && element.stereotype!.isNotEmpty) ||
      element.elementType == 'interface' ||
      element.elementType == 'enumeration';
  final header = hasStereotype ? 52.0 : 36.0;
  final rows = element.elementType == 'enumeration' ? literalCount : attributeCount + methodCount;
  final dividers = (attributeCount > 0 && methodCount > 0) ? 2.0 : 0.0;
  final height = (header + rows * _rowHeight + dividers + 10).clamp(60.0, 900.0);
  return Size(diagramNodeWidth, height);
}
