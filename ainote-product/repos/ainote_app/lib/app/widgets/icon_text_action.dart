import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/widgets/todo_list.dart';
import 'package:flutter/material.dart';

class IconTextAction extends StatefulWidget {
  final IconTextActionModel data;
  final Widget? extra;
  final bool? noIcon;
  final bool? disabled;
  final bool? highlight;
  final Function(TodoModel)? onEdit;
  final Function(TodoModel)? onDelete;

  const IconTextAction({
    super.key,
    required this.data,
    this.extra,
    this.noIcon,
    this.disabled,
    this.highlight,
    this.onEdit,
    this.onDelete,
  });

  @override
  State<IconTextAction> createState() => _IconTextActionState();
}

class _IconTextActionState extends State<IconTextAction> {
  @override
  Widget build(BuildContext context) {
    return Visibility(
      visible: widget.data.content.isNotEmpty,
      child: ToDoItem(
          icon:
              widget.data.emoji != null ? Text(widget.data.emoji ?? '') : null,
          noIcon: widget.data.noIcon,
          disabled: widget.disabled,
          extra: widget.extra,
          highlight: widget.highlight,
          onDelete: widget.onDelete,
          onEdit: widget.onEdit,
          todo: TodoModel(
              content: widget.data.content,
              description: widget.data.description,
              done: widget.data.done == true,
              scheduledAt: widget.data.scheduledAt)),
    );
  }
}

class IconTextActionList extends StatelessWidget {
  final List<IconTextActionModel> list;
  final bool? disabled;
  final bool? loading;

  const IconTextActionList({
    super.key,
    required this.list,
    this.disabled,
    this.loading,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: list.length,
        itemBuilder: (context, index) {
          IconTextActionModel item = list[index];
          if (item.content.isEmpty) return null;
          return IconTextAction(
            data: item,
            noIcon: item.noIcon,
            disabled: disabled,
            highlight: loading != true,
          );
        });
  }
}
