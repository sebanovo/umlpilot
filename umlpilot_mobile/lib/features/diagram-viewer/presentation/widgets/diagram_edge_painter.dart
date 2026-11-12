import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../theme/app_colors.dart';
import '../../../diagram-editor/domain/entities/uml_models.dart';
import 'diagram_geometry.dart';

class DiagramEdgePainter extends CustomPainter {
  DiagramEdgePainter({
    required this.elements,
    required this.relationships,
    required this.attributes,
    required this.methods,
    required this.literals,
    required this.multiplicities,
    required this.origin,
    required this.selectedRelationshipId,
  });

  final List<UmlElement> elements;
  final List<UmlRelationship> relationships;
  final Map<String, List<UmlAttribute>> attributes;
  final Map<String, List<UmlMethod>> methods;
  final Map<String, List<UmlLiteral>> literals;
  final Map<String, List<UmlMultiplicity>> multiplicities;
  final Offset origin;
  final String? selectedRelationshipId;

  @override
  void paint(Canvas canvas, Size size) {
    final byId = {for (final element in elements) element.id: element};
    for (final relationship in relationships) {
      final source = byId[relationship.sourceElementId];
      final target = byId[relationship.targetElementId];
      if (source == null || target == null) continue;

      final selected = relationship.id == selectedRelationshipId;
      final color = selected ? AppColors.primaryLight : AppColors.diagramEdge;
      final paint = Paint()
        ..color = color
        ..strokeWidth = selected ? 2.2 : 1.4
        ..style = PaintingStyle.stroke;
      final sourceRect = _rectFor(source);
      final targetRect = _rectFor(target);

      if (source.id == target.id) {
        _paintSelfLoop(canvas, sourceRect, relationship, paint, color);
        _paintAssociationClass(
          canvas,
          relationship,
          byId,
          Offset(sourceRect.right + 90, sourceRect.center.dy),
          paint,
          color,
          dashed: true,
        );
        continue;
      }

      final start = _boundaryPoint(sourceRect, targetRect.center);
      final end = _boundaryPoint(targetRect, sourceRect.center);

      if (relationship.relationshipType == 'realization') {
        _dashedLine(canvas, start, end, paint);
      } else {
        canvas.drawLine(start, end, paint);
      }

      switch (relationship.relationshipType) {
        case 'generalization':
        case 'realization':
          _triangle(canvas, end, start, color);
          break;
        case 'aggregation':
          // El rombo va en el extremo destino, igual que en la web.
          _diamond(canvas, end, start, color, filled: false);
          break;
        case 'composition':
          _diamond(canvas, end, start, color, filled: true);
          break;
        default:
          // Asociación y clase de asociación: sin punta de flecha.
          break;
      }

      _label(canvas, relationship.name ?? '', _relationshipLabelPosition(start, end), color);
      final list = multiplicities[relationship.id] ?? const [];
      final sourceMultiplicity = list.where((item) => item.end == 'source').firstOrNull;
      final targetMultiplicity = list.where((item) => item.end == 'target').firstOrNull;
      if (sourceMultiplicity != null && sourceMultiplicity.label.isNotEmpty) {
        _label(canvas, sourceMultiplicity.label, _offsetAlong(start, end, 18), color);
      }
      if (targetMultiplicity != null && targetMultiplicity.label.isNotEmpty) {
        _label(canvas, targetMultiplicity.label, _offsetAlong(end, start, 18), color);
      }

      _paintAssociationClass(canvas, relationship, byId, _midpoint(start, end), paint, color);
    }
  }

  Rect _rectFor(UmlElement element) {
    final size = nodeSizeFor(
      element,
      (attributes[element.id] ?? const []).length,
      (methods[element.id] ?? const []).length,
      (literals[element.id] ?? const []).length,
    );
    return Rect.fromLTWH(
      element.positionX.toDouble() - origin.dx,
      element.positionY.toDouble() - origin.dy,
      size.width,
      size.height,
    );
  }

  void _paintAssociationClass(
    Canvas canvas,
    UmlRelationship relationship,
    Map<String, UmlElement> byId,
    Offset anchor,
    Paint paint,
    Color color, {
    bool dashed = false,
  }) {
    final associationClassId = relationship.associationClassElementId;
    if (associationClassId == null) return;
    final associationClass = byId[associationClassId];
    if (associationClass == null) return;

    final rect = _rectFor(associationClass);
    // Mismo anclaje que la web: si el punto medio cae dentro del nodo, se
    // conecta al centro de su borde superior; si no, se recorta al rectángulo.
    final inside = rect.contains(anchor);
    final tx = inside ? rect.center.dx : anchor.dx.clamp(rect.left, rect.right).toDouble();
    final ty = inside ? rect.top : anchor.dy.clamp(rect.top, rect.bottom).toDouble();
    final endpoint = Offset(tx, ty);

    final connector = Paint()
      ..color = color
      ..strokeWidth = 1.2
      ..style = PaintingStyle.stroke;
    if (dashed) {
      _dashedLine(canvas, anchor, endpoint, connector);
    } else {
      canvas.drawLine(anchor, endpoint, connector);
    }
    canvas.drawCircle(anchor, 3, Paint()..color = color);
  }

