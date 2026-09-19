import 'note_module_payload.dart';

class TextDataModule extends NoteModulePayload {
  String? moduleId;
  String? title;
  String? content;
  List<dynamic>? items;

  @override
  String get noteModuleType =>
      NoteModuleType.TEXT_DATA.toString().split('.').last;

  TextDataModule({
    this.title,
    this.content,
    this.moduleId,
    this.items,
  });

  factory TextDataModule.fromJson(Map<String, dynamic> json) {
    return TextDataModule(
      moduleId: json['moduleId'],
      title: json['title'],
      content: json['content'],
      items: json['items'],
    );
  }

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'title': title,
      'content': content,
      'items': items,
    };
  }
}
