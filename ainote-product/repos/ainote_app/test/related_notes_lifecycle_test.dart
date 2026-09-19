import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/api/api_urls.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/ai_analysis_record_model.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_note_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/views/widgets/related_notes.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('editing same note invalidates a held organize response', () async {
    final organize = Completer<AiOrganizeModel>();
    final gateway = _LifecycleGateway(organize: (_) => organize.future);
    final editor = _noteEditControllerFor(
      noteId: 'note-a', gateway: gateway, loadLatest: (_) async => null);
    final running = editor.startSmartOrganize();
    await _flush();
    expect(gateway.organizeRecordIds, hasLength(1));
    editor.invalidateAnalysisForDraft();
    organize.complete(AiOrganizeModel(bodyText: 'stale draft output'));
    await running;
    expect(editor.smartOrganizeLogic.aiOrganizeResult.value.bodyText,
        isNot('stale draft output'));
    expect(editor.aiAnalysisDone.value, isFalse);
    expect(editor.aiAnalysisVisible.value, isFalse);
    expect(editor.noteHasSaved.value, isFalse);
  });

  test('late analysis metadata must not mark edited text saved', () async {
    final latest = Completer<AiAnalysisRecordModel?>();
    final controller = _noteEditControllerFor(
      noteId: 'note-a', gateway: _LifecycleGateway(), loadLatest: (_) => latest.future);
    controller.noteHasSaved.value = true;
    final loading = controller.getAiAnalysisRecordLatest();
    controller.noteHasSaved.value = false;
    latest.complete(_record('record-a', noteId: 'note-a'));
    await loading;
    expect(controller.noteHasSaved.value, isFalse);
  });

  group('related note API contract', () {
    late HttpClientAdapter originalAdapter;
    late _RecordingAdapter adapter;

    setUp(() {
      originalAdapter = MyDio.dio.httpClientAdapter;
      adapter = _RecordingAdapter();
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.baseUrl = 'https://example.test';
    });

    tearDown(() {
      MyDio.dio.httpClientAdapter = originalAdapter;
    });

    test('POSTs a trimmed non-blank recordId with retry disabled', () async {
      final notes = await AiApi.aiRelatedNote(recordId: '  record-42  ');

      expect(notes.single.id, 'note-1');
      expect(adapter.requests, hasLength(1));
      final request = adapter.requests.single;
      expect(request.method, 'POST');
      expect(request.path, ApiUrls.aiRelatedNote);
      expect(request.data, {'recordId': 'record-42'});
      expect(request.extra['ro_disable_retry'], isTrue);
      expect(request.connectTimeout, const Duration(seconds: 15));
      expect(request.sendTimeout, const Duration(seconds: 15));
      expect(request.receiveTimeout, const Duration(seconds: 80));
    });

    test('rejects a blank recordId before reaching the transport', () async {
      await expectLater(
        AiApi.aiRelatedNote(recordId: ' \n '),
        throwsArgumentError,
      );
      expect(adapter.requests, isEmpty);
    });

    test('accepts an empty data list as a valid no-results response', () async {
      adapter.responseData = [];

      expect(await AiApi.aiRelatedNote(recordId: 'record-empty'), isEmpty);
    });

    test('rejects a non-empty list with no usable related note', () async {
      adapter.responseData = [
        'bad',
        {'id': 7},
      ];

      await expectLater(
        AiApi.aiRelatedNote(recordId: 'record-invalid'),
        throwsA(
          isA<FormatException>().having(
            (error) => error.message,
            'message',
            contains('no usable note'),
          ),
        ),
      );
    });

    test('mixed lists preserve usable entries and discard malformed ones',
        () async {
      adapter.responseData = [
        'bad',
        {'id': 7},
        {
          'id': 'note-valid',
          'title': '可用标题',
          'content': '',
        },
      ];

      final notes = await AiApi.aiRelatedNote(recordId: 'record-mixed');

      expect(notes.map((note) => note.id), ['note-valid']);
    });
  });

  group('related note DTO', () {
    test('normalizes valid values and finite numeric scores', () {
      final note = AiRelatedNoteModel.fromJson({
        'id': ' note-1 ',
        'title': ' 标题 ',
        'content': ' 正文 ',
        'reason': ' 理由 ',
        'score': '0.75',
      });

      expect(note.toJson(), {
        'id': 'note-1',
        'title': '标题',
        'content': '正文',
        'reason': '理由',
        'score': 0.75,
      });
      expect(note.isUsable, isTrue);
    });

    test('treats malformed field types and non-finite scores as unusable', () {
      final note = AiRelatedNoteModel.fromJson({
        'id': 7,
        'title': <String>['not text'],
        'content': false,
        'reason': const <String, String>{'value': 'not text'},
        'score': 'NaN',
      });

      expect(note.id, isNull);
      expect(note.title, isNull);
      expect(note.content, isNull);
      expect(note.reason, isNull);
      expect(note.score, isNull);
      expect(note.isUsable, isFalse);
    });

    test('rejects ghost entries that have no visible title or content', () {
      final note = AiRelatedNoteModel.fromJson({
        'id': 'ghost',
        'title': ' ',
        'content': '\n',
        'reason': '只有理由',
      });

      expect(note.isUsable, isFalse);
    });
  });

  group('related note states', () {
    test('cached record success writes the current result', () async {
      final gateway = _LifecycleGateway(
        related: (_) async => [_related('success')],
      );
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      await _flush();

      expect(gateway.relatedRecordIds, ['record-a']);
      expect(controller.aiRelatedNoteList.single.id, 'success');
      expect(controller.aiRelatedNoteLoading.value, isFalse);
      expect(controller.aiRelatedNoteMessage.value, isEmpty);
    });

    test('cached record empty response has a deterministic empty state',
        () async {
      final gateway = _LifecycleGateway(related: (_) async => []);
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      await _flush();

      expect(controller.aiRelatedNoteList, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isFalse);
      expect(controller.aiRelatedNoteMessage.value, '暂无相关备忘录');
    });

    test('cached record failure has a deterministic error state', () async {
      final gateway = _LifecycleGateway(
        related: (_) async => throw StateError('offline'),
      );
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      await _flush();

      expect(controller.aiRelatedNoteList, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isFalse);
      expect(controller.aiRelatedNoteMessage.value, contains('加载失败'));
    });

    test('hanging related notes do not block core smart organize', () async {
      final relatedGate = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (_) => relatedGate.future);
      final controller = _controllerFor('note-a', gateway);

      await controller.smartOrganize().timeout(const Duration(seconds: 1));

      expect(controller.aiOrganizeLoading.value, isFalse);
      expect(controller.aiSuggestionLoading.value, isFalse);
      expect(controller.aiRelatedNoteLoading.value, isTrue);
      relatedGate.complete([]);
      await _flush();
      expect(controller.aiRelatedNoteLoading.value, isFalse);
    });

    test('cached record uses its trimmed record.id and clears old UI at once',
        () async {
      final relatedGate = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (_) => relatedGate.future);
      final controller = _controllerFor('note-a', gateway);
      controller.aiRelatedNoteList.add(_related('old'));

      controller.setAnalysisRecord(_record('  cached-id  ', noteId: 'note-a'));

      expect(controller.aiRelatedNoteList, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isTrue);
      expect(gateway.relatedRecordIds, ['cached-id']);
      relatedGate.complete([]);
      await _flush();
    });

    test('cached record without an id sends no related-notes request', () {
      final gateway = _LifecycleGateway();
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('  ', noteId: 'note-a'));

      expect(gateway.relatedRecordIds, isEmpty);
      expect(controller.aiRelatedNoteList, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isFalse);
      expect(controller.aiRelatedNoteMessage.value, '暂无相关备忘录');
    });

    test('meaningful validation without a record id sends no request',
        () async {
      final gateway = _LifecycleGateway(
        validate: (_) async =>
            AiValidateModel(meaningful: true, recordId: '  '),
      );
      final controller = _controllerFor('note-a', gateway);

      await controller.smartOrganize();

      expect(gateway.organizeRecordIds, isEmpty);
      expect(gateway.relatedRecordIds, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isFalse);
      expect(controller.aiRelatedNoteMessage.value, contains('缺少整理记录'));
    });
  });

  group('operation stale-write protection', () {
    test('cached record invalidates an in-flight live operation', () async {
      final validationGate = Completer<AiValidateModel>();
      final gateway = _LifecycleGateway(
        validate: (_) => validationGate.future,
        related: (recordId) async => [_related(recordId)],
      );
      final controller = _controllerFor('note-a', gateway);

      final liveRun = controller.smartOrganize();
      await _flush();
      controller.setAnalysisRecord(_record('record-cache', noteId: 'note-a'));
      await _flush();
      validationGate.complete(
        AiValidateModel(meaningful: true, recordId: 'record-live'),
      );
      await liveRun;
      await _flush();

      expect(gateway.organizeRecordIds, isEmpty);
      expect(gateway.relatedRecordIds, ['record-cache']);
      expect(controller.aiOrganizeResult.value.title, 'record-cache');
      expect(controller.aiRelatedNoteList.single.id, 'record-cache');
    });

    test('live operation invalidates an in-flight cached-record request',
        () async {
      final cachedRelatedGate = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (recordId) {
        if (recordId == 'record-cache') return cachedRelatedGate.future;
        return Future.value([_related(recordId)]);
      });
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-cache', noteId: 'note-a'));
      await controller.smartOrganize();
      cachedRelatedGate.complete([_related('late-cache')]);
      await _flush();

      expect(
        gateway.relatedRecordIds,
        ['record-cache', 'record-note-a'],
      );
      expect(controller.aiOrganizeResult.value.title, 'record-note-a');
      expect(controller.aiRelatedNoteList.single.id, 'record-note-a');
    });

    test('reset while validation is paused rejects the old response', () async {
      final validationGate = Completer<AiValidateModel>();
      final gateway = _LifecycleGateway(validate: (_) => validationGate.future);
      final controller = _controllerFor('note-a', gateway);

      final run = controller.smartOrganize();
      await _flush();
      controller.reset();
      validationGate.complete(
        AiValidateModel(meaningful: true, recordId: 'record-a'),
      );
      await run;

      expect(controller.aiValidateResult.value.meaningful, isNull);
      expect(gateway.organizeRecordIds, isEmpty);
      expect(gateway.relatedRecordIds, isEmpty);
    });

    test('reset while organize is paused prevents related notes from starting',
        () async {
      final organizeGate = Completer<AiOrganizeModel>();
      final gateway = _LifecycleGateway(organize: (_) => organizeGate.future);
      final controller = _controllerFor('note-a', gateway);

      final run = controller.smartOrganize();
      await _flush();
      expect(gateway.organizeRecordIds, ['record-note-a']);
      controller.reset();
      organizeGate.complete(AiOrganizeModel(title: 'stale'));
      await run;

      expect(controller.aiOrganizeResult.value.title, isNull);
      expect(gateway.relatedRecordIds, isEmpty);
    });

    test('reset while related notes are paused rejects the old response',
        () async {
      final relatedGate = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (_) => relatedGate.future);
      final controller = _controllerFor('note-a', gateway);

      await controller.smartOrganize();
      expect(gateway.relatedRecordIds, ['record-note-a']);
      controller.reset();
      relatedGate.complete([_related('stale')]);
      await _flush();

      expect(controller.aiRelatedNoteList, isEmpty);
      expect(controller.aiRelatedNoteLoading.value, isTrue);
    });

    test('switching note A to B allows B to start and rejects late A',
        () async {
      final validationA = Completer<AiValidateModel>();
      final gateway = _LifecycleGateway(validate: (noteId) {
        if (noteId == 'note-a') return validationA.future;
        return Future.value(
          AiValidateModel(meaningful: true, recordId: 'record-note-b'),
        );
      });
      final controller = _controllerFor('note-a', gateway);

      final runA = controller.smartOrganize();
      await _flush();
      controller.setNote(NoteModel(id: 'note-b', title: 'B'));
      final runB = controller.smartOrganize();
      await runB;
      validationA.complete(
        AiValidateModel(meaningful: true, recordId: 'record-note-a'),
      );
      await runA;
      await _flush();

      expect(gateway.validatedNoteIds, ['note-a', 'note-b']);
      expect(gateway.organizeRecordIds, ['record-note-b']);
      expect(gateway.relatedRecordIds, ['record-note-b']);
      expect(controller.aiOrganizeResult.value.title, 'record-note-b');
    });

    test('switching cached record A to B rejects late A', () async {
      final relatedA = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (recordId) {
        if (recordId == 'record-a') return relatedA.future;
        return Future.value([_related('from-b')]);
      });
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      controller.setAnalysisRecord(_record('record-b', noteId: 'note-a'));
      await _flush();
      expect(controller.aiRelatedNoteList.single.id, 'from-b');
      relatedA.complete([_related('late-a')]);
      await _flush();

      expect(gateway.relatedRecordIds, ['record-a', 'record-b']);
      expect(controller.aiRelatedNoteList.single.id, 'from-b');
    });

    test('cached record for another note cannot replace the active note',
        () async {
      final gateway = _LifecycleGateway(
        related: (_) async => [_related('active')],
      );
      final controller = _controllerFor('note-b', gateway);
      controller.setAnalysisRecord(_record('record-b', noteId: 'note-b'));
      await _flush();

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      await _flush();

      expect(gateway.relatedRecordIds, ['record-b']);
      expect(controller.aiRelatedNoteList.single.id, 'active');
      expect(controller.aiOrganizeResult.value.title, 'record-b');
    });

    test('cached record without noteId cannot bind to the active note',
        () async {
      final gateway = _LifecycleGateway(
        related: (recordId) async => [_related(recordId)],
      );
      final controller = _controllerFor('note-b', gateway);
      controller.setAnalysisRecord(_record('record-b', noteId: 'note-b'));
      await _flush();

      controller.setAnalysisRecord(AiAnalysisRecordModel(
        id: 'record-a',
        organizedNote: AiOrganizeModel(title: 'unverifiable'),
      ));
      await _flush();

      expect(gateway.relatedRecordIds, ['record-b']);
      expect(controller.aiRelatedNoteList.single.id, 'record-b');
      expect(controller.aiOrganizeResult.value.title, 'record-b');
    });

    test('onClose rejects a late related-notes response', () async {
      final relatedGate = Completer<List<AiRelatedNoteModel>>();
      final gateway = _LifecycleGateway(related: (_) => relatedGate.future);
      final controller = _controllerFor('note-a', gateway);

      controller.setAnalysisRecord(_record('record-a', noteId: 'note-a'));
      controller.onClose();
      relatedGate.complete([_related('late')]);
      await _flush();

      expect(controller.aiRelatedNoteList, isEmpty);
    });
  });

  group('note edit cached-bootstrap protection', () {
    test('late note A latest response cannot replace note B', () async {
      final latestA = Completer<AiAnalysisRecordModel?>();
      final latestB = Completer<AiAnalysisRecordModel?>();
      final gateway = _LifecycleGateway();
      final controller = _noteEditControllerFor(
        noteId: 'note-a',
        gateway: gateway,
        loadLatest: (noteId) =>
            noteId == 'note-a' ? latestA.future : latestB.future,
      );

      final requestA = controller.getAiAnalysisRecordLatest();
      await _flush();
      _setNoteEditActiveNote(controller, 'note-b');
      final requestB = controller.getAiAnalysisRecordLatest();
      latestB.complete(_record('record-b', noteId: 'note-b'));
      await requestB;

      latestA.complete(_record('record-a', noteId: 'note-a'));
      await requestA;
      await _flush();

      expect(controller.analysisRecord.value.id, 'record-b');
      expect(controller.smartOrganizeLogic.aiOrganizeResult.value.title,
          'record-b');
      expect(gateway.relatedRecordIds, ['record-b']);
    });

    test('late latest response cannot overwrite or cancel a newer live run',
        () async {
      final latest = Completer<AiAnalysisRecordModel?>();
      final validation = Completer<AiValidateModel>();
      final gateway = _LifecycleGateway(validate: (_) => validation.future);
      final controller = _noteEditControllerFor(
        noteId: 'note-a',
        gateway: gateway,
        loadLatest: (_) => latest.future,
      );

      final latestRequest = controller.getAiAnalysisRecordLatest();
      await _flush();
      final liveRun = controller.startSmartOrganize();
      await _flush();

      latest.complete(_record('record-cache', noteId: 'note-a'));
      await latestRequest;
      validation.complete(
        AiValidateModel(meaningful: true, recordId: 'record-live'),
      );
      await liveRun;
      await _flush();

      expect(gateway.organizeRecordIds, ['record-live']);
      expect(gateway.relatedRecordIds, ['record-live']);
      expect(controller.smartOrganizeLogic.aiOrganizeResult.value.title,
          'record-live');
      expect(controller.analysisRecord.value.id, isNot('record-cache'));
      expect(controller.aiAnalysisDone.value, isTrue);
    });

    test('latest loader failure falls back to guarded live analysis', () async {
      final gateway = _LifecycleGateway();
      final controller = _noteEditControllerFor(
        noteId: 'note-a',
        gateway: gateway,
        loadLatest: (_) async => throw StateError('cache unavailable'),
      );

      await controller.getAiAnalysisRecordLatest();
      await _flush();

      expect(gateway.validatedNoteIds, ['note-a']);
      expect(gateway.organizeRecordIds, ['record-note-a']);
      expect(controller.aiAnalysisVisible.value, isTrue);
      expect(controller.aiAnalysisDone.value, isTrue);
    });

    test('late latest loader failure cannot start analysis for another note',
        () async {
      final latestA = Completer<AiAnalysisRecordModel?>();
      final gateway = _LifecycleGateway();
      final controller = _noteEditControllerFor(
        noteId: 'note-a',
        gateway: gateway,
        loadLatest: (_) => latestA.future,
      );

      final requestA = controller.getAiAnalysisRecordLatest();
      await _flush();
      _setNoteEditActiveNote(controller, 'note-b');
      latestA.completeError(StateError('late cache failure'));
      await requestA;
      await _flush();

      expect(gateway.validatedNoteIds, isEmpty);
      expect(controller.note.value.id, 'note-b');
    });
  });

  testWidgets('tap shows full detail and ghost entries are not rendered',
      (tester) async {
    const title = '这是一个非常长且必须在详情中完整展示的备忘录标题';
    const content = '这是一段很长的正文，点击列表项后应该不截断地展示全部内容。';
    const reason = '它与当前备忘录共享同一个重要主题';
    final valid = AiRelatedNoteModel(
      id: 'valid',
      title: title,
      content: content,
      reason: reason,
    );
    const ghost = AiRelatedNoteModel(
      id: 'ghost',
      title: ' ',
      content: '',
      reason: '幽灵项理由',
    );

    await tester.pumpWidget(
      ScreenUtilInit(
        designSize: const Size(375, 812),
        builder: (_, __) => MaterialApp(
          home: Scaffold(
            body: SmartRelatedNotes(
              notes: [valid, ghost],
              loading: false,
              message: '',
            ),
          ),
        ),
      ),
    );

    expect(find.text('未命名备忘录'), findsNothing);
    expect(find.text('幽灵项理由'), findsNothing);
    await tester.tap(find.text(title));
    await tester.pumpAndSettle();

    expect(
      tester
          .widgetList<Text>(find.text(title))
          .any((text) => text.maxLines == null),
      isTrue,
    );
    expect(
      tester
          .widgetList<Text>(find.text(content))
          .any((text) => text.maxLines == null),
      isTrue,
    );
    expect(
      tester
          .widgetList<Text>(find.text('关联理由：$reason'))
          .any((text) => text.maxLines == null),
      isTrue,
    );
  });

  testWidgets('content-only usable note receives the unnamed title fallback',
      (tester) async {
    await tester.pumpWidget(
      ScreenUtilInit(
        designSize: const Size(375, 812),
        builder: (_, __) => const MaterialApp(
          home: Scaffold(
            body: SmartRelatedNotes(
              notes: [
                AiRelatedNoteModel(
                  id: 'content-only',
                  title: ' ',
                  content: '仅有正文',
                ),
              ],
              loading: false,
              message: '',
            ),
          ),
        ),
      ),
    );

    expect(find.text('未命名备忘录'), findsOneWidget);
    expect(find.text('仅有正文'), findsOneWidget);
  });
}

