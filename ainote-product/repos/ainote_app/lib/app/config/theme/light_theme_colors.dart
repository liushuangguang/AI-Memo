import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'my_colors.dart';

class LightThemeColors {
  // PRIMARY
  static const Color primaryColor = MyColors.primaryColor;

  static const TextStyle textStyle = TextStyle(color: MyColors.primaryColor);

  // AppBarTheme
  static AppBarTheme appBarTheme = AppBarTheme(
    elevation: 0.0,
    scrolledUnderElevation: 0.0,
    backgroundColor: Colors.transparent,
    systemOverlayStyle: SystemUiOverlayStyle.dark,
    iconTheme: IconThemeData(color: MyColors.primaryColor),
    actionsIconTheme: IconThemeData(color: MyColors.primaryColor),
  );

  //SCAFFOLD
  static const Color scaffoldBackgroundColor = MyColors.pageBackgroundColor;
  static const Color backgroundColor = MyColors.colorWhite;

  static BottomNavigationBarThemeData bottomNavigationBarTheme =
      BottomNavigationBarThemeData(
    elevation: 0.0,
    selectedItemColor: MyColors.primaryColor,
    unselectedItemColor: MyColors.thirdColor,
    backgroundColor: MyColors.bottomNavigationColor,
  );

  static ColorScheme colorScheme = ColorScheme.fromSwatch().copyWith(
    brightness: Brightness.light,
    onPrimary: MyColors.primaryColor,
  );
}
