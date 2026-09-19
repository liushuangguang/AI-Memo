import 'dart:convert';

import 'package:ainote_app/app/widgets/quill/embeds/dashed_line_embed.dart';
import 'package:ainote_app/app/widgets/quill/embeds/todo_embed.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/quill_delta.dart';

import '../embeds/ai_suggestion_embed.dart';

/// Extension functions on [QuillController]
/// that make it easier to insert the embed blocks
///
/// and provide some other extra utilities
extension QuillControllerExt on QuillController {
  int get index => selection.baseOffset;
  int get length => selection.extentOffset - index;

  /// insert delta
  void insertDelta(Delta delta) {
    this
      ..skipRequestKeyboard = true
      ..replaceText(
        index,
        delta.length,
        delta,
        null,
      )
      ..moveCursorToPosition(index + delta.length);
  }

  /// Insert iconTextData embed block, it requires the [iconTextDataString]
  void insertTodoBlock({
    required String data,
  }) {
    this
      ..skipRequestKeyboard = true
      ..replaceText(
        index,
        length,
        TodoEmbed(data),
        null,
      )
      ..moveCursorToPosition(index + 1);
  }

  /// Insert DashedLine block
  void insertDashedLineBlock() {
    this
      ..skipRequestKeyboard = true
      ..replaceText(
        index,
        length,
        DashedLineEmbed(),
        null,
      )
      ..moveCursorToPosition(index + 1);
  }

  /// Insert AiSuggestion block
  void insertAiSuggestionBlock({
    required String data,
  }) {
    this
      ..skipRequestKeyboard = true
      ..replaceText(
        index,
        length,
        AiSuggestionEmbed(data),
        null,
      )
      ..moveCursorToPosition(index + 1);
  }
}
