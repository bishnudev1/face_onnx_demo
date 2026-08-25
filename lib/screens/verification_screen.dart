import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../services/face_native_service.dart';

class VerificationScreen extends StatefulWidget {
  const VerificationScreen({super.key});

  @override
  State<VerificationScreen> createState() => _VerificationScreenState();
}

class _VerificationScreenState extends State<VerificationScreen> {
  static const String USER_ID = 'test_user';

  final ImagePicker _picker = ImagePicker();
  final FaceNativeService _service = FaceNativeService.instance;

  Uint8List? _capturedImage;
  List<double>? _embedding;
  bool _isProcessing = false;
  String? _error;
  VerificationResult? _result;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Verify Identity'),
        elevation: 0,
        actions: [
          if (_result != null)
            TextButton.icon(
              onPressed: _resetVerification,
              icon: const Icon(Icons.refresh),
              label: const Text('New Scan'),
            ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isProcessing) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Verifying your face...'),
          ],
        ),
      );
    }

    if (_result != null) {
      return _buildResultScreen();
    }

    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Face Verification',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 8),
          Text(
            'Take a photo to verify your identity',
            style: TextStyle(color: Colors.grey[600]),
          ),
          const SizedBox(height: 24),
          Expanded(
            child: Center(
              child: _capturedImage != null
                  ? _buildImagePreview()
                  : _buildCapturePrompt(),
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.red.shade200),
                ),
                child: Text(
                  _error!,
                  style: TextStyle(color: Colors.red.shade900),
                ),
              ),
            ),
          const SizedBox(height: 16),
          if (_capturedImage != null)
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _verifyFace,
                icon: const Icon(Icons.verified),
                label: const Text('Verify Identity'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
            )
          else
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _captureImage,
                icon: const Icon(Icons.camera_alt),
                label: const Text('Take Photo'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildCapturePrompt() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 200,
          height: 200,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.grey.shade200,
          ),
          child: Icon(Icons.person, size: 80, color: Colors.grey.shade400),
        ),
        const SizedBox(height: 24),
        Text(
          'No photo captured yet',
          style: TextStyle(fontSize: 18, color: Colors.grey.shade600),
        ),
        const SizedBox(height: 8),
        Text(
          'Tap the button below to take a photo',
          style: TextStyle(fontSize: 14, color: Colors.grey.shade500),
        ),
      ],
    );
  }

  Widget _buildImagePreview() {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: Image.memory(
          _capturedImage!,
          fit: BoxFit.contain,
          height: 400,
          width: double.infinity,
        ),
      ),
    );
  }

  Widget _buildResultScreen() {
    final isMatch = _result!.isMatch;
    final similarity = _result!.similarity;
    final percentage = (similarity * 100).toStringAsFixed(1);

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isMatch ? Colors.green.shade50 : Colors.red.shade50,
              ),
              child: Icon(
                isMatch ? Icons.check_circle : Icons.cancel,
                size: 80,
                color: isMatch ? Colors.green : Colors.red,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              isMatch ? 'Identity Verified! ✅' : 'Verification Failed ❌',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: isMatch ? Colors.green : Colors.red,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              _result!.message,
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 16, color: Colors.grey[700]),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.blue.shade200),
              ),
              child: Column(
                children: [
                  Text(
                    'Similarity Score',
                    style: TextStyle(fontSize: 14, color: Colors.blue.shade900),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$percentage%',
                    style: const TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Threshold: ${(_result!.threshold * 100).toStringAsFixed(0)}%',
                    style: TextStyle(fontSize: 12, color: Colors.blue.shade700),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton.icon(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.arrow_back),
                  label: const Text('Back'),
                ),
                const SizedBox(width: 12),
                ElevatedButton.icon(
                  onPressed: _resetVerification,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Try Again'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: isMatch ? Colors.green : Colors.blue,
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

  Future<void> _captureImage() async {
    try {
      setState(() {
        _error = null;
      });

      final image = await _picker.pickImage(
        source: ImageSource.camera,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 80,
      );

      if (image == null) return;

      final bytes = await image.readAsBytes();

      if (mounted) {
        setState(() {
          _capturedImage = Uint8List.fromList(bytes);
          _result = null;
          _embedding = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
        });
      }
    }
  }

  Future<void> _verifyFace() async {
    if (_capturedImage == null) return;

    setState(() {
      _isProcessing = true;
      _error = null;
      _result = null;
    });

    try {
      // Generate embedding
      final result = await _service.generateEmbedding(_capturedImage!);

      if (!mounted) return;

      if (!result.faceDetected) {
        setState(() {
          _isProcessing = false;
          _error = 'No face detected. Please try again.';
        });
        return;
      }

      _embedding = result.embedding!;

      // Check if user is registered
      final isRegistered = await _service.isUserRegistered(USER_ID);

      if (!isRegistered) {
        setState(() {
          _isProcessing = false;
          _error = 'User not registered. Please register first.';
        });
        return;
      }

      // Verify face
      final verificationResult = await _service.verifyFace(
        userId: USER_ID,
        embedding: _embedding!,
      );

      if (mounted) {
        setState(() {
          _isProcessing = false;
          _result = verificationResult;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isProcessing = false;
          _error = e.toString();
        });
      }
    }
  }

  void _resetVerification() {
    setState(() {
      _capturedImage = null;
      _embedding = null;
      _result = null;
      _error = null;
    });
  }
}
