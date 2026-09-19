import 'package:get/get.dart';

import '../controllers/note_list_controller.dart';

class NoteListBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<NoteListController>(
      () => NoteListController(),
    );
  }
}
