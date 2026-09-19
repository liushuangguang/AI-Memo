import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:loading_more_list/loading_more_list.dart';

import '../../../../config/theme/my_colors.dart';
import '../../../../widgets/todo_list.dart';
import '../../controllers/home_controller.dart';

class TodoMoreList extends StatelessWidget {
  final bool? disabled;
  final Function(TodoModel)? onEdit;
  final Function(TodoModel)? onDelete;

  const TodoMoreList({super.key, this.disabled, this.onEdit, this.onDelete});

  @override
  Widget build(BuildContext context) {
    final logic = Get.put(HomeController());
    var sourceList = logic.todoRepository;

    return LoadingMoreList(
      ListConfig<TodoModel>(
        itemBuilder: (
          context,
          item,
          index,
        ) {
          return ToDoItem(
            todo: item,
            onEdit: logic.editTodo,
            onDelete: logic.deleteTodo,
            disabled: false,
          );
        },
        sourceList: sourceList,
        indicatorBuilder: (context, status) => SizedBox(
          height: 40.h,
          child: Center(
            child: getLoadStatusWidget(status, sourceList.length),
          ),
        ),
        padding: const EdgeInsets.all(0.0),
      ),
    );
  }

  getLoadStatusWidget(IndicatorStatus status, int length) {
    switch (status) {
      case IndicatorStatus.loadingMoreBusying:
        return CupertinoActivityIndicator(
          color: MyColors.colorBlue,
        );
      case IndicatorStatus.noMoreLoad:
        if (length < 5) {
          return SizedBox.shrink();
        }
        return Text('没有更多数据了',
            style: TextStyle(fontSize: 14.sp, color: MyColors.colorGrey));
      case IndicatorStatus.empty:
        return Text('暂无任何待办',
            style: TextStyle(color: MyColors.colorGrey, fontSize: 14.sp));
      default:
        return CupertinoActivityIndicator(
          color: MyColors.colorBlue,
        );
    }
  }
}
