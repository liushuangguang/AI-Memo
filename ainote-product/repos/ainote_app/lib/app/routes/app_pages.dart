import 'package:get/get.dart';

import '../modules/ai/content_assist/bindings/content_assist_binding.dart';
import '../modules/ai/content_assist/views/content_assist_view.dart';
import '../modules/ai/smart_organize/bindings/smart_organize_binding.dart';
import '../modules/ai/smart_organize/views/smart_organize_view.dart';
import '../modules/home/bindings/home_binding.dart';
import '../modules/home/views/home_view.dart';
import '../modules/login/bindings/login_binding.dart';
import '../modules/login/views/login_view.dart';
import '../modules/note/note_edit/bindings/note_edit_binding.dart';
import '../modules/note/note_edit/views/note_edit_view.dart';
import '../modules/note/note_list1/bindings/note_list_binding.dart';
import '../modules/note/note_list1/views/note_list_view.dart';
import '../modules/root/bindings/root_binding.dart';
import '../modules/root/views/root_view.dart';
import '../modules/settings/bindings/settings_binding.dart';
import '../modules/settings/views/settings_view.dart';
import '../modules/todo/bindings/todo_binding.dart';
import '../modules/todo/views/todo_view.dart';
import '../modules/ai/ai_additional/views/ai_additional_view.dart';
import '../modules/ai/ai_additional/controllers/ai_additional_controller.dart';
import '../modules/vip/bindings/vip_binding.dart';
import '../modules/vip/views/vip_view.dart';

part 'app_routes.dart';

class AppPages {
  AppPages._();

  // static const INITIAL = Routes.SETTINGS;
  // static const INITIAL = Routes.LOGIN;
  static const INITIAL = Routes.ROOT;

  static final routes = [
    GetPage(
      name: _Paths.ROOT,
      page: () => const RootView(),
      binding: RootBinding(),
    ),
    GetPage(
      name: _Paths.HOME, // 主页
      page: () => const HomeView(),
      binding: HomeBinding(),
    ),
    GetPage(
      name: _Paths.NOTE_LIST,
      page: () => NoteListView(),
      binding: NoteListBinding(),
    ),
    GetPage(
      name: _Paths.TODO, // 待办
      page: () => const TodoView(),
      binding: TodoBinding(),
    ),
    GetPage(
      name: _Paths.SETTINGS, // 我的
      page: () => const SettingsView(),
      binding: SettingsBinding(),
    ),
    GetPage(
      name: _Paths.LOGIN,
      page: () => const LoginView(),
      binding: LoginBinding(),
    ),
    GetPage(
      name: _Paths.NOTE_EDIT,
      page: () => const NoteEditView(),
      binding: NoteEditBinding(),
    ),
    GetPage(
      name: _Paths.SMART_ORGANIZE,
      page: () => const SmartOrganizeView(),
      binding: SmartOrganizeBinding(),
    ),
    GetPage(
      name: _Paths.CONTENT_ASSIST,
      page: () => const ContentAssistView(),
      binding: ContentAssistBinding(),
    ),
    GetPage(
      name: '/ai-additional',
      page: () => AiAdditionalView(
        noteContent: Get.arguments['noteContent'],
        toAdditional: Get.arguments['toAdditional'],
        toAdditionalsOptions: Get.arguments['toAdditionalsOptions'],
      ),
      binding: BindingsBuilder(() {
        Get.lazyPut<AiAdditionalController>(
          () => AiAdditionalController(),
        );
      }),
    ),
    GetPage(
      name: _Paths.VIP,
      page: () => const VipView(),
      binding: VipBinding(),
    ),
  ];
}
