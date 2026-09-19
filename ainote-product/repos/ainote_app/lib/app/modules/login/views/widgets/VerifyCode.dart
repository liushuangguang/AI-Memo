import 'dart:async';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/login/controllers/login_controller.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:phone_form_field/phone_form_field.dart';

import 'flutter_verification_code.dart';

class VerifyCode extends StatefulWidget {
  const VerifyCode({super.key});

  @override
  State<VerifyCode> createState() => _VerifyCodeState();
}

class _VerifyCodeState extends State<VerifyCode> {
  var loginController = Get.find<LoginController>();

  String code = '';
  bool onEditing = false;

  String codePhone = '';

  void handleResendCode() async {
    if (loginController.seconds.value > 0) return;
    await loginController.resendCode();
    loginController.startTimer();

    setState(() {
      code = '';
    });
  }

  void handleNext() async {
    // 获取 token 登陆成功
    await loginController.login(code);
  }

  Timer? _timer;

  @override
  void initState() {
    super.initState();
    final args = Get.arguments;

    if (args is PhoneNumber) {
      codePhone = '+${args.countryCode} ${args.nsn}';
    }
    loginController.startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          "请输入验证码",
          style: TextStyle(
              fontSize: 30.sp,
              color: MyColors.loginPrimaryColor,
              fontWeight: FontWeight.bold),
        ).animate(delay: Duration(milliseconds: 100)).fadeIn(),
        8.verticalSpace,
        Text("已发送验证码至 $codePhone",
                style: TextStyle(
                    fontSize: 16.sp, color: MyColors.loginSecondaryColor))
            .animate(delay: Duration(milliseconds: 200))
            .fadeIn(),
        Center(
          child: VerificationCode(
            autofocus: true,
            textStyle: TextStyle(fontSize: 18.sp, height: 2),
            itemSize: 64.w,
            keyboardType: TextInputType.phone,
            digitsOnly: false,
            fullBorder: true,
            fillColor: Colors.white,
            underlineColor: MyColors.colorBlue,
            underlineUnfocusedColor: Color(0xFFCBCAD8),
            length: 4,
            margin: EdgeInsets.only(top: 24.w, right: 16.w),
            onCompleted: (String value) {
              setState(() {
                code = value;
              });
              handleNext();
            },
            onEditing: (bool value) {
              setState(() {
                onEditing = value;
              });
              if (!onEditing) FocusScope.of(context).unfocus();
            },
            onClearAll: handleResendCode,
            clearAll: Obx(() {
              return Container(
                width: Get.width - 64.w,
                padding: EdgeInsets.only(top: 8.w, bottom: 8.w),
                child: Padding(
                  padding: EdgeInsets.only(top: 8.0.w, bottom: 8.w),
                  child: Text(
                      loginController.seconds.value > 0
                          ? '${loginController.seconds.value}s 后重新获取 '
                          : '重新发送验证码',
                      style: TextStyle(
                          fontSize: 14.sp,
                          color: loginController.seconds.value > 0
                              ? MyColors.colorBlue.withValues(alpha: 0.5)
                              : MyColors.colorBlue)),
                ),
              );
            }),
          ),
        ),
        Obx(() {
          return PrimaryBtn(
            loading: loginController.loading.value,
            backgroundColor: MyColors.colorBlue,
            disabled: onEditing || code.length < 4,
            onPressed: handleNext,
            text: '确定',
          );
        }),
        12.verticalSpace,
        RichText(
          text: TextSpan(
            children: [
              TextSpan(
                text: "登录则视为您同意我们的 ",
                style: TextStyle(
                  fontSize: 14.sp,
                  color: MyColors.loginSecondaryColor.withValues(alpha: 0.7),
                ),
              ),
              TextSpan(
                text: "用户协议",
                style: TextStyle(
                  fontSize: 14.sp,
                  color: MyColors.colorBlue.withValues(alpha: 0.9),
                  decoration: TextDecoration.underline, // 可选：添加下划线以表示链接
                ),
                recognizer: TapGestureRecognizer()
                  ..onTap = () {
                    showCommonBottomSheet(
                        context: context,
                        title: '用户协议',
                        // content: WebViewPage(url: 'https://www.baidu.com', noHeader: true,));
                        content: Padding(
                          padding: const EdgeInsets.symmetric(
                              vertical: 8.0, horizontal: 16.0),
                          child: Text('（当前产品为体验版，用户协议和隐私协议待产品正式上线后拟定，敬请谅解~）'),
                        ));
                  },
              ),
              TextSpan(
                text: " 和 ",
                style: TextStyle(
                  fontSize: 14.sp,
                  color: MyColors.loginSecondaryColor.withValues(alpha: 0.7),
                ),
              ),
              TextSpan(
                text: "隐私协议",
                style: TextStyle(
                  fontSize: 14.sp,
                  color: MyColors.colorBlue.withValues(alpha: 0.9),
                  decoration: TextDecoration.underline, // 可选：添加下划线以表示链接
                ),
                recognizer: TapGestureRecognizer()
                  ..onTap = () {
                    showCommonBottomSheet(
                        context: context,
                        title: '用户协议',
                        // content: WebViewPage(url: 'https://www.baidu.com', noHeader: true,));
                        content: Padding(
                          padding: const EdgeInsets.symmetric(
                              vertical: 8.0, horizontal: 16.0),
                          child: Text('（当前产品为体验版，用户协议和隐私协议待产品正式上线后拟定，敬请谅解~）'),
                        ));
                  },
              ),
            ],
          ),
        ),
      ],
    );
  }
}
