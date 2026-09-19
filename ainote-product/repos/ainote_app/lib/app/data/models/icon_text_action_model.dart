import 'package:ainote_app/app/utils/date_format.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:uuid/uuid.dart';

class IconTextActionModel {
  late String id;
  String content;
  String? description;
  String? scheduledAt;
  bool? done;
  String? emoji;
  List<ExtraIconModel>? extraIcons; // 比如： 高德地图icon、拼多多icon;
  bool? noIcon;

  IconTextActionModel({
    this.scheduledAt,
    this.done,
    this.id = '',
    required this.content,
    this.description,
    this.emoji,
    this.extraIcons,
    this.noIcon,
  }) {
    id = id == '' ? const Uuid().v4() : id;
  }

  factory IconTextActionModel.fromJson(Map<String, dynamic> json) {
    return IconTextActionModel(
      id: json['id'] ?? json['itemId'] ?? const Uuid().v4(),
      content: json['content'] ?? json['suggestion'] ?? '',
      scheduledAt: parseApiDateString(json['scheduledAt'] ?? ''),
      done: json['done'] ?? false,
      description: json['description'] ?? '',
      emoji: json['emoji'],
      extraIcons: [],
      noIcon: isEmptyString(json['emoji']) &&
          isEmptyString(json['description']) &&
          isEmptyString(json['scheduledAt']),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'content': content,
        'scheduledAt': scheduledAt,
        'done': done,
        'description': description,
        'emoji': emoji,
        'extraIcons': extraIcons?.map((e) => e.toJson()).toList(),
        'noIcon': noIcon,
      };
}

class ExtraIconModel {
  String? icon;
  String? link;

  ExtraIconModel({this.icon, this.link});

  Map<String, dynamic> toJson() => {
        'icon': icon,
        'link': link,
      };
}