SmartOrganizeController _controllerFor(
  String noteId,
  _LifecycleGateway gateway,
) {
  final controller = SmartOrganizeController(aiGateway: gateway);
  controller.setNote(NoteModel(id: noteId, title: noteId));
  return controller;
}

AiAnalysisRecordModel _record(String id, {required String noteId}) =>
    AiAnalysisRecordModel(
      id: id,
      noteId: noteId,
      organizedNote: AiOrganizeModel(title: id),
    );

AiRelatedNoteModel _related(String id) => AiRelatedNoteModel(
      id: id,
      title: 'title-$id',
      content: 'content-$id',
      reason: 'reason-$id',
    );

Future<void> _flush() => Future<void>.delayed(Duration.zero);

NoteEditController _noteEditControllerFor({
  required String noteId,
  required _LifecycleGateway gateway,
  required AnalysisRecordLatestLoader loadLatest,
}) {
  final smartOrganizeController = SmartOrganizeController(aiGateway: gateway);
  final controller = NoteEditController(
    analysisRecordLatestLoader: loadLatest,
    smartOrganizeController: smartOrganizeController,
  );
  _setNoteEditActiveNote(controller, noteId);
  return controller;
}

void _setNoteEditActiveNote(NoteEditController controller, String noteId) {
  final note = NoteModel(id: noteId, title: noteId);
  controller.note.value = note;
  controller.smartOrganizeLogic.setNote(note);
}

