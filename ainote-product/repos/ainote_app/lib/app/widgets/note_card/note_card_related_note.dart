import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class NoteCardRelatedNote extends StatelessWidget {
  final VoidCallback? onDelete;

  const NoteCardRelatedNote({super.key, this.onDelete});

  @override
  Widget build(BuildContext context) {
    return NoteCardContainer(
      onDelete: onDelete,
      title: CardChip(
          text: '相关备忘录',
          color: MyColors.chipColorPurple,
          backgroundColor: MyColors.chipBgPurple),
      content: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("🚚  搬家前需要做哪些准备？",
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 12.sp,
                  color: MyColors.colorWhite.withOpacity(0.8))),
          10.verticalSpace,
        ],
      ),
    );
  }
}
