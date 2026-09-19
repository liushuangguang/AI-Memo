import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:get/get_core/src/get_main.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:skeletonizer/skeletonizer.dart';

class AiIllustration extends StatelessWidget {
  final String img;
  final bool loading;
  final VoidCallback? onSave;

  const AiIllustration(
      {super.key, required this.img, required this.loading, this.onSave});

  @override
  Widget build(BuildContext context) {
    void handleViewImage() async {
      showCommonBottomSheet(
          context: context,
          title: 'AI配图',
          leading: ClipOval(
            child: Image.network(
              img,
              width: 20.w,
              height: 20.w,
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
          onSave: onSave,
          expanded: false,
          content: Padding(
            padding: EdgeInsets.only(top: 16.0.w, left: 16.w, right: 16.w),
            child: ClipRRect(
              borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(12.w),
                  topRight: Radius.circular(12.w)),
              child: Image.network(
                img,
                width: Get.width,
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
          ));
    }

    return CardContainer(
      onTap: handleViewImage,
      bgColor: MyColors.cardBgWhite,
      borderColor: MyColors.cardBorderWhite,
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('AI配图',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16.sp,
                  color: MyColors.primaryColor)),
          IconButton(
              padding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              onPressed: handleViewImage,
              icon: Icon(Icons.arrow_forward_ios_rounded,
                  color: MyColors.primaryColor, size: 16.sp))
        ],
      ),
      content: loading
          ? SizedBox(
              width: Get.width,
              height: 80.w,
              child: Skeletonizer(
                enabled: loading,
                enableSwitchAnimation: true,
                child: Column(
                  children: [
                    Text(generateMockString(44)),
                  ],
                ),
              ))
          : ClipRRect(
              borderRadius: BorderRadius.circular(4.w),
              clipBehavior: Clip.hardEdge,
              child: Image.network(
                img,
                width: Get.width,
                height: 80.w,
                fit: BoxFit.fitWidth,
                frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
                  if (frame == null) {
                    return SizedBox(
                      width: Get.width,
                      height: 80.w,
                    );
                  }
                  return child;
                },
                errorBuilder: (context, error, stackTrace) {
                  return SizedBox(
                    width: Get.width,
                    height: 80.w,
                  );
                },
              ),
            ),
    );
  }
}
