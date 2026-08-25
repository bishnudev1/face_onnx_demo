import 'face_embedding.dart';

class VerificationResult {
  final double frontScore;
  final double leftScore;
  final double rightScore;
  final double averageScore;
  final double bestScore;
  final double threshold;
  final bool matched;

  const VerificationResult({
    required this.frontScore,
    required this.leftScore,
    required this.rightScore,
    required this.averageScore,
    required this.bestScore,
    required this.threshold,
    required this.matched,
  });

  List<double> get scores => [frontScore, leftScore, rightScore];

  factory VerificationResult.fromScores({
    required double frontScore,
    required double leftScore,
    required double rightScore,
    required double threshold,
  }) {
    final scores = [frontScore, leftScore, rightScore];

    final average = scores.reduce((a, b) => a + b) / scores.length;
    final best = scores.reduce((a, b) => a > b ? a : b);

    return VerificationResult(
      frontScore: frontScore,
      leftScore: leftScore,
      rightScore: rightScore,
      averageScore: average,
      bestScore: best,
      threshold: threshold,
      matched: average >= threshold,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'frontScore': frontScore,
      'leftScore': leftScore,
      'rightScore': rightScore,
      'averageScore': averageScore,
      'bestScore': bestScore,
      'threshold': threshold,
      'matched': matched,
    };
  }

  @override
  String toString() {
    return 'VerificationResult('
        'front: $frontScore, '
        'left: $leftScore, '
        'right: $rightScore, '
        'average: $averageScore, '
        'best: $bestScore, '
        'matched: $matched'
        ')';
  }
}
