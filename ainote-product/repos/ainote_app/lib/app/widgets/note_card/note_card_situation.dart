import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_module/scene_module.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class NoteCardSituation extends StatelessWidget {
  final SceneModule data;
  final VoidCallback? onDelete;

  const NoteCardSituation({super.key, this.onDelete, required this.data});

  @override
  Widget build(BuildContext context) {
    return NoteCardContainer(
        onDelete: onDelete,
        title: CardChip(
            text: '情景记录',
            color: MyColors.chipColorBrown,
            backgroundColor: MyColors.chipBgBrown),
        content: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(data.content ?? '',
                maxLines: 4,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                    fontWeight: FontWeight.w400,
                    fontSize: 10.sp,
                    color: MyColors.colorWhite.withOpacity(0.8))),
          ],
        ),
        detail: Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Align(
            alignment: Alignment.topLeft,
            child: Text(data.content ?? '',
                textAlign: TextAlign.left,
                style: TextStyle(
                  fontSize: 14.sp,
                )),
          ),
        ));
  }
}
