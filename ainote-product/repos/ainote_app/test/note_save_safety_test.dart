import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/utils/note_image.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(
          const MethodChannel('plugins.flutter.io/path_provider'),
          (_) async => 'L:/ainote-qa-20260906/temp');
  for (final channel in [
    'xyz.luan/audioplayers',
    'xyz.luan/audioplayers.global',
    'xyz.luan/audioplayers/events/todo_alarm',
    'xyz.luan/audioplayers.global/events',
    'vibration'
  ]) {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(MethodChannel(channel), (_) async => null);
  }
  test('selection preserves analysis but actual editor change invalidates it',
      () {
    final editor =
        NoteEditController(smartOrganizeController: SmartOrganizeController());
    editor.quillLogic = MyQuillController();
    editor.setNote(
        NoteModel(id: 'a', title: 'title', content: 'saved text', modules: []));
    editor.noteHasSaved.value = true;
    editor.aiAnalysisDone.value = true;
    editor.quillLogic.quillController.addListener(editor.handleQuillChange);
    editor.quillLogic.quillController.updateSelection(
        const TextSelection.collapsed(offset: 0), ChangeSource.local);
    expect(editor.aiAnalysisDone.value, isTrue);
    expect(editor.noteHasSaved.value, isTrue);
    editor.quillLogic.quillController.replaceText(
        0, 0, 'new draft ', const TextSelection.collapsed(offset: 10));
    expect(editor.aiAnalysisDone.value, isFalse);
    expect(editor.noteHasSaved.value, isFalse);
    editor.quillLogic.quillController.removeListener(editor.handleQuillChange);
    editor.quillLogic.quillController.dispose();
    editor.quillLogic.quillFocusNode.dispose();
    editor.titleController.dispose();
  });
  test('private image auth never goes to unrelated URLs', () {
    MyDio.dio.options.baseUrl = 'http://192.168.31.213:8080';
    MyDio.dio.options.headers['Authorization'] = 'Guest synthetic';
    MyDio.dio.options.headers['Device-Id'] = 'synthetic';
    expect(noteImageUrl('/v2/capture/images/abc'),
        'http://192.168.31.213:8080/v2/capture/images/abc');
    expect(
        noteImageHeaders('/v2/capture/images/abc')?['Device-Id'], 'synthetic');
    for (final url in [
      '',
      'relative.png',
      'data:image/png,xyz',
      'https://external.test/v2/capture/images/abc',
      'http://192.168.31.213:8080/public/a.png',
      'http://192.168.31.213:9999/v2/capture/images/a'
    ]) {
      expect(noteImageHeaders(url), isNull, reason: url);
    }
  });
  test('late module save preserves local dirty text and title', () {
    final editor =
        NoteEditController(smartOrganizeController: SmartOrganizeController());
    editor.quillLogic = MyQuillController();
    editor.note.value = NoteModel(id: 'a', title: 'old');
    editor.titleController.text = 'new local title';
    editor.quillLogic.quillController.document.insert(0, 'new local draft');
    editor.noteHasSaved.value = false;
    editor
        .applySavedNote(NoteModel(id: 'a', title: 'server title', modules: []));
    expect(editor.titleController.text, 'new local title');
    expect(editor.quillLogic.quillController.document.toPlainText(),
        contains('new local draft'));
    expect(editor.note.value.content, contains('new local draft'));
    expect(editor.noteHasSaved.value, isFalse);
    editor.applySavedNote(NoteModel(id: 'other', title: 'wrong', modules: []));
    expect(editor.note.value.id, 'a');
    editor.quillLogic.quillController.dispose();
    editor.quillLogic.quillFocusNode.dispose();
    editor.titleController.dispose();
  });

  test('save failure leaves the draft editable and retryable', () async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _SaveAdapter()..failNext = true;
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final editor = _editor(NoteModel(id: 'a', title: 'title'));
    editor.quillLogic.quillController.document.insert(0, 'draft');
    editor.noteHasSaved.value = false;

    expect(await editor.saveNote(true), isFalse);
    expect(editor.noteHasSaved.value, isFalse);
    expect(editor.quillLogic.quillIsReadOnly.value, isFalse);
    adapter.complete(NoteModel(id: 'a', title: 'saved'));
    expect(await editor.saveNote(true), isTrue);
    expect(editor.noteHasSaved.value, isTrue);
    _disposeEditor(editor);
  });

  test('delayed save cannot overwrite a switched note or a closed editor',
      () async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _SaveAdapter();
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);

    final editor = _editor(NoteModel(id: 'a', title: 'A'));
    editor.quillLogic.quillController.document.insert(0, 'A draft');
    editor.noteHasSaved.value = false;
    final saving = editor.saveNote(true);
    await adapter.called.future;
    editor.setNote(NoteModel(id: 'b', title: 'B', content: 'B content'));
    adapter.complete(NoteModel(id: 'a', title: 'server A'));
    expect(await saving, isFalse);
    expect(editor.note.value.id, 'b');
    expect(editor.titleController.text, 'B');
    _disposeEditor(editor);

    final closingAdapter = _SaveAdapter();
    MyDio.dio.httpClientAdapter = closingAdapter;
    final closed = _editor(NoteModel(id: 'c', title: 'C'));
    closed.quillLogic.quillController.document.insert(0, 'C draft');
    closed.noteHasSaved.value = false;
    final closing = closed.saveNote(true);
    await closingAdapter.called.future;
    closed.onClose();
    closingAdapter.complete(NoteModel(id: 'c', title: 'server C'));
    expect(await closing, isFalse);
  });

  test('editing during create keeps draft and module identity for next save',
      () async {
    final original = MyDio.dio.httpClientAdapter;
    final creation = Completer<NoteModel>();
    final adapter = _SaveAdapter()..createPending = creation;
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final editor = _editor(NoteModel(id: '', title: 'new'));
    editor.quillLogic.quillController.document.insert(0, 'initial text');
    final saving = editor.saveNote(true);
    await adapter.called.future;
    editor.quillLogic.quillController.document.insert(0, 'newer draft ');
    creation.complete(NoteModel(id: 'created', title: 'initial', modules: [
      {
        'noteModuleType': 'TEXT_DATA',
        'moduleId': 'server-text',
        'content': '[{"insert":"initial text\\n"}]',
        'items': []
      }
    ]));
    expect(await saving, isFalse);
    expect(editor.note.value.id, 'created');
    expect(editor.quillLogic.quillIsReadOnly.value, isFalse);
    expect(editor.note.value.modules!.single['moduleId'], 'server-text');
    expect(editor.quillLogic.quillController.document.toPlainText(),
        contains('newer draft'));
    final retry = editor.saveNote(true);
    adapter.complete(NoteModel(
        id: 'created',
        title: 'newer draft',
        modules: editor.note.value.modules));
    expect(await retry, isTrue);
    expect(adapter.paths, ['/v2/note/create', '/v2/note/module/update']);
    _disposeEditor(editor);
  });

  test('successful create writes the server id back to the note', () async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _SaveAdapter()
      ..createResult = NoteModel(id: 'created-1', title: 'new');
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final editor = _editor(NoteModel(id: '', title: 'new'));
    editor.quillLogic.quillController.document.insert(0, 'new content');
    editor.noteHasSaved.value = false;
    expect(await editor.saveNote(true), isTrue);
    expect(editor.note.value.id, 'created-1');
    _disposeEditor(editor);
  });
}

