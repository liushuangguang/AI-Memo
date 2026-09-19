import 'dart:convert';

import 'package:json_annotation/json_annotation.dart';
import 'package:uuid/uuid.dart';

@JsonSerializable()
class TodoModel {
  String? id;
  String title;
  String date;
  bool done;

  TodoModel({
    required this.title,
    required this.date,
    required this.done,
  }): id = const Uuid().v4();

  Map<String, dynamic> toJson() => {
        'id': id ?? const Uuid().v4(),
        'title': title,
        'date': date,
        'done': done,
      };

  factory TodoModel.fromJson(Map<String, dynamic> json) {
    var model = TodoModel(
      title: json['title'] ?? '',
      date: json['date'] ?? '',
      done: json['done'] ?? false,
    );
    model.id = json['id'] ?? const Uuid().v4();
    return model;
  }

  @override
  String toString() {
    return 'TodoModel{id: $id, title: $title, date: $date, todo: $done}';
  }
}
