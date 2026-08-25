class FaceEmbedding {
  final List<double> values;

  const FaceEmbedding(this.values);

  /// InsightFace w600k_mbf produces a 512-dimensional embedding.
  static const int dimension = 512;

  bool get isValid => values.length == dimension;

  factory FaceEmbedding.fromJson(List<dynamic> json) {
    final values = json.map((value) => (value as num).toDouble()).toList();

    if (values.length != dimension) {
      throw FormatException(
        'Invalid face embedding dimension: '
        '${values.length}. Expected $dimension.',
      );
    }

    return FaceEmbedding(values);
  }

  List<double> toJson() {
    return List<double>.from(values);
  }

  @override
  String toString() {
    return 'FaceEmbedding(dimension: ${values.length})';
  }
}
