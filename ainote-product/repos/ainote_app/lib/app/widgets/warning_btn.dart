import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../config/theme/my_colors.dart';

class WarningBtn extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final Color? shadowColor;
  final Color? backgroundColor;
  final Color? textColor;
  final double? width;
  final double? height;
  final EdgeInsetsGeometry? margin;

  const WarningBtn({
    super.key,
    required this.text,
    this.onPressed,
    this.shadowColor,
    this.backgroundColor,
    this.textColor,
    this.width,
    this.height,
    this.margin,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width ?? double.infinity,
      height: height ?? 54.h,
      margin: margin,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: backgroundColor ?? MyColors.colorDeleteRed,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.w),
          ),
          shadowColor: shadowColor ?? Colors.transparent,
        ),
        child: Text(
          text,
          style: TextStyle(
              fontSize: 16.sp, fontWeight: FontWeight.bold, color: textColor),
        ),
      ),
    );
  }
}
