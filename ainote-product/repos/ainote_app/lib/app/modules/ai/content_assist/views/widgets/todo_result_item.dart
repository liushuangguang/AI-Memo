import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/widgets/todo_list.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';

import 'ai_result_item.dart';

class TodoResultItem extends StatelessWidget {
  // [TodoModel(title: '待办1', time: '2024-10-1 10:00', done: false)]
  final List<TodoModel> todos;
  final VoidCallback onSave;

  const TodoResultItem({super.key, required this.todos, required this.onSave});

  @override
  Widget build(BuildContext context) {
    if (todos.isEmpty) {
      return const SizedBox.shrink();
    }
    return ResultItem(
      loading: false,
      header: Row(children: [
        ResultItemHeader(
          title: '待办/计划',
          iconPath: Assets.imagesTodoBook,
        ),
        const Spacer(),
        ResultItemAction(
          actionText: '存入正文',
          afterActionText: '存入成功',
          onSave: onSave,
        ),
      ]),
      body: TodoList(list: todos, disabled: true),
    );
  }
}
