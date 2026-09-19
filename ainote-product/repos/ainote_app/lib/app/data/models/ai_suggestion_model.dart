import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/utils/date_format.dart';
import 'package:ainote_app/app/utils/helper.dart';

List<T> _jsonList<T>(
  dynamic value,
  T Function(Map<String, dynamic>) parser,
) {
  final entries = value is List
      ? value
      : value is Map<String, dynamic>
          ? [value]
          : const [];
  return entries
      .whereType<Map>()
      .map((entry) => parser(Map<String, dynamic>.from(entry)))
      .toList();
}

class AiSuggestionModel {
  List<OverallSuggestionsModel>? overallSuggestions;
  List<OverallToDoItem>? overallToDoItems;
  List<SpecificSuggestionsModel>? specificSuggestions;
  List<SpecificToDoItem>? specificToDoItems;

  AiSuggestionModel({
    this.overallSuggestions,
    this.overallToDoItems,
    this.specificSuggestions,
    this.specificToDoItems,
  });

  factory AiSuggestionModel.fromJson(Map<String, dynamic> json) {
    return AiSuggestionModel(
      overallSuggestions: _jsonList(
        json['overall_Suggestions'],
        OverallSuggestionsModel.fromJson,
      ),
      overallToDoItems: _jsonList(
        json['overall_ToDo_Items'],
        OverallToDoItem.fromJson,
      ),
      specificSuggestions: _jsonList(
        json['specific_Suggestions'],
        SpecificSuggestionsModel.fromJson,
      ),
      specificToDoItems: _jsonList(
        json['specific_ToDo_Items'],
        SpecificToDoItem.fromJson,
      ),
    );
  }

  factory AiSuggestionModel.mock() {
    return AiSuggestionModel.fromJson({
      "overall_Suggestions": [
        {"Suggestion": generateMockString(16), "emoji": "⛅"}
      ],
      "overall_ToDo_Items": [],
      "specific_Suggestions": [
        {"Suggestion": generateMockString(16), "emoji": "🏺"},
        {"Suggestion": generateMockString(16), "emoji": "🏺"},
        {"Suggestion": generateMockString(16), "emoji": "🏺"},
      ],
      "specific_ToDo_Items": []
    });
  }

  Map<String, dynamic> toJson() {
    return {
      'overall_Suggestions':
          overallSuggestions?.map((x) => x.toJson()).toList(),
      'overall_ToDo_Items': overallToDoItems?.map((x) => x.toJson()).toList(),
      'specific_Suggestions':
          specificSuggestions?.map((x) => x.toJson()).toList(),
      'specific_ToDo_Items': specificToDoItems?.map((x) => x.toJson()).toList(),
    };
  }

  List<IconTextActionModel> toIconTextActionModelList() {
    return [
      ...?overallSuggestions?.map((e) => IconTextActionModel(
            content: e.suggestion ?? '',
            emoji: e.emoji,
          )),
      ...?overallToDoItems?.map((e) => IconTextActionModel(
            content: e.toDoContent ?? '',
            description: e.todoNotes,
            scheduledAt: e.time,
          )),
      ...?specificSuggestions?.map((e) => IconTextActionModel(
            content: e.suggestion ?? '',
            emoji: e.emoji,
          )),
      ...?specificToDoItems?.map((e) => IconTextActionModel(
            content: e.toDoContent ?? '',
            description: e.todoNotes ?? '',
            scheduledAt: e.time,
          )),
    ];
  }
}

class OverallSuggestionsModel {
  String? suggestion;
  String? emoji;

  OverallSuggestionsModel({required this.suggestion, required this.emoji});

  factory OverallSuggestionsModel.fromJson(Map<String, dynamic> json) {
    return OverallSuggestionsModel(
      suggestion: json['Suggestion'] ?? '',
      emoji: json['emoji'] ?? json['emoj'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'Suggestion': suggestion,
      'emoji': emoji,
    };
  }
}

class OverallToDoItem {
  String toDoContent;
  String todoNotes;
  String time;

  OverallToDoItem({
    required this.toDoContent,
    required this.todoNotes,
    required this.time,
  });

  factory OverallToDoItem.fromJson(Map<String, dynamic> json) {
    return OverallToDoItem(
      toDoContent: json['Todo_Content'] ?? json['ToDo_Content'] ?? '',
      todoNotes: json['Todo_notes'] ?? '',
      time: parseApiDateString(json['Time'] ?? ''),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'Todo_Content': toDoContent,
      'Todo_notes': todoNotes,
      'Time': time,
    };
  }
}

class SpecificSuggestionsModel {
  String suggestion;
  String emoji;

  SpecificSuggestionsModel({required this.suggestion, required this.emoji});

  factory SpecificSuggestionsModel.fromJson(Map<String, dynamic> json) {
    return SpecificSuggestionsModel(
      suggestion: json['Suggestion'] ?? '',
      emoji: json['emoji'] ?? json['emoj'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'Suggestion': suggestion,
      'emoji': emoji,
    };
  }
}

class SpecificToDoItem {
  String originalTodo;
  String toDoContent;
  String todoNotes;
  String? time;

  SpecificToDoItem({
    required this.originalTodo,
    required this.toDoContent,
    required this.todoNotes,
    this.time,
  });

  factory SpecificToDoItem.fromJson(Map<String, dynamic> json) {
    return SpecificToDoItem(
      originalTodo: json['original_todo'] ?? '',
      // Todo_Content is the canonical backend field. Keep accepting the
      // historical ToDo_Content spelling so older analysis records retain
      // their actual todo text when they are saved again.
      toDoContent: json['Todo_Content'] ?? json['ToDo_Content'] ?? '',
      todoNotes: json['Todo_notes'] ?? '',
      time: parseApiDateString(json['Time'] ?? ''),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'original_todo': originalTodo,
      'Todo_Content': toDoContent,
      'Todo_notes': todoNotes,
      'Time': time,
    };
  }
}
