import 'package:get/get.dart';

import '../controllers/note_edit_controller.dart';

class NoteEditBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<NoteEditController>(
      () => NoteEditController(),
    );
  }
}
