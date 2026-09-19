import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill_extensions/flutter_quill_extensions.dart'
    show QuillSharedExtensionsConfigurations;
import 'package:get/get.dart';

import 'my_quill_editor.dart';
import 'my_quill_toolbar.dart';

@immutable
class QuillScreenArgs {
  const QuillScreenArgs({required this.document});

  final Document document;
}

class QuillScreen extends StatefulWidget {
  const QuillScreen({
    required this.args,
    super.key,
  });

  final QuillScreenArgs args;

  @override
  State<QuillScreen> createState() => _QuillScreenState();
}

class _QuillScreenState extends State<QuillScreen> {
  var logic = Get.find<MyQuillController>(tag: 'screen');
  var _isReadOnly = false;

  @override
  void initState() {
    super.initState();
    logic.quillController.document = widget.args.document;
  }

  @override
  void dispose() {
    logic.quillController.dispose();
    logic.quillFocusNode.dispose();
    logic.quillScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    logic.quillController.readOnly = _isReadOnly;
    return Column(
      children: [
        if (!_isReadOnly)
          MyQuillToolbar(
            controller: logic.quillController,
            focusNode: logic.quillFocusNode,
          ),
        Builder(
          builder: (context) {
            return Expanded(
              child: MyQuillEditor(
                autoFocus: true,
                configurations: QuillEditorConfigurations(
                  characterShortcutEvents: standardCharactersShortcutEvents,
                  spaceShortcutEvents: standardSpaceShorcutEvents,
                  searchConfigurations: const QuillSearchConfigurations(
                    searchEmbedMode: SearchEmbedMode.plainText,
                  ),
                  sharedConfigurations: _sharedConfigurations,
                ),
                tag: 'screen',
              ),
            );
          },
        ),
      ],
    );
  }

  QuillSharedConfigurations get _sharedConfigurations {
    return QuillSharedConfigurations(
      // locale: Locale('en'),
      extraConfigurations: {
        QuillSharedExtensionsConfigurations.key:
            QuillSharedExtensionsConfigurations(
          assetsPrefix: 'assets', // Defaults to assets
        ),
      },
    );
  }
}
