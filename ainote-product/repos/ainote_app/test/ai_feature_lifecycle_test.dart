import 'dart:async';
import 'dart:convert';

import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_note_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/complete_info_model.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/ai_assist_content.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/widgets/ai_card/todo_search_detail.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('selected content is sent instead of an empty placeholder', () {
    expect(
      rewriteSelectedContent(
        useSelection: true,
        selectedTitleText: '标题上下文',
        selectedContentText: '  真正选中的正文  ',
      ),
      '真正选中的正文',
    );
    expect(
      rewriteSelectedContent(
        useSelection: true,
        selectedTitleText: '  仅选中标题  ',
        selectedContentText: ' ',
      ),
      '仅选中标题',
    );
    expect(
      rewriteSelectedContent(
        useSelection: false,
        selectedContentText: '正文',
      ),
      isNull,
    );
  });

  test('todo related requests preserve partial results', () async {
    final results = await loadTodoSearchResults(
      text: '待办内容',
      loadLinks: (_) async => throw StateError('network unavailable'),
      loadTitles: (_) async => [
        AiRelatedTitleModel(title: '成功的相关标题', emoji: '📝'),
      ],
    );

    expect(results.links, isEmpty);
    expect(results.titles.single.title, '成功的相关标题');
    expect(results.partiallyFailed, isTrue);
    expect(results.completelyFailed, isFalse);
  });

  test('todo related requests distinguish total failure from no matches',
      () async {
    final results = await loadTodoSearchResults(
      text: '待办内容',
      loadLinks: (_) async => throw StateError('links failed'),
      loadTitles: (_) async => throw StateError('titles failed'),
    );

    expect(results.links, isEmpty);
    expect(results.titles, isEmpty);
    expect(results.completelyFailed, isTrue);
  });

  test('smart organize calls related titles once and always ends loading',
      () async {
    final gateway = _PartialFailureGateway();
    final controller = SmartOrganizeController(aiGateway: gateway);

    await controller.smartOrganize();

    expect(gateway.relatedTitleCalls, 1);
    expect(controller.aiRelatedTitleList.single.title, '保留下来的结果');
    expect(controller.aiSuggestionMessage.value, contains('失败'));
    expect(controller.aiValidateLoading.value, isFalse);
    expect(controller.aiOrganizeLoading.value, isFalse);
    expect(controller.aiSuggestionLoading.value, isFalse);
    expect(controller.aiRelatedLinkLoading.value, isFalse);
    expect(controller.aiRelatedTitleLoading.value, isFalse);
    expect(controller.aiIllustrationLoading.value, isFalse);
    expect(controller.aiCategorizedNoteLoading.value, isFalse);
  });

  test('retry organize preserves successful related modules', () async {
    final gateway = _RetryOrganizeGateway();
    final controller = SmartOrganizeController(aiGateway: gateway);
    await controller.smartOrganize();
    expect(controller.aiOrganizeMessage.value, contains('失败'));
    await controller.retryOrganize();
    expect(controller.aiOrganizeResult.value.bodyText, '整理结果');
    expect(controller.aiOrganizeMessage.value, isEmpty);
    expect(controller.aiOrganizeLoading.value, isFalse);
    expect(gateway.validateCalls, 1);
    expect(gateway.relatedTitleCalls, 1);
    expect(controller.aiRelatedTitleList.single.title, '保留下来的结果');
  });

  test('smart organize is single-flight while a run is active', () async {
    final gate = Completer<void>();
    final gateway = _PartialFailureGateway(validationGate: gate);
    final controller = SmartOrganizeController(aiGateway: gateway);

    final first = controller.smartOrganize();
    final second = controller.smartOrganize();
    await Future<void>.delayed(Duration.zero);
    expect(gateway.validateCalls, 1);

    gate.complete();
    await Future.wait([first, second]);
    expect(gateway.validateCalls, 1);
    expect(gateway.relatedTitleCalls, 1);
  });

  test('suggestion decoder preserves split utf8 characters and SSE lines',
      () async {
    const payload = 'data:{"建议":"先整理中文内容"}\n';
    final bytes = utf8.encode(payload);
    final firstMultibyteByte = bytes.indexWhere((byte) => byte >= 0x80);
    final splitInsideCharacter = firstMultibyteByte + 1;
    final stream = Stream<List<int>>.fromIterable([
      bytes.sublist(0, splitInsideCharacter),
      bytes.sublist(splitInsideCharacter),
    ]);

    expect(
      await decodeAiSuggestionLines(stream).toList(),
      ['{"建议":"先整理中文内容"}'],
    );
  });

  test('complete info parser accepts backend snake case contract', () {
    final item = CompleteInfoModel.fromJson({
      'vague_phrase': '下周见',
      'inquiry_process': '具体是哪一天？',
      'options': [
        {'option': '下周一'},
        {'option': '下周五'},
      ],
    });

    expect(item.isUsable, isTrue);
    expect(item.vaguePhrase, '下周见');
    expect(item.options, ['下周一', '下周五']);
  });
}

class _PartialFailureGateway implements SmartOrganizeAiGateway {
  _PartialFailureGateway({this.validationGate});

  final Completer<void>? validationGate;
  int validateCalls = 0;
  int relatedTitleCalls = 0;

  @override
  Future<AiValidateModel> validateNote(String? noteId) async {
    validateCalls++;
    await validationGate?.future;
    return AiValidateModel(
      meaningful: true,
      recordId: 'record-id',
    );
  }

  @override
  Future<AiOrganizeModel> organizeNote({String? recordId}) async =>
      AiOrganizeModel();

  @override
  Stream<String> suggestion({String? recordId}) =>
      Stream<String>.error(StateError('stream failed'));

  @override
  Future<List<AiRelatedLinkModel>> relatedLink({String? recordId}) async =>
      throw StateError('links failed');

  @override
  Future<List<AiRelatedTitleModel>> relatedTitle({String? recordId}) async {
    relatedTitleCalls++;
    return [AiRelatedTitleModel(title: '保留下来的结果', emoji: '✅')];
  }

  @override
  Future<List<AiRelatedNoteModel>> relatedNote(
          {required String recordId}) async =>
      [];

  @override
  Future<String?> illustration({String? recordId}) async =>
      throw StateError('illustration failed');

  @override
  Future<List<AiCategorizedNoteModel>> categorizedNote(
          {String? recordId}) async =>
      [];
}

class _RetryOrganizeGateway extends _PartialFailureGateway {
  int organizeCalls = 0;
  @override
  Future<AiOrganizeModel> organizeNote({String? recordId}) async {
    if (++organizeCalls == 1) throw StateError('synthetic failure');
    return AiOrganizeModel(bodyText: '整理结果');
  }
}
