import 'package:ainote_app/app/modules/home/views/widgets/card_wrapper.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_editor.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../controllers/home_controller.dart';

class NoteCard extends StatelessWidget {
  const NoteCard({
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<HomeController>();

    return CardWrapper(
        title: '上次编辑',
        onTap: logic.toEditNote,
        child: SingleChildScrollView(
          scrollDirection: Axis.vertical,
          // physics: const NeverScrollableScrollPhysics(),
          child: Stack(
            children: [
              MyQuillEditor(
                autoFocus: false,
                tag: logic.quillTag,
              ),
              Container(
                width: double.infinity,
                height: 200,
                color: Colors.transparent,
              )
            ],
          ),
        ));
  }
}
