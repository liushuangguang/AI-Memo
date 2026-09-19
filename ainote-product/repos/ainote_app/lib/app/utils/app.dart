import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_statusbarcolor_ns/flutter_statusbarcolor_ns.dart';

void changeStatusBarColor([dark = false]) async {
  await FlutterStatusbarcolor.setStatusBarColor(Colors.transparent);

  if (dark) {
    await FlutterStatusbarcolor.setStatusBarColor(Colors.black);

    FlutterStatusbarcolor.setStatusBarWhiteForeground(true);

    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
      statusBarIconBrightness: Brightness.light,
    ));
  } else {
    await FlutterStatusbarcolor.setStatusBarColor(Colors.transparent);
    FlutterStatusbarcolor.setStatusBarWhiteForeground(false);
    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
      statusBarIconBrightness: Brightness.dark,
    ));
  }
}
