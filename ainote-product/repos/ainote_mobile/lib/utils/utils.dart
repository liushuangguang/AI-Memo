import 'dart:convert';

import 'package:ainote/constants/app_images.dart';
import 'package:ainote/models/ai_generate_model.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:markdown/markdown.dart' as md;
import 'package:share_plus/share_plus.dart';

import '../services/keychain_service.dart';

// Future<String?> saveImageToDocumentDirectory(XFile image) async {
//   try {
//     // 获取应用程序的文档目录
//     final directory = await getApplicationDocumentsDirectory();
//
//     // 获取当前时间戳
//     final timestamp = DateTime.now().millisecondsSinceEpoch.toString();
//
//     // 创建一个带有时间戳文件名的文件路径
//     final filePath = '${directory.path}/image_$timestamp.jpg';
//
//     // 将图像文件拷贝到文档目录
//     final File newImage = await File(image.path).copy(filePath);
//
//     // 返回保存后的文件路径
//     return newImage.path;
//   } catch (e) {
//     // 捕获任何错误并处理
//     print('保存图像时出错: $e');
//     return null;
//   }
// }

List<String> extractLinks(String input) {
  final RegExp linkRegExp = RegExp(
    r'(http|https)://[a-zA-Z0-9./?=_-]+',
    caseSensitive: false,
  );

  final Iterable<RegExpMatch> matches = linkRegExp.allMatches(input);

  return matches.map((match) => match.group(0)!).toList();
}

String markdownToHtml(String markdown) {
  return md.markdownToHtml(markdown);
}

AIGenerateModel parseAIGenerate(String input) {
  final RegExp elementPattern = RegExp(r'【(.*?)】\n(.*?)\n', dotAll: true);
  final Map<String, String> elements = {};

  for (final match in elementPattern.allMatches(input)) {
    final key = match.group(1)!.trim();
    final value = match.group(2)!.trim().replaceAll('\n\n', '\n');
    elements[key] = value;
  }

  return AIGenerateModel(
    title: elements['备忘录标题'],
    scene: elements['用户记录意图/情景'],
    content: elements['优化版正文'],
    suggestion: elements['建议信息'],
    tag: elements['标签'],
    category: elements['备忘录分类'],
    todo: elements['时间计划'],
    all: input,
  );
}

String aiTypeToText(int type) {
  /// analysisType: 1-一键整理，2-内容辅助，3-语音讨论
  if (type == 1) {
    return "一键整理";
  } else if (type == 2) {
    return "内容辅助";
  } else if (type == 3) {
    return "语音讨论";
  } else {
    return "未知类型";
  }
}

String aiTypeImageAsset(int type) {
  if (type == 1) {
    return AppImages.historyAIGenerateIcon.path;
  } else if (type == 2) {
    return AppImages.historyAIAssistantIcon.path;
  } else {
    return AppImages.historyAIVoiceIcon.path;
  }
}

String getMDShowingString(String text) {
  return text
      .replaceAll("- ", "\n- ")
      .replaceAll("### ", "\n### ")
      .replaceAll("## ", "\n## ")
      .replaceAll("# ", "\n# ");
}

String getCategory(String category) {
  final RegExp regExp = RegExp(r'\(([^)]+)\)');
  final match = regExp.firstMatch(category);
  if (match != null) {
    return match.group(1)!;
  }
  return '';
}

String getNoteTitle(String note) {
  RegExp regExp = RegExp(r'标题：(.+)\n');
  Match? match = regExp.firstMatch(note);
  return match != null ? match.group(1) ?? '' : '';
}

String getNoteBody(String note) {
  RegExp regExp = RegExp(r'正文：(.+)', dotAll: true);
  Match? match = regExp.firstMatch(note);
  return match != null ? match.group(1) ?? '' : '';
}

String getCategorizedReason(String response) {
  final RegExp regExp = RegExp(r'● 归类原因：(.*?)\n●', dotAll: true);
  final match = regExp.firstMatch(response);

  return match != null ? match.group(1)?.trim() ?? "" : "";
}

String getCategorizedTitle(String response) {
  final RegExp regExp = RegExp(r'● 信息分类：(.*?)\n●', dotAll: true);
  final match = regExp.firstMatch(response);

  if (match != null) {
    String title = match.group(1)?.trim() ?? "";
    title = title.replaceAll(RegExp(r'\s*\（.*?\）'), '');
    return title;
  }
  return "";
}

