import 'package:ainote_app/app/data/models/note_model.dart';

String? _text(dynamic value) {
  if (value is! String) return null;
  final normalized = value.trim();
  return normalized.isEmpty ? null : normalized;
}

class NoteThemeSourceModel {
  const NoteThemeSourceModel({required this.noteId, required this.title});

  final String noteId;
  final String title;

  factory NoteThemeSourceModel.fromJson(Map<String, dynamic> json) =>
      NoteThemeSourceModel(
        noteId: _text(json['noteId']) ?? '',
        title: _text(json['title']) ?? '未命名备忘录',
      );
}

class NoteThemeMergeHistoryModel {
  const NoteThemeMergeHistoryModel({
    required this.operationId,
    required this.mergedNoteId,
    required this.mergedTitle,
    required this.mergedContentPreview,
    required this.imageUrls,
    required this.sources,
    this.mergedAt,
  });

  final String operationId;
  final String mergedNoteId;
  final String mergedTitle;
  final String mergedContentPreview;
  final List<String> imageUrls;
  final List<NoteThemeSourceModel> sources;
  final DateTime? mergedAt;

  factory NoteThemeMergeHistoryModel.fromJson(Map<String, dynamic> json) {
    final rawImages = json['imageUrls'];
    final rawSources = json['sources'];
    return NoteThemeMergeHistoryModel(
      operationId: _text(json['operationId']) ?? '',
      mergedNoteId: _text(json['mergedNoteId']) ?? '',
      mergedTitle: _text(json['mergedTitle']) ?? '未命名备忘录',
      mergedContentPreview: _text(json['mergedContentPreview']) ?? '',
      imageUrls: rawImages is List
          ? rawImages.map(_text).whereType<String>().toList(growable: false)
          : const [],
      sources: rawSources is List
          ? rawSources
              .whereType<Map>()
              .map((item) => NoteThemeSourceModel.fromJson(
                  Map<String, dynamic>.from(item)))
              .where((item) => item.noteId.isNotEmpty)
              .toList(growable: false)
          : const [],
      mergedAt: DateTime.tryParse(_text(json['mergedAt']) ?? ''),
    );
  }
}

class NoteThemeModel {
  const NoteThemeModel({
    required this.id,
    required this.theme,
    required this.description,
    required this.mergeHistory,
    this.mergedNoteId,
  });

  final String id;
  final String theme;
  final String description;
  final String? mergedNoteId;
  final List<NoteThemeMergeHistoryModel> mergeHistory;

  NoteThemeMergeHistoryModel? get latestMerge =>
      mergeHistory.isEmpty ? null : mergeHistory.last;

  factory NoteThemeModel.fromJson(Map<String, dynamic> json) {
    final rawHistory = json['mergeHistory'];
    return NoteThemeModel(
      id: _text(json['id']) ?? '',
      theme: _text(json['theme']) ?? '',
      description: _text(json['description']) ?? '',
      mergedNoteId: _text(json['mergedNoteId']),
      mergeHistory: rawHistory is List
          ? rawHistory
              .whereType<Map>()
              .map((item) => NoteThemeMergeHistoryModel.fromJson(
                  Map<String, dynamic>.from(item)))
              .where((item) => item.operationId.isNotEmpty)
              .toList(growable: false)
          : const [],
    );
  }
}

class NoteThemeCandidateModel {
  const NoteThemeCandidateModel({
    required this.note,
    required this.reason,
    required this.score,
  });

  final NoteModel note;
  final String reason;
  final double score;

  factory NoteThemeCandidateModel.fromJson(Map<String, dynamic> json) {
    final rawScore = json['score'];
    final score = rawScore is num
        ? rawScore.toDouble()
        : double.tryParse(rawScore?.toString() ?? '') ?? 0;
    return NoteThemeCandidateModel(
      note: NoteModel.fromJson(json),
      reason: _text(json['reason']) ?? '',
      score: score.isFinite ? score.clamp(0, 1).toDouble() : 0,
    );
  }

  bool get isUsable => (note.id?.trim().isNotEmpty ?? false) &&
      ((note.title?.trim().isNotEmpty ?? false) ||
          (note.content?.trim().isNotEmpty ?? false));
}

class NoteThemeMergeResultModel {
  const NoteThemeMergeResultModel({
    required this.themeId,
    required this.note,
    required this.history,
  });

  final String themeId;
  final NoteModel note;
  final NoteThemeMergeHistoryModel history;

  factory NoteThemeMergeResultModel.fromJson(Map<String, dynamic> json) =>
      NoteThemeMergeResultModel(
        themeId: _text(json['themeId']) ?? '',
        note: NoteModel.fromJson(
            Map<String, dynamic>.from(json['note'] as Map)),
        history: NoteThemeMergeHistoryModel.fromJson(
            Map<String, dynamic>.from(json['history'] as Map)),
      );
}
