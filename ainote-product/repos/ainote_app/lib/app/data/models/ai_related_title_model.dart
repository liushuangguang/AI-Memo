import 'package:ainote_app/app/utils/helper.dart';

class AiRelatedTitleModel {
  String? title;
  String? emoji;

  AiRelatedTitleModel({
    this.title,
    this.emoji,
  });

  factory AiRelatedTitleModel.mock() {
    return AiRelatedTitleModel(
      title: generateMockString(20),
      emoji: "😀",
    );
  }

  factory AiRelatedTitleModel.fromJson(Map<String, dynamic> json) {
    return AiRelatedTitleModel(
      title: json['title'],
      emoji: json['emoji'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'emoji': emoji,
    };
  }
}
