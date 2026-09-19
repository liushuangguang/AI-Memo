import 'package:ainote_app/app/modules/home/views/widgets/card_wrapper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

import '../../../../data/models/todo_model.dart';
import '../../../../utils/quill_editor.dart';
import '../../controllers/home_controller.dart';
import 'todo_more_list.dart';

class TodoCard extends StatelessWidget {
  final double? height;
  const TodoCard({
    super.key,
    this.height,
  });

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<HomeController>();
    final TodoModel todo = TodoModel(
      content: '',
      scheduledAt: '',
      done: false,
    );

    void handleAddTodo() {
      showTodoModal(context,
          todo: todo, onConfirm: logic.addTodo, isEdit: false);
    }

    return CardWrapper(
        title: '待办清单',
        action: '添加待办',
        height: height,
        onTap: () {},
        onActionTap: handleAddTodo,
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 8.0.h),
          child: TodoMoreList(
            disabled: false,
          ),
        ));
  }
}
