
import 'package:ainote/android_window/show_alert_logic.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';


class ShowAlertPage extends StatelessWidget {
  ShowAlertPage({super.key});
  final logic = Get.put(ShowAlertLogic());

  Widget _titleBtnWidget() {
    return CupertinoSwitch(
        value: logic.onState.value,
        onChanged: (v) async {
          logic.onState.value = v;
          if(v) {
            if(await logic.checkPermission() == true){
              logic.setScreenshotListener();
              logic.restartAndroidWindowApp();
            } else {
              EasyLoading.showToast("请先开启下方全部权限");
              logic.onState.value = !v;
            }
          } else {
            logic.stopListener();
          }
        },
      );
  }

  Widget _lineWidget(String name, {
    required String value,
    bool pickOn = false,
    Function? pickTap,
    String? subValue,
    bool showSub = false,
    Function? onTapSub,
  }) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: [
                        Text(name, style: const TextStyle(fontSize: 16)),
                        const SizedBox(width: 8),
                        Visibility(
                          visible: subValue != null,
                          child: InkWell(
                            onTap: (){
                              if(onTapSub!= null) {
                                onTapSub();
                              }
                            },
                            child: Icon(showSub ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, size: 30),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(value, style: const TextStyle(fontSize: 14, color: Color(0x80000000))),
                  ],
                ),
              ),
              const SizedBox(width: 4),
              CupertinoSwitch(value: pickOn, onChanged: (v){
                if(pickTap != null) {
                  pickTap(v);
                }
              }),
            ],
          ),
          Visibility(
            visible: showSub,
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 10),
              child: Divider(color: Color(0xffe1e1e1), height: 1),
            ),
          ),
          Visibility(
            visible: showSub,
            child: Text(
              subValue ?? "",
              maxLines: 20,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 14,
                color: Color(0x80000000),
              ),
            ),
          ),
        ],
      ),
    );
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xffF3F5F8),
      appBar: AppBar(
        backgroundColor: Colors.white,
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("微信截图识别功能"),
            SizedBox(height: 4),
            Text("开启前需要开启下方全部权限", style: TextStyle(fontSize: 12, color: Color(0x80000000))),
          ],
        ),
        leading: GestureDetector(
          onTap: () => Get.back(),
          child: const Icon(Icons.chevron_left, size: 30),
        ),
        actions: [
          Obx(()=> _titleBtnWidget()),
        ],
      ),
      body: Obx(()=> ListView(
        children: [
          _lineWidget(
            "悬浮窗",
            value: "显示在其他应用的上层",
            subValue: "此权限用于实现悬浮窗保持在其他应用之上。点击本菜单后，在列表中找到本应用并点击然后在页面打开开关[允许显示在其他应用上层]，若直接进入开关界面，则直接打开开关。",
            showSub: logic.showDownDraw.value,
            onTapSub: (){
              logic.showDownDraw.value = !logic.showDownDraw.value;
            },
            pickOn: logic.canDrawOverlays.value,
            pickTap: (bool v){
              logic.canDrawOverlays.value = v;
              if (!v) {
                EasyLoading.showToast("权限已打开，可到手机权限设置页关闭");
                logic.canDrawOverlays.value = !v;
                return;
              }
              logic.openDrawOberlays();
            },
          ),
        ],
      )),
    );
  }
}