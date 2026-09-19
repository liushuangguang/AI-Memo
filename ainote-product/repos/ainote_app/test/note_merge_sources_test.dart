import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:ainote_app/app/widgets/note_card/note_merge_sources.dart';

List<NoteThemeMergeHistoryModel> history(String id) => [
      NoteThemeMergeHistoryModel(
          operationId: 'op-$id',
          mergedNoteId: id,
          mergedTitle: '合并',
          mergedContentPreview: '',
          imageUrls: [],
          sources: [
            NoteThemeSourceModel(noteId: 'source-$id', title: '来源 $id')
          ])
    ];

void main() {
  test('note theme IDs survive serialization', () {
    final note = NoteModel.fromJson({
      'id': 'a',
      'noteThemeIds': ['theme-a']
    });
    expect(note.toJson()['noteThemeIds'], ['theme-a']);
  });
  testWidgets('history retry and source opens exact saved original',
      (tester) async {
    var calls = 0;
    String? requested;
    await tester.pumpWidget(MaterialApp(
        home: Scaffold(
            body: NoteMergeSources(
                noteId: 'a',
                loadHistory: (id) async {
                  if (++calls == 1) throw StateError('offline');
                  return history(id);
                },
                loadNote: (id) async {
                  requested = id;
                  return NoteModel(
                      id: id,
                      title: '原始标题',
                      content: jsonEncode([
                        {'insert': '原始正文\n'}
                      ]));
                }))));
    await tester.pumpAndSettle();
    await tester.tap(find.text('合并来源加载失败，点击重试'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('来源 a'));
    await tester.pumpAndSettle();
    expect(calls, 2);
    expect(requested, 'source-a');
    expect(find.text('原始标题'), findsOneWidget);
    expect(find.textContaining('原始正文'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(find.text('来源 a'), findsOneWidget);
  });
  testWidgets('ABA note change discards late source navigation',
      (tester) async {
    final pending = Completer<NoteModel>();
    Widget page(String id) => MaterialApp(
        home: Scaffold(
            body: NoteMergeSources(
                noteId: id,
                loadHistory: (id) async => history(id),
                loadNote: (_) => pending.future)));
    await tester.pumpWidget(page('a'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('来源 a'));
    await tester.pump();
    await tester.pumpWidget(page('b'));
    await tester.pumpAndSettle();
    await tester.pumpWidget(page('a'));
    await tester.pumpAndSettle();
    pending.complete(NoteModel(id: 'source-a', title: '过期来源'));
    await tester.pumpAndSettle();
    expect(find.text('过期来源'), findsNothing);
    expect(find.text('来源 a'), findsOneWidget);
  });
}
