import 'package:flutter/material.dart';
import 'routes.dart';

class FaceVerificationApp extends StatelessWidget {
  const FaceVerificationApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Face Verification',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        primarySwatch: Colors.blue,
      ),
      initialRoute: AppRoutes.dashboard, // Changed to dashboard
      onGenerateRoute: AppRoutes.onGenerateRoute,
    );
  }
}
