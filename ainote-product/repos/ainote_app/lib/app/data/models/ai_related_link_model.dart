import 'package:ainote_app/app/utils/helper.dart';

class AiRelatedLinkModel {
  String? link;
  String? linkName;
  String? icon;
  String? snippet;

  AiRelatedLinkModel({
    this.link,
    this.linkName,
    this.icon,
    this.snippet,
  });

  factory AiRelatedLinkModel.mock() {
    return AiRelatedLinkModel(
      link: generateMockString(12),
      linkName: generateMockString(20),
      icon: "",
      snippet: generateMockString(32),
    );
  }

  factory AiRelatedLinkModel.fromJson(Map<String, dynamic> json) {
    return AiRelatedLinkModel(
      link: json['link'],
      linkName: json['linkName'],
      icon: getFaviconUrl(json['link']),
      snippet: json['snippet'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'link': link,
      'linkName': linkName,
      'icon': icon,
      'snippet': snippet,
    };
  }
}
