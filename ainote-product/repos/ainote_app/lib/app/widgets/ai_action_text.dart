import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';

class AiDoing extends StatelessWidget {
  final String? text;
  final TextStyle? style;
  final Color? color;
  const AiDoing({super.key, this.text, this.style, this.color});

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          text ?? 'AI理解信息中',
          style: style ??
              TextStyle(
                fontSize: 16.sp,
                fontWeight: FontWeight.bold,
                color: primaryColor,
                decoration: TextDecoration.none,
              ),
        ),
        SizedBox(width: 4.w),
        LoadingAnimationWidget.progressiveDots(
          color: color ?? primaryColor,
          size: 18.sp,
        )
      ],
    );
  }
}
