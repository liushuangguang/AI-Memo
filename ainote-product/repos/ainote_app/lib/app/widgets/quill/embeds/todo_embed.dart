import 'dart:convert' show jsonDecode, jsonEncode;

import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/edit_tool_bar.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:ainote_app/app/widgets/icon_text_action.dart';
import 'package:ainote_app/app/widgets/quill/extensions/quill_embed_ext.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill_extensions/flutter_quill_extensions.dart';

class TodoEmbed extends Embeddable {
  const TodoEmbed(
    String value,
  ) : super(myType, value);

  static const String myType = 'todo';

  static TodoEmbed fromDocument(Document document) =>
      TodoEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class TodoEmbedBuilderWidget extends EmbedBuilder {
  @override
  String get key => 'todo';

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
    IconTextActionModel iconTextData = IconTextActionModel(
        content: '',
        scheduledAt: '',
        done: false,
        description: "",
        noIcon: false);
    try {
      final data = jsonDecode(node.value.data);
      iconTextData = IconTextActionModel.fromJson(data);
    } catch (e) {}
    return IconTextAction(
      data: iconTextData,
      onEdit: (newTodo) {
        iconTextData.description = newTodo.description;
        iconTextData.content = newTodo.content;
        iconTextData.scheduledAt = newTodo.scheduledAt;

        final iconTextDataString = jsonEncode(iconTextData.toJson());

        var offset = node.documentOffset;
        // 删除老的节点
        deleteQuillCustomEmbedNode(controller, node);

        // 将光标移动到该节点
        controller.moveCursorToPosition(offset);
        controller.updateSelection(
          TextSelection.collapsed(
            offset: offset,
          ),
          ChangeSource.local,
        );
        // 插入新的节点
        controller.insertTodoBlock(data: iconTextDataString);
      },
      onDelete: (todo) {
        deleteQuillCustomEmbedNode(controller, node);
      },
    );
  }
}
