import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:provider/provider.dart';

import 'core/constants/api_constants.dart';
import 'features/auth/presentation/viewmodels/auth_viewmodel.dart';
import 'features/notifications/presentation/viewmodels/notifications_viewmodel.dart';
import 'injection/injection.dart';
import 'routes/app_router.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await dotenv.load();
  } catch (_) {
    // Sin .env la app usa los valores por defecto de ApiConstants.
  }
  await configureDependencies();
  // Restaura la sesión antes de construir el router para que los enlaces
  // directos (por ejemplo /share/:token) conozcan el estado de autenticación.
  await getIt<AuthViewModel>().restore();
  runApp(const UmlPilotApp());
}

class UmlPilotApp extends StatelessWidget {
  const UmlPilotApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthViewModel>.value(value: getIt<AuthViewModel>()),
        ChangeNotifierProvider<NotificationsViewModel>.value(value: getIt<NotificationsViewModel>()),
      ],
      child: MaterialApp.router(
        title: ApiConstants.appName,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.dark,
        routerConfig: getIt<AppRouter>().router,
      ),
    );
  }
}
