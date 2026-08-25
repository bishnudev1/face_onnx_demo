import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../services/face_native_service.dart';

class FaceTestScreen extends StatefulWidget {
  const FaceTestScreen({super.key});

  @override
  State<FaceTestScreen> createState() => _FaceTestScreenState();
}

class _FaceTestScreenState extends State<FaceTestScreen> {
  final ImagePicker _picker = ImagePicker();

  Uint8List? _imageBytes;

  bool _loading = false;
  bool _initialized = false;

  FaceEmbeddingResult? _result;

  String? _error;

  Future<void> _initialize() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final success = await FaceNativeService.instance.initialize();

      if (!mounted) return;

      setState(() {
        _initialized = success;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _loading = false;
        _error = e.toString();
      });
    }
  }

  Future<void> _pickImage() async {
    try {
      final file = await _picker.pickImage(source: ImageSource.gallery);

      if (file == null) return;

      final bytes = await file.readAsBytes();

      setState(() {
        _imageBytes = bytes;
        _result = null;
        _error = null;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
      });
    }
  }

  Future<void> _generateEmbedding() async {
    if (_imageBytes == null) {
      setState(() {
        _error = 'Please select an image first.';
      });
      return;
    }

    if (!_initialized) {
      setState(() {
        _error = 'Initialize the face engine first.';
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
      _result = null;
    });

    try {
      final result = await FaceNativeService.instance.generateEmbedding(
        _imageBytes!,
      );

      if (!mounted) return;

      setState(() {
        _result = result;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _loading = false;
        _error = e.toString();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final embedding = _result?.embedding;

    return Scaffold(
      appBar: AppBar(title: const Text('Face Engine Test')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ElevatedButton(
              onPressed: _loading ? null : _initialize,
              child: const Text('Initialize Face Engine'),
            ),

            const SizedBox(height: 12),

            Text(
              _initialized ? 'Engine: READY' : 'Engine: NOT INITIALIZED',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                color: _initialized ? Colors.green : Colors.red,
              ),
            ),

            const SizedBox(height: 24),

            ElevatedButton(
              onPressed: _loading ? null : _pickImage,
              child: const Text('Pick Face Image'),
            ),

            const SizedBox(height: 16),

            if (_imageBytes != null)
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.memory(
                  _imageBytes!,
                  height: 300,
                  fit: BoxFit.contain,
                ),
              ),

            const SizedBox(height: 16),

            ElevatedButton(
              onPressed: _loading ? null : _generateEmbedding,
              child: const Text('Generate Face Embedding'),
            ),

            const SizedBox(height: 20),

            if (_loading) const Center(child: CircularProgressIndicator()),

            if (_error != null)
              Container(
                padding: const EdgeInsets.all(12),
                color: Colors.red.shade50,
                child: Text(
                  _error!,
                  style: TextStyle(color: Colors.red.shade900),
                ),
              ),

            if (_result != null) ...[
              const Divider(),

              const SizedBox(height: 16),

              Text(
                _result!.faceDetected
                    ? 'FACE DETECTED ✅'
                    : 'NO FACE DETECTED ❌',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),

              const SizedBox(height: 12),

              Text(
                'Detection confidence: '
                '${_result!.confidence.toStringAsFixed(4)}',
              ),

              const SizedBox(height: 12),

              Text(
                'Embedding dimension: '
                '${embedding?.length ?? 0}',
              ),

              const SizedBox(height: 16),

              if (embedding != null)
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey.shade300),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    embedding
                        .map((value) => value.toStringAsFixed(6))
                        .join(', '),
                    style: const TextStyle(fontSize: 11),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}
