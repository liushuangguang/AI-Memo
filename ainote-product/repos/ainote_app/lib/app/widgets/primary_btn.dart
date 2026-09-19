import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class PrimaryBtn extends StatelessWidget {
  final String? text;
  final Widget? child;
  final bool? disabled;
  final bool? loading;
  final VoidCallback? onPressed;
  final Color? shadowColor;
  final Color? backgroundColor;
  final Color? textColor;
  final double? width;
  final double? height;
  final EdgeInsetsGeometry? margin;
  final Decoration? decoration;

  const PrimaryBtn(
      {super.key,
      this.text,
      this.onPressed,
      this.disabled,
      this.loading,
      this.shadowColor,
      this.backgroundColor,
      this.textColor,
      this.width,
      this.height,
      this.margin,
      this.decoration,
      this.child})
      : assert(text != null || child != null);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width ?? double.infinity,
      height: height ?? 54.h,
      margin: margin,
      clipBehavior: Clip.hardEdge,
      decoration: decoration ?? BoxDecoration(),
      child: ElevatedButton(
          onPressed: disabled == true
              ? null
              : () {
                  if (loading == true) return;
                  onPressed?.call();
                },
          style: ElevatedButton.styleFrom(
            backgroundColor: backgroundColor ?? MyColors.primaryColor,
            shadowColor: shadowColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8.w),
            ),
          ),
          child: child ??
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(text!,
                      style: TextStyle(
                        fontSize: 16.sp,
                        fontWeight: FontWeight.bold,
                        color: textColor ?? MyColors.colorWhite,
                      )),
                  if (loading == true)
                    Container(
                      width: 16.w,
                      height: 16.w,
                      margin: EdgeInsets.only(left: 8.0.w),
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    ),
                ],
              )),
    );
  }
}
