import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

class LoginWrap extends StatelessWidget {
  final Widget child;

  const LoginWrap({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
        top: false,
        bottom: true,
        child: Scaffold(
          extendBodyBehindAppBar: true,
          appBar: AppBar(
            backgroundColor: MyColors.pageBackgroundColor,
            title: const Text('AI 备忘录'),
            centerTitle: true,
            leading: IconButton(
                onPressed: () => Get.back(),
                icon: Icon(Icons.arrow_back_ios_new,
                    color: MyColors.primaryColor, size: 24.w)),
          ),
          body: SafeArea(
            child: Container(
              padding: EdgeInsets.only(left: 24.w, right: 24.w, top: 30.w),
              constraints: const BoxConstraints.expand(),
              child: SingleChildScrollView(child: Center(child: child)),
            ),
          ),
        ));
  }
}
