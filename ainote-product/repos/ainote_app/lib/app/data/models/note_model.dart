import 'dart:convert';

import 'package:json_annotation/json_annotation.dart';

import 'note_module/note_module_payload.dart';

// dart run build_runner build
part 'note_model.g.dart';

@JsonSerializable()
class NoteModel {
  String? id;
  String? title;
  int? noteType;
  String? imageUrl;
  String? deviceId;
  String? content;
  List<dynamic>? modules;
  String? createdAt;
  String? updatedAt;
  bool? deleted;
  String? deletedAt;
  int? dimension;
  List<String>? noteThemeIds;

  NoteModel({
    this.id,
    this.title,
    this.noteType,
    this.imageUrl,
    this.deviceId,
    this.content,
    this.modules,
    this.createdAt,
    this.updatedAt,
    this.deleted,
    this.deletedAt,
    this.dimension,
    this.noteThemeIds,
  });

  factory NoteModel.fromJson(Map<String, dynamic> json) =>
      _$NoteModelFromJson(json);

  factory NoteModel.empty() => _$NoteModelFromJson(emptyJson);

  static Map<String, dynamic> get emptyJson => {
        "id": "",
        "title": "",
        "noteType": 0,
        "imageUrl": "",
        "deviceId": "",
        "rawNote": jsonEncode([
          {"insert": "\n"}
        ]),
        "modules": [
          {
            "segmentation": "",
            "moduleId": "",
            "noteModuleType": "",
            "items": []
          }
        ],
        "createdAt": "",
        "updatedAt": "",
        "deleted": false,
        "deletedAt": "",
        "dimension": 0
      };

  Map<String, dynamic> toJson() => _$NoteModelToJson(this);

// @override
// String toString() => jsonEncode(toJson);
}

@JsonSerializable()
class NoteModule {
  String? segmentation;
  String? moduleId;
  String? noteModuleType;
  String? title;
  String? content;

  NoteModule({
    this.segmentation,
    this.moduleId,
    this.noteModuleType,
    this.title,
    this.content,
  });

  factory NoteModule.fromJson(Map<String, dynamic> json) =>
      _$NoteModuleFromJson(json);

  Map<String, dynamic> toJson() => _$NoteModuleToJson(this);
}

const noteModuleType = [
  'TEXT_DATA',
  'RELATED_NOTE',
  'AI_PICTURE',
  'QUESTION_ANSWER',
  'BACKLOG',
  'AI_RECOMMENDATION'
];
