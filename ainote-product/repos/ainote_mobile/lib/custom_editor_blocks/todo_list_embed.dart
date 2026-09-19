import 'dart:convert' show jsonDecode, jsonEncode;

import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

import '../classes/dashed_line_painter.dart';
import '../models/todo_model.dart';
import '../screens/ai_voice_discuss/ai_voice_result_card.dart';
import '../screens/widgets/todo_list.dart';
import '../utils/utils.dart';

class TodoListEmbed extends Embeddable {
  const TodoListEmbed(
    String value,
  ) : super(noteType, value);

  static const String noteType = 'todo_list';

  static TodoListEmbed fromDocument(Document document) =>
      TodoListEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class TodoListEmbedBuilder extends EmbedBuilder {
  @override
  String get key => 'todo_list';

  @override
  String toPlainText(Embed node) {
    return node.value.data;
  }

  @override
  Widget build(
    BuildContext context,
    QuillController controller,
    Embed node,
    bool readOnly,
    bool inline,
    TextStyle textStyle,
  ) {
    List<ToDoItem> todoList = [];
    try {
      final list = jsonDecode(node.value.data);
      for (int i = 0; i < list.length; i++) {
        todoList.add(ToDoItem(
          todo: TodoModel.fromJson(list[i]),
          onEdit: (todo) {
            // todo bug 删除更新有问题
            int index = list.indexWhere((element) => element['id'] == todo.id);
            list[index] = todo.toJson();
            deleteQuillCustomEmbedNode(controller, node);
            insertQuillCustomEmbedNode(
                controller, TodoListEmbed(jsonEncode(list)));
          },
        ));
      }
    } catch (e) {
      print('Error parsing todo list: $e');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 16),
        DashedLine(),
        const SizedBox(height: 10),
        const Text('待办/计划', textAlign: TextAlign.left, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24),),
        TodoList(
            list: todoList,
            onDelete: () {
              // todo bug 删除有问题
              deleteQuillCustomEmbedNode(controller, node);
            })
      ],
    );
  }
}
