import 'package:ainote/models/todo_model.dart';
import 'package:ainote/screens/widgets/todo_list.dart';
import 'package:flutter/material.dart';

import '../../constants/app_images.dart';
import 'ai_result_item.dart';

class TodoResultItem extends StatelessWidget {
  // [TodoModel(title: '待办1', time: '2024-10-1 10:00', done: false)]
  final List<TodoModel> todos;
  final Future<bool> Function(List<TodoModel>) onSave;


  const TodoResultItem(
      {super.key, required this.todos, required this.onSave});

  @override
  Widget build(BuildContext context) {
    List<ToDoItem> list = todos
        .map((d) => ToDoItem(todo: d))
        .toList();
    if(todos.isEmpty) {
      return const SizedBox.shrink();
    }
    return ResultItem(
      loading: false,
      header: Row(children: [
        ResultItemHeader(
          title: '待办/计划',
          iconPath: AppImages.todoBookIcon.path,
        ),
        const Spacer(),
        ResultItemAction(
          actionText: '存入正文',
          afterActionText: '存入成功',
          onSave: () {
            onSave(todos);
          },
        ),
      ]),
      body: TodoList(list: list),
    );
  }
}
