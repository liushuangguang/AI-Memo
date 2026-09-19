import 'dart:math';

import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';

String randomId() {
  return const Uuid().v4();
}

// 生成固定长度的随机字符串
String generateMockString([int length = 10]) {
  final StringBuffer buffer = StringBuffer();
  final Random random = Random();
  for (int i = 0; i < length; i++) {
    buffer.write(String.fromCharCode(random.nextInt(26) + 97));
  }
  return '[Mock] ${buffer.toString()}';
}

int generateRandomInt(int min, int max) {
  final Random random = Random();
  return min + random.nextInt(max - min + 1);
}

// 判断是否为 mock string
bool isMockData(String? str) => str != null && str.startsWith('[Mock]');

// 根据网站链接获取 favicon 图标
String? getFaviconUrl(String? url) {
  if (url == null || url.isEmpty) return null;
  String origin = Uri.parse(url).origin;
  return origin.isNotEmpty ? '$origin/favicon.ico' : null;
}

bool isEmptyString(String? str) {
  return str == null || str.isEmpty;
}
