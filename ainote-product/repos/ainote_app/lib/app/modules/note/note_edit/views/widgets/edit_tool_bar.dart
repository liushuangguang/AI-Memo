import 'dart:convert';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/views/smart_organize_view.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_toolbar.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill_extensions/flutter_quill_extensions.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

import '../../../../../../generated/assets.dart';
import '../../../../../widgets/retry_btn.dart';
import '../../controllers/note_edit_controller.dart';
import 'note_analysis_progress.dart';

class EditToolBar extends StatelessWidget {
  const EditToolBar({super.key});

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<NoteEditController>();
    final viewInsetsBottom = MediaQuery.of(context).viewInsets.bottom;

    return Obx(() {
      return Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Visibility(
            visible: logic.aiAnalysisVisible.value && viewInsetsBottom == 0,
            child: Container(
              constraints: BoxConstraints(minWidth: Get.width),
              padding: EdgeInsets.only(left: 16.w, right: 16.w, top: 12.w),
              child: Flex(
                direction: Axis.horizontal,
                children: [
                  Expanded(
                    flex: 2,
                    child: Container(
                      height: 38.w,
                      clipBehavior: Clip.hardEdge,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12.w),
                      ),
                      child: GestureDetector(
                        onTap: () async {
                          if (!logic.noteHasSaved.value) {
                            if (!await logic.saveNote(true)) return;
                            await logic.startSmartOrganize();
                          }
                          if (!context.mounted) return;
                          showCupertinoDialog(
                            context: context,
                            builder: (context) {
                              return SmartOrganizeView();
                            },
                          );
                        },
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            NoteAnalysisProgress(
                                done: logic.aiAnalysisDone.value),
                            SingleChildScrollView(
                              scrollDirection: Axis.horizontal,
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  8.horizontalSpace,
                                  SvgPicture.asset(Assets.imagesAiProgressIcon),
                                  8.horizontalSpace,
                                  logic.aiAnalysisDone.value
                                      ? Text.rich(
                                          TextSpan(
                                            text: '智能整理完成',
                                            style:
                                                TextStyle(color: Colors.white),
                                            children: [
                                              TextSpan(
                                                text:
                                                    '（${logic.aiAnalysisTime.value}）',
                                                style: TextStyle(
                                                    fontSize: 12.sp,
                                                    color: Colors.white
                                                        .withOpacity(0.5)),
                                              ),
                                            ],
                                          ),
                                        )
                                      : Text(
                                          '智能整理中',
                                          style: TextStyle(
                                              fontSize: 12.sp,
                                              color: Colors.white),
                                        ),
                                  Icon(
                                    Icons.keyboard_arrow_right,
                                    color: Colors.white,
                                    size: 20.sp,
                                  ),
                                  6.horizontalSpace,
                                ],
                              ),
                            )
                          ],
                        ),
                      ),
                    ),
                  ),
                  if (logic.aiAnalysisDone.value)
                    Expanded(
                        flex: 1,
                        child: RetryBtn(
                          onTap: logic.retryAnalysis,
                          textColor: Colors.white,
                          backgroundColor: Color(0xFF332D41),
                          height: 38.w,
                          margin: EdgeInsets.only(left: 8.w),
                        ))
                ],
              ),
            ),
          ),
          Visibility(
            visible: !logic.quillLogic.quillIsReadOnly.value &&
                logic.editorToolVisible.value,
            child: Container(
              width: Get.width,
              decoration: BoxDecoration(
                color: MyColors.dotGridColor,
              ),
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: MyQuillToolbar(
                  controller: logic.quillLogic.quillController,
                  focusNode: logic.quillLogic.quillFocusNode,
                ),
              ),
            ),
          ),
          AnimatedOpacity(
            opacity: logic.bottomToolVisible.value ? 1 : 0,
            duration: Duration(milliseconds: 200),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Container(
                height: 48.w,
                constraints: BoxConstraints(minWidth: Get.width),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    for (var d in logic.toolBarItems) ...[
                      if (d['id'] == 'image')
                        QuillToolbar(
                          configurations: const QuillToolbarConfigurations(),
                          child: QuillToolbarImageButton(
                            controller: logic.quillLogic.quillController,
                            options: QuillToolbarImageButtonOptions(
                                childBuilder: (options, extraOptions) {
                              return IconButton(
                                  onPressed: d['disabled']
                                      ? null
                                      : extraOptions.onPressed,
                                  icon: Image.asset(
                                    height: 20.w,
                                    d['disabled'] ? d['icon'] : d['activeIcon'],
                                  ));
                            }),
                          ),
                        ),
                      if (d['id'] == 'text')
                        IconButton(
                            onPressed: d['disabled'] ? null : d['onTap'],
                            icon: Image.asset(
                              height: 20.w,
                              d['disabled'] ? d['icon'] : d['activeIcon'],
                            )),
                      if (d['id'] != 'image' && d['id'] != 'text')
                        IconButton(
                            onPressed: d['disabled'] ? null : d['onTap'],
                            icon: Image.asset(
                              height: 20.w,
                              d['disabled'] ? d['icon'] : d['activeIcon'],
                            )),
                      if (logic.toolBarItems.last != d)
                        VerticalDivider(
                          color: Color.fromRGBO(215, 215, 215, 0.3),
                          indent: 20.w,
                          endIndent: 20.w,
                        ),
                    ]
                  ],
                ),
              ),
            ),
          ),
        ],
      );
    });
  }
}
