import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';

import 'dark_theme_colors.dart';
import 'light_theme_colors.dart';

class MyTheme {
  static setSystemUI() async {
    // only portrait mode
    await SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
    ]);

    // remove status bar bg color
    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle.dark.copyWith(
      statusBarColor: Colors.transparent,
    ));
  }

  static getThemeData() {
    return getThemeIsLight ? getLightThemeData() : getDarkThemeData();
  }

  static getLightThemeData() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      primaryColor: LightThemeColors.primaryColor,
      textTheme: TextTheme(
        bodyLarge: LightThemeColors.textStyle,
        bodyMedium: LightThemeColors.textStyle,
        bodySmall: LightThemeColors.textStyle,
      ),
      colorScheme: const ColorScheme.light().copyWith(
        primary: LightThemeColors.primaryColor,
        secondary: MyColors.colorBlue,
      ),
      scaffoldBackgroundColor: LightThemeColors.scaffoldBackgroundColor,
      appBarTheme: LightThemeColors.appBarTheme,
      bottomNavigationBarTheme: LightThemeColors.bottomNavigationBarTheme,
      textSelectionTheme: TextSelectionThemeData(
        cursorColor: MyColors.colorBlue,
        selectionColor: MyColors.colorBlue.withOpacity(0.3),
        selectionHandleColor: MyColors.colorBlue,
      ),
    );
  }

  static getDarkThemeData() {
    return ThemeData(
      brightness: Brightness.dark,
      useMaterial3: true,
      primaryColor: DarkThemeColors.primaryColor,
      textTheme: TextTheme(
        bodyLarge: DarkThemeColors.textStyle,
        bodyMedium: DarkThemeColors.textStyle,
        bodySmall: DarkThemeColors.textStyle,
      ),
      colorScheme: const ColorScheme.dark().copyWith(
        primary: DarkThemeColors.primaryColor,
      ),
      // splashColor: Colors.transparent,
      // highlightColor: Colors.transparent,
      // hoverColor: Colors.transparent,
      scaffoldBackgroundColor: DarkThemeColors.scaffoldBackgroundColor,
      appBarTheme: DarkThemeColors.appBarTheme,
      bottomNavigationBarTheme: DarkThemeColors.bottomNavigationBarTheme,
      textSelectionTheme: TextSelectionThemeData(
        cursorColor: MyColors.colorBlue,
        selectionColor: MyColors.colorBlue.withOpacity(0.3),
        selectionHandleColor: MyColors.colorBlue,
      ),
    );
  }

  static getAppBar(
      {double? toolbarHeight, Widget? leading, List<Widget>? actions}) {
    return AppBar(
      leading: leading,
      actions: actions,
      toolbarHeight: toolbarHeight,
      systemOverlayStyle: SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness:
            getThemeIsLight ? Brightness.dark : Brightness.light,
        statusBarBrightness:
            getThemeIsLight ? Brightness.dark : Brightness.light,
      ),
    );
  }

  /// update app theme and save theme type to shared pref
  static changeTheme() {
    // *) check if the current theme is light (default is light)
    bool isLightTheme = MyStorage.getThemeIsLight();

    // *) store the new theme mode on get storage
    MyStorage.setThemeIsLight(!isLightTheme);

    // *) let GetX change theme
    Get.changeThemeMode(!isLightTheme ? ThemeMode.light : ThemeMode.dark);
  }

  /// check if the theme is light or dark
  static bool get getThemeIsLight => MyStorage.getThemeIsLight();
}
