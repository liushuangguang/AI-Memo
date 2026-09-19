import 'package:ainote_app/app/modules/home/views/note_add_view.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:flutter/material.dart';

import 'package:get/get.dart';
import '../../note/note_list1/bindings/note_list_binding.dart';
import '../../note/note_list1/views/note_list_view.dart';
import '../controllers/home_controller.dart';

class HomeView extends GetView<HomeController> {
  const HomeView({super.key});

  @override
  Widget build(BuildContext context) {
    Get.put(HomeController());
    return Scaffold(
      body: Navigator(
        key: Get.nestedKey(1),
        initialRoute: Routes.NOTE_ADD,
        onGenerateRoute: (RouteSettings settings) {
          if (settings.name == Routes.NOTE_ADD) {
            return GetPageRoute(
              settings: settings,
              page: () => const NoteAddView(),
              transition: Transition.upToDown,
            );
          } else if (settings.name == Routes.NOTE_LIST) {
            return GetPageRoute(
              settings: settings,
              page: () => NoteListView(),
              binding: NoteListBinding(),
              transition: Transition.downToUp,
            );
          }
          return null;
        },
      ),
    );
  }
}
