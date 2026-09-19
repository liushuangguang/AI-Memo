import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/widgets/ai_card/todo_search_detail.dart';
import 'package:flutter/material.dart';

import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

import '../../generated/assets.dart';
import '../data/models/todo_model.dart';
import '../utils/quill_editor.dart';

class TodoList extends StatelessWidget {
  final List<TodoModel> list;
  final VoidCallback? onDelete;
  final VoidCallback? onUpdate;
  final bool? disabled;

  const TodoList({
    super.key,
    required this.list,
    this.onDelete,
    this.onUpdate,
    this.disabled,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: list.length,
        itemBuilder: (context, index) {
          return ToDoItem(
            todo: list[index],
            disabled: disabled,
          );
        });
  }
}

class ToDoItem extends StatefulWidget {
  final TodoModel todo;
  final bool? disabled;
  final Function(TodoModel)? onEdit;
  final Function(TodoModel)? onDelete;
  final Function(TodoModel)? onSearch;
  final Widget? icon;
  final Widget? extra;
  final bool? noIcon;
  final bool? highlight;

  const ToDoItem(
      {super.key,
      required this.todo,
      this.onEdit,
      this.disabled = false,
      this.onDelete,
      this.onSearch,
      this.icon,
      this.noIcon,
      this.highlight,
      this.extra});

  @override
  State<ToDoItem> createState() => _ToDoItemState();
}

class _ToDoItemState extends State<ToDoItem> {
  bool todoSearchVisible = false;
  late TodoModel _todo;

  void showTodoSearch(BuildContext context) {
    if (todoSearchVisible) return;
    setState(() {
      todoSearchVisible = true;
    });
    SmartDialog.showAttach(
      keepSingle: true,
      targetContext: context,
      targetBuilder: (targetOffset, targetSize) {
        return Offset(targetOffset.dx, targetOffset.dy);
      },
      highlightBuilder: (targetOffset, targetSize) {
        return Positioned(
            top: targetOffset.dy,
            left: targetOffset.dx,
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12.w),
                color: MyColors.colorWhite,
              ),
              width: targetSize.width,
              height: targetSize.height,
            ));
      },
      alignment: Alignment.bottomCenter,
      builder: (context) {
        return Padding(
          padding: EdgeInsets.only(top: 12.w, left: 16.w, right: 16.w),
          child:
              TodoSearchDetail(text: "${_todo.content}\n${_todo.description}"),
        );
      },
      onDismiss: () {
        setState(() {
          todoSearchVisible = false;
        });
      },
    );
  }

  void _onChange(bool? value) {
    if (widget.disabled == true || todoSearchVisible) return;
    setState(() {
      _todo.done = value!;
    });
    widget.onEdit?.call(_todo);
  }

  void _handleEdit() {
    if (widget.disabled == true || todoSearchVisible) return;
    showTodoModal(context, todo: _todo, onConfirm: (_todo) {
      widget.onEdit?.call(_todo);
    }, isEdit: true, onDelete: widget.onDelete);
  }

  @override
  void initState() {
    _todo = widget.todo;
    super.initState();
  }

  @override
  void dispose() {
    SmartDialog.dismiss();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        _handleEdit();
      },
      child: Container(
        width: Get.width,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12.w),
        ),
        child: ListTile(
          contentPadding: EdgeInsets.only(bottom: 6.w),
          minVerticalPadding: 0,
          title: Opacity(
            opacity: widget.highlight == false ? 0.5 : 1,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                if (widget.noIcon != true)
                  widget.icon != null
                      ? Padding(
                          padding: EdgeInsets.only(
                              left: 16.w, top: 12.w, right: 12.w),
                          child: widget.icon,
                        )
                      : Checkbox(
                          value: _todo.done,
                          onChanged: _onChange,
                          fillColor: _todo.done
                              ? WidgetStatePropertyAll(MyColors.primaryColor)
                              : null,
                          checkColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(4.w),
                          ),
                        ),
                Expanded(
                  child: Padding(
                    padding: widget.noIcon == true
                        ? EdgeInsets.only(left: 12.0.w)
                        : const EdgeInsets.all(0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        10.verticalSpace,
                        Text(
                          _todo.content,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                              color: _todo.done
                                  ? MyColors.primaryColor.withOpacity(0.3)
                                  : MyColors.primaryColor,
                              fontSize: 14.sp,
                              decorationColor:
                                  MyColors.primaryColor.withOpacity(0.3),
                              decoration: _todo.done
                                  ? TextDecoration.lineThrough
                                  : null),
                        ),
                        if (_todo.description != null &&
                            _todo.description!.isNotEmpty)
                          Text(_todo.description!,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                  fontSize: 14.sp,
                                  color: _todo.done
                                      ? MyColors.primaryColor.withOpacity(0.3)
                                      : MyColors.primaryColor.withOpacity(0.5),
                                  decoration: _todo.done
                                      ? TextDecoration.lineThrough
                                      : null)),
                        if (_todo.scheduledAt != null &&
                            _todo.scheduledAt!.isNotEmpty)
                          Text(_todo.scheduledAt!,
                              style: TextStyle(
                                fontSize: 14.sp,
                                color: _todo.done
                                    ? MyColors.primaryColor.withOpacity(0.3)
                                    : MyColors.primaryColor.withOpacity(0.5),
                              )),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          trailing: Visibility(
            visible: true,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (widget.extra != null) widget.extra!,
                if (widget.highlight != false && widget.disabled != true)
                  IconButton(
                    onPressed: () {
                      widget.onSearch?.call(_todo);
                      if (widget.highlight == false || todoSearchVisible)
                        return;
                      showTodoSearch(context);
                    },
                    icon: SvgPicture.asset(
                      Assets.imagesTodoRightIcon,
                      width: 18.w,
                      fit: BoxFit.fitWidth,
                    ),
                  )
              ],
            ),
          ),
        ),
      ),
    );
  }
}
