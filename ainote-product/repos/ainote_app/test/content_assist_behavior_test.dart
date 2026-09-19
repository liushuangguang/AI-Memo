import 'dart:async';
import 'dart:io';

import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/content_assist_model.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/ai_assist_content.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('content assist direction state', () {
    test('normalizes wrapped recommendations and removes duplicates', () {
      expect(
        normalizeAssistDirections([
          ' 【整理为群聊摘要】 ',
          '整理为群聊摘要',
          ' ',
          '转为FAQ问答',
        ]),
        ['整理为群聊摘要', '转为FAQ问答'],
      );
    });

    test('keeps an explicit entry choice when recommendations arrive', () {
      expect(
        selectionAfterRecommendations(
          currentSelection: const ['简单优化表达'],
          recommendations: const ['整理为群聊摘要', '提取交易信息'],
          preserveCurrentSelection: true,
        ),
        ['简单优化表达'],
      );
    });

    test('defaults to at most three recommendations without an explicit choice',
        () {
      expect(
        selectionAfterRecommendations(
          currentSelection: const [],
          recommendations: const ['A', 'B', 'C', 'D'],
          preserveCurrentSelection: false,
        ),
        ['A', 'B', 'C'],
      );
    });
  });

  group('content assist widget', () {
    testWidgets(
        'shows dynamic recommendations and does not overwrite entry choice',
        (tester) async {
      final directions = Completer<ContentAssistDirectionModel>();
      List<String>? submittedDirections;

      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-1'),
          initialDirection: '简单优化表达',
          noSelectedContentText: true,
          validateContentAssist: _valid,
          loadAssistDirections: (_) => directions.future,
          rewriteContent: ({
            recordId,
            assistDirections,
            selectedContent,
          }) async =>
              ContentAssistResultModel(rewrittenContent: '', todos: []),
          onConfirm: (items) {
            submittedDirections = items.cast<String>();
          },
        ),
      ));

      expect(find.text('AI理解信息中'), findsOneWidget);
      directions.complete(ContentAssistDirectionModel(
        reason: '（选中理由）适合将群聊内容结构化',
        availableAssistantDirection: ['【整理为群聊摘要】', '提取交易信息'],
      ));
      await tester.pumpAndSettle();

      expect(find.text('AI推荐方向'), findsOneWidget);
      expect(find.text('整理为群聊摘要'), findsWidgets);
      await tester.tap(
        find.byKey(const ValueKey('ai-recommendation-整理为群聊摘要')),
      );
      await tester.pump();
      await tester.tap(find.text('开始辅写'));
      await tester.pumpAndSettle();

      expect(
        submittedDirections,
        ['简单优化表达', '整理为群聊摘要'],
      );
    });

    testWidgets(
        'keeps footer visible without overflow on a narrow large-text sheet',
        (tester) async {
      await _setSurface(tester, const Size(320, 568));
      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-1'),
          noSelectedContentText: false,
          selectedContentText: '需要辅写的文本',
          validateContentAssist: _valid,
          loadAssistDirections: (_) async => ContentAssistDirectionModel(
            reason: '内容适合结构化处理',
            availableAssistantDirection: [
              '整理为群聊摘要',
              '提取交易信息',
              '转为FAQ问答',
              '提炼关键词标签',
            ],
          ),
          rewriteContent: _emptyRewrite,
        ),
        textScale: 1.3,
      ));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.byKey(const Key('ai-assist-scroll-body')), findsOneWidget);
      expect(find.byKey(const Key('ai-assist-start')), findsOneWidget);
      expect(find.byKey(const Key('ai-assist-close')), findsOneWidget);
      expect(
        tester.getBottomRight(find.byKey(const Key('ai-assist-close'))).dy,
        lessThanOrEqualTo(568),
      );
    });

    testWidgets(
        'rewrite failure can retry into preview without direct writeback',
        (tester) async {
      var attempts = 0;
      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-1'),
          initialDirection: '简单优化表达',
          noSelectedContentText: true,
          validateContentAssist: _valid,
          loadAssistDirections: (_) async => ContentAssistDirectionModel(
            availableAssistantDirection: ['整理为群聊摘要'],
          ),
          rewriteContent: ({
            recordId,
            assistDirections,
            selectedContent,
          }) async {
            attempts++;
            if (attempts == 1) throw Exception('temporary failure');
            return ContentAssistResultModel(
              rewrittenContent: '这是预览内容',
              todos: [],
            );
          },
          resultBuilder: (result, selectedItems, originContent) => Text(
            '预览：${result.rewrittenContent}',
          ),
        ),
      ));
      await tester.pumpAndSettle();

      await tester.tap(find.text('开始辅写'));
      await tester.pumpAndSettle();
      expect(find.text('辅写失败，请稍后重试'), findsOneWidget);

      await tester.tap(find.text('开始辅写'));
      await tester.pumpAndSettle();
      expect(find.text('预览：这是预览内容'), findsOneWidget);
      expect(find.text('关闭'), findsOneWidget);
      expect(attempts, 2);
    });

    testWidgets('entry choice remains usable when recommendation loading fails',
        (tester) async {
      List<String>? submittedDirections;
      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-1'),
          initialDirection: '简单优化表达',
          noSelectedContentText: true,
          validateContentAssist: _valid,
          loadAssistDirections: (_) async => throw Exception('offline'),
          rewriteContent: _emptyRewrite,
          onConfirm: (items) {
            submittedDirections = items.cast<String>();
          },
        ),
      ));
      await tester.pumpAndSettle();

      expect(find.text('AI推荐加载失败，仍可使用已选方向'), findsOneWidget);
      expect(find.text('开始辅写'), findsOneWidget);
      await tester.tap(find.text('开始辅写'));
      await tester.pumpAndSettle();
      expect(submittedDirections, ['简单优化表达']);
    });

    testWidgets('does not call AI when the note still has no id', (tester) async {
      var validations = 0;
      var rewrites = 0;
      await tester.pumpWidget(_testApp(AiAssistContent(
        noteModel: NoteModel(id: '', title: '未保存'),
        initialDirection: '简单优化表达',
        noSelectedContentText: true,
        validateContentAssist: (_) async {
          validations++;
          return _valid(null);
        },
        rewriteContent: ({recordId, assistDirections, selectedContent}) async {
          rewrites++;
          return ContentAssistResultModel(rewrittenContent: '不应调用', todos: []);
        },
      )));
      await tester.pumpAndSettle();
      expect(validations, 0);
      expect(rewrites, 0);
      expect(find.textContaining('备忘录尚未保存'), findsOneWidget);
    });

    testWidgets(
        'late direction and rewrite responses do not setState after close',
        (tester) async {
      final directions = Completer<ContentAssistDirectionModel>();
      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-1'),
          validateContentAssist: _valid,
          loadAssistDirections: (_) => directions.future,
          rewriteContent: _emptyRewrite,
        ),
      ));
      await tester.pumpWidget(const SizedBox.shrink());
      directions.complete(ContentAssistDirectionModel(
        availableAssistantDirection: ['整理为群聊摘要'],
      ));
      await tester.pump();
      expect(tester.takeException(), isNull);

      final rewrite = Completer<ContentAssistResultModel>();
      await tester.pumpWidget(_testApp(
        AiAssistContent(
          noteModel: NoteModel(id: 'note-2'),
          initialDirection: '简单优化表达',
          noSelectedContentText: true,
          validateContentAssist: _valid,
          loadAssistDirections: (_) async => ContentAssistDirectionModel(
            availableAssistantDirection: ['整理为群聊摘要'],
          ),
          rewriteContent: ({
            recordId,
            assistDirections,
            selectedContent,
          }) =>
              rewrite.future,
        ),
      ));
      await tester.pumpAndSettle();
      await tester.tap(find.text('开始辅写'));
      await tester.pump();
      await tester.pumpWidget(const SizedBox.shrink());
      rewrite.complete(ContentAssistResultModel(
        rewrittenContent: '迟到结果',
        todos: [],
      ));
      await tester.pump();
      expect(tester.takeException(), isNull);
    });

    testWidgets('custom direction sheet confirms only once', (tester) async {
      var confirmations = 0;
      await tester.pumpWidget(_testApp(
        TalkToAI(
          handleConfirm: (_) {
            confirmations++;
            return true;
          },
          handleCancel: () {},
        ),
      ));

      await tester.tap(find.byType(TextField));
      await tester.pumpAndSettle();
      final fields = find.byType(TextField);
      expect(fields, findsNWidgets(2));
      await tester.enterText(fields.last, '生成大纲');
      await tester.tap(find.text('确定').last);
      await tester.pumpAndSettle();

      expect(confirmations, 1);
    });

    testWidgets('renders fixed sheet evidence with selected dynamic directions',
        (tester) async {
      await tester.runAsync(_loadEvidenceFont);
      await _setSurface(tester, const Size(420, 923));
      final evidenceKey = GlobalKey();
      await tester.pumpWidget(_testApp(
        RepaintBoundary(
          key: evidenceKey,
          child: AiAssistContent(
            noteModel: NoteModel(id: 'note-evidence'),
            noSelectedContentText: true,
            validateContentAssist: _valid,
            loadAssistDirections: (_) async => ContentAssistDirectionModel(
              reason: '笔记包含群聊记录和交易信息，可按下列方向辅写。',
              availableAssistantDirection: [
                '整理为群聊摘要',
                '提取交易信息',
                '转为FAQ问答',
                '提炼关键词标签',
              ],
            ),
            rewriteContent: _emptyRewrite,
          ),
        ),
        fontFamily: 'EvidenceCjk',
      ));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);

      if (const bool.fromEnvironment('UPDATE_CONTENT_ASSIST_EVIDENCE')) {
        await expectLater(
          find.byKey(evidenceKey),
          matchesGoldenFile('content_assist_bug004_005_fixed.png'),
        );
      }
    }, skip: !Platform.isWindows);
  });
}

