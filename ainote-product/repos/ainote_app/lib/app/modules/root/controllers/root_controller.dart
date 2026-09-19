import 'package:get/get.dart';

import '../../../../generated/assets.dart';
import '../../../routes/app_pages.dart';

class RootController extends GetxController {
  RxInt tabIndex = 0.obs;
  RxBool bottomBarVisible = true.obs;
  List<Map<String, dynamic>> tabItems = [
    {
      'label': '备忘',
      'route': Routes.HOME,
      'icon': Assets.homeBottomIconHome,
      'activeIcon': Assets.homeBottomIconHomeActive,
    },
    {
      'label': '待办',
      'route': Routes.TODO,
      'icon': Assets.homeBottomIconCheck,
      'activeIcon': Assets.homeBottomIconCheckActive,
    },
    {
      'label': '我的',
      'route': Routes.SETTINGS,
      'icon': Assets.homeBottomIconSettings,
      'activeIcon': Assets.homeBottomIconSettingsActive,
    },
  ];

  jumpPageIndex(int index) {
    tabIndex.value = index;
  }

  setBottomBarVisible(bool visible) {
    bottomBarVisible.value = visible;
  }
}
