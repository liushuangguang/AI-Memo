import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/note_image.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class ThemeRelatedCardWidget extends StatelessWidget {
  final bool isSelected;
  final NoteThemeCandidateModel candidate;

  const ThemeRelatedCardWidget({
    super.key,
    required this.isSelected,
    required this.candidate,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 16.0, right: 8.0),
            child: Container(
              height: 88.w,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
              ),
              padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 8.h),
              child: Row(
                children: [
                  Container(
                    width: 54.w,
                    height: 54.h,
                    margin: EdgeInsets.only(right: 8.w),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(5),
                    ),
                    child: candidate.note.imageUrl?.isNotEmpty == true
                        ? Image.network(
                          noteImageUrl(candidate.note.imageUrl!),
                          headers: noteImageHeaders(candidate.note.imageUrl!),
                            width: 54.w,
                            height: 54.w,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => const Icon(
                              Icons.description_outlined,
                            ),
                          )
                        : const Icon(Icons.description_outlined),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                candidate.note.title?.trim().isNotEmpty == true
                                    ? candidate.note.title!.trim()
                                    : "未命名备忘录",
                                style: TextStyle(
                                    fontSize: 16.sp,
                                    fontWeight: FontWeight.w700),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                        8.verticalSpace,
                        Text(
                          candidate.reason.isNotEmpty
                              ? candidate.reason
                              : (candidate.note.content ?? ''),
                          style: TextStyle(
                            fontSize: 14.sp,
                            color: MyColors.thirdColor,
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
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(right: 16.0),
          child: Container(
            width: 48,
            height: 88.w,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              color: Colors.white,
              border: Border.all(color: MyColors.cardBorderWhite, width: 1),
            ),
            child: Center(
              child: Icon(
                isSelected ? Icons.check_circle : Icons.circle_outlined,
                size: 20.w,
                color: MyColors.colorBlue,
              ),
            ),
          ),
        )
      ],
    );
  }
}
