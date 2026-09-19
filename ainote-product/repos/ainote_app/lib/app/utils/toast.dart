import 'dart:async';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:toastification/toastification.dart';

class Toast {
  static Timer? _debounceTimer;

  static void info(String msg, {bool isLong = false}) {
    toastification.show(
      title: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.info_outline, color: Colors.blueAccent),
          SizedBox(width: 4),
          Text(msg,
              style: TextStyle(color: MyColors.primaryColor, fontSize: 14.sp)),
        ],
      ),
      type: ToastificationType.info,
      closeOnClick: true,
      showProgressBar: false,
      style: ToastificationStyle.simple,
      autoCloseDuration: Duration(seconds: 2),
      borderRadius: BorderRadius.circular(30),
      backgroundColor: Colors.white,
      borderSide: BorderSide(color: Colors.transparent),
      alignment: Alignment.topCenter,
      padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 6.w),
    );
  }

  static void success(String msg, {bool isLong = false}) {
    toastification.show(
      title: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.check_circle, color: Color(0xff32bc32)),
          SizedBox(width: 4),
          Text(msg,
              style: TextStyle(color: Color(0xff32bc32), fontSize: 14.sp)),
        ],
      ),
      closeOnClick: true,
      showProgressBar: false,
      style: ToastificationStyle.simple,
      autoCloseDuration: Duration(seconds: 2),
      borderRadius: BorderRadius.circular(30),
      backgroundColor: Colors.white,
      borderSide: BorderSide(color: Colors.transparent),
      alignment: Alignment.topCenter,
      padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 6.w),
    );
  }

  static void error(String msg, {bool isLong = false}) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(Duration(milliseconds: 500), () {
      toastification.show(
        title: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.warning_rounded, color: Color(0xffff0000)),
            SizedBox(width: 4),
            Text(msg,
                style: TextStyle(color: Color(0xffff0000), fontSize: 14.sp)),
          ],
        ),
        type: ToastificationType.error,
        style: ToastificationStyle.simple,
        autoCloseDuration: Duration(seconds: 2),
        borderRadius: BorderRadius.circular(30),
        borderSide: BorderSide(color: Colors.transparent),
        backgroundColor: Colors.white,
        alignment: Alignment.topCenter,
        padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 6.w),
      );
    });
  }
}
