import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:ainote_app/app/widgets/ai_card/note_enrichment.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/data/models/icon_text_action_model.dart';

void main() {
  testWidgets('organize opt-in automatically loads once per record', (tester) async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _RoutingAdapter();
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    Widget page() => MaterialApp(home: Scaffold(body: SingleChildScrollView(
      child: NoteEnrichment(note: NoteModel(id: 'a'), recordId: 'record-a',
        autoLoad: true, onSaved: (_) {}))));
    await tester.pumpWidget(page());
    await tester.pumpAndSettle();
    expect(adapter.calls.map((e) => e.path).toSet(), {
      '/v2/note/analysis/reflection', '/v2/note/analysis/productRecommendations'});
    await tester.pumpWidget(page());
    await tester.pumpAndSettle();
    expect(adapter.calls, hasLength(2));
  });

  testWidgets('reflection replacement needs confirmation and changes only text module', (tester) async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _RoutingAdapter();
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.httpClientAdapter = original);
    final note = NoteModel(id: 'a', title: '旧标题', imageUrl: '/v2/capture/images/a',
      modules: [{'moduleId': 'text-a', 'noteModuleType': 'TEXT_DATA', 'content': '旧正文',
        'items': [{'itemId': 'todo-a'}]}]);
    NoteModel? replaced;
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: SingleChildScrollView(
      child: NoteEnrichment(note: note, recordId: 'record-a', autoLoad: true,
        onSaved: (_) {}, onReplaced: (saved) => replaced = saved)))));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('替换正文'));
    await tester.tap(find.text('替换正文'));
    await tester.pumpAndSettle();
    expect(adapter.calls, hasLength(2));
    await tester.tap(find.text('取消').last);
    await tester.pumpAndSettle();
    expect(replaced, isNull);
    await tester.tap(find.text('替换正文'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('确认替换'));
    await tester.pumpAndSettle();
    final request = adapter.calls.last;
    expect(request.path, '/v2/note/module/update');
    expect(request.data['moduleId'], 'text-a');
    expect(request.data['module']['items'], [{'itemId': 'todo-a'}]);
    expect(request.data['module']['content'], contains('有来源的回顾'));
    expect(note.modules!.single['content'], '旧正文');
    expect(replaced?.id, 'a');
  });
  test('suggestion retention deduplicates and removes retained previews', () {
    final controller = SmartOrganizeController();
    controller.contentList.add(IconTextActionModel(content: '购买猫粮'));
    controller.aiSuggestionList.addAll([
      IconTextActionModel(content: '购买猫粮'), IconTextActionModel(content: '比较配料')]);
    controller.saveAllAiSuggestion();
    expect(controller.contentList.map((item) => item.content), ['购买猫粮', '比较配料']);
    expect(controller.aiSuggestionList, isEmpty);
  });

  test('saved question-answer module supports request and stored schemas', () {
    final module = QuestionAnswerModule(questionAnswerItems: [QuestionAnswerItem(question: '回顾', answer: '内容')]);
    expect(QuestionAnswerModule.fromJson(module.toJson()).questionAnswerItems!.single.answer, '内容');
    expect(QuestionAnswerModule.fromJson({'items': [{'question': '回顾', 'answer': '内容'}]})
        .questionAnswerItems!.single.answer, '内容');
  });

  testWidgets('no requests before opt-in; late reply from another note is discarded', (tester) async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _Adapter();
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() { MyDio.dio.httpClientAdapter = original; });
    Widget page(String id) => MaterialApp(home: Scaffold(body: SingleChildScrollView(
      child: NoteEnrichment(note: NoteModel(id: id), recordId: 'record-$id', onSaved: (_) {}))));
    await tester.pumpWidget(page('a'));
    expect(adapter.calls, isEmpty);
    await tester.tap(find.text('开始分析').first);
    await tester.pump();
    for (var i = 0; i < 10 && adapter.calls.isEmpty; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
    expect(adapter.calls.single.data, {'recordId': 'record-a'});
    await tester.pumpWidget(page('b'));
    adapter.result.complete({'summary': '旧笔记结果', 'sources': []});
    await tester.pumpAndSettle();
    expect(find.text('旧笔记结果'), findsNothing);
    expect(find.text('开始分析'), findsNWidgets(2));
  });

  testWidgets('empty shopping results do not invent a product', (tester) async {
    final original = MyDio.dio.httpClientAdapter;
    final adapter = _Adapter()..result.complete([]);
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() { MyDio.dio.httpClientAdapter = original; });
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: SingleChildScrollView(
      child: NoteEnrichment(note: NoteModel(id: 'a'), recordId: 'record', onSaved: (_) {})))));
    await tester.tap(find.text('开始分析').last);
    await tester.pumpAndSettle();
    expect(find.text('未发现明确的采购需求或可用结果。'), findsOneWidget);
    expect(find.text('查看来源详情'), findsNothing);
  });

  testWidgets('malformed enrichment sources and products show an error safely',
      (tester) async {
    final malformed = <dynamic>[
      {'summary': 'ok', 'sources': 'not-a-list'},
      {'summary': 'ok', 'sources': [1]},
      {'summary': 'ok', 'sources': [{'id': 's', 'title': 't'}]},
      [1],
      [{'productName': 42}],
    ];
    for (var i = 0; i < malformed.length; i++) {
      final original = MyDio.dio.httpClientAdapter;
      final adapter = _MalformedAdapter(malformed[i], reflection: i < 3);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.baseUrl = 'https://example.test';
      addTearDown(() => MyDio.dio.httpClientAdapter = original);
      await tester.pumpWidget(MaterialApp(home: Scaffold(body:
        SingleChildScrollView(child: NoteEnrichment(
          note: NoteModel(id: 'a'), recordId: 'record', onSaved: (_) {})))));
      await tester.tap(find.text('开始分析').at(i < 3 ? 0 : 1));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'case $i');
      expect(find.text('暂时未能获取结果，请重试。'), findsWidgets, reason: 'case $i');
    }
  });
}

