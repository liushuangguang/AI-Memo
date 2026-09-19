import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/utils/helper.dart';

class AiOrganizeModel {
  String? title;
  List<TodoModel>? todoList;
  List<String>? tag;
  String? content;
  String? userScenario;
  String? bodyText;
  String? memoClassification;

  AiOrganizeModel({
    this.title,
    this.todoList,
    this.tag,
    this.content,
    this.userScenario,
    this.bodyText,
    this.memoClassification,
  });

  factory AiOrganizeModel.fromJson(Map<String, dynamic> json) {
    List<TodoModel> todoList = [];
    for (var item in (json['todoList'] ?? [])) {
      todoList.add(TodoModel.fromJson(item));
    }
    return AiOrganizeModel(
      title: json['title'],
      todoList: todoList,
      tag: json['tag'] == null ? [] : List<String>.from(json['tag']),
      content: json['content'],
      userScenario: json['userScenario'],
      bodyText: json['bodyText'],
      memoClassification: json['memoClassification'],
    );
  }

  factory AiOrganizeModel.mock() {
    return AiOrganizeModel(
      title: generateMockString(
        5,
      ),
      todoList: [],
      tag: [generateMockString(5), generateMockString(5)],
      content: generateMockString(16),
      userScenario: generateMockString(8),
      bodyText: generateMockString(20),
      memoClassification: generateMockString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'todoList': todoList?.map((e) => e.toJson()).toList(),
      'tag': tag,
      'content': content,
      'userScenario': userScenario,
      'bodyText': bodyText,
      'memoClassification': memoClassification,
    };
  }
}
