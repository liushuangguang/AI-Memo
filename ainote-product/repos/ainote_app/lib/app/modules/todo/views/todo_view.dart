import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/config/theme/my_styles.dart';
import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/modules/home/controllers/home_controller.dart';
import 'package:ainote_app/app/modules/home/views/widgets/todo_card.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'package:get/get.dart';

import '../controllers/todo_controller.dart';

class TodoView extends GetView<TodoController> {
  const TodoView({super.key});

  @override
  Widget build(BuildContext context) {
    Get.put(TodoController());

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

    return Scaffold(
      appBar: AppBar(
        leadingWidth: 0,
        leading: Container(),
        centerTitle: false,
        title: Text(
          '待办',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24.sp),
        ),
      ),
      body: Padding(
        padding: EdgeInsets.symmetric(horizontal: 16.w),
        child: TodoCard(height: Get.height - 220.h),
      ),
      floatingActionButton: FloatingActionButton(
          backgroundColor: MyColors.colorBlue,
          onPressed: handleAddTodo,
          child: const Icon(Icons.add, size: 24)),
    );
  }
}
