class CategorizedNoteModel {
  int? id;
  int? noteAnalysisId;
  String? noteText;
  int? noteType;
  int? status;

  CategorizedNoteModel({
    this.id,
    this.noteAnalysisId,
    this.noteText,
    this.noteType,
    this.status,
  });

  factory CategorizedNoteModel.fromJson(Map<String, dynamic> json) {
    return CategorizedNoteModel(
      id: json['id'] as int?,
      noteAnalysisId: json['noteAnalysisId'] as int?,
      noteText: json['noteText'] as String?,
      noteType: json['noteType'] as int?,
      status: json['status'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'noteAnalysisId': noteAnalysisId,
      'noteText': noteText,
      'noteType': noteType,
      'status': status,
    };
  }
}