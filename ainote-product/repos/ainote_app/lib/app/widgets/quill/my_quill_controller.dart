import 'dart:async';
import 'dart:convert';

import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:flutter/material.dart';
import 'package:flutter_keyboard_visibility_temp_fork/flutter_keyboard_visibility_temp_fork.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:get/get.dart';
import 'package:flutter_quill/flutter_quill.dart';

class MyQuillController extends GetxController {
  final quillController = QuillController.basic();
  final quillFocusNode = FocusNode();
  final quillScrollController = ScrollController();

  final quillIsReadOnly = false.obs;
  final quillIsEmpty = true.obs;

  final quillOnlyView = false.obs;

  // 监听键盘是否弹出
  late StreamSubscription<bool> keyboardSubscription;

  final selectedText = ''.obs;

  @override
  void onInit() {
    initializeQuillDocument();
    super.onInit();
    quillController.readOnly = quillIsReadOnly.value;
    var keyboardVisibilityController = KeyboardVisibilityController();
    keyboardSubscription =
        keyboardVisibilityController.onChange.listen((bool visible) {
      if (visible == false && quillFocusNode.hasFocus) {
        setQuillIsReadOnly(true);
        quillFocusNode.unfocus();
      }
    });

    quillController.onSelectionChanged = (TextSelection textSelection) {
      if (textSelection.isValid) {
        String fullText = quillController.plainTextEditingValue.text;
        selectedText.value = removeNewLinesAndSpaces(
            textSelection.textInside(fullText).replaceAll('￼', ''));
      }
    };
  }

  @override
  void onClose() {
    quillController.dispose();
    quillFocusNode.dispose();
    keyboardSubscription.cancel();
    super.onClose();
  }

  void setQuillOnlyView(bool onlyView) {
    quillController.readOnly = onlyView;
    quillOnlyView.value = onlyView;
    update();
  }

  void setQuillIsReadOnly(bool readOnly) {
    quillIsReadOnly.value = readOnly;
    quillController.readOnly = readOnly;
    setQuillOnlyView(readOnly);
    if (readOnly) {
      quillFocusNode.unfocus();
    } else {
      quillFocusNode.requestFocus();
    }
    update();
  }

  void toggleQuillIsReadOnly() {
    setQuillIsReadOnly(!quillIsReadOnly.value);
    update();
  }

  void setQuillDocument(Document doc) {
    quillController.document = doc;

    quillIsEmpty.bindStream(quillController.document.changes.map((event) {
      return quillController.document
          .toPlainText()
          .replaceAll("\n", "")
          .trim()
          .isEmpty;
    }));
    quillController.moveCursorToEnd();
    update();
  }

  void setQuillDocumentFromJsonString(String data) {
    try {
      setQuillDocument(Document.fromJson(jsonDecode(data)));
    } catch (e) {
      setQuillDocument(Document.fromJson([
        {"insert": "$data\n"}
      ]));
    }
  }

  void setQuillDocumentFromDelta(Delta delta) {
    setQuillDocument(Document.fromDelta(delta));
  }

  void setQuillDocumentFromMarkdown(String markdown) {
    var delta = convertMarkdownToDelta(markdown);
    setQuillDocumentFromDelta(delta);
  }

  /// 初始化 Quill 文档
  void initializeQuillDocument() {
    setQuillDocument(Document.fromJson([
      {"insert": "\n"}
    ]));
  }
}