Future<AiValidateModel> _valid(String? noteId) async => AiValidateModel(
      meaningful: true,
      recordId: 'record-1',
    );

Future<ContentAssistResultModel> _emptyRewrite({
  String? recordId,
  List<String>? assistDirections,
  String? selectedContent,
}) async =>
    ContentAssistResultModel(rewrittenContent: '', todos: []);

Widget _testApp(
  Widget child, {
  double textScale = 1,
  String? fontFamily,
}) {
  return ScreenUtilInit(
    designSize: const Size(375, 812),
    builder: (context, _) => MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(fontFamily: fontFamily),
      builder: (context, appChild) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: TextScaler.linear(textScale),
        ),
        child: appChild!,
      ),
      home: Scaffold(body: child),
    ),
  );
}

Future<void> _loadEvidenceFont() async {
  final bytes = await File('C:\\Windows\\Fonts\\msyh.ttc').readAsBytes();
  final loader = FontLoader('EvidenceCjk')
    ..addFont(Future.value(ByteData.sublistView(Uint8List.fromList(bytes))));
  await loader.load();
}

Future<void> _setSurface(WidgetTester tester, Size size) async {
  tester.view.devicePixelRatio = 1;
  tester.view.physicalSize = size;
  addTearDown(() {
    tester.view.resetPhysicalSize();
    tester.view.resetDevicePixelRatio();
  });
}
