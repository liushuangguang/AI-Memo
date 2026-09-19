import 'package:ainote/models/todo_model.dart';

class ContentAssistDirectionModel {
  int? code; // 200 - ok 500 - 无意义
  // 推荐理由
  String? message;

  // 推荐item
  List<dynamic>? data;

  ContentAssistDirectionModel({this.code, this.message, this.data});

  factory ContentAssistDirectionModel.fromJson(Map<String, dynamic>? json) {
    return ContentAssistDirectionModel(
        code: json?['code'],
        message: json?['message'] ?? '',
        data: json?['data'] ?? []);
  }

  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'message': message,
      'data': data,
    };
  }

  @override
  String toString() {
    return "ContentAssistDirectionModel{code: $code, message: $message, data: $data}";
  }
}

class ContentAssistResultModel {
  String noteId;
  String rewrite_content;
  List<TodoModel> todos; // 代办信息 ['待办1', '2024-10-1 10:00']

  ContentAssistResultModel({
    required this.noteId,
    required this.rewrite_content,
    required this.todos,
  });

  factory ContentAssistResultModel.fromJson(Map<String, dynamic> json) {
    return ContentAssistResultModel(
      noteId: json['noteId'] ?? '',
      rewrite_content: json['rewrite_content'] ?? '',
      todos: json['todos'] ?? [],
    );
  }

  // rawStr = "noteId=note_1051rewrite_content=今晚组织30分钟工作总结会，回顾上周工作进展。to-do=待办内容1：晚上主持工作总结会待办事件：2024-08-28to-do=待办内容2";
  factory ContentAssistResultModel.fromRawString(String? rawStr) {
    rawStr = rawStr ?? '';
    if (rawStr.isEmpty) {
      return ContentAssistResultModel(
        noteId: '',
        rewrite_content: '',
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
              title: match.group(1) ?? '',
              date: match.group(2) ?? '',
              done: false));
        }
      }

      print('ai result todo2: $todos');
    } catch (e) {
      print("ContentAssistResultModel.fromRawString error: $e");
    }

    return ContentAssistResultModel(
      noteId: noteId,
      rewrite_content: rewrite_content,
      todos: todos,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'noteId': noteId,
      'rewrite_content': rewrite_content,
      'todos': todos,
    };
  }

  @override
  String toString() {
    return 'ContentAssistResultModel{noteId: $noteId, rewrite_content: $rewrite_content, todos: $todos}';
  }
}
