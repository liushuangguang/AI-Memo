import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/login/views/fill_phone.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'package:get/get.dart';

import '../controllers/login_controller.dart';

class LoginView extends GetView<LoginController> {
  const LoginView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: SafeArea(
          top: false,
          child: Container(
            height: Get.height,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  Color(0xFFF1F1F1), // 渐变开始颜色
                  Color(0xFFE9E9E9), // 渐变开始颜色
                ],
              ),
            ),
            child: SingleChildScrollView(
              child: Container(
                height: Get.height,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      Color(0xFFF1F1F1), // 渐变开始颜色
                      Color(0xFFE9E9E9), // 渐变开始颜色
                    ],
                  ),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    100.verticalSpace,
                    Image.asset(
                      Assets.loginLogo,
                      width: 163.w,
                      height: 163.w,
                    ),
                    Text(
                      "AI备忘录",
                      style: TextStyle(
                          fontSize: 30.sp,
                          color: MyColors.loginPrimaryColor,
                          fontWeight: FontWeight.bold),
                    ).animate(delay: Duration(milliseconds: 100)).fadeIn(),
                    20.verticalSpace,
                    Text("智能整理你的信息库",
                            style: TextStyle(
                                fontSize: 22.sp,
                                color: MyColors.loginSecondaryColor))
                        .animate(delay: Duration(milliseconds: 200))
                        .shimmer(),
                    Spacer(),
                    Image.asset(
                      Assets.loginPoster,
                      width: double.infinity,
                      fit: BoxFit.fitWidth,
                    ).animate(delay: Duration(milliseconds: 300)).fadeIn(),
                    120.verticalSpace,
                  ],
                ),
              ),
            ),
          ),
        ),
        floatingActionButtonLocation: FloatingActionButtonLocation.centerDocked,
        floatingActionButton: PrimaryBtn(
          margin: EdgeInsets.only(left: 16.w, right: 16.w, bottom: 24.w),
          onPressed: () {
            Get.to(() => const FillPhone(), transition: Transition.downToUp);
          },
          text: "开始",
        ).animate(delay: Duration(milliseconds: 500)).fadeIn());
  }
}
