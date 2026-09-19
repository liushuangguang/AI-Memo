import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';

import '../classes/dashed_line_painter.dart';

class DividerBlockEmbed extends CustomBlockEmbed {
  const DividerBlockEmbed(String value) : super(noteType, value);

  static const String noteType = 'divider';

  static DividerBlockEmbed fromDocument(Document document) =>
      DividerBlockEmbed(jsonEncode(document.toDelta().toJson()));

  Document get document => Document.fromJson(jsonDecode(data));
}

class DividerEmbedBuilder extends EmbedBuilder {

  @override
  String get key => DividerBlockEmbed.noteType;

  @override
  Widget build(
      BuildContext context,
      QuillController controller,
      Embed node,
      bool readOnly,
      bool inline,
      TextStyle textStyle,
      ) {

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16.0),
      child: DashedLine(),
    );
  }
}



