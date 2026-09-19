import 'dart:convert';

import 'package:ainote/constants/app_images.dart';
import 'package:ainote/models/editor_todo_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/markdown_quill.dart';
import 'package:markdown/markdown.dart' as md;


class TodoBlockEmbed extends CustomBlockEmbed {
  const TodoBlockEmbed(String value) : super(noteType, value);

  static const String noteType = 'list';

  static TodoBlockEmbed fromDocument(Document document) =>
      TodoBlockEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class TodoEmbedBuilder extends EmbedBuilder {

  @override
  String get key => TodoBlockEmbed.noteType;

  @override
  Widget build(
      BuildContext context,
      QuillController controller,
      Embed node,
      bool readOnly,
      bool inline,
      TextStyle textStyle,
      ) {
    final notes = TodoBlockEmbed(node.value.data).document.toPlainText();
    final todo = EditorTodoModel.fromJson(jsonDecode(notes));
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            behavior: HitTestBehavior.translucent,
            onTap: () {
              final checked = !todo.isChecked;
              final newTodo = EditorTodoModel(
                isChecked: checked,
                item: todo.item,
                dateString: todo.dateString,
              );
              final newTodoJson = jsonEncode(newTodo.toJson());
              final mdDocument = md.Document(encodeHtml: false);
              final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);
              final delta = mdToDelta.convert(newTodoJson);
              final block = BlockEmbed.custom(
                TodoBlockEmbed.fromDocument(Document.fromDelta(delta)),
              );
              final offset = getEmbedNode(controller, controller.selection.start).offset;
              controller.replaceText(
                  offset, 1, block, TextSelection.collapsed(offset: offset));
            },
            child: Padding(
              padding: const EdgeInsets.only(right: 16.0),
              child: Image.asset(
                todo.isChecked ? AppImages.todoCheckedIcon.path : AppImages.todoUncheckedIcon.path,
                width: 20,
                height: 20,
              ),
            ),
          ),
          Expanded(
            child: Text(
              todo.item,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w400,
                color: Color(0xFF4C4A5C),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 13),
            child: Text(
              todo.dateString,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w400,
                color: Color(0xFF4C4A5C),
              ),
            ),
          ),
        ],
      ),
    );
  }
}



