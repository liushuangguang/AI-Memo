import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/note_image.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class NoteCardIllustration extends StatelessWidget {
  final String img;
  final VoidCallback? onDelete;

  const NoteCardIllustration({super.key, required this.img, this.onDelete});

  @override
  Widget build(BuildContext context) {
    return NoteCardContainer(
      onDelete: onDelete,
      title: CardChip(
          text: 'AI配图',
          color: MyColors.chipColorGreen,
          backgroundColor: MyColors.chipBgGreen),
      content: ClipRRect(
        borderRadius: BorderRadius.only(
            topLeft: Radius.circular(12.w), topRight: Radius.circular(12.w)),
        child: Image.network(
                          noteImageUrl(img),
                          headers: noteImageHeaders(img),
          width: double.infinity,
          fit: BoxFit.fitWidth,
          frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
            if (frame == null) {
              return const SizedBox.shrink();
            }
            return child;
          },
          errorBuilder: (context, error, stackTrace) {
            return SizedBox.shrink();
          },
        ),
      ),
    );
  }
}
