class AiRelatedNoteModel {
  final String? id;
  final String? title;
  final String? content;
  final String? reason;
  final double? score;

  const AiRelatedNoteModel(
      {this.id, this.title, this.content, this.reason, this.score});

  factory AiRelatedNoteModel.fromJson(Map<String, dynamic> json) {
    final rawScore = json['score'];
    final parsedScore = rawScore is num
        ? rawScore.toDouble()
        : rawScore is String
            ? double.tryParse(rawScore.trim())
            : null;
    final score =
        parsedScore != null && parsedScore.isFinite ? parsedScore : null;
    String? text(dynamic value) => value is String ? value.trim() : null;
    return AiRelatedNoteModel(
      id: text(json['id']),
      title: text(json['title']),
      content: text(json['content']),
      reason: text(json['reason']),
      score: score,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'content': content,
        'reason': reason,
        'score': score,
      };

  bool get isUsable =>
      (id?.trim().isNotEmpty ?? false) &&
      ((title?.trim().isNotEmpty ?? false) ||
          (content?.trim().isNotEmpty ?? false));
}
