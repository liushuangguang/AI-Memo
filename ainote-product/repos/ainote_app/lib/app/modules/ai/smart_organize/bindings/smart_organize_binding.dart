import 'package:get/get.dart';

import '../controllers/smart_organize_controller.dart';

class SmartOrganizeBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<SmartOrganizeController>(
      () => SmartOrganizeController(),
    );
  }
}
