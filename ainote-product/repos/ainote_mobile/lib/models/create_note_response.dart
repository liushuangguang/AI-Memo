class CreateNoteResponse {
  final int? id;
  final String? title;
  final String? deviceId;
  final String? noteId;
  final String? rawNote;
  final String? noteAnalysisContent;
  final String? pinyinTags;
  final String? tags;
  final List<String>? tagList;

  CreateNoteResponse({
    this.id,
    this.title,
    this.deviceId,
    this.noteId,
    this.rawNote,
    this.noteAnalysisContent,
    this.pinyinTags,
    this.tags,
    this.tagList,
  });

  //fromJson
  factory CreateNoteResponse.fromJson(Map<String, dynamic> json) {
    return CreateNoteResponse(
      id: json['id'] as int?,
      title: json['title'] as String?,
      deviceId: json['deviceId'] as String?,
      noteId: json['noteId'] as String?,
      rawNote: json['rawNote'] as String?,
      noteAnalysisContent: json['noteAnalysisContent'] as String?,
      pinyinTags: json['pinyinTags'] as String?,
      tags: json['tags'] as String?,
      tagList: (json['tagList'] as List<dynamic>?)?.map((e) => e as String).toList(),
    );
  }


  //toJson
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'deviceId': deviceId,
      'noteId': noteId,
      'rawNote': rawNote,
      'noteAnalysisContent': noteAnalysisContent,
      'pinyinTags': pinyinTags,
      'tags': tags,
      'tagList': tagList,
    };
  }
}