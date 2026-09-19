import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/utils/json_format.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses the four suggestion fields as arrays', () {
    final model = AiSuggestionModel.fromJson({
      'overall_Suggestions': [
        {'Suggestion': '总体建议一', 'emoji': '🌤️'},
        {'Suggestion': '总体建议二', 'emoji': '🧭'},
      ],
      'overall_ToDo_Items': [
        {
          'ToDo_Content': '完成总体待办',
          'Todo_notes': '备注',
          'Time': '2023-12-20 12:00:00',
        },
      ],
      'specific_Suggestions': [
        {'Suggestion': '具体建议', 'emoji': '🏺'},
      ],
      'specific_ToDo_Items': [
        {
          'original_todo': '原待办',
          'Todo_Content': '完成具体待办',
          'Todo_notes': '备注',
          'Time': null,
        },
      ],
    });

    expect(model.overallSuggestions, hasLength(2));
    expect(model.overallSuggestions![1].suggestion, '总体建议二');
    expect(model.overallToDoItems, hasLength(1));
    expect(model.specificSuggestions, hasLength(1));
    expect(model.specificToDoItems, hasLength(1));
    expect(model.toIconTextActionModelList(), hasLength(5));
    expect(model.toJson()['overall_Suggestions'], isA<List<dynamic>>());
  });

  test('keeps parsing legacy single overall suggestion objects safely', () {
    final model = AiSuggestionModel.fromJson({
      'overall_Suggestions': {'Suggestion': '旧格式', 'emoji': '⛅'},
      'overall_ToDo_Items': null,
      'specific_Suggestions': null,
      'specific_ToDo_Items': null,
    });

    expect(model.overallSuggestions, hasLength(1));
    expect(model.toIconTextActionModelList().single.content, '旧格式');
  });

  test('specific todo uses canonical content and preserves it in payload', () {
    final model = AiSuggestionModel.fromJson({
      'specific_ToDo_Items': [
        {
          'original_todo': '原待办',
          'Todo_Content': 'canonical 正文',
          'ToDo_Content': 'legacy 正文',
          'Todo_notes': '',
          'Time': null,
        },
      ],
    });

    final todo = model.specificToDoItems!.single;
    expect(todo.toDoContent, 'canonical 正文');
    expect(model.toJson()['specific_ToDo_Items'], [
      {
        'original_todo': '原待办',
        'Todo_Content': 'canonical 正文',
        'Todo_notes': '',
        'Time': '',
      },
    ]);
    expect(model.toIconTextActionModelList().single.content, 'canonical 正文');
  });

  test('overall todo accepts canonical content and round-trips canonically',
      () {
    final item = OverallToDoItem.fromJson({
      'Todo_Content': 'canonical overall',
      'Todo_notes': '',
      'Time': null,
    });
    expect(item.toDoContent, 'canonical overall');
    expect(item.toJson()['Todo_Content'], 'canonical overall');
    expect(item.toJson().containsKey('ToDo_Content'), isFalse);
  });

  test('overall todo accepts legacy content spelling', () {
    final item = OverallToDoItem.fromJson({
      'ToDo_Content': 'legacy overall',
      'Todo_notes': '',
      'Time': null,
    });
    expect(item.toDoContent, 'legacy overall');
  });

  test('overall todo prefers canonical content over legacy spelling', () {
    final item = OverallToDoItem.fromJson({
      'Todo_Content': 'canonical overall',
      'ToDo_Content': 'legacy overall',
      'Todo_notes': '',
      'Time': null,
    });
    expect(item.toDoContent, 'canonical overall');
  });

  test('overall todo serializes and reparses without losing content', () {
    final original = OverallToDoItem(
      toDoContent: 'round-trip overall',
      todoNotes: 'notes',
      time: '',
    );
    final reparsed = OverallToDoItem.fromJson(original.toJson());
    expect(reparsed.toDoContent, 'round-trip overall');
    expect(reparsed.todoNotes, 'notes');
  });

  test('specific todo accepts historical content spelling', () {
    final model = AiSuggestionModel.fromJson({
      'specific_ToDo_Items': [
        {
          'original_todo': '原待办',
          'ToDo_Content': 'legacy 正文',
          'Todo_notes': '',
          'Time': '',
        },
      ],
    });

    expect(model.specificToDoItems!.single.toDoContent, 'legacy 正文');
    expect(model.toJson()['specific_ToDo_Items'].single['Todo_Content'],
        'legacy 正文');
  });

  test('trimData removes only SSE data prefixes at line starts', () {
    const input = 'data: {"message":"contains data: inside"}\n'
        'data:{"next":"value"}\n'
        'plain data: text';

    expect(trimData(input),
        '{"message":"contains data: inside"}\n{"next":"value"}\nplain data: text');
  });
}
