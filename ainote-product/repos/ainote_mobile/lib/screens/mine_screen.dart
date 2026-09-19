import 'package:ainote/android_window/show_alert_page.dart';
import 'package:ainote/constants/app_images.dart';
import 'package:ainote/screens/mine_screen_logic.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class MineScreen extends StatelessWidget {
  MineScreen({super.key});
  final MineScreenLogic logic = Get.put(MineScreenLogic());
  Widget _lineWidget(String name, {String? img, String? subName, Color? subColor, Function? onTap}) {
    return InkWell(
      onTap: (){
        if(onTap != null) onTap();
      },
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(10),
          color: Colors.white,
        ),
        child: Row(
          children: [
            ClipOval(
              child: Image.asset(
                img ?? AppImages.tabWechatIcon.path,
                width: 24,
                height: 24,
                fit: BoxFit.cover,
              ),
            ),
            const SizedBox(width: 12),
            Text(name, style: const TextStyle(fontSize: 14)),
            Expanded(
              child: Container(
                alignment: Alignment.centerRight,
                child: Text(subName??"", style: TextStyle(fontSize: 14, color: subColor)),
              ),
            ),
            const SizedBox(width: 12),
            const Icon(Icons.keyboard_arrow_right_outlined, size: 24),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xffF3F5F8),
      appBar: AppBar(
        title: const Text("个人中心", style: TextStyle(fontWeight: FontWeight.w500)),
        backgroundColor: Colors.white,
      ),
      body: ListView(
        children: [
          const SizedBox(height: 8),
          Obx(()=>Column(
            children: [
              _lineWidget(
                "微信截图识别",
                img: AppImages.tabWechatIcon.path,
                subName: logic.onState.value ? "已开启": "未开启",
                subColor: logic.onState.value ? const Color(0xff009D4F) : const Color(0xffFF7D3C),
                onTap: () async {
                  await Get.to(()=> ShowAlertPage());
                  logic.onRefresh();
                },
              )
            ],
          )),
        ],
      ),
    );
  }
}
