import 'dart:convert' show jsonDecode;

import 'package:ainote_app/app/widgets/dashed_line.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_quill/flutter_quill.dart';

class DashedLineEmbed extends Embeddable {
  const DashedLineEmbed() : super(myType, '');

  static const String myType = 'dashed_line';

  static DashedLineEmbed fromDocument(Document document) => DashedLineEmbed();

  Document get document => Document.fromJson(jsonDecode(data));
}

class DashedLineEmbedBuilderWidget extends EmbedBuilder {
  @override
  String get key => 'dashed_line';

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
    return DashedLine();
  }
}
