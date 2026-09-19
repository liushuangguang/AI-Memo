
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'android_window_page.dart';


class AndroidWindowApp extends StatelessWidget {
  const AndroidWindowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ScreenUtilInit(
      designSize: const Size(300,700),
      minTextAdapt: true,
      splitScreenMode: true,
      builder: (context , child) {
        return MaterialApp(
          color: Colors.transparent,
          home: AndroidWindowPage(),
          debugShowCheckedModeBanner: false,
        );
      },
    );
  }
}