NoteEditController _editor(NoteModel note) {
  note.modules ??= [
    {
      'noteModuleType': 'TEXT_DATA',
      'moduleId': 'text-${note.id}',
      'content': note.content ?? '[{"insert":"\\n"}]',
      'items': []
    }
  ];
  final editor =
      NoteEditController(smartOrganizeController: SmartOrganizeController());
  editor.quillLogic = MyQuillController();
  editor.setNote(note);
  return editor;
}

void _disposeEditor(NoteEditController editor) {
  if (!editor.isClosed) editor.onClose();
  editor.quillLogic.quillController.dispose();
  editor.quillLogic.quillFocusNode.dispose();
}

class _SaveAdapter implements HttpClientAdapter {
  final called = Completer<void>();
  final pending = Completer<NoteModel>();
  bool failNext = false;
  NoteModel? createResult;
  Completer<NoteModel>? createPending;
  final List<String> paths = [];
  @override
  Future<ResponseBody> fetch(RequestOptions options,
      Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    paths.add(options.path);
    if (!called.isCompleted) called.complete();
    if (failNext) {
      failNext = false;
      throw DioException(requestOptions: options, error: 'offline');
    }
    final result = options.path.contains('/create')
        ? (createPending != null
            ? await createPending!.future
            : createResult ?? NoteModel(id: 'created', title: 'saved'))
        : await pending.future;
    return ResponseBody.fromString(
        jsonEncode({'code': 200, 'data': result.toJson()}), 200,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType]
        });
  }

  void complete(NoteModel note) {
    if (!pending.isCompleted) pending.complete(note);
  }

  @override
  void close({bool force = false}) {}
}
