import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/widgets/todo_list.dart';
import 'package:get/get.dart';

class GalleryController extends GetxController {
  //TODO: Implement GalleryController

  final count = 0.obs;
  final list = <ToDoItem>[].obs;

  @override
  void onInit() {
    super.onInit();
  }

  @override
  void onReady() {
    super.onReady();
  }

  @override
  void onClose() {
    super.onClose();
  }

  void increment() => count.value++;

  void addItem(TodoModel item) {
    list.add(ToDoItem(todo: item));
  }
}
