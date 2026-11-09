class AppConstants {
  AppConstants._();

  static const Duration connectTimeout = Duration(seconds: 15);
  static const Duration receiveTimeout = Duration(minutes: 3);
  static const Duration notificationsPollInterval = Duration(seconds: 30);

  static const double gridSize = 15;
  static const double canvasExtent = 4000;

  static const int maxXmiBytes = 10 * 1024 * 1024;
  static const int userSearchMinChars = 2;
  static const int userSearchDebounceMs = 300;

  static const String diagramEditorRoute = '/projects';
}
