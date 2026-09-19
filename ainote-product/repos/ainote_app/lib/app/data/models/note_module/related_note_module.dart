import 'note_module_payload.dart';

class RelatedNoteModule extends NoteModulePayload {
  String? moduleId;

  List<String>? relatedNoteIds;

  RelatedNoteModule({
    this.relatedNoteIds,
    this.moduleId,
  });

  @override
  String get noteModuleType =>
      NoteModuleType.RELATED_NOTE.toString().split('.').last;

  factory RelatedNoteModule.fromJson(Map<String, dynamic> json) {
    return RelatedNoteModule(
      relatedNoteIds: json['relatedNoteIds']?.cast<String>(),
      moduleId: json['moduleId'],
    );
  }

  @override
  Map<String, dynamic> toJson() {
    return {
      'relatedNoteIds': relatedNoteIds,
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
    };
  }
}
