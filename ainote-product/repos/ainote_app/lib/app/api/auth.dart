import 'api_urls.dart';
import 'my_dio.dart';

class AuthAPi {
  /*
  *
  * {
  "code": 0,
  "message": "string",
  "data": {},
  "success": true,
  "failure": true
}
  * */
  static authSendCode(String phone) async {
    return await MyDio.postJSON(
      '${ApiUrls.authSendCode}?phone=$phone&type=LOGIN', // type=LOGIN | RESET
    );
  }

  static authRegister({required String phone, required String code}) async {
    /**
     *
     * 200 注册成功
     * 400 请求参数错误
     * 409 用户已存在
     * 500 Internal Server Error
     *
     * {
        "code": 0,
        "message": "string",
        "data": {
        "token": "string"
        },
        "success": true,
        "failure": true
        }
     * */
    await MyDio.postJSON(
      ApiUrls.authRegister,
      data: {
        "mobile": phone,
        "code": code,
      },
    );
  }

  static authLogin({required String phone, required String code}) async {
    /**
     * 200 登录成功
     * 401 凭证无效
     * 500 Internal Server Error
     *
     * {
        "code": 0,
        "message": "string",
        "data": {
        "token": "string"
        },
        "success": true,
        "failure": true
        }
     * */
    return await MyDio.postJSON(
      ApiUrls.authLogin,
      data: {
        "mobile": phone,
        "code": code,
      },
    );
  }
}
