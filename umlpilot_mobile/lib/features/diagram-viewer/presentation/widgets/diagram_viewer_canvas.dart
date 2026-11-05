import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../theme/app_colors.dart';
import '../../../diagram-editor/domain/entities/uml_models.dart';
import 'diagram_edge_painter.dart';
import 'diagram_geometry.dart';
import 'diagram_node_card.dart';

class DiagramViewerCanvas extends StatefulWidget {
  const DiagramViewerCanvas({
    super.key,
    required this.elements,
    required this.relationships,
    required this.attributes,
    required this.methods,
    required this.literals,
    required this.multiplicities,
    this.selectedElementId,
    this.selectedRelationshipId,
    this.onSelectElement,
    this.onSelectRelationship,
    this.onBackgroundTap,
  });

  final List<UmlElement> elements;
  final List<UmlRelationship> relationships;
  final Map<String, List<UmlAttribute>> attributes;
  final Map<String, List<UmlMethod>> methods;
  final Map<String, List<UmlLiteral>> literals;
  final Map<String, List<UmlMultiplicity>> multiplicities;
  final String? selectedElementId;
  final String? selectedRelationshipId;
  final ValueChanged<String?>? onSelectElement;
  final ValueChanged<String?>? onSelectRelationship;
  final VoidCallback? onBackgroundTap;

  @override
  State<DiagramViewerCanvas> createState() => DiagramViewerCanvasState();
}

class _CanvasLayout {
  const _CanvasLayout(this.origin, this.size, this.content);

  final Offset origin;
  final Size size;
  final Rect content;
}

class DiagramViewerCanvasState extends State<DiagramViewerCanvas> {
  final TransformationController _controller = TransformationController();
  Size _viewport = Size.zero;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Size _elementSize(UmlElement element) {
    return nodeSizeFor(
      element,
      (widget.attributes[element.id] ?? const []).length,
      (widget.methods[element.id] ?? const []).length,
      (widget.literals[element.id] ?? const []).length,
    );
  }

  Rect _elementRect(UmlElement element, Offset origin) {
    final size = _elementSize(element);
    return Rect.fromLTWH(
      element.positionX.toDouble() - origin.dx,
      element.positionY.toDouble() - origin.dy,
      size.width,
      size.height,
    );
  }

  _CanvasLayout _layout() {
    if (widget.elements.isEmpty) {
      return const _CanvasLayout(Offset.zero, Size(1200, 1200), Rect.fromLTWH(180, 180, 0, 0));
    }

    var minX = double.infinity;
    var minY = double.infinity;
    var maxX = -double.infinity;
    var maxY = -double.infinity;
    for (final element in widget.elements) {
      final rect = _elementRect(element, Offset.zero);
      minX = math.min(minX, rect.left);
      minY = math.min(minY, rect.top);
      maxX = math.max(maxX, rect.right);
      maxY = math.max(maxY, rect.bottom);
    }

    const padding = 260.0;
    final origin = Offset(minX - padding, minY - padding);
    final width = math.max(maxX - minX + padding * 2, 1200.0);
    final height = math.max(maxY - minY + padding * 2, 900.0);
    final content = Rect.fromLTWH(padding, padding, maxX - minX, maxY - minY);
    return _CanvasLayout(origin, Size(width, height), content);
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

  double _distanceToSegment(Offset point, Offset a, Offset b) {
    final delta = b - a;
    final lengthSquared = delta.dx * delta.dx + delta.dy * delta.dy;
    if (lengthSquared == 0) return (point - a).distance;
    var t = ((point.dx - a.dx) * delta.dx + (point.dy - a.dy) * delta.dy) / lengthSquared;
    t = t.clamp(0.0, 1.0);
    final projection = a + delta * t;
    return (point - projection).distance;
  }

  String? _relationshipAt(Offset point) {
    final layout = _layout();
    final byId = {for (final element in widget.elements) element.id: element};
    String? best;
    var bestDistance = 18.0;
    for (final relationship in widget.relationships) {
      final source = byId[relationship.sourceElementId];
      final target = byId[relationship.targetElementId];
      if (source == null || target == null || source.id == target.id) continue;
      final sourceRect = _elementRect(source, layout.origin);
      final targetRect = _elementRect(target, layout.origin);
      final start = _boundaryPoint(sourceRect, targetRect.center);
      final end = _boundaryPoint(targetRect, sourceRect.center);
      final distance = _distanceToSegment(point, start, end);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = relationship.id;
      }
    }
    return best;
  }

  void fitToContent() {
    final layout = _layout();
    if (widget.elements.isEmpty || _viewport == Size.zero) return;
    final content = layout.content.inflate(60);
    final scale = math.min(_viewport.width / content.width, _viewport.height / content.height).clamp(0.2, 1.5);
    final dx = (_viewport.width - content.width * scale) / 2 - content.left * scale;
    final dy = (_viewport.height - content.height * scale) / 2 - content.top * scale;
    _controller.value = Matrix4.identity()
      ..setTranslationRaw(dx, dy, 0)
      ..multiply(Matrix4.diagonal3Values(scale, scale, 1));
  }

  @override
  Widget build(BuildContext context) {
    final layout = _layout();

    return LayoutBuilder(
      builder: (context, constraints) {
        _viewport = constraints.biggest;
        return InteractiveViewer(
          transformationController: _controller,
          constrained: false,
          boundaryMargin: const EdgeInsets.all(400),
          minScale: 0.15,
          maxScale: 2.5,
          child: SizedBox(
            width: layout.size.width,
            height: layout.size.height,
            child: GridPaper(
              color: AppColors.border.withValues(alpha: 0.55),
              interval: 60,
              divisions: 4,
              subdivisions: 1,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTapUp: (details) {
                        final relationshipId = _relationshipAt(details.localPosition);
                        if (relationshipId != null) {
                          widget.onSelectRelationship?.call(relationshipId);
                        } else {
                          widget.onBackgroundTap?.call();
                        }
                      },
                      child: CustomPaint(
                        painter: DiagramEdgePainter(
                          elements: widget.elements,
                          relationships: widget.relationships,
                          attributes: widget.attributes,
                          methods: widget.methods,
                          literals: widget.literals,
                          multiplicities: widget.multiplicities,
                          origin: layout.origin,
                          selectedRelationshipId: widget.selectedRelationshipId,
                        ),
                      ),
                    ),
                  ),
                  for (final element in widget.elements)
                    Positioned(
                      left: element.positionX.toDouble() - layout.origin.dx,
                      top: element.positionY.toDouble() - layout.origin.dy,
                      width: _elementSize(element).width,
                      height: _elementSize(element).height,
                      child: GestureDetector(
                        onTap: () => widget.onSelectElement?.call(element.id),
                        child: DiagramNodeCard(
                          element: element,
                          attributes: widget.attributes[element.id] ?? const [],
                          methods: widget.methods[element.id] ?? const [],
                          literals: widget.literals[element.id] ?? const [],
                          selected: widget.selectedElementId == element.id,
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
