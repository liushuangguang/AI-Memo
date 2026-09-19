import 'dart:convert';

import 'package:ainote/models/todo_model.dart';
import 'package:json_annotation/json_annotation.dart';

@JsonSerializable()
class VoiceTodoMsgModel {
  String? message;

  // [TodoModel(title: '待办1', time: '2024-10-1 10:00', done: false)]
  List<TodoModel>? todos;
  String? note; // 备忘录信息

  VoiceTodoMsgModel({this.message, this.todos, this.note});

  factory VoiceTodoMsgModel.fromJson(Map<String, dynamic> json) {
    return VoiceTodoMsgModel(
      message: json['message'],
      todos: json['todos'],
      note: json['note'],
    );
  }

  factory VoiceTodoMsgModel.fromRawString(String? rawStr) {
    rawStr = rawStr ?? '';
    if (rawStr.isEmpty) {
      return VoiceTodoMsgModel();
    }

    print('getOrganizeToDoListNote rawStr: $rawStr');
    var message = '';
    List<TodoModel> todos = [];
    var note = '';
    try {
      if (rawStr.endsWith(":}")) {
        rawStr = rawStr.replaceAll(":}", ":null}");
      }

      var json = jsonDecode(rawStr);
      message = json['message'];
      var baseInfo = json['baseInfo'];

      if (baseInfo != null) {
        var todoList = baseInfo['to_do_list'] ?? [];
        var noteList = baseInfo['note_list'] ?? [];

        note = noteList[0]['note'] ?? '';

        for (var todo in todoList) {
          var task = todo['task'].contains('暂无') ? '' : todo['task'];
          var date = todo['time'].contains('暂无') ? '' : todo['time'];
          if(task.trim().isNotEmpty) {
            todos.add(TodoModel(title: task, date: date, done: false));
          }
        }
      }
    } catch (e) {
      print("VoiceTodoMsgModel.fromRawString error: $e");
    }

    return VoiceTodoMsgModel(
      message: message,
      note: note,
      todos: todos,
    );
  }

  @override
  String toString() {
    return 'VoiceTodoMsgModel{message: $message, note: $note, todos: $todos}';
  }
}
