
import 'dart:convert';
import 'dart:developer';

import 'package:ainote/constants/constants.dart';
import 'package:ainote/models/type_model.dart';
import 'package:ainote/screens/ai_generate_screen.dart';
import 'package:ainote/screens/home_screen.dart';
import 'package:ainote/services/api_service.dart';
import 'package:event_bus/event_bus.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:android_window/main.dart' as android_window;
import 'package:permission_handler/permission_handler.dart';

import '../models/note_model.dart';
import 'ScreenshotObserver.dart';

class ShowAlertLogic extends GetxController{


  RxBool onState = false.obs;
  RxBool canDrawOverlays = false.obs;  //悬浮窗权限
  RxBool showDownDraw = false.obs;
  
  String? filePath;
  int startWidth = 300;
  int startHeight = 700;

  Future<void> onRefresh() async {
    canDrawOverlays.value = await android_window.canDrawOverlays();
    onState.value = await android_window.isRunning();
  }

  @override
  void onInit(){
    super.onInit();
    onRefresh();
  }

  Future<void> openDrawOberlays() async {
    if(!await android_window.canDrawOverlays()) {
      await android_window.requestPermission();
      onRefresh();
    }
  }

  //自动请求权限
  Future<bool?> checkPermission() async {
    if(!await Permission.storage.request().isGranted){
      return false;
    }
    if(!await android_window.canDrawOverlays()) {
      return false;
    }
    return true;
  }

  // 打开弹窗，显示默认样式
  Future<void> restartAndroidWindowApp() async {
    if(await android_window.isRunning()) {
      android_window.close();
    }
    _setWindowListener();
    android_window.open(
      size: const Size(1.0, 1.0),
      position: const Offset(10, 200),
    );
  }

  //设置截图监听器
  Future<void> setScreenshotListener() async {
    ScreenshotObserver.startObserving();
    ScreenshotObserver.setOnScreenshotListener((path) async {
      if(path == null) {
        return;
      }
      filePath = path;
      // restartAndroidWindowApp();
      if(!await android_window.isRunning()) {
        return;
      }
      android_window.post("filePath",filePath);
    });
  }

  void stopListener() {
    ScreenshotObserver.stopObserving();
    android_window.close();
  }

  

  //设置监听到弹窗的消息
  void _setWindowListener() async {
    android_window.setHandler((name,data) async {
      print("app收到消息,name=$name, data= $data");
      //这里接收到弹窗给的消息，并处理
      switch(name){
        case("ready"):
          break;
        case("createNote"):
          String? noteBody = await APIService.createImageNote(filePath: filePath!);
          if(noteBody == null) {
            android_window.post("serData", {"res": false, "filePath":filePath});
          } else {
            android_window.post("serData", {"res": true, "filePath":filePath, "data": noteBody});
          }
          break;
        case("openPage"):
          if (data == null) {
            Navigator.of(Get.context!).pushAndRemoveUntil(
              MaterialPageRoute(builder: (context) =>const HomeScreen()),
              (Route<dynamic> route) => false,
            );
          } else {
            // log("data.toString() ==== " + data.toString());
            final jsonResponse = jsonDecode(data.toString());
            List<TypeEntry> noteTypes = [];
            final typeList = await APIService().getTypeDict("noteType");
            if (typeList != null) {
                noteTypes = typeList.first.entryList;
            }
            
            eventBus.fire("get_note_list");
            Navigator.push(
              Get.context!,
              CupertinoPageRoute(builder: (_) {
                return AIGenerateScreen(
                  note: NoteModel.fromJson(jsonResponse as Map<String, dynamic>),
                  noteTypes: noteTypes,
                  onNoteUpdated: () {
                    eventBus.fire("get_note_list");
                  },
                  onLocalNoteUpdated: () {
                    eventBus.fire("get_loca_note_list");
                  },
                );
              }),
            );
          }
          break;
        default:
          break;
      }
      return null;
    });
  }
}