import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/web_view_page.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'package:get/get.dart';

import '../controllers/vip_controller.dart';

class VipView extends GetView<VipController> {
  const VipView({super.key});

  @override
  Widget build(BuildContext context) {
    Get.lazyPut(() => VipController());

    return Scaffold(
      backgroundColor: Color(0xFF0D368C),
      appBar: AppBar(
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: Brightness.light,
          statusBarBrightness: Brightness.light,
        ),
        leading: GestureDetector(
            onTap: () {
              Navigator.of(context).pop();
            },
            child: const Icon(Icons.close_rounded, color: Colors.white)),
      ),
      body: SafeArea(
        child: Container(
          color: Color(0xFF0D368C),
          padding: EdgeInsets.symmetric(horizontal: 20.w),
          child: SingleChildScrollView(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                SizedBox(height: 10.h),
                Align(
                  alignment: Alignment.topCenter,
                  child: Text(
                    '会员升级',
                    style: TextStyle(
                      fontSize: 32.sp,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                ),
                SizedBox(height: 16.h),
                Text(
                  '解锁全部AI功能，无积分焦虑',
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: Color.fromRGBO(255, 255, 255, 0.7),
                  ),
                ),
                Padding(
                  padding: EdgeInsets.symmetric(vertical: 24.w),
                  child: Image.asset(
                    Assets.imagesVip,
                    height: 160.w,
                  ),
                ),
                Align(
                  alignment: Alignment.topLeft,
                  child: Text(
                    '套餐1（单月消费，下个月清零）',
                    style: TextStyle(
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFFA0B6E2),
                    ),
                  ),
                ),
                Container(
                  padding: EdgeInsets.all(20.w),
                  margin: EdgeInsets.only(top: 16.w, bottom: 24.w),
                  decoration: BoxDecoration(
                    color: Color.fromRGBO(9, 39, 101, 0.6),
                    borderRadius: BorderRadius.circular(16.w),
                    border: Border.all(
                      color: Colors.white,
                      width: 1.w,
                    ),
                  ),
                  child: Column(
                    children: [
                      Row(children: [
                        Text('1000积分',
                            style: TextStyle(
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            )),
                        Spacer(),
                        Text(
                          '￥19.9',
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w600,
                            color: Color.fromRGBO(255, 212, 0, 1),
                          ),
                        ),
                      ]),
                      8.verticalSpace,
                      Wrap(children: [
                        Text('大约可以自动整理250篇中等长度的备忘/代办信息',
                            style: TextStyle(
                              fontSize: 14.sp,
                              fontWeight: FontWeight.w400,
                              color: Color.fromRGBO(255, 255, 255, 0.5),
                            )),
                      ])
                    ],
                  ),
                ),
                Align(
                  alignment: Alignment.topLeft,
                  child: Text(
                    '套餐2（适合年消费，第二年清零）',
                    style: TextStyle(
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFFA0B6E2),
                    ),
                  ),
                ),
                Container(
                  padding: EdgeInsets.all(20.w),
                  margin: EdgeInsets.only(top: 16.w),
                  decoration: BoxDecoration(
                    color: Color.fromRGBO(9, 39, 101, 0.6),
                    borderRadius: BorderRadius.circular(16.w),
                  ),
                  child: Column(
                    children: [
                      Row(children: [
                        Text('12000积分',
                            style: TextStyle(
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            )),
                        Spacer(),
                        Text(
                          '￥240',
                          style: TextStyle(
                            decoration: TextDecoration.lineThrough,
                            decorationColor: Color.fromRGBO(255, 212, 0, 0.5),
                            fontSize: 14.sp,
                            fontWeight: FontWeight.w600,
                            color: Color.fromRGBO(255, 212, 0, 0.5),
                          ),
                        ),
                        12.horizontalSpace,
                        Text(
                          '￥199',
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w600,
                            color: Color.fromRGBO(255, 212, 0, 1),
                          ),
                        ),
                      ]),
                      8.verticalSpace,
                      Wrap(children: [
                        Text('大约可以自动整理3000篇中等长度的备忘/代办信息',
                            style: TextStyle(
                              fontSize: 14.sp,
                              fontWeight: FontWeight.w400,
                              color: Color.fromRGBO(255, 255, 255, 0.5),
                            )),
                      ])
                    ],
                  ),
                )
              ])),
        ),
      ),
      bottomNavigationBar: Container(
          padding:
              EdgeInsets.only(left: 20.w, right: 20.w, bottom: 32.w, top: 20.w),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ElevatedButton(
                onPressed: () {
                  _showBuyDialog(context);
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.w),
                  ),
                  minimumSize: Size(double.infinity, 46.w),
                ),
                child: Text('立即购买',
                    style: TextStyle(
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF0D368C),
                    )),
              ),
              16.verticalSpace,
              RichText(
                  text: TextSpan(children: [
                TextSpan(
                  text: '下此订单，代表您同意 ',
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFFA0B6E2),
                  ),
                ),
                TextSpan(
                  text: '服务条款',
                  style: TextStyle(
                    fontSize: 14.sp,
                    color: Color.fromRGBO(255, 212, 0, 1),
                  ),
                  recognizer: TapGestureRecognizer()
                    ..onTap = () {
                      _showDialog(context, '服务条款');
                    },
                ),
                TextSpan(
                  text: ' 和 ',
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: Color(0xFFA0B6E2),
                  ),
                ),
                TextSpan(
                  text: ' 隐私政策 ',
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: Color.fromRGBO(255, 212, 0, 1),
                  ),
                  recognizer: TapGestureRecognizer()
                    ..onTap = () {
                      _showDialog(context, '隐私政策');
                    },
                ),
              ]))
            ],
          )),
    );
  }

  void _showDialog(BuildContext context, String title) {
    showCommonBottomSheet(
        context: context,
        title: title,
        content: WebViewPage(
          url: 'https://aifunc.top',
          noHeader: true,
        ));
  }

  void _showBuyDialog(BuildContext context) {
    SmartDialog.show(
      alignment: Alignment.bottomCenter,
      builder: (_) => Container(
        width: double.infinity,
        padding: EdgeInsets.only(top: 36.w, bottom: 40.w),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16.w),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              '支付方式',
              style: TextStyle(
                fontSize: 22.sp,
                fontWeight: FontWeight.w600,
              ),
            ),
            36.verticalSpace,
            Row(
                mainAxisSize: MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  GestureDetector(
                    onTap: () {
                      // 打开微信支付
                      SmartDialog.showToast('微信支付');
                    },
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                      SvgPicture.asset(
                        Assets.imagesWechat,
                        width: 56.w,
                      ),
                      24.verticalSpace,
                      Text('微信',
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w600,
                            color: Color.fromRGBO(40, 196, 69, 1),
                          )),
                    ]),
                  ),
                  GestureDetector(
                    onTap: () {
                      // 打开支付宝支付
                      SmartDialog.showToast('支付宝支付');
                    },
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                      SvgPicture.asset(
                        Assets.imagesAlipay,
                        width: 56.w,
                      ),
                      24.verticalSpace,
                      Text('支付宝',
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w600,
                            color: Color.fromRGBO(0, 159, 232, 1),
                          )),
                    ]),
                  ),
                ]),
          ],
        ),
      ),
    );
  }
}
