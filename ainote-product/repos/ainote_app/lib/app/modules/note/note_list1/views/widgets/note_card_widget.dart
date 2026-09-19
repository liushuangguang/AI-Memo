import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/note_image.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

import '../../controllers/note_list_controller.dart';

class NoteCardWidget extends StatelessWidget {
  final NoteModel note;

  const NoteCardWidget({super.key, required this.note});

  Widget _labelWidget(int labelType) {
    // 标签Widget，需要修改
    String lableText = "";
    if (labelType == 1) {
      lableText = "置顶";
    } else if (labelType == 2) {
      lableText = "自动整理中";
    } else if (labelType == 3) {
      lableText = "已整理";
    }
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(5),
        color:
            labelType == 1 ? MyColors.pageBackgroundColor : Color(0xffE6E9FF),
      ),
      padding: EdgeInsets.symmetric(horizontal: 4.w, vertical: 2.h),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Visibility(
            visible: labelType == 2,
            child: CupertinoActivityIndicator(
              radius: 6.0,
              animating: true,
              color: Colors.blue,
            ),
          ),
          Text(
            lableText,
            style: TextStyle(
              color: labelType == 1 ? MyColors.thirdColor : MyColors.colorBlue,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<NoteListController>();
    var noteContent = deltaStringToText(note.content ?? '');
    // 去除 json 相关字符串
    noteContent = noteContent.replaceAll(RegExp(r'[{}\n#-*]'), '');

    return GestureDetector(
      onTap: () => Get.toNamed(Routes.NOTE_EDIT,
          arguments: note, parameters: {'mode': NoteEditMode.preview.name}),
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(5),
        ),
        margin: EdgeInsets.symmetric(horizontal: 15.w, vertical: 5.h),
        padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 8.h),
        child: Row(
          children: [
            Visibility(
              visible: note.imageUrl != null,
              child: Container(
                width: 54.w,
                height: 54.h,
                margin: EdgeInsets.only(right: 8.w),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(5),
                ),
                child: Image.network(
                          noteImageUrl(note.imageUrl ?? ""),
                          headers: noteImageHeaders(note.imageUrl ?? ""),
                  width: 54.w,
                  height: 54.w,
                  fit: BoxFit.contain,
                  errorBuilder: (context, error, stackTrace) => Container(),
                ),
              ),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          note.title ?? "无标题",
                          style: TextStyle(
                              fontSize: 16.sp, fontWeight: FontWeight.w700),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Visibility(
                        visible: false, //这里要改成标签类型
                        child: _labelWidget(note.noteType ?? 0),
                      ),
                      8.horizontalSpace,
                      InkWell(
                          onTap: () => showNoteActionSheet(context,
                                  noteId: note.id!, onDelete: () {
                                logic.deleteNote(note.id!);
                              }),
                          child: Icon(Icons.more_horiz, size: 30.w)),
                    ],
                  ),
                  8.verticalSpace,
                  Text(
                    noteContent ?? "(空)",
                    style: TextStyle(
                      fontSize: 14.sp,
                      color: (noteContent ?? "").isEmpty
                          ? MyColors.thirdColor
                          : MyColors.secondaryColor,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  )
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
