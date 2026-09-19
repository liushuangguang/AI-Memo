import 'package:get/get.dart';

import '../controllers/good_recommend_controller.dart';

class GoodRecommendBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<GoodRecommendController>(
      () => GoodRecommendController(),
    );
  }
}
