import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../core/session/session_controller.dart';
import '../features/auth/presentation/pages/login_page.dart';
import '../features/auth/presentation/pages/register_page.dart';
import '../features/auth/presentation/pages/splash_page.dart';
import '../features/chat/presentation/pages/chat_page.dart';
import '../features/chat/presentation/viewmodels/chat_viewmodel.dart';
import '../features/diagram-editor/presentation/viewmodels/diagram_viewer_viewmodel.dart';
import '../features/diagram-viewer/presentation/pages/diagram_viewer_page.dart';
import '../features/projects/presentation/pages/project_detail_page.dart';
import '../features/projects/presentation/pages/projects_page.dart';
import '../features/projects/presentation/viewmodels/project_detail_viewmodel.dart';
import '../features/projects/presentation/viewmodels/projects_viewmodel.dart';
import '../features/sharing/presentation/viewmodels/public_diagram_viewmodel.dart';
import '../injection/injection.dart';
import '../pages/public_diagram_page.dart';

class AppRouter {
  AppRouter(this._session);

  final SessionController _session;

  late final GoRouter router = GoRouter(
    initialLocation: '/splash',
    refreshListenable: _session,
    redirect: _redirect,
    routes: [
      GoRoute(path: '/splash', builder: (_, _) => const SplashPage()),
      GoRoute(path: '/login', builder: (_, _) => const LoginPage()),
      GoRoute(path: '/register', builder: (_, _) => const RegisterPage()),
      GoRoute(path: '/', redirect: (_, _) => '/projects'),
      GoRoute(
        path: '/projects',
        builder: (_, _) => ChangeNotifierProvider<ProjectsViewModel>(
          create: (_) => getIt<ProjectsViewModel>(),
          child: const ProjectsPage(),
        ),
      ),
      GoRoute(
        path: '/projects/:projectId',
        builder: (_, _) => ChangeNotifierProvider<ProjectDetailViewModel>(
          create: (_) => getIt<ProjectDetailViewModel>(),
          child: const ProjectDetailPage(),
        ),
      ),
      GoRoute(
        path: '/projects/:projectId/chat',
        builder: (_, _) => ChangeNotifierProvider<ChatViewModel>(
          create: (_) => getIt<ChatViewModel>(),
          child: const ChatPage(),
        ),
      ),
      GoRoute(
        path: '/projects/:projectId/diagrams/:diagramId',
        builder: (_, state) => ChangeNotifierProvider<DiagramViewerViewModel>(
          create: (_) => getIt<DiagramViewerViewModel>(),
          child: DiagramViewerPage(
            projectId: state.pathParameters['projectId']!,
            diagramId: state.pathParameters['diagramId']!,
          ),
        ),
      ),
      GoRoute(
        path: '/share/:token',
        builder: (_, state) => ChangeNotifierProvider<PublicDiagramViewModel>(
          create: (_) => getIt<PublicDiagramViewModel>(),
          child: PublicDiagramPage(token: state.pathParameters['token']!),
        ),
      ),
    ],
  );

  String? _redirect(BuildContext context, GoRouterState state) {
    final location = state.matchedLocation;
    final isPublic = location == '/splash' ||
        location == '/login' ||
        location == '/register' ||
        location.startsWith('/share/');

    if (!_session.isRestored) {
      return location == '/splash' ? null : '/splash';
    }

    final authenticated = _session.isAuthenticated;
    if (!authenticated && !isPublic) {
      final from = Uri.encodeComponent(state.uri.toString());
      return '/login?from=$from';
    }

    if (authenticated && (location == '/login' || location == '/register' || location == '/splash')) {
      final from = state.uri.queryParameters['from'];
      return (from != null && from.isNotEmpty) ? from : '/projects';
    }

    return null;
  }
}
