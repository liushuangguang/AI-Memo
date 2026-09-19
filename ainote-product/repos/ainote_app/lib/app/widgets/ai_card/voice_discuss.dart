import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/bindings/voice_discuss_binding.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/views/voice_discuss_view.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

class VoiceDiscuss extends StatelessWidget {
  const VoiceDiscuss({super.key, this.note});

  final NoteModel? note;

  @override
  Widget build(BuildContext context) {
    Future<void> openVoiceDiscussion() async {
      final noteEditController = Get.isRegistered<NoteEditController>()
          ? Get.find<NoteEditController>()
          : null;
      final currentNote = note ?? noteEditController?.note.value;
      if (currentNote?.id?.trim().isEmpty ?? true) {
        Toast.info('请先保存当前笔记，再开始语音讨论');
        return;
      }

      final result = await Get.to<Map<String, dynamic>>(
        () => const VoiceDiscussView(),
        binding: VoiceDiscussBinding(),
        arguments: currentNote,
      );
      final refreshedNote = result?['note'];
      if (result?['noteUpdated'] == true &&
          refreshedNote is NoteModel &&
          noteEditController != null) {
        // setNote refreshes the editor and its smart-organize controller. Do not
        // read stale smart-organize state after this boundary.
        noteEditController.setNote(refreshedNote);
      }
    }

    return CardContainer(
      bgColor: MyColors.cardBgGreen,
      borderColor: MyColors.cardBorderGreen,
      onTap: openVoiceDiscussion,
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('语音讨论',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16.sp,
                  color: MyColors.colorGreen)),
          IconButton(
              padding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              tooltip: '开始语音讨论',
              onPressed: openVoiceDiscussion,
              icon: Icon(Icons.arrow_forward_ios_rounded,
                  color: MyColors.colorGreen, size: 16.sp))
        ],
      ),
      content: Text('我们讨论一下你关心的话题吧~',
          style: TextStyle(
              fontWeight: FontWeight.w400,
              fontSize: 14.sp,
              color: MyColors.primaryColor.withValues(alpha: 0.5))),
    );
  }
}
