import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:ainote_app/app/widgets/ai_action_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:skeletonizer/skeletonizer.dart';

class LoadingText extends StatelessWidget {
  final String? text;
  const LoadingText({super.key, this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 16.w, top: 8.w),
      child: Stack(
        alignment: Alignment.centerLeft,
        children: [
          Skeletonizer(
            enabled: true,
            enableSwitchAnimation: true,
            child: SizedBox(
              width: double.infinity,
              child: Text(
                generateMockString(10),
                style: TextStyle(fontSize: 24.sp, color: MyColors.thirdColor),
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.only(left: 8.0.w),
            child: AiDoing(
                text: text,
                style: TextStyle(fontSize: 12.sp, color: MyColors.thirdColor),
                color: MyColors.thirdColor),
          )
        ],
      ),
    );
  }
}
