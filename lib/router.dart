import 'package:go_router/go_router.dart';

import 'screens/document_screen.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';

final router = GoRouter(
  initialLocation: '/login',
  routes: [
    GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
    GoRoute(path: '/register', builder: (context, state) => const RegisterScreen()),
    GoRoute(path: '/', builder: (context, state) => const DocumentScreen()),
  ],
);
