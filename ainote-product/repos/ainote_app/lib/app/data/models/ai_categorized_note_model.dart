import '../../utils/helper.dart';

class AiCategorizedNoteModel {
  String? moduleId;
  String? sourceNoteId;
  String? noteModuleType;
  List<CategorizedNoteItem>? items;
  String? segmentation;

  AiCategorizedNoteModel({
    this.moduleId,
    this.sourceNoteId,
    this.noteModuleType,
    this.items,
    this.segmentation,
  });

  factory AiCategorizedNoteModel.fromJson(Map<String, dynamic> json) =>
      AiCategorizedNoteModel(
        moduleId: json["moduleId"],
        sourceNoteId: json["sourceNoteId"],
        noteModuleType: json["noteModuleType"],
        items: json["items"] == null
            ? []
            : List<CategorizedNoteItem>.from(
                json["items"].map((x) => CategorizedNoteItem.fromJson(x))),
        segmentation: json["segmentation"],
      );

  factory AiCategorizedNoteModel.mock() => AiCategorizedNoteModel(
        moduleId: generateMockString(10),
        sourceNoteId: generateMockString(10),
        noteModuleType: generateMockString(10),
        items: [
          CategorizedNoteItem.mock(),
          CategorizedNoteItem.mock(),
          CategorizedNoteItem.mock()
        ],
        segmentation: generateMockString(10),
      );

  Map<String, dynamic> toJson() => {
        "moduleId": moduleId,
        "sourceNoteId": sourceNoteId,
        "noteModuleType": noteModuleType,
        "items": items == null
            ? []
            : List<dynamic>.from(items!.map((x) => x.toJson())),
        "segmentation": segmentation,
      };
}

class CategorizedNoteItem {
  String? itemId;
  String? noteModuleType;
  String? key;
  String? value;

  CategorizedNoteItem({
    this.itemId,
    this.noteModuleType,
    this.key,
    this.value,
  });

  factory CategorizedNoteItem.fromJson(Map<String, dynamic> json) =>
      CategorizedNoteItem(
        itemId: json["itemId"],
        noteModuleType: json["noteModuleType"],
        key: json["key"],
        value: json["value"],
      );

  factory CategorizedNoteItem.mock() => CategorizedNoteItem(
        itemId: generateMockString(10),
        noteModuleType: generateMockString(10),
        key: generateMockString(10),
        value: generateMockString(30),
      );

  Map<String, dynamic> toJson() => {
        "itemId": itemId,
        "noteModuleType": noteModuleType,
        "key": key,
        "value": value,
      };
}
