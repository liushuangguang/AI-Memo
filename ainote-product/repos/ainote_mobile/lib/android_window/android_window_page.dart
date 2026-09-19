import 'dart:io';

import 'package:ainote/constants/app_images.dart';
import 'package:android_window/android_window.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

import 'android_masking.dart';
import 'android_progress_button.dart';
import 'android_window_logic.dart';

class AndroidWindowPage extends StatelessWidget {
  AndroidWindowPage({super.key});
  final AndroidWindowLogic logic = Get.put(AndroidWindowLogic());

  Widget _maskingWidget() {
    if (logic.clicked.value) {
      if (logic.dataRes.value == true) {
        //显示true的蒙版
        return const Center(
          child: Icon(Icons.check_circle, size: 24,color: Color(0xff3a51ff)),
        );
      } else if (logic.dataRes.value == false) {
        //显示false的蒙版
        return const Center(
          child: Icon(Icons.error, size: 24,color: Color(0xffff3a3a)),
        );
      }
    }
    return AndroidMasking();
  }

  Widget _imageWidget() {
    return InkWell(
      onTap: (){
        if(logic.imagePath.value == null) {
          return;
        }
        if(!logic.clicked.value){
          logic.clicked.value = true;
          logic.clicked.refresh();
          AndroidWindow.resize(logic.startWidth * 2, logic.startHeight * 2 + 12);
          logic.update();
        }
      },
      child: SizedBox(
        width: logic.clicked.value? 0.5.sw : 1.sw,
        height: logic.clicked.value? 0.5.sh : 1.sh,
        child: Stack(
          children: [
            logic.imagePath.value == null
                ? Container(
                  color: Colors.transparent,
                  width: logic.clicked.value? 0.5.sw : 1.sw,
                  height: logic.clicked.value? 0.5.sh : 1.sh,
                ):
            FutureBuilder<File>(
              future: logic.getImageFile(logic.imagePath.value), // 假设这是一个获取File的异步操作
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.done && snapshot.data != null) {
                  // 图片文件已经成功获取
                  return ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: Image.file(
                      snapshot.data!,
                      width: logic.clicked.value? 0.5.sw : 1.sw,
                      height: logic.clicked.value? 0.5.sh : 1.sh,
                      fit: BoxFit.cover,
                    ),
                  );
                }
                return const CircularProgressIndicator(); // 图片加载中显示进度指示器
              },
            ),
            Positioned(
              width: logic.clicked.value? 0.5.sw : 1.sw,
              height: logic.clicked.value? 0.5.sh : 1.sh,
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(logic.clicked.value && logic.dataRes.value != null ? 0.5 : 0), // 半透明黑色
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
            Positioned(
              width: logic.clicked.value? 0.5.sw : 1.sw,
              height: logic.clicked.value? 0.5.sh : 1.sh,
              child: _maskingWidget(),
            ),
          ],
        ),
      ),
    );
  }


  Widget _btnWidget(String name, {Color? backColor, Color? fontColor, Color? borderColor, double? width, double? height, Function? onTap}) {
    return InkWell(
      onTap: (){
        if (onTap != null) {
          onTap();
        }
      },
      child: Container(
        alignment: Alignment.center,
        width: width ?? 1.sw,
        height: height ?? 30,
        decoration: BoxDecoration(
          color: backColor ?? const Color(0xff3A51FF),
          borderRadius: BorderRadius.circular(50),
          border: Border.all(width: 1, color: borderColor ?? backColor ?? fontColor ?? const Color(0xff3A51FF)),
        ),
        child: Text(name, style: TextStyle(fontSize: 12, color: fontColor ?? Colors.white)),
      ),
    );
  }

  

  Widget _cardWidget() {
    return Stack(
      children: [
        SingleChildScrollView(
          child: Container(
            margin: const EdgeInsets.only(top: 12, right: 12),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  logic.showText,
                  style: const TextStyle(fontSize: 12),
                  maxLines: 2,
                ),
                const SizedBox(height: 8),
                logic.dataRes.value == null 
                ? AndroidProgressButton()
                : _btnWidget(
                    "点击查看",
                    onTap: (){
                      if(logic.dataRes.value == null) {
                        return;
                      }
                      AndroidWindow.launchApp();
                      AndroidWindow.post("openPage", logic.noteBody);
                      logic.hideApp();
                    },
                ),
                const SizedBox(height: 8),
                _btnWidget(
                  "隐藏至后台",
                  backColor: Colors.white,
                  borderColor: const Color(0xff3A51FF),
                  fontColor: const Color(0xff3A51FF),
                  onTap: (){
                    logic.hideApp();
                  },
                ),
                // const Expanded(child: SizedBox.shrink()),
              ],
            ),
          ),
        ),
        Positioned(
          top: 0,
          right: 0,
          width: 24,
          height: 24,
          child: InkWell(
            onTap: () {
              logic.cardOn.value = false;
              logic.update();
              // AndroidWindow.resize(logic.startWidth, logic.startHeight);
            },
            child: const Icon(Icons.highlight_off_outlined, size: 24, color: Colors.white),
          ),
        ),
      ],
    );
  }

  Widget _showImageWidget(){
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        // AndroidMasking()
        _imageWidget(),
        Expanded(child: _cardWidget()),
      ],
    );
  }

  Widget _showStartWidget(){
    return SizedBox(
      width: 1.sw,
      height: 1.sh,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          InkWell(
            onTap: (){
              logic.showImage.value = true;
              AndroidWindow.resize(logic.startWidth, logic.startHeight);
              AndroidWindow.post("createNote", "");
              logic.update();
            },
            child: Container(
              width: 0.7.sw,
              height: 0.8.sh,
              padding: EdgeInsets.symmetric(horizontal: 20.w),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(50),
                color: const Color(0xff3A51FF),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Image.asset(AppImages.tabScreenshot.path, width: 34.w, height: 32.w, fit: BoxFit.cover),
                  Expanded(
                    child: Align(
                      alignment: Alignment.centerRight,
                      child: Text("生成代办", style: TextStyle(color: Colors.white, fontSize: 30.sp)),
                    ),
                  ),
                ],
              ),
            ),
          ),
          SizedBox(width: 20.w),
          InkWell(
            onTap: (){
              logic.hideApp();
            },
            child: Container(
              alignment: Alignment.center,
              width: 0.2.sw,
              height: 0.2.sw,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(50)
              ),
              child: Icon(Icons.close, color: Colors.black, size: 0.2.sw),
            ),
          ),
        ],
      ),
    );
  }

  Widget _containerWidget(context){
    Widget childWidget;
    if(logic.imagePath.value == null){
      childWidget = Container();
    } else if(!logic.showImage.value) {
      childWidget = _showStartWidget();
    } else if(!logic.clicked.value || !logic.cardOn.value){
      childWidget = _imageWidget();
    } else {
      childWidget = _showImageWidget();
    }

    return Container(
      width: 1.sw,
      height: 1.sh,
      color: Colors.transparent,
      child: childWidget
    );
  }

  @override
  Widget build(BuildContext context) {
    return AndroidWindow(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        // body: GetBuilder<AndroidWindowLogic>(builder: (c) => _containerWidget(context)),
        body: Obx(()=> _containerWidget(context)),
      ),
    );
  }
}