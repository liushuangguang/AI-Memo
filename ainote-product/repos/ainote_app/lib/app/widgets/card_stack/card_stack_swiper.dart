import 'dart:math';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/widgets/card_stack/model/card_model.dart';
import 'package:ainote_app/app/widgets/card_stack/model/card_orientation.dart';
import 'package:ainote_app/app/widgets/card_stack/widget/card_stack_widget.dart';
import 'package:card_swiper/card_swiper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:skeletonizer/skeletonizer.dart';

class CardStackSwiper extends StatelessWidget {
  final List<dynamic> list;
  final bool loading;
  final Widget Function(BuildContext ctx, dynamic item) childBuilder;
  final double? cardWidth;
  final Color? cardBgColor;

  const CardStackSwiper(
      {super.key,
      required this.list,
      required this.loading,
      required this.childBuilder,
      this.cardWidth,
      this.cardBgColor});

  @override
  Widget build(BuildContext context) {
    var maxLength = 0;
    for (var item in list) {
      maxLength = max(maxLength, item.items?.length ?? 0);
    }

    CardModel buildItemCard(BuildContext buildContext, item) {
      final double containerWidth =
          cardWidth ?? MediaQuery.of(buildContext).size.width - 64.w;
      return CardModel(
        backgroundColor: MyColors.cardBgWhite,
        radius: Radius.circular(8.0.w),
        shadowColor: Colors.black.withOpacity(0),
        child: Skeletonizer(
          enabled: loading,
          enableSwitchAnimation: true,
          child: Container(
            width: containerWidth,
            padding: EdgeInsets.all(12.0.w),
            clipBehavior: Clip.hardEdge,
            decoration: BoxDecoration(
              color: cardBgColor ?? MyColors.cardBgWhite,
              borderRadius: BorderRadius.circular(8.0.w),
              border: Border.all(
                color:
                    cardBgColor?.withOpacity(0.5) ?? MyColors.cardBorderWhite,
              ),
            ),
            child: childBuilder(context, item),
          ),
        ),
      );
    }

    CardStackWidget buildCardStackWidget(BuildContext context, int index) {
      var items = list[index].items ?? [];
      return CardStackWidget.builder(
        count: items.length,
        builder: (index) => buildItemCard(context, items[index]),
        opacityChangeOnDrag: true,
        swipeOrientation: CardOrientation.both,
        cardDismissOrientation: CardOrientation.both,
        positionFactor: 0.8,
        scaleFactor: 1.2,
        alignment: Alignment.center,
        reverseOrder: true,
        reverseSize: true,
        dismissedCardDuration: const Duration(milliseconds: 300),
      );
    }

    return SizedBox(
      height: 100.h + maxLength * 10.h,
      width: double.infinity,
      child: Swiper(
          itemBuilder: (BuildContext context, int index) {
            return Container(
              padding: EdgeInsets.only(bottom: 16.w),
              child: buildCardStackWidget(context, index),
            );
          },
          itemCount: list.length,
          viewportFraction: 1,
          loop: false,
          autoplay: list.length > 1,
          autoplayDisableOnInteraction: true,
          autoplayDelay: 10000,
          pagination: list.length > 1
              ? SwiperPagination(
                  margin: EdgeInsets.only(top: 16.w),
                  builder: DotSwiperPaginationBuilder(
                      size: 4.w,
                      activeSize: 5.w,
                      color: MyColors.colorRed.withOpacity(0.5),
                      activeColor: MyColors.colorRed.withOpacity(0.8)),
                )
              : null),
    );
  }
}
