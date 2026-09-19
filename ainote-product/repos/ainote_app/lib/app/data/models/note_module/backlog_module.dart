import 'note_module_payload.dart';

class BacklogModule extends NoteModulePayload {
  String? moduleId;
  String? description;
  String? name;

  BacklogModule({
    this.description,
    this.name,
    this.moduleId,
  });

  @override
  String get noteModuleType =>
      NoteModuleType.BACKLOG.toString().split('.').last;

  // 从 JSON 数据创建对象
  factory BacklogModule.fromJson(Map<String, dynamic> json) {
    return BacklogModule(
      description: json['description'],
      name: json['name'],
      moduleId: json['moduleId'],
    );
  }

  // 将对象转换为 JSON 数据
  @override
  Map<String, dynamic> toJson() {
    return {
      'description': description,
      'name': name,
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
    };
  }
}
