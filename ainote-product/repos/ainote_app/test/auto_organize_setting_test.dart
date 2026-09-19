import 'dart:async';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('auto organize change is persisted before enabled is displayed',
      () async {
    final gate = Completer<void>();
    final writes = <bool>[];
    final controller = SmartOrganizeController(
      initialAutoOrganize: false,
      persistAutoOrganize: (value) async {
        writes.add(value);
        await gate.future;
      },
    );
    final saving = controller.setAutoOrganize(true);
    expect(controller.autoOrganize.value, isFalse);
    expect(controller.autoOrganizeSaving.value, isTrue);
    expect(await controller.setAutoOrganize(false), isFalse);
    gate.complete();
    expect(await saving, isTrue);
    expect(controller.autoOrganize.value, isTrue);
    expect(writes, [true]);
  });

  test('failed setting write leaves the old switch value intact', () async {
    final controller = SmartOrganizeController(
      initialAutoOrganize: false,
      persistAutoOrganize: (_) async => throw StateError('storage unavailable'),
    );
    await expectLater(controller.setAutoOrganize(true), throwsStateError);
    expect(controller.autoOrganize.value, isFalse);
    expect(controller.autoOrganizeSaving.value, isFalse);
  });

  test(
      'background automatic run respects enabled preference and completed state',
      () async {
    final smart = SmartOrganizeController(
        initialAutoOrganize: false, persistAutoOrganize: (_) async {});
    final editor = _AutomaticEditor(smart);
    editor.note.value = NoteModel.empty()..id = 'synthetic-note';
    editor.noteHasSaved.value = true;
    await editor.startAiAutoAnalysis();
    expect(editor.runs, 0);
    await smart.setAutoOrganize(true);
    await editor.startAiAutoAnalysis();
    expect(editor.runs, 1);
    editor.aiAnalysisDone.value = true;
    await editor.startAiAutoAnalysis();
    expect(editor.runs, 1);
    await editor.startAiAutoAnalysis(force: true);
    expect(editor.runs, 2);
  });
}

class _AutomaticEditor extends NoteEditController {
  _AutomaticEditor(SmartOrganizeController smart)
      : super(smartOrganizeController: smart);
  int runs = 0;
  @override
  Future<void> startSmartOrganize() async {
    runs++;
  }
}
