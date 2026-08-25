import 'dart:math' as math;
import 'dart:typed_data';

import '../data/local/face_storage.dart';
import '../data/models/face_embedding.dart';
import '../data/models/verification_result.dart';
import '../native/face_engine_channel.dart';

class FaceVerificationService {
  final FaceEngineChannel _faceEngine;
  final FaceStorage _storage;

  FaceVerificationService({FaceEngineChannel? faceEngine, FaceStorage? storage})
    : _faceEngine = faceEngine ?? FaceEngineChannel(),
      _storage = storage ?? FaceStorage();

  /// Initializes the native face-recognition engine.
  Future<void> initialize() async {
    await _faceEngine.initialize();
  }

  /// Generates an embedding for one image.
  Future<FaceEmbedding> generateEmbedding(Uint8List imageBytes) async {
    return _faceEngine.generateEmbedding(imageBytes);
  }

  /// Generates and stores the three enrollment embeddings.
  Future<void> enroll({
    required Uint8List frontImage,
    required Uint8List leftImage,
    required Uint8List rightImage,
  }) async {
    final frontEmbedding = await _faceEngine.generateEmbedding(frontImage);

    final leftEmbedding = await _faceEngine.generateEmbedding(leftImage);

    final rightEmbedding = await _faceEngine.generateEmbedding(rightImage);

    await _storage.saveEnrollment(
      front: frontEmbedding,
      left: leftEmbedding,
      right: rightEmbedding,
    );
  }

  /// Generates an embedding from the verification selfie
  /// and compares it against all three stored embeddings.
  Future<VerificationResult> verify(Uint8List selfieImage) async {
    final front = await _storage.getFront();
    final left = await _storage.getLeft();
    final right = await _storage.getRight();

    if (front == null || left == null || right == null) {
      throw StateError(
        'No complete face enrollment found. '
        'Please enroll front, left and right faces first.',
      );
    }

    final query = await _faceEngine.generateEmbedding(selfieImage);

    final frontScore = cosineSimilarity(query, front);

    final leftScore = cosineSimilarity(query, left);

    final rightScore = cosineSimilarity(query, right);

    return VerificationResult.fromScores(
      frontScore: frontScore,
      leftScore: leftScore,
      rightScore: rightScore,
      // Temporary value for experimentation.
      // We will determine the real threshold from testing.
      threshold: 0.50,
    );
  }

  Future<bool> hasEnrollment() {
    return _storage.hasEnrollment();
  }

  Future<void> clearEnrollment() {
    return _storage.clearEnrollment();
  }

  Future<void> dispose() async {
    await _faceEngine.dispose();
  }

  /// Cosine similarity between two face embeddings.
  ///
  /// Because the InsightFace embeddings are normalized by the
  /// native recognition pipeline, this should normally be very
  /// close to a dot product. We still calculate the full cosine
  /// similarity here for safety.
  static double cosineSimilarity(FaceEmbedding a, FaceEmbedding b) {
    if (!a.isValid || !b.isValid) {
      throw ArgumentError(
        'Both embeddings must contain exactly '
        '${FaceEmbedding.dimension} values.',
      );
    }

    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < FaceEmbedding.dimension; i++) {
      final valueA = a.values[i];
      final valueB = b.values[i];

      dot += valueA * valueB;
      normA += valueA * valueA;
      normB += valueB * valueB;
    }

    final denominator = math.sqrt(normA) * math.sqrt(normB);

    if (denominator == 0.0) {
      return 0.0;
    }

    return dot / denominator;
  }
}
