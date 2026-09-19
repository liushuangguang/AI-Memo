import 'package:ainote_app/app/utils/toast.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/svg.dart';

import '../../../../../../generated/assets.dart';
import '../../../../utils/alarm.dart';

class VoiceCard extends StatelessWidget {
  const VoiceCard({super.key});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
        onTap: () {
          Toast.info('功能正在开发中');
          Alarm().playVibrator();
        },
        child: Container(
          decoration: BoxDecoration(
            color: Color(0xFFF0FFF7),
            borderRadius: BorderRadius.circular(12.w),
          ),
          width: 108.w,
          height: 100.h,
          child: SvgPicture.asset(
            Assets.imagesHomeVoice,
            fit: BoxFit.scaleDown,
          ),
        ));
  }
}
