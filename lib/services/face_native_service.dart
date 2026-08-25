import 'dart:typed_data';

import 'package:flutter/services.dart';

class FaceNativeService {
  FaceNativeService._();

  static final FaceNativeService instance = FaceNativeService._();

  static const MethodChannel _channel = MethodChannel(
    'com.example.face_onnx_demo/face',
  );

  // ================================================================
  // INITIALIZATION
  // ================================================================

  /// Loads det_500m.onnx and w600k_mbf.onnx
  /// inside the native Android layer.
  Future<bool> initialize() async {
    final result = await _channel.invokeMethod<dynamic>('initialize');

    if (result is Map) {
      return result['success'] == true;
    }

    return false;
  }

  // ================================================================
  // EMBEDDING GENERATION
  // ================================================================

  /// Generates a 512-dimensional face embedding.
  Future<FaceEmbeddingResult> generateEmbedding(Uint8List imageBytes) async {
    final result = await _channel.invokeMethod<dynamic>('generateEmbedding', {
      'image': imageBytes,
    });

    if (result is! Map) {
      throw Exception('Invalid response from native face engine.');
    }

    final faceDetected = result['faceDetected'] == true;

    final confidence = (result['confidence'] as num?)?.toDouble() ?? 0.0;

    if (!faceDetected) {
      return FaceEmbeddingResult(
        faceDetected: false,
        confidence: confidence,
        embedding: null,
      );
    }

    final rawEmbedding = result['embedding'];

    if (rawEmbedding is! List) {
      throw Exception('Native engine returned an invalid embedding.');
    }

    final embedding = rawEmbedding
        .map<double>((value) => (value as num).toDouble())
        .toList();

    return FaceEmbeddingResult(
      faceDetected: true,
      confidence: confidence,
      embedding: embedding,
    );
  }

  // ================================================================
  // REGISTRATION METHODS
  // ================================================================

  /// Register a face for a user.
  /// Returns RegistrationResult with progress information.
  /// Each user needs 3 images for complete registration.
  Future<RegistrationResult> registerFace({
    required String userId,
    required List<double> embedding,
  }) async {
    final result = await _channel.invokeMethod<dynamic>('registerFace', {
      'userId': userId,
      'embedding': embedding,
    });

    if (result is! Map) {
      throw Exception('Invalid registration response.');
    }

    return RegistrationResult(
      success: result['success'] == true,
      message: result['message'] ?? '',
      userId: result['userId'],
      isComplete: result['isComplete'] == true,
    );
  }

  /// Check if a user has completed registration (3 images).
  Future<bool> isUserRegistered(String userId) async {
    final result = await _channel.invokeMethod<dynamic>('isUserRegistered', {
      'userId': userId,
    });

    if (result is! Map) {
      throw Exception('Invalid response.');
    }

    return result['isRegistered'] == true;
  }

  /// Get registration progress for a user (0-3).
  Future<RegistrationProgress> getRegistrationProgress(String userId) async {
    final result = await _channel.invokeMethod<dynamic>(
      'getRegistrationProgress',
      {'userId': userId},
    );

    if (result is! Map) {
      throw Exception('Invalid response.');
    }

    return RegistrationProgress(
      progress: result['progress'] ?? 0,
      required: result['required'] ?? 3,
      isComplete: result['isComplete'] == true,
      userId: userId,
    );
  }

  // ================================================================
  // VERIFICATION METHODS
  // ================================================================

  /// Verify a face against a specific registered user.
  Future<VerificationResult> verifyFace({
    required String userId,
    required List<double> embedding,
  }) async {
    final result = await _channel.invokeMethod<dynamic>('verifyFace', {
      'userId': userId,
      'embedding': embedding,
    });

    if (result is! Map) {
      throw Exception('Invalid verification response.');
    }

    return VerificationResult(
      isMatch: result['isMatch'] == true,
      similarity: (result['similarity'] as num).toDouble(),
      message: result['message'] ?? '',
      threshold: (result['threshold'] as num?)?.toDouble() ?? 0.6,
    );
  }

