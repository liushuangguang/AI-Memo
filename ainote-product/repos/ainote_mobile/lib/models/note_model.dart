import 'package:ainote/models/ai_history_model.dart';
import 'package:ainote/utils/utils.dart';

import 'ai_generate_model.dart';

class NoteModel {
  int? id;
  String? title;
  String? deviceId;
  String? rawNote;
  String? noteAnalysisContent;
  String? pinyinTags;
  String? tags;
  List<String>? tagList;
  List<String>? hitTags;
  String? createdAt;
  String? updatedAt;
  String? deletedAt;
  int? version;
  bool? deleted;
  int? noteType;
  int? dimension;
  List<AIHistoryModel>? noteAnalysisHistories;
  List<AIGenerateModel>? generatedList;

  NoteModel({
    this.id,
    this.title,
    this.deviceId,
    this.rawNote,
    this.noteAnalysisContent,
    this.pinyinTags,
    this.tags,
    this.tagList,
    this.hitTags,
    this.createdAt,
    this.updatedAt,
    this.deletedAt,
    this.version,
    this.deleted,
    this.noteType,
    this.dimension,
    this.noteAnalysisHistories,
    this.generatedList,
  });

  factory NoteModel.fromJson(Map<String, dynamic> json) {
    return NoteModel(
      id: json['id'] as int?,
      title: json['title'] as String?,
      deviceId: json['deviceId'] as String?,
      rawNote: json['rawNote'] as String?,
      noteAnalysisContent: json['noteAnalysisContent'] as String?,
      pinyinTags: json['pinyinTags'] as String?,
      tags: json['tags'] as String?,
      tagList: (json['tagList'] as List<dynamic>?)?.map((e) => e as String).toList(),
      hitTags: (json['hitTags'] as List<dynamic>?)?.map((e) => e as String).toList(),
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      deletedAt: json['deletedAt'] as String?,
      version: json['version'] as int?,
      deleted: json['deleted'] as bool?,
      noteType: json['noteType'] as int?,
      dimension: json['dimension'] as int?,
      noteAnalysisHistories: (json['noteAnalysisHistories'] as List<dynamic>?)?.map((e) => AIHistoryModel.fromJson(e as Map<String, dynamic>)).toList(),
      // generatedList: (json['generatedList'] as List<dynamic>?)?.map((e) => AIGenerateModel.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'deviceId': deviceId,
      'rawNote': rawNote,
      'noteAnalysisContent': noteAnalysisContent,
      'pinyinTags': pinyinTags,
      'tags': tags,
      'tagList': tagList,
      'hitTags': hitTags,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'deletedAt': deletedAt,
      'version': version,
      'deleted': deleted,
      'noteType': noteType,
      'dimension': dimension,
      'noteAnalysisHistories': noteAnalysisHistories?.map((e) => e.toJson()).toList(),
      'generatedList': generatedList?.map((e) => e.toJson()).toList(),
    };
  }

  void convertToGenerated() {
    generatedList ??= [];
    if (noteAnalysisHistories == null) return;
    for (final history in noteAnalysisHistories!) {
      final generated = parseAIGenerate(history.organizedNoteText ?? "");
      generatedList!.add(generated);
    }
    //
    // AIGenerateModel model = AIGenerateModel(
    //   all: "all",
    //   title: title ?? "default title",
    //   content: rawNote ?? "default content",
    //   suggestion: "suggestion",
    //   guess: "guess",
    //   todo: "todo",
    //   scene: "scene",
    //   category: "category",
    //   tag: tags ?? "default tag",
    // );
    //
    // generatedList!.add(model);
  }

  void insertAIHistory(AIHistoryModel history) {
    noteAnalysisHistories ??= [];
    noteAnalysisHistories!.insert(0, history);
  }

  void insertAIGenerate(AIGenerateModel generate) {
    generatedList ??= [];
    generatedList!.insert(0, generate);
  }
}