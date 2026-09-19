import 'dart:async';

import 'package:ainote_app/app/api/auth.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:ainote_app/app/modules/login/views/verify_code.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/logger.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:get/get.dart';
import 'package:phone_form_field/phone_form_field.dart';

import 'auth_transition.dart';

class LoginController extends GetxController {
  final phoneNum = ''.obs;
  final code = ''.obs;
  final loading = false.obs;
  final seconds = 30.obs;
  late Timer _timer;

  @override
  void onInit() {
    super.onInit();

    // get phone number from storage
    phoneNum.value = MyStorage.getPhoneNumber() ?? '';
    // get locale country code
    // String? countryCode = PlatformDispatcher.instance.locale.countryCode;
    // if (countryCode != null) {
    //   phoneController.value = PhoneNumber(
    //       isoCode: IsoCode.fromJson(countryCode), nsn: phoneNumber ?? '');
    //
    //   setState(() {
    //     isValid = formKey.currentState!.validate();
    //   });
    // }
  }

  @override
  void onReady() {
    super.onReady();
  }

  @override
  void onClose() {
    _timer.cancel();
    super.onClose();
  }

  void startTimer() {
    seconds.value = 30;
    const oneSec = Duration(seconds: 1);
    _timer = Timer.periodic(oneSec, (Timer timer) {
      if (seconds.value == 0) {
        timer.cancel();
      } else {
        seconds.value--;
      }
    });
  }

  // 获取验证码
  Future<void> getVerificationCode(PhoneNumber p) async {
    loading.value = true;
    try {
      phoneNum.value = p.nsn;
      MyStorage.setPhoneNumber(p.nsn);

      var resp = await AuthAPi.authSendCode(p.nsn);
      if (resp.data['code'] != 200) {
        Toast.success('获取验证码失败，请稍后重试');
        return;
      }
      Toast.success('验证码已发送，请注意查收');
      Get.to(() => const VerifyCodePage(),
          transition: Transition.fadeIn, arguments: p);
    } catch (_) {
      logger.e('getVerificationCode failed');
    }

    loading.value = false;
  }

  Future<void> resendCode() async {
    try {
      await AuthAPi.authSendCode(phoneNum.value);
      Toast.success('验证码已发送，请注意查收');
    } catch (_) {
      logger.e('resendCode failed');
      Toast.error('获取验证码失败，请稍后重试');
    }
  }

  // 登陆获取token或用户信息
  Future<void> login(String c) async {
    loading.value = true;
    code.value = c;
    try {
      final resp =
          await AuthAPi.authLogin(code: code.value, phone: phoneNum.value);
      final transitioned = await completeLoginResponseTransition(
        responseData: resp.data,
        persistAuthorization: MyStorage.setAuthToken,
        installHeaders: MyDio.setAuthHeaders,
        enterApp: () => Get.offNamed(Routes.ROOT),
      );
      if (!transitioned) {
        Toast.error('登录响应无效，请稍后重试');
      }
    } catch (_) {
      logger.e('login failed');
      Toast.error('验证码不正确');
    } finally {
      loading.value = false;
    }
  }
}
