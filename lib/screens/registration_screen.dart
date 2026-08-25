import 'dart:typed_data';
import 'package:face_onnx_demo/screens/verification_screen.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../services/face_native_service.dart';

class RegistrationScreen extends StatefulWidget {
  const RegistrationScreen({super.key});

  @override
  State<RegistrationScreen> createState() => _RegistrationScreenState();
}

class _RegistrationScreenState extends State<RegistrationScreen> {
  static const String USER_ID = 'test_user';
  static const int REQUIRED_IMAGES = 3;

  final ImagePicker _picker = ImagePicker();
  final FaceNativeService _service = FaceNativeService.instance;

  List<Uint8List?> _capturedImages = List.filled(REQUIRED_IMAGES, null);
  List<List<double>?> _embeddings = List.filled(REQUIRED_IMAGES, null);
  List<bool> _isProcessing = List.filled(REQUIRED_IMAGES, false);
  List<String?> _errors = List.filled(REQUIRED_IMAGES, null);

  bool _isComplete = false;
  bool _isRegistering = false;
  String? _registrationMessage;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Register Face'),
        elevation: 0,
        actions: [
          if (_isComplete)
            TextButton.icon(
              onPressed: _resetRegistration,
              icon: const Icon(Icons.refresh),
              label: const Text('Reset'),
            ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isRegistering) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Registering your face...'),
          ],
        ),
      );
    }

    if (_isComplete) {
      return _buildSuccessScreen();
    }

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildProgressIndicator(),
          const SizedBox(height: 24),
          Text(
            'Capture ${REQUIRED_IMAGES} face images',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 8),
          Text(
            'Take photos from different angles for better accuracy',
            style: TextStyle(color: Colors.grey[600]),
          ),
          const SizedBox(height: 24),
          Expanded(
            child: GridView.builder(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 0.8,
              ),
              itemCount: REQUIRED_IMAGES,
              itemBuilder: (context, index) {
                return _buildImageSlot(index);
              },
            ),
          ),
          if (_registrationMessage != null)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange.shade200),
                ),
                child: Text(
                  _registrationMessage!,
                  style: TextStyle(color: Colors.orange.shade900),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildProgressIndicator() {
    final captured = _capturedImages.where((img) => img != null).length;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.blue.shade50,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Progress',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: Colors.blue.shade900,
                ),
              ),
              Text(
                '$captured / $REQUIRED_IMAGES',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: Colors.blue.shade900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: captured / REQUIRED_IMAGES,
            backgroundColor: Colors.blue.shade100,
            valueColor: AlwaysStoppedAnimation<Color>(
              captured == REQUIRED_IMAGES ? Colors.green : Colors.blue,
            ),
            minHeight: 8,
            borderRadius: BorderRadius.circular(4),
          ),
        ],
      ),
    );
  }

  Widget _buildImageSlot(int index) {
    final image = _capturedImages[index];
    final isProcessing = _isProcessing[index];
    final error = _errors[index];

    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: image != null ? Colors.green : Colors.grey.shade300,
          width: image != null ? 2 : 1,
        ),
      ),
      child: InkWell(
        onTap: isProcessing ? null : () => _captureImage(index),
        borderRadius: BorderRadius.circular(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (image != null)
              Expanded(
                child: ClipRRect(
                  borderRadius: const BorderRadius.vertical(
                    top: Radius.circular(11),
                  ),
                  child: Image.memory(
                    image,
                    fit: BoxFit.cover,
                    width: double.infinity,
                  ),
                ),
              )
            else if (isProcessing)
              const CircularProgressIndicator()
            else if (error != null)
              Icon(Icons.error_outline, color: Colors.red, size: 32)
            else
              Icon(
                Icons.add_photo_alternate,
                size: 48,
                color: Colors.grey.shade400,
              ),
            const SizedBox(height: 8),
            Text(
              image != null ? 'Image ${index + 1} ✓' : 'Capture ${index + 1}',
              style: TextStyle(
                fontSize: 12,
                fontWeight: image != null ? FontWeight.bold : FontWeight.normal,
                color: image != null ? Colors.green : Colors.grey.shade600,
              ),
            ),
            if (error != null)
              Padding(
                padding: const EdgeInsets.all(4.0),
                child: Text(
                  error,
                  style: const TextStyle(fontSize: 10, color: Colors.red),
                  textAlign: TextAlign.center,
                ),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Widget _buildSuccessScreen() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.check_circle, size: 80, color: Colors.green),
            const SizedBox(height: 24),
            const Text(
              'Registration Complete! 🎉',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              'Your face has been registered successfully.',
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              'You can now verify your identity.',
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 32),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton.icon(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.arrow_back),
                  label: const Text('Back to Dashboard'),
                ),
                const SizedBox(width: 12),
                ElevatedButton.icon(
                  onPressed: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const VerificationScreen(),
                      ),
                    );
                  },
                  icon: const Icon(Icons.verified),
                  label: const Text('Verify Now'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _captureImage(int index) async {
    try {
      setState(() {
        _isProcessing[index] = true;
        _errors[index] = null;
      });

      final image = await _picker.pickImage(
        source: ImageSource.camera,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 80,
      );

      if (image == null) {
        setState(() {
          _isProcessing[index] = false;
        });
        return;
      }

      final bytes = await image.readAsBytes();
      final imageBytes = Uint8List.fromList(bytes);

      // Generate embedding
      final result = await _service.generateEmbedding(imageBytes);

      if (!mounted) return;

      if (!result.faceDetected) {
        setState(() {
          _capturedImages[index] = null;
          _embeddings[index] = null;
          _errors[index] = 'No face detected. Try again.';
          _isProcessing[index] = false;
        });
        return;
      }

      setState(() {
        _capturedImages[index] = imageBytes;
        _embeddings[index] = result.embedding!;
        _isProcessing[index] = false;
      });

      // Check if all images are captured
      final allCaptured = _capturedImages.every((img) => img != null);

      if (allCaptured) {
        _registerFaces();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isProcessing[index] = false;
          _errors[index] = e.toString();
        });
      }
    }
  }

  Future<void> _registerFaces() async {
    setState(() {
      _isRegistering = true;
      _registrationMessage = null;
    });

    try {
      // Use the first embedding for registration (or average all three)
      // For simplicity, we'll register with the first embedding
      // The user can add all 3 by calling registerFace multiple times
      for (int i = 0; i < REQUIRED_IMAGES; i++) {
        final embedding = _embeddings[i];
        if (embedding != null) {
          final result = await _service.registerFace(
            userId: USER_ID,
            embedding: embedding,
          );

          if (!mounted) return;

          if (i == REQUIRED_IMAGES - 1 || result.isComplete) {
            setState(() {
              _isRegistering = false;
              _isComplete = true;
              _registrationMessage = 'Registration successful!';
            });
            return;
          }
        }
      }

      // Fallback: if registration didn't complete
      setState(() {
        _isRegistering = false;
        _registrationMessage = 'Registration incomplete. Please try again.';
      });
    } catch (e) {
      setState(() {
        _isRegistering = false;
        _registrationMessage = 'Error: ${e.toString()}';
      });
    }
  }

  void _resetRegistration() {
    setState(() {
      _capturedImages = List.filled(REQUIRED_IMAGES, null);
      _embeddings = List.filled(REQUIRED_IMAGES, null);
      _isProcessing = List.filled(REQUIRED_IMAGES, false);
      _errors = List.filled(REQUIRED_IMAGES, null);
      _isComplete = false;
      _registrationMessage = null;
    });
  }
}
