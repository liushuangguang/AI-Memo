import 'package:ainote_app/app/api/vip.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/vip_points_model.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:ainote_app/app/modules/login/controllers/auth_transition.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

class SettingsController extends GetxController {
  final phoneNum = ''.obs;
  final points = '0.00'.obs;
  final totalPoints = 100.0.obs;
  final percent = 0.0.obs;
  final pointsHistoryAll = <PointsHistory?>[].obs;
  final pointsHistoryShort = <PointsHistory?>[].obs;

  late ScrollController scrollController;

  @override
  void onInit() {
    super.onInit();
    getPoints();

    phoneNum.value = MyStorage.getPhoneNumber() ?? '';

    scrollController = ScrollController();
  }

  Future<void> getPoints() async {
    var res = await VipApi.getPoints();
    // 87.11600000000001 保留2位小数
    if (res.points != null) {
      points.value = res.points!.toStringAsFixed(2);
      percent.value = res.points! / totalPoints.value;
      pointsHistoryAll.value = res.pointsHistory ?? [];
      pointsHistoryShort.value = pointsHistoryAll.sublist(
        0,
        pointsHistoryAll.length < 5 ? pointsHistoryAll.length : 5,
      );
    }
  }

  Future<void> refreshPage() async {
    getPoints();
  }

  Future<void> updatePoints() async {
    await VipApi.updatePoints();
    getPoints();
  }

  Future<void> logout() async {
    await completeLogoutTransition(
      deleteAuthorization: MyStorage.deleteAuthToken,
      installHeaders: MyDio.setAuthHeaders,
      enterLoggedOutApp: () => Get.offAllNamed(Routes.LOGIN),
    );
  }
}
