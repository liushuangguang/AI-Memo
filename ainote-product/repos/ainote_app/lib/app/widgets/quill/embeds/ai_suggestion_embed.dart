import 'dart:convert' show jsonDecode, jsonEncode;

import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/views/widgets/ai_suggestion.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:ainote_app/app/widgets/quill/extensions/quill_embed_ext.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_quill/flutter_quill.dart';

class AiSuggestionEmbed extends Embeddable {
  const AiSuggestionEmbed(
    String value,
  ) : super(myType, value);

  static const String myType = 'ai_suggestion';

  static AiSuggestionEmbed fromDocument(Document document) =>
      AiSuggestionEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class AiSuggestionEmbedBuilderWidget extends EmbedBuilder {
  @override
  String get key => 'ai_suggestion';

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
    List<IconTextActionModel> list = [];
    try {
      list = (jsonDecode(node.value.data) as List)
          .map<IconTextActionModel>((e) => IconTextActionModel.fromJson(e))
          .toList();
    } catch (e) {}
    return AiSuggestion(
      loading: false,
      disabled: true,
      extra: true,
      list: list,
      onDelete: () {
        deletePreviousNode(controller, node);
        deleteQuillCustomEmbedNode(controller, node);
        // 收起键盘
        FocusManager.instance.primaryFocus?.unfocus();
      },
      onSaveAll: () {
        // 删除当前节点
        controller.moveCursorToPosition(node.previous!.documentOffset);
        deletePreviousNode(controller, node);
        deleteQuillCustomEmbedNode(controller, node);

        // 插入到正文
        for (var e in list) {
          controller.insertTodoBlock(data: jsonEncode(e.toJson()));
        }
        // 收起键盘
        FocusManager.instance.primaryFocus?.unfocus();
      },
      onSaveOne: (d) {
        // 删除当前节点
        var prev = node.previous;
        controller.moveCursorToPosition(node.documentOffset);
        deleteQuillCustomEmbedNode(controller, node);

        // 插入到正文
        controller.moveCursorToPosition(prev!.documentOffset - 1);
        controller.insertTodoBlock(data: jsonEncode(d.toJson()));
        controller.moveCursorToEnd();

        // 删除当前数据
        list.remove(d);
        // 插入新数据
        controller.insertAiSuggestionBlock(
            data: jsonEncode(list.map((e) => e.toJson()).toList()));

        // 收起键盘
        FocusManager.instance.primaryFocus?.unfocus();
      },
      rawStr: '',
    );
  }
}
