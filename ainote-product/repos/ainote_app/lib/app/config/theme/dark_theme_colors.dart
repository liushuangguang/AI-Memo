import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'my_colors.dart';

class DarkThemeColors {
  // PRIMARY
  static const Color primaryColor = MyColors.colorWhite;

  static const TextStyle textStyle = TextStyle(color: MyColors.colorWhite);

  // AppBarTheme
  static AppBarTheme appBarTheme = AppBarTheme(
    elevation: 0.0,
    scrolledUnderElevation: 0.0,
    backgroundColor: Colors.transparent,
    systemOverlayStyle: SystemUiOverlayStyle.light,
    iconTheme: IconThemeData(color: MyColors.colorWhite),
    actionsIconTheme: IconThemeData(color: MyColors.colorWhite),
  );

  //SCAFFOLD
  static const Color scaffoldBackgroundColor =
      MyColors.pageBackgroundBlackColor;
  static const Color backgroundColor = MyColors.colorWhite;

  static BottomNavigationBarThemeData bottomNavigationBarTheme =
      BottomNavigationBarThemeData(
    elevation: 0.0,
    selectedItemColor: MyColors.colorWhite,
    unselectedItemColor: MyColors.thirdColor,
    backgroundColor: MyColors.bottomNavigationBlackColor,
  );

  static ColorScheme colorScheme = ColorScheme.fromSwatch().copyWith(
    brightness: Brightness.dark,
    onPrimary: MyColors.primaryColor,
  );
}
