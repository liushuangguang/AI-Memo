import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

import '../../../../../../generated/assets.dart';
import '../../../../routes/app_pages.dart';
import '../../../../widgets/dot_grid_container.dart';
import '../../../note/note_edit/controllers/note_edit_controller.dart';

class AddNoteCard extends StatelessWidget {
  const AddNoteCard({super.key});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
        onTap: () {
          Get.toNamed(Routes.NOTE_EDIT,
              arguments: null, parameters: {'mode': NoteEditMode.create.name});
        },
        child: Container(
          padding: EdgeInsets.symmetric(horizontal: 12.w),
          clipBehavior: Clip.hardEdge,
          decoration: BoxDecoration(
            color: MyColors.dotGridColor,
            borderRadius: BorderRadius.circular(12.w),
          ),
          width: 230.w,
          height: 100.h,
          child: DotGridContainer(
              child: SvgPicture.asset(
            Assets.imagesHomeAddNote,
          )),
        ));
  }
}