class _LifecycleGateway implements SmartOrganizeAiGateway {
  _LifecycleGateway({
    this.validate,
    this.organize,
    this.related,
  });

  final Future<AiValidateModel> Function(String? noteId)? validate;
  final Future<AiOrganizeModel> Function(String recordId)? organize;
  final Future<List<AiRelatedNoteModel>> Function(String recordId)? related;
  final List<String?> validatedNoteIds = [];
  final List<String> organizeRecordIds = [];
  final List<String> relatedRecordIds = [];

  @override
  Future<AiValidateModel> validateNote(String? noteId) {
    validatedNoteIds.add(noteId);
    return validate?.call(noteId) ??
        Future.value(
          AiValidateModel(meaningful: true, recordId: 'record-$noteId'),
        );
  }

  @override
  Future<AiOrganizeModel> organizeNote({String? recordId}) {
    final id = recordId!;
    organizeRecordIds.add(id);
    return organize?.call(id) ?? Future.value(AiOrganizeModel(title: id));
  }

  @override
  Future<List<AiRelatedNoteModel>> relatedNote({required String recordId}) {
    relatedRecordIds.add(recordId);
    return related?.call(recordId) ?? Future.value([]);
  }

  @override
  Future<List<AiCategorizedNoteModel>> categorizedNote({String? recordId}) =>
      Future.value([]);

  @override
  Future<String?> illustration({String? recordId}) => Future.value(null);

  @override
  Future<List<AiRelatedLinkModel>> relatedLink({String? recordId}) =>
      Future.value([]);

  @override
  Future<List<AiRelatedTitleModel>> relatedTitle({String? recordId}) =>
      Future.value([]);

  @override
  Stream<String> suggestion({String? recordId}) => const Stream.empty();
}

class _RecordingAdapter implements HttpClientAdapter {
  dynamic responseData = [
    {
      'id': 'note-1',
      'title': '相关标题',
      'content': '相关正文',
      'reason': '相关理由',
      'score': 0.9,
    },
    {
      'id': 'ghost',
      'title': ' ',
      'content': '',
    },
  ];
  final List<RequestOptions> requests = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return ResponseBody.fromString(
      jsonEncode({'data': responseData}),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
