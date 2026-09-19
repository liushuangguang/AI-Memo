import 'dart:convert';
import 'dart:typed_data';

import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/api/api_urls.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late HttpClientAdapter previous;
  late _GenerationAdapter adapter;
  late BaseOptions previousOptions;

  setUp(() {
    previous = MyDio.dio.httpClientAdapter;
    previousOptions = MyDio.dio.options;
    adapter = _GenerationAdapter();
    MyDio.dio.httpClientAdapter = adapter;
    MyDio.dio.options = BaseOptions(
      baseUrl: 'https://example.test',
      receiveTimeout: const Duration(seconds: 30),
    );
  });

  tearDown(() {
    MyDio.dio.httpClientAdapter = previous;
    MyDio.dio.options = previousOptions;
  });

  test(
      'blocking AI operations allow provider fallback without automatic replay',
      () async {
    await AiApi.aiValidateNote('synthetic');
    await AiApi.aiOrganizeNote(recordId: 'synthetic');
    await AiApi.validateContentAssist('synthetic');
    await AiApi.getAssistantDirection('synthetic');
    await AiApi.startRewriteContent(
        recordId: 'synthetic', assistDirections: ['群聊摘要']);
    expect(adapter.requests.map((e) => e.path), [
      ApiUrls.aiValidateNote,
      ApiUrls.aiOrganizeNote,
      ApiUrls.contentAssistValidate,
      ApiUrls.selectedAssistantDirection,
      ApiUrls.rewriteContent,
    ]);
    for (final request in adapter.requests) {
      expect(request.receiveTimeout, const Duration(seconds: 200));
      expect(request.extra['ro_disable_retry'], isTrue);
    }
    // Ordinary note writes and reads retain their original transport budget.
    expect(MyDio.dio.options.receiveTimeout, const Duration(seconds: 30));
  });

  test('rewrite preserves visibly selected direction and selected text',
      () async {
    await AiApi.startRewriteContent(
      recordId: 'synthetic',
      assistDirections: ['整理为群聊摘要', '简单优化表达'],
      selectedContent: '  要整理的选中文本  ',
    );
    expect(adapter.requests.single.data, {
      'recordId': 'synthetic',
      'assistDirections': ['整理为群聊摘要', '简单优化表达'],
      'selectedContent': '要整理的选中文本',
    });
  });

  test('suggestion streams retain generation budget and forbid replay', () async {
    await AiApi.aiSuggestion(recordId: 'synthetic').toList();
    await (await AiApi.aiSuggestionStream(recordId: 'synthetic'))!.toList();
    expect(adapter.requests, hasLength(2));
    for (final request in adapter.requests) {
      expect(request.responseType, ResponseType.stream);
      expect(request.receiveTimeout, const Duration(seconds: 200));
      expect(request.extra['ro_disable_retry'], isTrue);
      expect(request.headers['Accept'], 'text/event-stream');
    }
  });
}

class _GenerationAdapter implements HttpClientAdapter {
  final requests = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(RequestOptions options,
      Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    requests.add(options);
    return ResponseBody.fromString(
        jsonEncode({
          'code': 200,
          'data': {
            'isMeaningful': true,
            'recordId': 'synthetic',
            'title': '群聊摘要',
            'bodyText': '真实整理结果',
            'availableAssistantDirection': ['整理为群聊摘要'],
            'rewrittenContent': '整理后的正文',
            'todos': [],
          },
        }),
        200,
        headers: {
          Headers.contentTypeHeader: ['application/json']
        });
  }

  @override
  void close({bool force = false}) {}
}
