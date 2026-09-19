import 'package:ainote_app/app/data/models/note_module/text_data_module.dart';

enum NoteModuleType {
  TEXT_DATA, // 正文
  RELATED_NOTE, // 相关笔记
  AI_PICTURE, // AI图片
  QUESTION_ANSWER, // 猜你想看
  BACKLOG, // 待办
  AI_RECOMMENDATION, // AI推荐
  KEY_VALUE_PAIR, // 键值对
  SCENARIO_RECORD, // 情景模式
}

enum RecommendationItemType { PRODUCT }

abstract class NoteModulePayload {
  String? moduleId;

  String get noteModuleType;

  Map<String, dynamic> toJson();

  NoteModulePayload();

  factory NoteModulePayload.fromJson(Map<String, dynamic> json) {
    throw UnimplementedError();
  }
}

T? findNoteModuleByType<T extends NoteModulePayload>(
    List<dynamic>? list,
    NoteModuleType type,
    dynamic Function(Map<String, dynamic> json) jsonParser) {
  try {
    var data = list?.firstWhere((element) {
      return element?['noteModuleType'] == type.toString().split('.').last;
    });
    if (data != null) {
      return jsonParser(data);
    } else {
      return null;
    }
  } catch (_) {
    return null;
  }
}

int? findNoteModuleIndexByType<T extends NoteModulePayload>(
    List<dynamic>? list,
    NoteModuleType type,
    dynamic Function(Map<String, dynamic> json) jsonParser) {
  try {
    var index = list?.indexWhere((element) {
      return element?['noteModuleType'] == type.toString().split('.').last;
    });
    return index;
  } catch (_) {
    return null;
  }
}

(int? index, T? data) findNoteModuleByTypeAndIndex<T extends NoteModulePayload>(
    List<dynamic>? list,
    NoteModuleType type,
    dynamic Function(Map<String, dynamic> json) jsonParser) {
  try {
    var index = findNoteModuleIndexByType(list, type, jsonParser);
    var data = index != null ? jsonParser(list![index]) : null;
    return (index, data);
  } catch (_) {
    return (null, null);
  }
}

bool findNoteModuleByTypeAndText(
    List<dynamic>? list, NoteModuleType type, String text) {
  try {
    return list?.any((element) {
          return element?['noteModuleType'] ==
                  type.toString().split('.').last &&
              element?['text'] == text;
        }) ==
        true;
  } catch (_) {
    return false;
  }
}

bool findNoteModuleByTypeAndItemText(
    List<dynamic>? list, NoteModuleType type, String text) {
  try {
    return list?.any((element) {
          return element?['noteModuleType'] ==
                  type.toString().split('.').last &&
              element?['items']?[0]?['title'] == text;
        }) ==
        true;
  } catch (_) {
    return false;
  }
}