class _RoutingAdapter implements HttpClientAdapter {
  final calls = <RequestOptions>[];
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream,
      Future<void>? cancelFuture) async {
    calls.add(options);
    final Object data = options.path.endsWith('/reflection')
      ? {'summary': '有来源的回顾', 'sources': [{'id': 'b', 'title': '历史', 'content': '经验'}]}
      : options.path.endsWith('/productRecommendations') ? []
      : {'id': 'a', 'title': '旧标题', 'modules': [options.data['module']]};
    return ResponseBody.fromString(jsonEncode({'code': 200, 'data': data}), 200,
      headers: {Headers.contentTypeHeader: [Headers.jsonContentType]});
  }
  @override
  void close({bool force = false}) {}
}

class _Adapter implements HttpClientAdapter {
  final result = Completer<dynamic>();
  final calls = <RequestOptions>[];
  final called = Completer<void>();
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    calls.add(options);
    if (!called.isCompleted) called.complete();
    return ResponseBody.fromString(jsonEncode({'code': 200, 'data': await result.future}), 200,
      headers: {Headers.contentTypeHeader: [Headers.jsonContentType]});
  }
  @override
  void close({bool force = false}) {}
}

class _MalformedAdapter implements HttpClientAdapter {
  _MalformedAdapter(this.data, {required this.reflection});
  final dynamic data;
  final bool reflection;
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream,
      Future<void>? cancelFuture) async {
    final payload = reflection == options.path.endsWith('/reflection') ? data :
        (data is List ? data : <dynamic>[]);
    return ResponseBody.fromString(jsonEncode({'code': 200, 'data': payload}), 200,
        headers: {Headers.contentTypeHeader: [Headers.jsonContentType]});
  }
  @override
  void close({bool force = false}) {}
}
