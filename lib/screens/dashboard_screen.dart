import 'package:flutter/material.dart';
import '../services/face_native_service.dart';
import 'registration_screen.dart';
import 'verification_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool _isInitialized = false;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _initializeEngine();
  }

  Future<void> _initializeEngine() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final success = await FaceNativeService.instance.initialize();

      if (mounted) {
        setState(() {
          _isInitialized = success;
          _isLoading = false;
          if (!success) {
            _error = 'Failed to initialize face engine';
          }
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = e.toString();
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;
    final isSmallScreen = screenSize.width < 360;
    final isMediumScreen = screenSize.width < 600;
    final isLargeScreen = screenSize.width >= 900;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Face Verification'),
        elevation: 0,
        actions: [
          if (_isInitialized)
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _initializeEngine,
              tooltip: 'Re-initialize',
            ),
        ],
      ),
      body: _buildBody(
        context,
        isSmallScreen: isSmallScreen,
        isMediumScreen: isMediumScreen,
        isLargeScreen: isLargeScreen,
      ),
    );
  }

  Widget _buildBody(
    BuildContext context, {
    required bool isSmallScreen,
    required bool isMediumScreen,
    required bool isLargeScreen,
  }) {
    if (_isLoading) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Initializing Face Engine...'),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: EdgeInsets.all(isSmallScreen ? 16.0 : 24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Text('Error', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 8),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _initializeEngine,
                icon: const Icon(Icons.refresh),
                label: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    if (!_isInitialized) {
      return Center(
        child: Padding(
          padding: EdgeInsets.all(isSmallScreen ? 16.0 : 24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.face_4, size: 80, color: Colors.grey),
              const SizedBox(height: 16),
              const Text(
                'Face Engine Not Ready',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                'Please check your device and try again.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _initializeEngine,
                icon: const Icon(Icons.refresh),
                label: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    return Padding(
      padding: EdgeInsets.all(isSmallScreen ? 16.0 : 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isSmallScreen) const SizedBox(height: 16),
          Text(
            'Welcome to Face Verification',
            style: isSmallScreen
                ? Theme.of(context).textTheme.titleLarge
                : Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 8),
          Text(
            'Register your face or verify your identity',
            style:
                (isSmallScreen
                        ? Theme.of(context).textTheme.bodyMedium
                        : Theme.of(context).textTheme.bodyLarge)
                    ?.copyWith(color: Colors.grey[600]),
          ),
          SizedBox(height: isSmallScreen ? 24 : 40),
          Expanded(
            child: isLargeScreen
                ? _buildLargeScreenGrid(context, isSmallScreen: isSmallScreen)
                : _buildResponsiveGrid(
                    context,
                    isSmallScreen: isSmallScreen,
                    isMediumScreen: isMediumScreen,
                  ),
          ),
          SizedBox(height: isSmallScreen ? 8 : 16),
          Text(
            'Status: Ready',
            style: TextStyle(
              color: Colors.green,
              fontSize: isSmallScreen ? 10 : 12,
              fontWeight: FontWeight.w500,
            ),
          ),
          SizedBox(height: isSmallScreen ? 4 : 8),
        ],
      ),
    );
  }

  Widget _buildResponsiveGrid(
    BuildContext context, {
    required bool isSmallScreen,
    required bool isMediumScreen,
  }) {
    final crossAxisCount = isSmallScreen ? 1 : 2;
    final aspectRatio = isSmallScreen
        ? 1.5
        : 0.9; // Increased aspect ratio for small screens

    return GridView.builder(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: crossAxisCount,
        crossAxisSpacing: isSmallScreen ? 12 : 16,
        mainAxisSpacing: isSmallScreen ? 12 : 16,
        childAspectRatio: aspectRatio,
      ),
      itemCount: 2,
      itemBuilder: (context, index) {
        if (index == 0) {
          return _buildDashboardCard(
            context,
            icon: Icons.app_registration,
            title: 'Register',
            subtitle: 'Register your face with 3 images',
            color: Colors.blue,
            isSmallScreen: isSmallScreen,
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const RegistrationScreen()),
              );
            },
          );
        } else {
          return _buildDashboardCard(
            context,
            icon: Icons.verified,
            title: 'Verify',
            subtitle: 'Verify your identity',
            color: Colors.green,
            isSmallScreen: isSmallScreen,
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const VerificationScreen()),
              );
            },
          );
        }
      },
    );
  }

  Widget _buildLargeScreenGrid(
    BuildContext context, {
    required bool isSmallScreen,
  }) {
    return Row(
      children: [
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: _buildDashboardCard(
              context,
              icon: Icons.app_registration,
              title: 'Register',
              subtitle: 'Register your face with 3 images',
              color: Colors.blue,
              isSmallScreen: isSmallScreen,
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const RegistrationScreen()),
                );
              },
            ),
          ),
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 16.0),
            child: _buildDashboardCard(
              context,
              icon: Icons.verified,
              title: 'Verify',
              subtitle: 'Verify your identity',
              color: Colors.green,
              isSmallScreen: isSmallScreen,
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const VerificationScreen()),
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDashboardCard(
    BuildContext context, {
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
    required bool isSmallScreen,
  }) {
    final iconSize = isSmallScreen ? 32.0 : 48.0;
    final padding = isSmallScreen ? 12.0 : 20.0;
    final titleSize = isSmallScreen ? 16.0 : 20.0;
    final subtitleSize = isSmallScreen ? 11.0 : 12.0;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(isSmallScreen ? 12 : 16),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(isSmallScreen ? 12 : 16),
        child: Padding(
          padding: EdgeInsets.all(padding),
          child: LayoutBuilder(
            builder: (context, constraints) {
              return Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                    padding: EdgeInsets.all(isSmallScreen ? 10 : 16),
                    decoration: BoxDecoration(
                      color: color.withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(icon, size: iconSize, color: color),
                  ),
                  SizedBox(height: isSmallScreen ? 8 : 16),
                  Flexible(
                    child: Text(
                      title,
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  SizedBox(height: isSmallScreen ? 4 : 8),
                  Flexible(
                    child: Text(
                      subtitle,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: subtitleSize,
                        color: Colors.grey[600],
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