  Offset _boundaryPoint(Rect rect, Offset toward) {
    final center = rect.center;
    final dx = toward.dx - center.dx;
    final dy = toward.dy - center.dy;
    double t = double.infinity;
    if (dx.abs() > 0.001) t = math.min(t, (dx > 0 ? rect.right - center.dx : rect.left - center.dx) / dx);
    if (dy.abs() > 0.001) t = math.min(t, (dy > 0 ? rect.bottom - center.dy : rect.top - center.dy) / dy);
    if (!t.isFinite) return center;
    return center + Offset(dx * t, dy * t);
  }

  Offset _midpoint(Offset a, Offset b) => Offset((a.dx + b.dx) / 2, (a.dy + b.dy) / 2);

  Offset _relationshipLabelPosition(Offset start, Offset end) {
    final mid = _midpoint(start, end);
    final delta = end - start;
    final length = delta.distance;
    if (length == 0) return mid - const Offset(0, 12);

    var nx = -delta.dy / length;
    var ny = delta.dx / length;
    // Horizontal: arriba. Vertical: a la derecha. Diagonal: normal exterior.
    if (delta.dx.abs() >= delta.dy.abs()) {
      if (ny > 0) {
        nx = -nx;
        ny = -ny;
      }
    } else if (nx < 0) {
      nx = -nx;
      ny = -ny;
    }
    return mid + Offset(nx * 12, ny * 12);
  }

  Offset _offsetAlong(Offset from, Offset to, double distance) {
    final delta = to - from;
    final length = delta.distance;
    if (length == 0) return from;
    return from + delta / length * distance;
  }

  void _dashedLine(Canvas canvas, Offset a, Offset b, Paint paint) {
    const dash = 6.0;
    const gap = 4.0;
    final delta = b - a;
    final total = delta.distance;
    if (total == 0) return;
    final unit = delta / total;
    var traveled = 0.0;
    while (traveled < total) {
      final end = math.min(traveled + dash, total);
      canvas.drawLine(a + unit * traveled, a + unit * end, paint);
      traveled += dash + gap;
    }
  }

  void _triangle(Canvas canvas, Offset tip, Offset from, Color color) {
    final angle = math.atan2(tip.dy - from.dy, tip.dx - from.dx);
    const size = 12.0;
    final p1 = tip - Offset(math.cos(angle - 0.45), math.sin(angle - 0.45)) * size;
    final p2 = tip - Offset(math.cos(angle + 0.45), math.sin(angle + 0.45)) * size;
    final path = Path()
      ..moveTo(tip.dx, tip.dy)
      ..lineTo(p1.dx, p1.dy)
      ..lineTo(p2.dx, p2.dy)
      ..close();
    canvas.drawPath(path, Paint()..color = AppColors.diagramBackground..style = PaintingStyle.fill);
    canvas.drawPath(path, Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 1.4);
  }

  void _diamond(Canvas canvas, Offset start, Offset toward, Color color, {required bool filled}) {
    final delta = toward - start;
    final length = delta.distance;
    if (length == 0) return;
    final unit = delta / length;
    final perpendicular = Offset(-unit.dy, unit.dx);
    const size = 12.0;
    final p1 = start;
    final p2 = start + unit * size + perpendicular * (size * 0.55);
    final p3 = start + unit * (size * 2);
    final p4 = start + unit * size - perpendicular * (size * 0.55);
    final path = Path()
      ..moveTo(p1.dx, p1.dy)
      ..lineTo(p2.dx, p2.dy)
      ..lineTo(p3.dx, p3.dy)
      ..lineTo(p4.dx, p4.dy)
      ..close();
    if (filled) {
      canvas.drawPath(path, Paint()..color = color..style = PaintingStyle.fill);
    } else {
      canvas.drawPath(path, Paint()..color = AppColors.diagramBackground..style = PaintingStyle.fill);
      canvas.drawPath(path, Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 1.4);
    }
  }

  void _paintSelfLoop(Canvas canvas, Rect rect, UmlRelationship relationship, Paint paint, Color color) {
    final start = Offset(rect.right, rect.center.dy - 18);
    final control1 = Offset(rect.right + 90, rect.center.dy - 80);
    final control2 = Offset(rect.right + 90, rect.center.dy + 80);
    final end = Offset(rect.right, rect.center.dy + 18);

    final path = Path()
      ..moveTo(start.dx, start.dy)
      ..cubicTo(control1.dx, control1.dy, control2.dx, control2.dy, end.dx, end.dy);
    canvas.drawPath(path, paint);

    // El marcador va en el extremo de retorno (destino), como en la web.
    switch (relationship.relationshipType) {
      case 'generalization':
      case 'realization':
        _triangle(canvas, end, control2, color);
        break;
      case 'aggregation':
        _diamond(canvas, end, control2, color, filled: false);
        break;
      case 'composition':
        _diamond(canvas, end, control2, color, filled: true);
        break;
      default:
        break;
    }

    if (relationship.name != null && relationship.name!.isNotEmpty) {
      _label(canvas, relationship.name!, Offset(rect.right + 104, rect.center.dy), color);
    }
  }

  void _label(Canvas canvas, String text, Offset position, Color color) {
    if (text.isEmpty) return;
    final painter = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(
          fontSize: 10,
          color: color,
          backgroundColor: Colors.transparent,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    painter.paint(canvas, position - Offset(painter.width / 2, painter.height / 2));
  }

  @override
  bool shouldRepaint(covariant DiagramEdgePainter oldDelegate) {
    return oldDelegate.elements != elements ||
        oldDelegate.relationships != relationships ||
        oldDelegate.multiplicities != multiplicities ||
        oldDelegate.origin != origin ||
        oldDelegate.selectedRelationshipId != selectedRelationshipId;
  }
}