String getCategorizedContent(String response) {
  // 先去掉“● 信息分类”和“● 归类原因”的内容
  response = response.replaceAll(RegExp(r'● 信息分类：.*?\n'), '');
  response = response.replaceAll(RegExp(r'● 归类原因：.*?\n'), '');

  // 使用正则表达式找到所有“● xxx”的内容
  final RegExp regExp = RegExp(r'● (.*?)：(.*?)\n', dotAll: true);
  final matches = regExp.allMatches(response);

  // 构建剩下的内容
  StringBuffer contentBuffer = StringBuffer();
  for (final match in matches) {
    final String sectionTitle = match.group(1)?.trim() ?? "";
    final String sectionContent = match.group(2)?.trim() ?? "";

    // 过滤掉内容为“（空）”的部分
    if (sectionContent != '（空）') {
      contentBuffer.writeln('● $sectionTitle：$sectionContent');
    }
  }

  return contentBuffer.toString().trim();
}

String formatDateTimeFromDateTime(DateTime dateTime,
    {bool showCharacter = true, bool showYear = false}) {
  String month = dateTime.month.toString();
  String day = dateTime.day.toString();
  String hour = dateTime.hour.toString();
  String minute = dateTime.minute.toString().padLeft(2, '0');

  return showCharacter
      ? showYear
          ? "${dateTime.year}年$month月$day日 $hour:$minute"
          : "$month月$day日 $hour:$minute"
      : showYear
          ? "${dateTime.year}/$month/$day $hour:$minute"
          : "$month/$day $hour:$minute";
}

String formatDateTime(String? dateTimeString, {bool showCharacter = true}) {
  if (dateTimeString == "" ||
      dateTimeString == "null" ||
      dateTimeString == null) {
    return getCurrentTime(showCharacter: showCharacter);
  }
  DateTime dateTime = DateTime.parse(dateTimeString);

  String month = dateTime.month.toString();
  String day = dateTime.day.toString();
  String hour = dateTime.hour.toString();
  String minute = dateTime.minute.toString().padLeft(2, '0');

  return showCharacter
      ? "$month月$day日 $hour:$minute"
      : "$month/$day $hour:$minute";
}

String getCurrentTime({bool showCharacter = true}) {
  DateTime now = DateTime.now();
  int month = now.month;
  int day = now.day;
  int hour = now.hour;
  int minute = now.minute;

  String formattedTime = showCharacter
      ? "$month月$day日 $hour:${minute.toString().padLeft(2, '0')}"
      : "$month/$day $hour:${minute.toString().padLeft(2, '0')}";
  return formattedTime;
}

List<T> parseData<T>(String input, T Function(Map<String, dynamic>) fromJson) {
  final items =
      input.split('data:').where((element) => element.trim().isNotEmpty);

  return items.map((item) {
    final jsonData = item.trim();
    return fromJson(json.decode(jsonData));
  }).toList();
}

String removeNewLinesAndSpaces(String input) {
  return input.replaceAll(RegExp(r'\s+\b\b|\b\s+'), '');
}

void shareH5(String noteId) async {
  final deviceId = await KeyChainService().getDeviceId() ?? "";

  if (deviceId.isEmpty || noteId.isEmpty) {
    EasyLoading.showToast('分享失败，请稍后再试');
    return;
  }

  Share.share(
      '我正在使用【智能备忘笔记】，快来看看吧！https://aifunc.top/h5/share.html?noteId=$noteId&deviceId=$deviceId');
}

void deleteQuillCustomEmbedNode(QuillController controller, Embed node) {
  controller.document.delete(
    node.documentOffset,
    node.length,
  );
}

void insertQuillCustomEmbedNode(QuillController controller, Embeddable data) {
  controller.updateSelection(
      TextSelection(
          baseOffset: controller.document.length,
          extentOffset: controller.document.length),
      ChangeSource.local);

  controller.document.insert(
    controller.selection.extentOffset,
    data,
  );

  controller.document.insert(
    controller.selection.extentOffset + 1,
    '\n',
  );

  controller.updateSelection(
    TextSelection.collapsed(
      offset: controller.selection.extentOffset + 1,
    ),
    ChangeSource.local,
  );
}

DateTime? parseDateString(String dateString) {
  try {
    return DateTime.parse(dateString);
  } catch (e) {
    return parseDateWithRegex(dateString);
  }
}

DateTime? parseDateWithRegex(String dateString) {
  final regex = RegExp(
      r'(\d{4})[-/年]?(\d{1,2})?[-/月日]?(\d{0,2})[Tt\s]*(\d{1,2})?:?(\d{1,2})?:?(\d{1,2})?[.:]?(\d+)?');
  final match = regex.firstMatch(dateString);

  print(match);
  if (match == null) {
    return null;
  }

  final year = int.parse(match.group(1) ?? '0');
  final month = int.parse(match.group(2) ?? '0');
  final day = int.parse(match.group(3) ?? '0');
  final hour = match.group(4) != null ? int.parse(match.group(4) ?? '0') : 0;
  final minute = match.group(5) != null ? int.parse(match.group(5) ?? '0') : 0;
  final second = match.group(6) != null ? int.parse(match.group(6) ?? '0') : 0;

  return DateTime(year, month, day, hour, minute, second);
}
