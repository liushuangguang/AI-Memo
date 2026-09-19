import 'dart:math';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/widgets/card_stack/card_stack.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:card_swiper/card_swiper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get_rx/src/rx_types/rx_types.dart';
import 'package:skeletonizer/skeletonizer.dart';

import '../card_stack/card_stack_swiper.dart';

class RelatedNote extends StatelessWidget {
  final String title;
  final List<AiCategorizedNoteModel> list;
  final bool loading;
  final double? containerWidth;

  const RelatedNote({
    super.key,
    required this.title,
    required this.list,
    required this.loading,
    this.containerWidth,
  });

  @override
  Widget build(BuildContext context) {
    var _list = list;
    if (loading) {
      _list = [
        AiCategorizedNoteModel.mock(),
        AiCategorizedNoteModel.mock(),
        AiCategorizedNoteModel.mock(),
      ];
    }

    Widget buildItemCard(BuildContext context, item) {
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
                  style:
                      TextStyle(fontSize: 16.sp, fontWeight: FontWeight.w500)),
              SizedBox(height: 8.h),
              Text(item.value ?? '',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.left,
                  softWrap: false,
                  style:
                      TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w400)),
            ],
          ))
        ],
      );
    }

    return CardContainer(
      needHighlight: false,
      bgColor: MyColors.cardBgWhite,
      borderColor: MyColors.cardBorderWhite,
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 16.sp,
            ),
          ),
          TextButton(
              onPressed: () {},
              child: Text("保存",
                  style: TextStyle(
                      color: MyColors.colorBlue,
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w500)))
        ],
      ),
      content: CardStackSwiper(
        list: _list,
        loading: loading,
        childBuilder: buildItemCard,
      ),
    );
  }
}
