import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:markdown/markdown.dart' as md;
import 'package:markdown_quill/markdown_quill.dart';

import '../data/models/todo_model.dart';
import '../widgets/todo_sheet.dart';
import 'date_format.dart';

void showTodoModal(
  context, {
  TodoModel? todo,
  Function(TodoModel)? onConfirm,
  Function(TodoModel)? onDelete,
  isEdit = false,
}) {
  showCupertinoModalBottomSheet(
    context: context,
    enableDrag: false,
    useRootNavigator: true,
    builder: (context) {
      return TodoSheet(
        isEdit: isEdit,
        todoContent: todo?.content,
        todoDesc: todo?.description,
        todoDate: parseDateString(todo?.scheduledAt ?? ''),
        onDelete: () {
          onDelete?.call(todo!);
        },
        onTodoCreated: (content, date, desc) {
          final _todo = TodoModel(
            id: todo?.id ?? '',
            content: content ?? "",
            description: desc ?? "",
            scheduledAt: date != null ? formatDate(date) : '',
            done: todo?.done ?? false,
          );
          onConfirm?.call(_todo);
        },
      );
    },
  );
}

void updateQuillCustomEmbedNode(
    QuillController controller, Embed node, Object fromDocument) {
  controller.replaceText(
    node.offset,
    node.length,
    fromDocument,
    TextSelection.collapsed(offset: node.offset),
  );
}

void deleteQuillCustomEmbedNode(QuillController controller, Embed node) {
  controller.document.delete(
    node.documentOffset,
    node.length,
  );
}

void deletePreviousNode(QuillController controller, Embed node) {
  if (node.previous is Node) {
    controller.document.delete(
      node.previous!.documentOffset,
      node.previous!.length,
    );
  }
}

void replaceNode(QuillController controller, Embed node, Object data) {
  controller.replaceText(
    node.offset,
    node.length,
    data,
    TextSelection.collapsed(offset: node.offset),
  );
}

void insertQuillCustomEmbedNode(QuillController controller, Embeddable data,
    [noNewLine = false]) {
  controller.updateSelection(
    TextSelection.collapsed(
      offset: controller.selection.extentOffset + 1,
    ),
    ChangeSource.local,
  );

  controller.document.insert(controller.selection.extentOffset, data);

  controller.updateSelection(
    TextSelection.collapsed(
      offset: controller.selection.extentOffset + 1,
    ),
    ChangeSource.local,
  );

  controller.document.insert(controller.selection.extentOffset, ' ');

  if (noNewLine) {
    return;
  }

  controller.updateSelection(
    TextSelection.collapsed(
      offset: controller.selection.extentOffset + 1,
    ),
    ChangeSource.local,
  );

  controller.document.insert(controller.selection.extentOffset, '\n');
  controller.updateSelection(
    TextSelection.collapsed(
      offset: controller.selection.extentOffset + 1,
    ),
    ChangeSource.local,
  );
}

String getQuillJsonString(QuillController controller) {
  return jsonEncode(controller.document.toDelta().toJson());
}

String getNoteTitle(QuillController controller) {
  var noteText = controller.document.toPlainText().trim().replaceAll('￼', '');
  return noteText
      .substring(0, noteText.length > 13 ? 13 : noteText.length)
      .split('\n')[0];
}

Delta convertMarkdownToDelta(String markdown) {
  if (markdown.isEmpty) {
    return Delta();
  }
  try {
    final mdDocument = md.Document(encodeHtml: false);
    final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);

    final delta = mdToDelta.convert(markdown);

    return delta;
  } catch (e) {
    return Delta();
  }
}

String convertDeltaToMarkdown(Delta delta) {
  final deltaToMd = DeltaToMarkdown();
  final markdown = deltaToMd.convert(delta);
  return markdown;
}

// delta -> text
String deltaToText(Delta delta) {
  return delta.toList().map((e) => e.data).join();
}

// delta string -> text
String deltaStringToText(String deltaString) {
  try {
    final delta = Delta.fromJson(jsonDecode(deltaString));
    return deltaToText(delta);
  } catch (e) {
    return deltaString;
  }
}

String removeNewLinesAndSpaces(String input) {
  return input.replaceAll(RegExp(r'\s+\b\b|\b\s+'), '');
}