  /// Verify a face against all registered users (1:N matching).
  Future<VerificationAgainstAllResult> verifyFaceAgainstAll({
    required List<double> embedding,
  }) async {
    final result = await _channel.invokeMethod<dynamic>(
      'verifyFaceAgainstAll',
      {'embedding': embedding},
    );

    if (result is! Map) {
      throw Exception('Invalid verification response.');
    }

    return VerificationAgainstAllResult(
      userId: result['userId'],
      similarity: (result['similarity'] as num?)?.toDouble() ?? 0.0,
      isMatch: result['isMatch'] == true,
    );
  }

  // ================================================================
  // USER MANAGEMENT METHODS
  // ================================================================

  /// Get list of all registered user IDs.
  Future<List<String>> getRegisteredUsers() async {
    final result = await _channel.invokeMethod<dynamic>('getRegisteredUsers');

    if (result is! Map) {
      throw Exception('Invalid response.');
    }

    final users = result['users'];
    if (users is! List) {
      return [];
    }

    return users.map<String>((user) => user.toString()).toList();
  }

  /// Delete a user's registration.
  Future<bool> deleteRegistration(String userId) async {
    final result = await _channel.invokeMethod<dynamic>('deleteRegistration', {
      'userId': userId,
    });

    if (result is! Map) {
      throw Exception('Invalid response.');
    }

    return result['success'] == true;
  }

  /// Clear all registrations.
  Future<bool> clearAllRegistrations() async {
    final result = await _channel.invokeMethod<dynamic>(
      'clearAllRegistrations',
    );

    if (result is! Map) {
      throw Exception('Invalid response.');
    }

    return result['success'] == true;
  }

  // ================================================================
  // EXISTING METHODS (KEPT FOR BACKWARD COMPATIBILITY)
  // ================================================================

  /// Compares the verification embedding against
  /// the three locally stored enrollment embeddings.
  /// (Legacy method - kept for backward compatibility)
  Future<FaceComparisonResult> compareEmbeddings({
    required List<double> query,
    required List<double> front,
    required List<double> left,
    required List<double> right,
  }) async {
    final result = await _channel.invokeMethod<dynamic>('compareEmbeddings', {
      'query': query,
      'front': front,
      'left': left,
      'right': right,
    });

    if (result is! Map) {
      throw Exception('Invalid comparison response.');
    }

    return FaceComparisonResult(
      front: (result['front'] as num).toDouble(),
      left: (result['left'] as num).toDouble(),
      right: (result['right'] as num).toDouble(),
      average: (result['average'] as num).toDouble(),
      best: (result['best'] as num).toDouble(),
    );
  }

  Future<void> dispose() async {
    await _channel.invokeMethod<dynamic>('dispose');
  }
}

// ================================================================
// DATA CLASSES
// ================================================================

class FaceEmbeddingResult {
  final bool faceDetected;
  final double confidence;
  final List<double>? embedding;

  const FaceEmbeddingResult({
    required this.faceDetected,
    required this.confidence,
    required this.embedding,
  });
}

class FaceComparisonResult {
  final double front;
  final double left;
  final double right;
  final double average;
  final double best;

  const FaceComparisonResult({
    required this.front,
    required this.left,
    required this.right,
    required this.average,
    required this.best,
  });
}

class RegistrationResult {
  final bool success;
  final String message;
  final String? userId;
  final bool isComplete;

  const RegistrationResult({
    required this.success,
    required this.message,
    this.userId,
    this.isComplete = false,
  });
}

class RegistrationProgress {
  final int progress;
  final int required;
  final bool isComplete;
  final String userId;

  const RegistrationProgress({
    required this.progress,
    required this.required,
    required this.isComplete,
    required this.userId,
  });
}

class VerificationResult {
  final bool isMatch;
  final double similarity;
  final String message;
  final double threshold;

  const VerificationResult({
    required this.isMatch,
    required this.similarity,
    required this.message,
    required this.threshold,
  });
}

class VerificationAgainstAllResult {
  final String? userId;
  final double similarity;
  final bool isMatch;

  const VerificationAgainstAllResult({
    this.userId,
    required this.similarity,
    required this.isMatch,
  });
}
