import 'package:flutter/material.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../../models/todo_model.dart';
import '../../sheets/todo_sheet.dart';
import '../../utils/utils.dart';

class TodoList extends StatefulWidget {
  final List<ToDoItem> list;
  final VoidCallback? onDelete;
  final VoidCallback? onUpdate;

  const TodoList({
    super.key,
    required this.list,
    this.onDelete,
    this.onUpdate,
  });

  @override
  State<TodoList> createState() => _TodoListState();
}

class _TodoListState extends State<TodoList> {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: widget.list,
    );
  }
}

class ToDoItem extends StatefulWidget {
  final TodoModel todo;
  final bool? disabled;
  final Function(TodoModel)? onEdit;

  const ToDoItem(
      {super.key, required this.todo, this.onEdit, this.disabled = false});

  @override
  State<ToDoItem> createState() => _ToDoItemState();
}

class _ToDoItemState extends State<ToDoItem> {
  TodoModel _todo = TodoModel.fromJson({});

  void _onChange(bool? value) {
    if (widget.disabled!) return;
    setState(() {
      _todo.done = value!;
    });
    widget.onEdit?.call(_todo);
  }

  void _handleEdit() {
    showCupertinoModalBottomSheet(
      context: context,
      builder: (context) {
        return TodoSheet(
          isEdit: true,
          todoContent: _todo.title,
          todoDate: parseDateString(_todo.date),
          onTodoCreated: (todoContent, todoDate) {
            setState(() {
              _todo.title = todoContent ?? "";
              _todo.date = todoDate != null
                  ? formatDateTimeFromDateTime(todoDate, showYear: true)
                  : '';
            });
            widget.onEdit?.call(_todo);
          },
        );
      },
    );
  }

  @override
  void initState() {
    _todo = widget.todo;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    const color = Color(0xFF4C4A5C);
    return InkWell(
      onTap: () {
        _handleEdit();
      },
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.transparent,
          borderRadius: BorderRadius.all(Radius.circular(16)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.max,
          children: [
            Flexible(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Checkbox(
                    value: _todo.done,
                    onChanged: _onChange,
                    fillColor: _todo.done
                        ? WidgetStatePropertyAll(primaryColor)
                        : null,
                    activeColor: color,
                    checkColor: Colors.white,
                    shape: const CircleBorder(
                      side: BorderSide(color: color),
                    ),
                  ),
                  Flexible(
                    child: Text(_todo.title,
                        textAlign: TextAlign.left,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: color, fontSize: 14)),
                  ),
                  const SizedBox(width: 4),
                ],
              ),
            ),
            Text(_todo.date,
                textAlign: TextAlign.right,
                style: const TextStyle(color: color, fontSize: 14)),
          ],
        ),
      ),
    );
  }
}
