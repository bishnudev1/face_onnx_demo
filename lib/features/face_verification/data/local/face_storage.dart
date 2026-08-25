import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/face_embedding.dart';

class FaceStorage {
  static const String _frontKey = 'face_embedding_front';
  static const String _leftKey = 'face_embedding_left';
  static const String _rightKey = 'face_embedding_right';

  Future<void> saveEnrollment({
    required FaceEmbedding front,
    required FaceEmbedding left,
    required FaceEmbedding right,
  }) async {
    if (!front.isValid || !left.isValid || !right.isValid) {
      throw ArgumentError(
        'All face embeddings must contain exactly '
        '${FaceEmbedding.dimension} values.',
      );
    }

    final prefs = await SharedPreferences.getInstance();

    await prefs.setString(_frontKey, jsonEncode(front.toJson()));
    await prefs.setString(_leftKey, jsonEncode(left.toJson()));
    await prefs.setString(_rightKey, jsonEncode(right.toJson()));
  }

  Future<FaceEmbedding?> getFront() async {
    return _getEmbedding(_frontKey);
  }

  Future<FaceEmbedding?> getLeft() async {
    return _getEmbedding(_leftKey);
  }

  Future<FaceEmbedding?> getRight() async {
    return _getEmbedding(_rightKey);
  }

  Future<bool> hasEnrollment() async {
    final prefs = await SharedPreferences.getInstance();

    return prefs.containsKey(_frontKey) &&
        prefs.containsKey(_leftKey) &&
        prefs.containsKey(_rightKey);
  }

  Future<void> clearEnrollment() async {
    final prefs = await SharedPreferences.getInstance();

    await prefs.remove(_frontKey);
    await prefs.remove(_leftKey);
    await prefs.remove(_rightKey);
  }

  Future<FaceEmbedding?> _getEmbedding(String key) async {
    final prefs = await SharedPreferences.getInstance();

    final stored = prefs.getString(key);

    if (stored == null) {
      return null;
    }

    try {
      final decoded = jsonDecode(stored);

      if (decoded is! List) {
        throw const FormatException('Stored embedding is not a JSON array.');
      }

      return FaceEmbedding.fromJson(decoded);
    } catch (e) {
      throw FormatException('Failed to load stored face embedding: $e');
    }
  }
}
