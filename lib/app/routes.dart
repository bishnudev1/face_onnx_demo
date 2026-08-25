import 'package:flutter/material.dart';
import '../screens/dashboard_screen.dart';
import '../screens/registration_screen.dart';
import '../screens/verification_screen.dart';

class AppRoutes {
  static const String dashboard = '/';
  static const String registration = '/registration';
  static const String verification = '/verification';
  static const String faceTest = '/face-test';

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case dashboard:
        return MaterialPageRoute(
          builder: (_) => const DashboardScreen(),
          settings: settings,
        );

      case registration:
        return MaterialPageRoute(
          builder: (_) => const RegistrationScreen(),
          settings: settings,
        );

      case verification:
        return MaterialPageRoute(
          builder: (_) => const VerificationScreen(),
          settings: settings,
        );

      default:
        return MaterialPageRoute(
          builder: (_) => const _UnknownRouteScreen(),
          settings: settings,
        );
    }
  }
}

class _UnknownRouteScreen extends StatelessWidget {
  const _UnknownRouteScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Page Not Found')),
      body: const Center(child: Text('The requested page does not exist.')),
    );
  }
}
