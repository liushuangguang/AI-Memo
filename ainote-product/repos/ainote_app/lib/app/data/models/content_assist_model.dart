import 'package:ainote_app/app/data/models/todo_model.dart';

class ContentAssistDirectionModel {
  // 推荐理由
  String? reason;

  // 推荐item
  List<String>? availableAssistantDirection;

  ContentAssistDirectionModel({this.reason, this.availableAssistantDirection});

  factory ContentAssistDirectionModel.fromJson(Map<String, dynamic>? json) {
    return ContentAssistDirectionModel(
        reason: json?['reason'] ?? '',
        availableAssistantDirection:
            (json?['availableAssistantDirection'] ?? [])
                .map<String>((e) => e.toString())
                .toList());
  }

  Map<String, dynamic> toJson() {
    return {
      'reason': reason,
      'availableAssistantDirection': availableAssistantDirection,
    };
  }

  @override
  String toString() {
    return "ContentAssistDirectionModel{reason: $reason, availableAssistantDirection: $availableAssistantDirection";
  }
}

class ContentAssistResultModel {
  String rewrittenContent;
  List<TodoModel> todos; // 代办信息 ['待办1', '2024-10-1 10:00']

  ContentAssistResultModel({
    required this.rewrittenContent,
    required this.todos,
  });

  factory ContentAssistResultModel.fromJson(Map<String, dynamic> json) {
    return ContentAssistResultModel(
      rewrittenContent: json['rewrittenContent'] ?? '',
      todos: (json['todos'] ?? [])
          .map<TodoModel>((e) => TodoModel.fromJson(e))
          .toList(),
    );
  }

  // rawStr = "noteId=note_1051rewrite_content=今晚组织30分钟工作总结会，回顾上周工作进展。to-do=待办内容1：晚上主持工作总结会待办事件：2024-08-28to-do=待办内容2";
  factory ContentAssistResultModel.fromRawString(String? rawStr) {
    rawStr = rawStr ?? '';
    if (rawStr.isEmpty) {
      return ContentAssistResultModel(
        rewrittenContent: '',
        todos: [],
      );
    }

    var noteId = '';
    var rewrite_content = '';
    List<TodoModel> todos = [];
    try {
      var noteId_index = rawStr.indexOf('noteId=');
      var rewrite_content_index = rawStr.indexOf('rewrite_content=');
      var todo_start_index = rawStr.indexOf('to-do=');

      noteId = noteId = rawStr
          .substring(noteId_index, rewrite_content_index)
          .replaceAll('noteId=', '')
          .replaceAll('note_', '');

      rewrite_content = rawStr
          .substring(rewrite_content_index, todo_start_index)
          .replaceAll('rewrite_content=', '');

      var rest = rawStr.substring(todo_start_index);
      RegExp regExp = RegExp(
          r"待办内容\d*：(.*?)待办时间\d*：(\d{4}[-/]\d{1,2}[-/]\d{1,2}( \d{2}:\d{2}:\d{2})?)");
      Iterable<Match> matches = regExp.allMatches(rest);
      for (Match match in matches) {
        if (match.group(2) != null && match.group(2)!.isNotEmpty) {
          todos.add(TodoModel(
              content: match.group(1) ?? '',
              scheduledAt: match.group(2) ?? '',
              done: false));
        }
      }
    } catch (e) {}

    return ContentAssistResultModel(
      rewrittenContent: rewrite_content,
      todos: todos,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'rewrittenContent': rewrittenContent,
      'todos': todos.map((e) => e.toJson()).toList(),
    };
  }

  @override
  String toString() {
    return 'ContentAssistResultModel{rewrittenContent: $rewrittenContent, todos: ${todos.map((e) => e.toJson()).toList()}';
  }
}
