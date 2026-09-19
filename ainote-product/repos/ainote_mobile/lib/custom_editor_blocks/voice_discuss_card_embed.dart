import 'dart:convert' show jsonDecode, jsonEncode;

import 'package:ainote/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';

import '../screens/ai_voice_discuss/ai_voice_result_card.dart';
import '../utils/utils.dart';

class VoiceDiscussCardEmbed extends Embeddable {
  const VoiceDiscussCardEmbed(
    String value,
  ) : super(noteType, value);

  static const String noteType = 'voice_discuss_card';

  static VoiceDiscussCardEmbed fromDocument(Document document) =>
      VoiceDiscussCardEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class VoiceDiscussCardEmbedBuilder extends EmbedBuilder {
  @override
  String get key => 'voice_discuss_card';

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

    final id = jsonDecode(node.value.data);

    return AiVoiceResultCard(
      onDelete: () {
        deleteQuillCustomEmbedNode(controller, node);
        APIService.deleteNoteDiscussAudioById(id);
      },
    );
  }
}
