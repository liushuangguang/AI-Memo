import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

final kTheme = _buildTheme();

const _primaryColor = Color(0xFF12102F);
const _onPrimaryColor = Color(0xFFFDFCFF);
const _secondaryColor = Color(0xFFDFDEE8);
const _onSecondaryColor = Color(0xFF12102F);

ThemeData _buildTheme() {
  var colorScheme = const ColorScheme.light().copyWith(
    primary: _primaryColor,
    secondary: _secondaryColor,
    onPrimary: _onPrimaryColor,
    onSecondary: _onSecondaryColor,
  );
  return ThemeData(
    useMaterial3: true,
    primaryColor: _primaryColor,
    brightness: Brightness.light,
    colorScheme: colorScheme,
    splashColor: Colors.transparent,
    highlightColor: Colors.transparent,
    hoverColor: Colors.transparent,
    appBarTheme: const AppBarTheme(
      elevation: 0.0,
      scrolledUnderElevation: 0.0,
      backgroundColor: Colors.transparent,
      systemOverlayStyle: SystemUiOverlayStyle.dark,
    ),
    textSelectionTheme: TextSelectionThemeData(
      cursorColor: Colors.blueAccent, // 设置光标的颜色
      selectionColor: Colors.blue.withOpacity(0.3), // 设置选中文本的背景色
      selectionHandleColor: Colors.blueAccent, // 设置选中文本的边框颜色
    ),
  );
}
