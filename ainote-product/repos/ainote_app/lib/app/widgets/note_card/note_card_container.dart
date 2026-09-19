import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_svg/flutter_svg.dart';

class NoteCardContainer extends StatelessWidget {
  final CardChip title;
  final Widget content;
  final Widget? detail;
  final bool? expanded;
  final VoidCallback? onDelete;

  const NoteCardContainer(
      {super.key,
      required this.title,
      required this.content,
      this.onDelete,
      this.expanded,
      this.detail});

  @override
  Widget build(BuildContext context) {
    void showDetail() {
      showCommonBottomSheet(
          context: context,
          expanded: expanded,
          leading: title,
          trailing: IconButton(
              onPressed: onDelete != null
                  ? () {
                      confirmDelete(context, onDelete: () {
                        onDelete?.call();
                        Future.delayed(Duration(milliseconds: 200), () {
                          SmartDialog.dismiss();
                        });
                      });
                    }
                  : null,
              icon: SvgPicture.asset(
                Assets.imagesDelete,
                width: 20.w,
              )),
          content: Container(
            constraints: BoxConstraints(minHeight: 280.w),
            padding: EdgeInsets.only(top: 16.w, left: 16.w, right: 16.w),
            child: detail ?? content,
          ));
    }

    return GestureDetector(
      onTap: showDetail,
      child: Container(
        width: 240.w,
        height: 170.w,
        margin: EdgeInsets.only(right: 12.w),
        padding: EdgeInsets.only(top: 8.w, left: 12.w, right: 12.w),
        decoration: BoxDecoration(
          color: MyColors.noteCardBgColor,
          borderRadius: BorderRadius.circular(12.w),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [title, 6.verticalSpace, Flexible(child: content)],
        ),
      ),
    );
  }
}
