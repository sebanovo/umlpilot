import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/features/diagram-editor/domain/entities/uml_models.dart';
import 'package:umlpilot_mobile/features/diagram-viewer/presentation/widgets/diagram_viewer_canvas.dart';
import 'package:umlpilot_mobile/theme/app_theme.dart';

void main() {
  testWidgets('renderiza clases, interfaces, enumeraciones, notas y todos los tipos de relación', (tester) async {
    final elements = [
      const UmlElement(
        id: 'c1',
        diagramId: 'd1',
        name: 'Cliente',
        visibility: 'public',
        elementType: 'class',
        positionX: 40,
        positionY: 40,
        width: 200,
        height: 100,
      ),
      const UmlElement(
        id: 'i1',
        diagramId: 'd1',
        name: 'Repositorio',
        visibility: 'public',
        elementType: 'interface',
        positionX: 360,
        positionY: 40,
        width: 200,
        height: 100,
      ),
      const UmlElement(
        id: 'e1',
        diagramId: 'd1',
        name: 'Estado',
        visibility: 'public',
        elementType: 'enumeration',
        positionX: 40,
        positionY: 300,
        width: 200,
        height: 100,
      ),
      const UmlElement(
        id: 'n1',
        diagramId: 'd1',
        name: 'Nota importante',
        visibility: 'public',
        elementType: 'note',
        positionX: -140,
        positionY: 220,
        width: 220,
        height: 60,
      ),
    ];

    final attributes = {
      'c1': [
        const UmlAttribute(
          id: 'a1',
          elementId: 'c1',
          name: 'nombre',
          dataType: 'String',
          visibility: 'private',
          orderIndex: 0,
        ),
      ],
    };
    final methods = {
      'c1': [
        const UmlMethod(
          id: 'm1',
          elementId: 'c1',
          name: 'guardar',
          returnType: 'void',
          visibility: 'public',
          orderIndex: 0,
          parameters: [],
        ),
      ],
    };
    final literals = {
      'e1': [
        const UmlLiteral(id: 'l1', elementId: 'e1', name: 'ACTIVO', orderIndex: 0),
      ],
    };

    final relationships = [
      const UmlRelationship(
        id: 'r1',
        diagramId: 'd1',
        sourceElementId: 'c1',
        targetElementId: 'i1',
        relationshipType: 'association',
        direction: 'bidirectional',
        name: 'usa',
      ),
      const UmlRelationship(
        id: 'r2',
        diagramId: 'd1',
        sourceElementId: 'c1',
        targetElementId: 'e1',
        relationshipType: 'association_class',
        direction: 'bidirectional',
        associationClassElementId: 'i1',
      ),
      const UmlRelationship(
        id: 'r3',
        diagramId: 'd1',
        sourceElementId: 'c1',
        targetElementId: 'c1',
        relationshipType: 'generalization',
        direction: 'unidirectional',
      ),
      const UmlRelationship(
        id: 'r4',
        diagramId: 'd1',
        sourceElementId: 'i1',
        targetElementId: 'e1',
        relationshipType: 'composition',
        direction: 'unidirectional',
      ),
      const UmlRelationship(
        id: 'r5',
        diagramId: 'd1',
        sourceElementId: 'e1',
        targetElementId: 'i1',
        relationshipType: 'aggregation',
        direction: 'unidirectional',
      ),
      const UmlRelationship(
        id: 'r6',
        diagramId: 'd1',
        sourceElementId: 'i1',
        targetElementId: 'c1',
        relationshipType: 'realization',
        direction: 'unidirectional',
      ),
    ];

    final multiplicities = {
      'r1': [
        const UmlMultiplicity(
          id: 'mm1',
          relationshipId: 'r1',
          end: 'source',
          min: 1,
          max: 1,
          isOrdered: false,
          isUnique: false,
        ),
        const UmlMultiplicity(
          id: 'mm2',
          relationshipId: 'r1',
          end: 'target',
          min: 0,
          max: -1,
          isOrdered: false,
          isUnique: false,
        ),
      ],
    };

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.dark,
        home: Scaffold(
          body: SizedBox(
            width: 800,
            height: 600,
            child: DiagramViewerCanvas(
              elements: elements,
              relationships: relationships,
              attributes: attributes,
              methods: methods,
              literals: literals,
              multiplicities: multiplicities,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Cliente'), findsOneWidget);
    expect(find.text('Repositorio'), findsOneWidget);
    expect(find.text('Estado'), findsOneWidget);
    expect(find.text('Nota importante'), findsOneWidget);
    expect(find.textContaining('nombre'), findsOneWidget);
    expect(find.textContaining('guardar'), findsOneWidget);
    expect(find.text('ACTIVO'), findsOneWidget);
  });
}
