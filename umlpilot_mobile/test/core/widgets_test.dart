import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/widgets/app_button.dart';
import 'package:umlpilot_mobile/core/widgets/empty_state.dart';

void main() {
  testWidgets('el estado vacío muestra título y acción', (tester) async {
    var tapped = false;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: EmptyState(
            title: 'No tienes proyectos',
            message: 'Crea tu primer proyecto',
            actionLabel: 'Crear',
            onAction: () => tapped = true,
          ),
        ),
      ),
    );

    expect(find.text('No tienes proyectos'), findsOneWidget);
    expect(find.text('Crear'), findsOneWidget);

    await tester.tap(find.text('Crear'));
    expect(tapped, isTrue);
  });

  testWidgets('el botón muestra el indicador de carga y se deshabilita', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AppButton(label: 'Guardar', loading: true, onPressed: () {}),
        ),
      ),
    );

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNull);
  });
}
