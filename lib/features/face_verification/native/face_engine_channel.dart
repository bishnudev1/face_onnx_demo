import 'dart:typed_data';

import 'package:flutter/services.dart';

import '../data/models/face_embedding.dart';

class FaceEngineChannel {
  static const MethodChannel _channel = MethodChannel(
    'com.example.face_onnx_demo/face',
  );

  Future<void> initialize() async {
    await _channel.invokeMethod<void>('initialize');
  }

  Future<FaceEmbedding> generateEmbedding(Uint8List imageBytes) async {
    final result = await _channel.invokeMethod<List<dynamic>>(
      'generateEmbedding',
      {'image': imageBytes},
    );

    if (result == null) {
      throw StateError('Native face engine returned no embedding.');
    }

    return FaceEmbedding.fromJson(result);
  }

  Future<void> dispose() async {
    await _channel.invokeMethod<void>('dispose');
  }
}
