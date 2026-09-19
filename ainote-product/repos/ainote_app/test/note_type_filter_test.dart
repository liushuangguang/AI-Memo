import 'dart:async';

import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/modules/home/controllers/home_controller.dart';
import 'package:ainote_app/app/modules/note/note_list1/bindings/note_list_repository.dart';
import 'package:ainote_app/app/modules/note/note_list1/controllers/note_list_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('note type options always expose one all-items choice first', () {
    final options = noteTypeOptions([
      NoteTypeModel(id: 0, name: '未分类'),
      NoteTypeModel(id: 7, name: '工作'),
      NoteTypeModel(id: allNoteTypesId, name: '重复的全部'),
      NoteTypeModel(id: 8, name: ' 全部 '),
    ]);

    expect(options.map((item) => item.id), [allNoteTypesId, 0, 7]);
    expect(options.first.name, '全部');
  });

  test('home publishes delayed categories through one shared reactive list',
      () async {
    final completer = Completer<List<NoteTypeModel>>();
    var calls = 0;
    final controller = HomeController(noteTypesLoader: () {
      calls++;
      return completer.future;
    });
    final sharedList = controller.noteTypeList;

    final first = controller.getNoteTypeList();
    final second = controller.getNoteTypeList();
    expect(identical(first, second), isTrue);
    expect(controller.noteTypesLoading.value, isTrue);
    expect(sharedList, isEmpty);

    completer.complete([NoteTypeModel(id: 7, name: '工作')]);
    await first;

    expect(calls, 1);
    expect(identical(controller.noteTypeList, sharedList), isTrue);
    expect(sharedList.map((item) => item.name), ['工作']);
    expect(controller.noteTypesLoading.value, isFalse);
  });

  test('all-items choice maps back to an unfiltered repository request', () {
    expect(normalizeNoteTypeFilter(allNoteTypesId), isNull);
    expect(normalizeNoteTypeFilter(0), 0);
    expect(normalizeNoteTypeFilter(7), 7);
  });

  test('all-items is immediate and categories fill the same reactive list',
      () async {
    final completer = Completer<List<NoteTypeModel>>();
    final controller = NoteListController();
    final reactiveList = controller.noteTypeList;

    final update = controller.updateNoteTypes(completer.future);
    expect(reactiveList.map((item) => item.name), ['全部']);

    completer.complete([
      NoteTypeModel(id: 7, name: '工作'),
      NoteTypeModel(id: 8, name: '生活'),
    ]);
    await update;

    expect(identical(controller.noteTypeList, reactiveList), isTrue);
    expect(reactiveList.map((item) => item.name), ['全部', '工作', '生活']);
  });
}
