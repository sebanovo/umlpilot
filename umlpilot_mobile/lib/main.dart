import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import 'core/constants/app_constants.dart';
import 'features/home/presentation/pages/home_page.dart';
import 'injection/injection.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await _loadEnv();
  await configureDependencies();
  runApp(const UmlPilotApp());
}

/// Carga el `.env`; si no existe, se usan los valores por defecto.
Future<void> _loadEnv() async {
  try {
    await dotenv.load();
  } catch (_) {
    // ApiConstants aplica valores por defecto cuando el .env no está disponible.
  }
}

class UmlPilotApp extends StatelessWidget {
  const UmlPilotApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.dark,
      home: const HomePage(),
    );
  }
}
