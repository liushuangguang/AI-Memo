import 'note_module_payload.dart';

class QuestionAnswerModule extends NoteModulePayload {
  String? moduleId;
  String? title;
  String? description;
  List<QuestionAnswerItem>? questionAnswerItems;

  @override
  String get noteModuleType =>
      NoteModuleType.QUESTION_ANSWER.toString().split('.').last;

  QuestionAnswerModule({
    this.moduleId,
    this.title,
    this.description,
    this.questionAnswerItems,
  });

  factory QuestionAnswerModule.fromJson(Map<String, dynamic> json) {
    return QuestionAnswerModule(
      moduleId: json['moduleId'],
      title: json['title'],
      description: json['description'],
      questionAnswerItems: (json['items'] ?? json['questionAnswerItems'])
          ?.map<QuestionAnswerItem>((e) => QuestionAnswerItem.fromJson(e))
          .toList(),
    );
  }

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'title': title,
      'description': description,
      'questionAnswerItems':
          questionAnswerItems?.map((e) => e.toJson()).toList(),
    };
  }
}

class QuestionAnswerItem {
  String? question;
  String? answer;

  NoteModuleType noteModuleType;

  QuestionAnswerItem({
    this.question,
    this.answer,
    this.noteModuleType = NoteModuleType.QUESTION_ANSWER,
  });

  factory QuestionAnswerItem.fromJson(Map<String, dynamic> json) {
    return QuestionAnswerItem(
      question: json['question'],
      answer: json['answer'],
      noteModuleType: NoteModuleType.QUESTION_ANSWER,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'question': question,
      'answer': answer,
      'noteModuleType': noteModuleType.toString().split('.').last,
    };
  }
}
