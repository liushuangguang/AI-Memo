import 'dart:ui';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/login/controllers/login_controller.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:phone_form_field/phone_form_field.dart';

class PhoneForm extends StatefulWidget {
  final String? phoneNumber;
  final String? countryCode;
  final Function? onSave;

  const PhoneForm({super.key, this.phoneNumber, this.countryCode, this.onSave});

  @override
  State<PhoneForm> createState() => _PhoneFormState();
}

class _PhoneFormState extends State<PhoneForm> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final PhoneController phoneController = PhoneController();
  final FocusNode phoneFocusNode = FocusNode();
  bool isValid = false;

  var loginController = Get.find<LoginController>();

  void handleSave(PhoneNumber? p) async {
    if (p != null && p.isValid()) {
      await loginController.getVerificationCode(p);
    } else {
      Toast.error('请输入手机号');
    }
  }

  void clear() {
    phoneController.value = PhoneNumber(nsn: '', isoCode: IsoCode.CN);
    setState(() {
      isValid = false;
    });
  }

  void initData() async {
    try {
      String? phoneNumber = loginController.phoneNum.value;

      phoneController.value =
          PhoneNumber(isoCode: IsoCode.CN, nsn: phoneNumber);
      setState(() {
        isValid = phoneController.value.nsn.isNotEmpty;
      });
    } catch (e) {
      // get locale country code
      String? countryCode = PlatformDispatcher.instance.locale.countryCode;
      if (countryCode != null) {
        phoneController.value =
            PhoneNumber(isoCode: IsoCode.fromJson(countryCode), nsn: '');
      }
    }
  }

  @override
  void initState() {
    super.initState();
    Future.delayed(Duration(milliseconds: 500)).then((value) {
      initData();
      phoneFocusNode.requestFocus();
    });
  }

  @override
  void dispose() {
    super.dispose();
    phoneController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          "登录",
          style: TextStyle(
              fontSize: 30.sp,
              color: MyColors.loginPrimaryColor,
              fontWeight: FontWeight.bold),
        ).animate(delay: Duration(milliseconds: 100)).fadeIn(),
        8.verticalSpace,
        Text("未注册手机号验证后将自动登录",
                style: TextStyle(
                    fontSize: 16.sp, color: MyColors.loginSecondaryColor))
            .animate(delay: Duration(milliseconds: 200))
            .fadeIn(),
        const SizedBox(height: 26),
        Form(
          key: formKey,
          child: PhoneFormField(
            focusNode: phoneFocusNode,
            controller: phoneController,
            onTapOutside: (d) {
              phoneFocusNode.unfocus();
            },
            isCountrySelectionEnabled: false,
            isCountryButtonPersistent: true,
            countryButtonStyle: CountryButtonStyle(
                showDropdownIcon: false,
                showFlag: false,
                padding: EdgeInsets.symmetric(horizontal: 12.w),
                textStyle: TextStyle(
                  color: MyColors.primaryColor.withValues(alpha: 0.6),
                  fontSize: 16.sp,
                )),
            autofillHints: const [AutofillHints.telephoneNumber],
            style: TextStyle(
              fontSize: 16.sp,
            ),
            decoration: InputDecoration(
                contentPadding: EdgeInsets.symmetric(vertical: 10.w),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8.w),
                  borderSide: BorderSide(
                    color: Color(0xFFCBCAD8),
                  ),
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.all(Radius.circular(8.w)),
                  borderSide: BorderSide(
                    color: Color(0xFFCBCAD8),
                  ),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.all(Radius.circular(8.w)),
                  borderSide: BorderSide(color: MyColors.colorBlue),
                ),
                hintText: '请输入手机号',
                errorStyle: TextStyle(fontSize: 12.sp),
                hintStyle: TextStyle(
                    color: MyColors.primaryColor.withValues(alpha: 0.5),
                    fontSize: 16.sp,
                    fontWeight: FontWeight.w400),
                suffixIcon: AnimatedOpacity(
                  opacity: phoneController.value.nsn.isNotEmpty ? 1 : 0,
                  duration: Duration(milliseconds: 1000),
                  child: IconButton(
                      onPressed: clear,
                      icon: Icon(
                        color: MyColors.primaryColor,
                        Icons.clear_rounded,
                        size: 20.sp,
                      )),
                )),
            enabled: true,
            validator: PhoneValidator.compose([
              PhoneValidator.required(context, errorText: '手机号不能为空'),
              PhoneValidator.validMobile(context, errorText: '手机号不正确'),
            ]),
            autovalidateMode: AutovalidateMode.onUnfocus,
            onSaved: handleSave,
            onSubmitted: handleSave,
            // onSubmitted
            onChanged: (PhoneNumber p) {
              setState(() {
                isValid = formKey.currentState?.validate() == true;
              });
            },
          ).animate(delay: Duration(milliseconds: 300)).fadeIn(),
        ),
        SizedBox(height: 24.w),
        Obx(() {
          return PrimaryBtn(
            loading: loginController.loading.value,
            disabled: !isValid,
            backgroundColor: MyColors.colorBlue,
            onPressed: () {
              if (isValid) {
                formKey.currentState?.save();
              }
            },
            text: '获取验证码',
          );
        }).animate(delay: Duration(milliseconds: 500)).fadeIn(),
      ],
    );
  }
}
