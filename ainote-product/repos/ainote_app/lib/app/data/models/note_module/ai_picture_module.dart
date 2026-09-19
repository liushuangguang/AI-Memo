import 'note_module_payload.dart';

class AIPictureModule extends NoteModulePayload {
  String? moduleId;
  String? title;
  String? description;
  String? imageUrl;

  AIPictureModule({
    this.moduleId,
    this.title,
    this.description,
    this.imageUrl,
  });

  factory AIPictureModule.fromJson(Map<String, dynamic> json) {
    return AIPictureModule(
      moduleId: json['moduleId'],
      title: json['title'],
      description: json['description'],
      imageUrl: json['imageUrl'],
    );
  }

  @override
  String get noteModuleType =>
      NoteModuleType.AI_PICTURE.toString().split('.').last;

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'title': title,
      'description': description,
      'imageUrl': imageUrl,
    };
  }
}
