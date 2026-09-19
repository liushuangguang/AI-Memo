import 'package:ainote_app/app/config/theme/my_theme.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'package:get/get.dart';

import '../../home/views/home_view.dart';
import '../../todo/views/todo_view.dart';
import '../../settings/views/settings_view.dart';
import '../controllers/root_controller.dart';

class RootView extends GetView<RootController> {
  const RootView({super.key});

  @override
  Widget build(BuildContext context) {
    DateTime? lastTime;
    return Obx(() {
      return PopScope(
        canPop: false,
        onPopInvokedWithResult: (bool didPop, result) {
          if (lastTime == null) {
            lastTime = DateTime.now();
            Toast.info('再按一次退出应用');
          } else {
            if (DateTime.now().difference(lastTime!) >
                const Duration(seconds: 2)) {
              lastTime = DateTime.now();
              Toast.info('再按一次退出应用');
            } else {
              lastTime = null;
              SystemNavigator.pop(animated: true);
            }
          }
        },
        child: Scaffold(
            appBar: MyTheme.getAppBar(toolbarHeight: 0),
            body: SafeArea(
              child: IndexedStack(
                index: controller.tabIndex.value,
                children: [
                  const HomeView(),
                  const TodoView(),
                  const SettingsView(),
                ],
              ),
            ),
            bottomNavigationBar: controller.bottomBarVisible.value
                ? BottomNavigationBar(
                    elevation: 0,
                    type: BottomNavigationBarType.fixed,
                    currentIndex: controller.tabIndex.value,
                    onTap: controller.jumpPageIndex,
                    items: [
                        for (var d in controller.tabItems)
                          BottomNavigationBarItem(
                              icon: Image.asset(
                                d['icon'],
                                width: 32.w,
                                height: 32.w,
                              ),
                              activeIcon: Image.asset(
                                d['activeIcon'],
                                width: 32.w,
                                height: 32.w,
                              ),
                              label: d['label'])
                      ])
                : null),
      );
    });
  }
}
