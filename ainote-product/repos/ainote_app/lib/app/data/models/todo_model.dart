import 'package:ainote_app/app/utils/date_format.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:uuid/uuid.dart';

class TodoModel {
  late String id;
  String content;
  String? description;
  String? scheduledAt;
  bool done;

  TodoModel({
    required this.content,
    this.scheduledAt,
    required this.done,
    this.id = '',
    this.description,
  }) {
    id = id == '' ? const Uuid().v4() : id;
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'content': content,
        'scheduledAt': scheduledAt,
        'done': done,
        'description': description,
      };

  factory TodoModel.mock() {
    return TodoModel(
      content: generateMockString(16),
      scheduledAt: generateMockString(12),
      done: false,
      description: generateMockString(32),
    );
  }

  factory TodoModel.fromJson(Map<String, dynamic> json) {
    var model = TodoModel(
      content: json['content'] ?? '',
      scheduledAt: parseApiDateString(json['scheduledAt'] ?? ''),
      done: json['done'] ?? false,
      description: json['description'] ?? '',
    );
    model.id = json['id'] ?? const Uuid().v4();
    return model;
  }

  @override
  String toString() {
    return 'TodoModel{id: $id, content: $content, scheduledAt: $scheduledAt, todo: $done, description: $description}';
  }
}
