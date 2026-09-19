import 'dart:io';

import 'package:android_window/android_window.dart';
import 'package:get/get.dart';

class AndroidWindowLogic extends GetxController{
  
  RxnString imagePath = RxnString(null);
  RxBool showImage = false.obs;//是否开始展示图片
  RxBool clicked = false.obs; //用户点击过
  RxBool cardOn = true.obs;//卡片关闭
  RxnBool dataRes = RxnBool(null); //服务器数据生成状态，null还没有生成，true成功，false失败
  int startWidth = 300;
  int startHeight = 700;
  // Map<String, dynamic>? noteJson;
  String? noteBody;

  // 这是一个获取图片文件的异步方法
  Future<File> getImageFile(path) async => File(path);

  
  @override
  void onInit(){
    super.onInit();
    setAndroidListener();
  }

  @override
  void onReady(){
    super.onReady();
    AndroidWindow.post("ready","");
  }

  void setAndroidListener(){
    AndroidWindow.setHandler((name, data) async {
      print("window端收到消息，name = $name, data= $data");
      switch(name) {
        case "filePath":
          AndroidWindow.resize((startWidth*1.5).toInt(), startHeight~/6);
          //收到图片
          imagePath.value = data?.toString();
          showImage.value = false;
          clicked.value = false;
          cardOn.value = true;
          dataRes.value = null;
          break;
        case "serData":
          if (data != null && (data as Map)["filePath"] == imagePath.value) {
            dataRes.value = data["res"]??false;
            noteBody = data["data"];
          }
          Future.delayed(const Duration(seconds: 10)).then((t){
            hideApp();
          });
          break;
        default:
          break;
      }
      return null;
    });
  }

  String get showText{
    if(!clicked.value) {
      return "";
    }
    if(dataRes.value == null) {
      return "识别到截图，正在提取信息生成备忘录/代办";
    } else if(dataRes.value!) {
      return "备忘录/代办生成完毕";
    } else {
      return "网络链接异常，生成失败，您可通过点击备忘录的图片识别功能重试";
    }
  }

  void hideApp(){
    AndroidWindow.resize(1, 1);
    imagePath.value = null;
    showImage.value = false;
    clicked.value = false;
    cardOn.value = true;
    dataRes.value = null;
    update();
  }
}