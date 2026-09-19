import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../../../generated/assets.dart';

class NoteEmptyCard extends StatelessWidget {
  const NoteEmptyCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        top: 24.w,
      ),
      child: Column(
        children: [
          Text(
            '最强大、最丰富的AI能力',
            style: TextStyle(
              color: MyColors.colorBrown,
              fontWeight: FontWeight.w700,
              fontSize: 16.sp,
            ),
          ),
          16.verticalSpace,
          Container(
              clipBehavior: Clip.hardEdge,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
              ),
              child: Image(
                image: AssetImage(Assets.imagesHomeDesc),
                fit: BoxFit.scaleDown,
              ))
        ],
      ),
    );
  }
}
