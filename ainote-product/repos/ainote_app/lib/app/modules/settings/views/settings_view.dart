import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/config/app/app_config.dart';
import 'package:ainote_app/app/modules/settings/views/widgets/points_list.dart';
import 'package:ainote_app/app/services/capture/screenshot_capture_setting_tile.dart';
import 'package:ainote_app/app/modules/vip/views/vip_view.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

import '../controllers/settings_controller.dart';

class SettingsView extends GetView<SettingsController> {
  const SettingsView({super.key});

  @override
  Widget build(BuildContext context) {
    Get.lazyPut(() => SettingsController());

    void handleLogout() {
      Get.dialog(AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16.w),
        ),
        backgroundColor: Colors.white,
        title: Row(
          children: [
            Icon(Icons.info_outline, size: 20.w, color: MyColors.colorBlue),
            SizedBox(width: 12.w),
            Text('确认退出当前账号吗？',
                style: TextStyle(fontSize: 16.sp, fontWeight: FontWeight.w600)),
          ],
        ),
        content: Text(
          '退出后，您将需要重新登录才能继续使用应用。',
          style: TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w400),
        ),
        actions: [
          TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('取消',
                  style:
                      TextStyle(fontSize: 14.sp, color: MyColors.colorBlue))),
          TextButton(
              onPressed: () async {
                Navigator.of(context).pop();
                await controller.logout();
              },
              child: Text('退出',
                  style: TextStyle(fontSize: 14.sp, color: MyColors.colorRed))),
        ],
      ));
    }

    void handleLevelUp() {
      showCupertinoDialog(
        context: context,
        builder: (context) {
          return VipView();
        },
      );
    }

    return Scaffold(
      appBar: AppBar(
        leadingWidth: 0,
        leading: Container(),
        title: Text('个人中心',
            style: TextStyle(
              fontSize: 24.sp,
              fontWeight: FontWeight.w600,
            )),
        actions: [
          if (!AppConfig.guestMode) ...[
            Obx(() {
              return Text(controller.phoneNum.value,
                  style: TextStyle(
                    color: MyColors.colorBlue,
                    fontSize: 16.sp,
                    fontWeight: FontWeight.w500,
                  ));
            }),
            IconButton(
                onPressed: handleLogout,
                icon: Icon(Icons.logout_rounded, size: 20.w)),
          ],
        ],
      ),
      backgroundColor: Color(0xffF3F5F8),
      body: RefreshIndicator(
        onRefresh: controller.refreshPage,
        child: SingleChildScrollView(
          controller: controller.scrollController,
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 20.w),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Container(
                  decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8.w),
                      color: Colors.white),
                  margin: EdgeInsets.only(top: 24.w),
                  padding:
                      EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.w),
                  width: double.infinity,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text('免费积分',
                              style: TextStyle(
                                  fontSize: 16.sp,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.black)),
                          SizedBox(width: 12.w),
                          Text('免费积分每个月重置',
                              style: TextStyle(
                                  fontSize: 12.sp,
                                  fontWeight: FontWeight.w400,
                                  color: Colors.black.withValues(alpha: .8))),
                          const Spacer(),
                          Container(
                            padding: EdgeInsets.symmetric(
                                horizontal: 8.w, vertical: 4.w),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(4.w),
                              color: Color(0xffEDEDED),
                            ),
                            alignment: Alignment.center,
                            child: Text('套餐1',
                                style: TextStyle(
                                    fontSize: 12.sp,
                                    fontWeight: FontWeight.w500,
                                    color: Colors.black)),
                          ),
                        ],
                      ),
                      SizedBox(height: 17.w),
                      Obx(() {
                        return RichText(
                            text: TextSpan(children: [
                          TextSpan(
                              text: '剩余',
                              style: TextStyle(
                                  fontSize: 20.sp,
                                  fontWeight: FontWeight.w400,
                                  color: Colors.black)),
                          TextSpan(
                              text: ' ${controller.points.value} ',
                              style: TextStyle(
                                  fontSize: 28.sp,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.black)),
                          TextSpan(
                              text:
                                  '/ ${controller.totalPoints.value.toString()}',
                              style: TextStyle(
                                  fontSize: 20.sp,
                                  fontWeight: FontWeight.w400,
                                  color: Colors.black)),
                        ]));
                      }),
                      SizedBox(height: 16.w),
                      // 进度条区域
                      Center(
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(3),
                            color: Color(0xffE7E7E7),
                          ),
                          alignment: Alignment.centerLeft,
                          height: 8.w,
                          width: 340.w,
                          // 进度条激活区域
                          child: Obx(() {
                            return Container(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(3),
                                color: Colors.black.withValues(alpha: .6),
                              ),
                              height: 8.w,
                              // 剩余 30%
                              width: 340 * controller.percent.value.w,
                            );
                          }),
                        ),
                      ),
                      SizedBox(height: 16.w),

                      // 升级按钮
                      GestureDetector(
                        onTap: handleLevelUp,
                        child: Container(
                          height: 71.w,
                          width: double.infinity,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8.w),
                            gradient: LinearGradient(
                              begin: Alignment.centerLeft,
                              end: Alignment.centerRight,
                              colors: [Color(0xffFFEDB6), Color(0xffF7C848)],
                            ),
                          ),
                          child: Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Image.asset(Assets.imagesV,
                                        width: 16.w, height: 16.w),
                                    SizedBox(width: 8.w),
                                    Text('升级',
                                        style: TextStyle(
                                            fontSize: 14.sp,
                                            color: Color(0xff614900),
                                            fontWeight: FontWeight.w600)),
                                  ],
                                ),
                                SizedBox(height: 8.w),
                                Text('解锁全部AI功能，无积分焦虑',
                                    style: TextStyle(
                                        fontSize: 12.sp,
                                        color: Color(0xff614900),
                                        fontWeight: FontWeight.w600)),
                              ],
                            ),
                          ),
                        ),
                      ),
                      SizedBox(height: 16.w),
                      // 分享按钮
                      GestureDetector(
                        onTap: () {
                          Toast.info('敬请期待');
                        },
                        child: Container(
                          height: 71.w,
                          width: double.infinity,
                          decoration: BoxDecoration(
                            color: Color.fromRGBO(58, 81, 255, 0.15),
                            borderRadius: BorderRadius.circular(8.w),
                          ),
                          child: Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text('分享获得免费积分（即将上线）',
                                    style: TextStyle(
                                        fontSize: 14.sp,
                                        color: Color.fromRGBO(58, 81, 255, 1),
                                        fontWeight: FontWeight.w600)),
                                SizedBox(height: 8.w),
                                Text('介绍1位新朋友，获得20积分',
                                    style: TextStyle(
                                        fontSize: 12.sp,
                                        color: Color.fromRGBO(58, 81, 255, 1),
                                        fontWeight: FontWeight.w400)),
                              ],
                            ),
                          ),
                        ),
                      ),
                      // 积分消耗记录
                      SizedBox(height: 16.w),
                      Row(
                        children: [
                          Text('积分消耗记录',
                              style: TextStyle(
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w700,
                              )),
                          Spacer(),
                          Visibility(
                            visible: controller.pointsHistoryAll.length > 3,
                            child: TextButton(
                              onPressed: () {
                                showModalBottomSheet(
                                    context: context,
                                    builder: (context) {
                                      return Padding(
                                        padding: EdgeInsets.all(24.w),
                                        child: PointsList(
                                          pointsList:
                                              controller.pointsHistoryAll,
                                        ),
                                      );
                                    });
                              },
                              child: Text(
                                '查看更多',
                                style: TextStyle(
                                    fontSize: 14.sp,
                                    fontWeight: FontWeight.w400,
                                    color: Color.fromRGBO(58, 81, 255, 1)),
                              ),
                            ),
                          ),
                        ],
                      ),
                      SizedBox(height: 16.w),
                      Visibility(
                        visible: controller.pointsHistoryShort.isNotEmpty,
                        child: PointsList(
                            canScroll: false,
                            pointsList: controller.pointsHistoryShort),
                      )
                    ],
                  ),
                ),
                16.verticalSpace,
                // 填写邀请码
                GestureDetector(
                  onTap: () {
                    Toast.info('敬请期待');
                  },
                  child: Container(
                    height: 71.w,
                    width: double.infinity,
                    padding: EdgeInsets.symmetric(horizontal: 16.w),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8.w),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('填写邀请码',
                            style: TextStyle(
                                fontSize: 16.sp, fontWeight: FontWeight.w600)),
                        SizedBox(height: 8.w),
                        Text('通过邀请码登录的用户会有额外的积分奖励',
                            style: TextStyle(
                                fontSize: 12.sp, fontWeight: FontWeight.w400)),
                      ],
                    ),
                  ),
                ),
                16.verticalSpace,
                const ScreenshotCaptureSettingTile(),

                // 只在开发环境显示
                if (kDebugMode) ...[
                  TextButton(
                      onPressed: () {
                        controller.getPoints();
                      },
                      child: Text('获取积分接口调试',
                          style: TextStyle(
                              color: MyColors.colorRed, fontSize: 16.sp))),
                  TextButton(
                      onPressed: () {
                        controller.updatePoints();
                        // 滚动到顶部
                        controller.scrollController.animateTo(
                            controller
                                .scrollController.position.minScrollExtent,
                            duration: Duration(milliseconds: 300),
                            curve: Curves.ease);
                      },
                      child: Text('充值积分接口调试',
                          style: TextStyle(
                              color: MyColors.colorRed, fontSize: 16.sp))),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
