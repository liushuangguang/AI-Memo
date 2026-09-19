import 'dart:convert';
import 'dart:typed_data';
import 'dart:async';

import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('same-note addNoteModule requests are serial and retain both modules',
      () async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _MutationAdapter(blockFirst: true);
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final note = NoteModel(id: 'note-1');
    final first = NoteApi.addNoteModule(note, _module('one'));
    final second = NoteApi.addNoteModule(note, _module('two'));
    await adapter.firstStarted.future;
    expect(adapter.moduleTitles, ['one']);
    expect(adapter.secondStarted.isCompleted, isFalse);
    adapter.releaseFirst();
    final results = await Future.wait([first, second]);
    expect(adapter.moduleTitles, ['one', 'two']);
    expect(results.last.modules, hasLength(2));
  });

  test('a failed same-note mutation does not strand the queue', () async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _MutationAdapter(failFirst: true);
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final note = NoteModel(id: 'note-2');
    final first = NoteApi.addNoteModule(note, _module('failed'));
    final second = NoteApi.addNoteModule(note, _module('survives'));
    await expectLater(first, throwsA(isA<DioException>()));
    final result = await second;
    expect(adapter.moduleTitles, ['failed', 'survives']);
    expect(result.modules, hasLength(1));
    expect((result.modules!.single as Map)['title'], 'survives');
  });
}

QuestionAnswerModule _module(String title) => QuestionAnswerModule(
      title: title,
      questionAnswerItems: [QuestionAnswerItem(question: title, answer: title)],
    );

class _MutationAdapter implements HttpClientAdapter {
  _MutationAdapter({this.blockFirst = false, this.failFirst = false});
  final bool blockFirst;
  final bool failFirst;
  final firstStarted = Completer<void>();
  final secondStarted = Completer<void>();
  final _firstGate = Completer<void>();
  final moduleTitles = <String>[];
  final modules = <Map<String, dynamic>>[];
  var count = 0;

  @override
  Future<ResponseBody> fetch(RequestOptions options,
      Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    count++;
    final title = (options.data['module']['title'] as String?) ?? '';
    moduleTitles.add(title);
    if (count == 1 && !firstStarted.isCompleted) firstStarted.complete();
    if (count == 2 && !secondStarted.isCompleted) secondStarted.complete();
    if (count == 1 && blockFirst) await _firstGate.future;
    if (count == 1 && failFirst) {
      throw DioException(
          requestOptions: options, error: 'first mutation failed');
    }
    modules.add(Map<String, dynamic>.from(options.data['module'] as Map));
    final note = NoteModel(id: options.data['id'] as String, modules: modules);
    return ResponseBody.fromString(
        jsonEncode({'code': 200, 'data': note.toJson()}), 200,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType]
        });
  }

  void releaseFirst() {
    if (!_firstGate.isCompleted) _firstGate.complete();
  }

  @override
  void close({bool force = false}) {}
}
