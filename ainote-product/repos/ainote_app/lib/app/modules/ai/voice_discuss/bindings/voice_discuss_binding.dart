import 'package:get/get.dart';

import '../controllers/voice_discuss_controller.dart';

class VoiceDiscussBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<VoiceDiscussController>(VoiceDiscussController.new);
  }
}
