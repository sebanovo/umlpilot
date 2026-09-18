/// Constantes generales de la aplicación.
class AppConstants {
  AppConstants._();

  static const String appName = 'UMLPilot';

  /// Ollama puede tardar en cargar el modelo en memoria la primera vez.
  static const Duration requestTimeout = Duration(minutes: 3);

  static const String assistantSystemPrompt =
      'Eres el asistente de UMLPilot, una herramienta para crear diagramas UML. '
      'Ayudas a los usuarios a modelar clases, casos de uso y diagramas de '
      'secuencia, y a resolver dudas sobre notación UML. Responde de forma '
      'breve, clara y en español.';
}
