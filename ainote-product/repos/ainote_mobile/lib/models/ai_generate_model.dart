import 'package:any_link_preview/any_link_preview.dart';
import 'package:favicon/favicon.dart';

class AILinkModel {
  String? text;
  String? link;

  AILinkModel({
    this.text,
    this.link,
  });

  factory AILinkModel.fromJson(Map<String, dynamic> json) {
    return AILinkModel(
      text: json['text'] as String?,
      link: json['link'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'text': text,
      'link': link,
    };
  }
}

class AIGuessModel {
  String? relatedInfo;
  String? fieldInfo;

  AIGuessModel({
    this.relatedInfo,
    this.fieldInfo,
  });

  factory AIGuessModel.fromJson(Map<String, dynamic> json) {
    return AIGuessModel(
      relatedInfo: json['relatedInfo'] as String?,
      fieldInfo: json['fieldInfo'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'relatedInfo': relatedInfo,
      'fieldInfo': fieldInfo,
    };
  }

}

class AIGenerateModel {
  String all;
  String? title;
  String? content;
  String? suggestion;
  String? guess;
  String? todo;
  String? scene;
  String? category;
  String? tag;
  List<String>? links;
  List<Metadata?>? linkMetadatas;
  List<Favicon?>? favIco;
  AIGuessModel? aiGuess;
  List<AILinkModel>? aiLink;
  String? imageLink;

  AIGenerateModel({
    required this.all,
    this.title,
    this.content,
    this.suggestion,
    this.guess,
    this.todo,
    this.scene,
    this.category,
    this.tag,
    this.links,
    this.linkMetadatas,
    this.favIco,
    this.aiGuess,
    this.aiLink,
    this.imageLink,
  });

  factory AIGenerateModel.fromJson(Map<String, dynamic> json) {
    return AIGenerateModel(
      all: json['all'] as String,
      title: json['title'] as String?,
      content: json['content'] as String?,
      suggestion: json['suggestion'] as String?,
      guess: json['guess'] as String?,
      todo: json['todo'] as String?,
      scene: json['scene'] as String?,
      category: json['category'] as String?,
      tag: json['tag'] as String?,
      links: json['links'] != null ? List<String>.from(json['links']) : null,
      linkMetadatas: json['linkMetadatas'] != null ? List<Metadata>.from(json['linkMetadatas']) : null,
      favIco: json['favIco'] != null ? List<Favicon>.from(json['favIco']) : null,
      aiGuess: json['aiGuess'] != null ? AIGuessModel.fromJson(json['aiGuess']) : null,
      aiLink: json['aiLink'] != null ? List<AILinkModel>.from(json['aiLink']) : null,
      imageLink: json['imageLink'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'all': all,
      'title': title,
      'content': content,
      'suggestion': suggestion,
      'guess': guess,
      'todo': todo,
      'scene': scene,
      'category': category,
      'tag': tag,
      'links': links,
      'linkMetadatas': linkMetadatas,
      'favIco': favIco,
      'aiGuess': aiGuess?.toJson(),
      'aiLink': aiLink?.map((e) => e.toJson()).toList(),
      'imageLink': imageLink,
    };
  }
}