import 'package:flutter/material.dart';

import '../../../chat/presentation/widgets/chat_fab.dart';
import '../../../../core/constants/app_constants.dart';

/// Pantalla principal. Por ahora solo expone el acceso al asistente.
class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(AppConstants.appName)),
      body: Center(
        child: Text(
          'Tus diagramas UML, en cualquier lugar.',
          style: Theme.of(context).textTheme.titleMedium,
          textAlign: TextAlign.center,
        ),
      ),
      floatingActionButton: const ChatFab(),
    );
  }
}
