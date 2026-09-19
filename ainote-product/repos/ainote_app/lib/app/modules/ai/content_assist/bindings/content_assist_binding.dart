import 'package:get/get.dart';

import '../controllers/content_assist_controller.dart';

class ContentAssistBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<ContentAssistController>(
      () => ContentAssistController(),
    );
  }
}
