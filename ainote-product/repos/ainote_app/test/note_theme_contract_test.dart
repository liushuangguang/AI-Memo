import 'dart:convert';
import 'dart:typed_data';

import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/api/note_theme.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('theme DTO', () {
    test('keeps durable merge provenance and source titles', () {
      final theme = NoteThemeModel.fromJson({
        'id': 'theme-1',
        'theme': '旅行计划',
        'description': '整合行程',
        'mergedNoteId': 'merged-1',
        'mergeHistory': [
          {
            'operationId': 'op-1',
            'mergedNoteId': 'merged-1',
            'mergedTitle': '行程汇总',
            'mergedContentPreview': '摘要',
            'imageUrls': ['https://example.test/a.jpg'],
            'sources': [
              {'noteId': 'note-a', 'title': '酒店'},
              {'noteId': 'note-b', 'title': '车票'},
            ],
            'mergedAt': '2026-09-06T12:00:00',
          }
        ],
      });

      expect(theme.latestMerge?.sources.map((item) => item.noteId),
          ['note-a', 'note-b']);
      expect(theme.latestMerge?.imageUrls, ['https://example.test/a.jpg']);
      expect(theme.mergedNoteId, 'merged-1');
    });
  });

  group('theme API contract', () {
    late HttpClientAdapter originalAdapter;
    late _ThemeAdapter adapter;

    setUp(() {
      originalAdapter = MyDio.dio.httpClientAdapter;
      adapter = _ThemeAdapter();
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.baseUrl = 'https://example.test';
    });

    tearDown(() => MyDio.dio.httpClientAdapter = originalAdapter);

    test('create sends bounded trimmed theme fields', () async {
      adapter.responseData = {
        'data': {
          'id': 'theme-1',
          'theme': '旅行',
          'description': '行程',
          'mergeHistory': [],
        }
      };

      await NoteThemeApi.createTheme(theme: '  旅行 ', description: ' 行程 ');

      final request = adapter.requests.single;
      expect(request.path, '/note/theme/create');
      expect(request.data, {'theme': '旅行', 'description': '行程'});
      expect(request.extra['ro_disable_retry'], isTrue);
    });

    test('merge sends confirmed selection and stable idempotency key', () async {
      adapter.responseData = {
        'data': {
          'themeId': 'theme-1',
          'note': {'id': 'merged-1', 'title': '合并笔记'},
          'history': {
            'operationId': 'op-1',
            'mergedNoteId': 'merged-1',
            'mergedTitle': '合并笔记',
            'mergedContentPreview': '摘要',
            'imageUrls': [],
            'sources': [],
          }
        }
      };

      await NoteThemeApi.merge(
        themeId: 'theme-1',
        sourceNoteIds: ['note-a', 'note-b'],
        idempotencyKey: 'request-12345678',
      );

      final request = adapter.requests.single;
      expect(request.path, '/note/theme/theme-1/merge');
      expect(request.data, {
        'sourceNoteIds': ['note-a', 'note-b'],
        'selectionConfirmed': true,
        'idempotencyKey': 'request-12345678',
      });
      expect(request.extra['ro_disable_retry'], isTrue);
      expect(request.receiveTimeout, const Duration(seconds: 120));
    });
  });
}

class _ThemeAdapter implements HttpClientAdapter {
  dynamic responseData;
  final List<RequestOptions> requests = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return ResponseBody.fromString(
      jsonEncode(responseData),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
