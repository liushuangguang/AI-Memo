// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'note_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

NoteModel _$NoteModelFromJson(Map<String, dynamic> json) => NoteModel(
      id: json['id'] as String?,
      title: json['title'] as String?,
      noteType: (json['noteType'] as num?)?.toInt(),
      imageUrl: json['imageUrl'] as String?,
      deviceId: json['deviceId'] as String?,
      content: json['content'] as String?,
      modules: json['modules'] as List<dynamic>?,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
      deleted: json['deleted'] as bool?,
      deletedAt: json['deletedAt'] as String?,
      dimension: (json['dimension'] as num?)?.toInt(),
      noteThemeIds: (json['noteThemeIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
    );

Map<String, dynamic> _$NoteModelToJson(NoteModel instance) => <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'noteType': instance.noteType,
      'imageUrl': instance.imageUrl,
      'deviceId': instance.deviceId,
      'content': instance.content,
      'modules': instance.modules,
      'createdAt': instance.createdAt,
      'updatedAt': instance.updatedAt,
      'deleted': instance.deleted,
      'deletedAt': instance.deletedAt,
      'dimension': instance.dimension,
      'noteThemeIds': instance.noteThemeIds,
    };

NoteModule _$NoteModuleFromJson(Map<String, dynamic> json) => NoteModule(
      segmentation: json['segmentation'] as String?,
      moduleId: json['moduleId'] as String?,
      noteModuleType: json['noteModuleType'] as String?,
      title: json['title'] as String?,
      content: json['content'] as String?,
    );

Map<String, dynamic> _$NoteModuleToJson(NoteModule instance) =>
    <String, dynamic>{
      'segmentation': instance.segmentation,
      'moduleId': instance.moduleId,
      'noteModuleType': instance.noteModuleType,
      'title': instance.title,
      'content': instance.content,
    };
