import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/widgets/card_stack/card_stack_swiper.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class NoteCardCategory extends StatelessWidget {
  final AiCategorizedNoteModel data;
  final VoidCallback? onDelete;
  const NoteCardCategory({super.key, required this.data, this.onDelete});

  @override
  Widget build(BuildContext context) {
    return NoteCardContainer(
      onDelete: onDelete,
      title: CardChip(
          text: '信息归类',
          color: MyColors.chipColorBlue,
          backgroundColor: MyColors.chipBgBlue),
      content: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("已自动归类信息到${data.items?.length ?? 0}类系统备忘录中",
                style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 12.sp,
                    color: MyColors.colorWhite.withOpacity(0.8))),
            10.verticalSpace,
            Transform.translate(
              offset: Offset(0, -20.w),
              child: Transform.scale(
                scaleY: 0.8,
                child: CardStackSwiper(
                  cardBgColor: Color(0xFF5C556D),
                  cardWidth: 200.w,
                  list: [
                    data,
                  ],
                  loading: false,
                  childBuilder: (BuildContext context, item) {
                    return Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                            child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(item.key ?? '',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                textAlign: TextAlign.left,
                                softWrap: false,
                                style: TextStyle(
                                    fontSize: 12.sp,
                                    fontWeight: FontWeight.w500,
                                    color: MyColors.colorWhite)),
                            SizedBox(height: 4.h),
                            Text(item.value ?? '',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                textAlign: TextAlign.left,
                                softWrap: false,
                                style: TextStyle(
                                    fontSize: 10.sp,
                                    fontWeight: FontWeight.w400,
                                    color: MyColors.colorWhite)),
                          ],
                        ))
                      ],
                    );
                  },
                ),
              ),
            ),
          ],
        ),
      ),
      detail: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text("已自动归类信息到${data.items?.length ?? 0}类系统备忘录中",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14.sp,
              )),
          10.verticalSpace,
          CardStackSwiper(
            list: [
              data,
            ],
            loading: false,
            childBuilder: (BuildContext context, item) {
              return Row(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                      child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(item.key ?? '',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.left,
                          softWrap: false,
                          style: TextStyle(
                              fontSize: 16.sp, fontWeight: FontWeight.w500)),
                      SizedBox(height: 8.h),
                      Text(item.value ?? '',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.left,
                          softWrap: false,
                          style: TextStyle(
                              fontSize: 14.sp, fontWeight: FontWeight.w400)),
                    ],
                  ))
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}
