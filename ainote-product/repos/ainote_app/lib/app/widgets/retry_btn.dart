import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class RetryBtn extends StatelessWidget {
  final String? text;
  final VoidCallback? onTap;
  final Color? backgroundColor;
  final Color? textColor;
  final double? width;
  final double? height;
  final EdgeInsetsGeometry? margin;

  const RetryBtn({
    super.key,
    this.text,
    this.onTap,
    this.backgroundColor,
    this.textColor,
    this.width,
    this.height,
    this.margin,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: backgroundColor ?? Colors.white,
          borderRadius: BorderRadius.circular(12.w),
        ),
        padding: EdgeInsets.symmetric(horizontal: 13.w, vertical: 7.w),
        margin: margin ?? EdgeInsets.only(bottom: 16.w, left: 8.w),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.refresh,
              size: 16.sp,
              color: textColor ?? Theme.of(context).primaryColor,
            ),
            SizedBox(width: 10.w),
            Text(
              text ?? '重新生成',
              style: TextStyle(
                  fontSize: 14.sp,
                  color: textColor ?? Color(0xFF3F3C61),
                  fontWeight: FontWeight.normal),
            ),
          ],
        ),
      ),
    );
  }
}
