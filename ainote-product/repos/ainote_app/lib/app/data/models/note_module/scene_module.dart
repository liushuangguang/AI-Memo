import 'note_module_payload.dart';

class SceneModule extends NoteModulePayload {
  String? moduleId;
  String? title;
  String? content;

  SceneModule({
    this.moduleId,
    this.title,
    this.content,
  });

  factory SceneModule.fromJson(Map<String, dynamic> json) {
    return SceneModule(
      moduleId: json['moduleId'],
      title: json['title'],
      content: json['content'],
    );
  }

  @override
  String get noteModuleType =>
      NoteModuleType.SCENARIO_RECORD.toString().split('.').last;

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'title': title,
      'content': content,
    };
  }
}
