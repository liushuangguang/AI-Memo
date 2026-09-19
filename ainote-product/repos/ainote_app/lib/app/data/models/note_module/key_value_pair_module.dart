import 'note_module_payload.dart';

class KeyValuePairModule extends NoteModulePayload {
  String? moduleId;
  String? sourceNoteId;
  String? description;
  List<KeyValuePairItem>? keyValueItems;

  @override
  String get noteModuleType =>
      NoteModuleType.KEY_VALUE_PAIR.toString().split('.').last;

  KeyValuePairModule({
    this.moduleId,
    this.sourceNoteId,
    this.description,
    this.keyValueItems,
  });

  // 从 JSON 数据创建对象
  factory KeyValuePairModule.fromJson(Map<String, dynamic> json) {
    return KeyValuePairModule(
      moduleId: json['moduleId'],
      sourceNoteId: json['sourceNoteId'],
      description: json['description'],
      keyValueItems: json['keyValueItems'] != null
          ? (json['keyValueItems'] as List)
              .map((e) => KeyValuePairItem.fromJson(e))
              .toList()
          : null,
    );
  }

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'sourceNoteId': sourceNoteId,
      'description': description,
      'keyValueItems': keyValueItems?.map((e) => e.toJson()).toList(),
    };
  }
}

class KeyValuePairItem {
  String? itemId;
  String? segmentation;
  String? key;
  String? value;

  KeyValuePairItem({
    this.itemId,
    this.segmentation,
    this.key,
    this.value,
  });

  factory KeyValuePairItem.fromJson(Map<String, dynamic> json) {
    return KeyValuePairItem(
      itemId: json['itemId'],
      segmentation: json['segmentation'],
      key: json['key'],
      value: json['value'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'itemId': itemId,
      'segmentation': segmentation,
      'key': key,
      'value': value,
    };
  }
}
