import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/home/controllers/home_controller.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:marquee/marquee.dart';
import 'package:skeletonizer/skeletonizer.dart';

import '../../controllers/note_edit_controller.dart';
import 'note_type_dropdown.dart';

class NoteHeader extends StatelessWidget {
  final String? title;
  final int? type;
  final bool? loading;
  final bool? disabled;
  final bool? isThemeLoading;
  const NoteHeader(
      {super.key,
      this.title,
      this.type,
      this.loading,
      this.disabled,
      this.isThemeLoading});

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<NoteEditController>();
    final homeLogic = Get.find<HomeController>();

    var _title = title ?? logic.note.value.title ?? '暂无标题';
    return Stack(
      children: [
        SizedBox(
          width: double.infinity,
          height: 58.w,
        ),
        Positioned(
          bottom: 0,
          left: 0,
          right: 0,
          child: Container(
            width: double.infinity,
            height: 50.h,
            decoration: BoxDecoration(
              color: MyColors.colorYellow,
              borderRadius: BorderRadius.only(
                topLeft: Radius.circular(12.w),
                topRight: Radius.circular(12.w),
              ),
            ),
          ),
        ),
        Positioned(
          bottom: 0,
          right: 0,
          child: Container(
            width: 12.w,
            height: 50.h,
            decoration: BoxDecoration(
              color: MyColors.colorYellowDark,
              borderRadius: BorderRadius.only(
                bottomLeft: Radius.circular(12.w),
                topRight: Radius.circular(12.w),
              ),
            ),
          ),
        ),
        Positioned(
            top: 8.w,
            right: 0,
            child: Container(
              width: 130.w,
              height: 38.h,
              decoration: BoxDecoration(
                color: MyColors.colorYellowDark,
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(12.w),
                  topRight: Radius.circular(12.w),
                ),
              ),
            )),
        Positioned(
          top: 0,
          left: 0,
          child: Container(
              width: 230.w,
              height: 46.h,
              decoration: BoxDecoration(
                color: MyColors.colorYellow,
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(12.w),
                  topRight: Radius.circular(12.w),
                ),
              ),
              child: Skeletonizer(
                enabled: loading == true,
                enableSwitchAnimation: true,
                child: Center(
                  child: Container(
                    padding: EdgeInsets.only(left: 16.w, right: 16.w),
                    width: double.infinity,
                    child: isThemeLoading == true
                        ? AutoSizeText("标题",
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                            style: TextStyle(
                              fontSize: 16.sp,
                              fontWeight: FontWeight.bold,
                              color: MyColors.colorBrown.withOpacity(0.5),
                            ))
                        : _title.length < 14
                            ? AutoSizeText(_title,
                                overflow: TextOverflow.ellipsis,
                                maxLines: 1,
                                style: TextStyle(
                                  fontSize: 16.sp,
                                  fontWeight: FontWeight.bold,
                                  color: MyColors.colorBrown,
                                ))
                            : Marquee(
                                text: _title,
                                fadingEdgeEndFraction: 0.1,
                                style: TextStyle(
                                  fontSize: 16.sp,
                                  fontWeight: FontWeight.bold,
                                  color: MyColors.colorBrown,
                                )),
                  ),
                ),
              )),
        ),
        Positioned(
            right: 0,
            top: 0,
            child: SizedBox(
                width: 130.w,
                child: GestureDetector(
                  onTap: () {
                    logic.quillLogic.quillFocusNode.unfocus();
                  },
                  child: Obx(() => NoteTagDropdown(
                        disabled: disabled,
                        loading: homeLogic.noteTypesLoading.value,
                        list: homeLogic.noteTypeList.toList(),
                        value: type ?? logic.note.value.noteType,
                        onChang: (value) {
                          logic.updateNoteType(value);
                        },
                      )),
                ))),
      ],
    );
  }
}
