
import 'package:get/get.dart';
import 'package:android_window/main.dart' as android_window;

class MineScreenLogic extends GetxController{
  RxBool onState = false.obs;

  @override
  Future<void> onInit() async {
    super.onInit();
  }

  @override
  Future<void> onReady() async {
    super.onReady();
    onRefresh();
  }

  Future<void> onRefresh() async {
    onState.value = await android_window.isRunning();
  }
}