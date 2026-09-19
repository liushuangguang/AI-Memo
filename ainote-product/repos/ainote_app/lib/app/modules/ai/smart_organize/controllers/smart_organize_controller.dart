import 'dart:async';
import 'package:dio/dio.dart';

import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:ainote_app/app/data/models/ai_analysis_record_model.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_note_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_module/ai_picture_module.dart';
import 'package:ainote_app/app/data/models/note_module/key_value_pair_module.dart';
import 'package:ainote_app/app/data/models/note_module/note_module_payload.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:ainote_app/app/data/models/note_module/scene_module.dart';
import 'package:ainote_app/app/data/models/note_module/text_data_module.dart';
import 'package:ainote_app/app/utils/json_format.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class SmartOrganizeController extends GetxController {
  SmartOrganizeController({
    SmartOrganizeAiGateway? aiGateway,
    bool? initialAutoOrganize,
    Future<void> Function(bool)? persistAutoOrganize,
  })  : _aiGateway = aiGateway ?? const DefaultSmartOrganizeAiGateway(),
        _persistAutoOrganize =
            persistAutoOrganize ?? MyStorage.setAutoOrganizeEnabled,
        autoOrganize =
            (initialAutoOrganize ?? MyStorage.getAutoOrganizeEnabled()).obs;

  final SmartOrganizeAiGateway _aiGateway;
  final Future<void> Function(bool) _persistAutoOrganize;
  Future<void>? _smartOrganizeFuture;
  _OperationIdentity? _activeOperation;
  Object _operationToken = Object();
  String? _activeRecordId;
  final RxBool autoOrganize;
  final autoOrganizeSaving = false.obs;
  final replacing = false.obs;
  final contentList = <IconTextActionModel>[].obs;
  final expandable = true.obs;

  final listViewController = ScrollController();

  final note = NoteModel.empty().obs;

  // ai 检查是否有意义
  final aiValidateResult = AiValidateModel().obs;
  final aiValidateLoading = true.obs;

  // ai 智能整理
  final aiOrganizeResult = AiOrganizeModel().obs;
  final aiOrganizeLoading = true.obs;
  final aiOrganizeMessage = ''.obs;

  // ai 智能建议
  final aiSuggestionStr = ''.obs;
  final aiSuggestionLoading = true.obs;
  final aiSuggestion = AiSuggestionModel().obs;
  final aiSuggestionList = <IconTextActionModel>[].obs;
  final aiSuggestionMessage = ''.obs;

  // ai 相关链接
  final aiRelatedLinkList = <AiRelatedLinkModel>[].obs;
  final aiRelatedLinkLoading = true.obs;

  // ai 相关标题
  final aiRelatedTitleList = <AiRelatedTitleModel>[].obs;
  final aiRelatedTitleLoading = true.obs;

  // ai 相关备忘录
  final aiRelatedNoteList = <AiRelatedNoteModel>[].obs;
  final aiRelatedNoteLoading = true.obs;
  final aiRelatedNoteMessage = ''.obs;

  // ai 配图
  final aiIllustration = ''.obs;
  final aiIllustrationLoading = true.obs;

  // ai 信息分类
  final aiCategorizedNoteList = <AiCategorizedNoteModel>[].obs;
  final aiCategorizedNoteLoading = true.obs;
  final aiCategorizedNoteMessage = ''.obs;

  @override
  void onClose() {
    reset();
    super.onClose();
  }

  bool parseAiSuggestion(String str) {
    AiSuggestionModel? data = parseAiSuggestionModelFromJsonString(str);
    if (data != null) {
      aiSuggestion.value = data;
      aiSuggestionStr.value = '';
      aiSuggestionList.value = aiSuggestion.value.toIconTextActionModelList();
      aiSuggestionList.removeWhere((item) => item.content.trim().isEmpty);
      return aiSuggestionList.isNotEmpty;
    }
    return false;
  }

  void updateNoteTextDataModule() {
    try {
      TextDataModule textDataModule = TextDataModule();
      if (note.value.modules == null || note.value.modules?.isEmpty == true) {
        note.value.modules = [textDataModule.toJson()];
      }

      note.value.modules ??= [];
      int? index = note.value.modules
          ?.indexWhere((e) => e['noteModuleType'] == 'TEXT_DATA');
      if (index != null && index >= 0) {
        textDataModule = TextDataModule.fromJson(note.value.modules?[index]);
      }

      textDataModule.items = contentList;
      note.value.modules?[index ?? 0] = textDataModule.toJson();
    } catch (e) {}
  }

  Future<bool> replaceAllNote() async {
    if (replacing.value ||
        aiOrganizeLoading.value ||
        aiSuggestionLoading.value ||
        aiOrganizeMessage.isNotEmpty) return false;
    replacing.value = true;
    try {
      final operation = _captureOperation();
      final saved = await NoteApi.replaceNote(
          note: note.value,
          organizedNote: aiOrganizeResult.value,
          list: contentList);
      if (!_isCurrent(operation)) return false;
      note.value = saved;
      Toast.success("替换成功");
      return true;
    } finally {
      if (!isClosed) replacing.value = false;
    }
  }

  void setAiValidateResult(AiValidateModel result) {
    aiValidateResult.value = result;
  }

  void setNote(NoteModel data) {
    note.value = data;
    reset();
  }

  void setAnalysisRecord(AiAnalysisRecordModel result) {
    final recordNoteId = _normalizedId(result.noteId);
    final currentNoteId = _normalizedId(note.value.id);
    if (recordNoteId == null || recordNoteId != currentNoteId) return;

    _invalidateOperations();
    final normalizedRecordId = _normalizedId(result.id);
    _activeRecordId = normalizedRecordId;
    final operation = _captureOperation(recordId: normalizedRecordId);
    _activeOperation = operation;
    aiRelatedNoteList.clear();
    aiValidateResult.value = AiValidateModel(
      meaningful: true,
      recordId: normalizedRecordId,
    );
    aiSuggestionStr.value = '';
    aiSuggestionMessage.value = '';
    aiOrganizeMessage.value = '';
    aiValidateLoading.value = false;
    aiOrganizeLoading.value = false;
    aiSuggestionLoading.value = false;
    aiRelatedLinkLoading.value = false;
    aiRelatedTitleLoading.value = false;
    aiRelatedNoteLoading.value = true;
    aiRelatedNoteMessage.value = '';
    aiIllustrationLoading.value = false;
    aiCategorizedNoteLoading.value = false;
    obs;

    aiOrganizeResult.value = result.organizedNote ?? AiOrganizeModel();
    aiSuggestion.value = result.aiSuggestion ?? AiSuggestionModel();
    aiRelatedLinkList.value = result.relatedLinkList ?? [];
    aiRelatedTitleList.value = result.relatedTitleList ?? [];
    aiIllustration.value = result.aiIllustration ?? '';
    aiSuggestionList.value = aiSuggestion.value.toIconTextActionModelList();
    aiCategorizedNoteList.value = result.categorizedNote ?? [];
    fromOrganizeResultToContentList();
    if (normalizedRecordId != null) {
      unawaited(_startAiRelatedNote(
        recordId: normalizedRecordId,
        operation: operation,
      ));
    } else {
      aiRelatedNoteLoading.value = false;
      aiRelatedNoteMessage.value = '暂无相关备忘录';
    }
  }

  void fromOrganizeResultToContentList() {
    contentList.clear();
    // 生成content
    contentList.addAll(aiOrganizeResult.value.todoList
            ?.map((e) => IconTextActionModel(
                  id: e.id,
                  content: e.content,
                  done: e.done,
                  scheduledAt: e.scheduledAt,
                  description: e.description,
                ))
            .toList() ??
        []);
  }

  void reset() {
    _invalidateOperations();
    _clearState();
  }

  void _invalidateOperations() {
    _operationToken = Object();
    _activeOperation = null;
    _activeRecordId = null;
    _smartOrganizeFuture = null;
  }

  void _clearState() {
    aiValidateResult.value = AiValidateModel();
    aiOrganizeResult.value = AiOrganizeModel();
    aiSuggestion.value = AiSuggestionModel();
    aiCategorizedNoteList.value = [];
    aiCategorizedNoteMessage.value = '';
    aiRelatedLinkList.clear();
    aiRelatedTitleList.clear();
    aiRelatedNoteList.clear();
    aiSuggestionStr.value = '';
    aiSuggestionMessage.value = '';
    aiOrganizeMessage.value = '';
    aiValidateLoading.value = true;
    aiOrganizeLoading.value = true;
    aiSuggestionLoading.value = true;
    aiRelatedLinkLoading.value = true;
    aiRelatedTitleLoading.value = true;
    aiRelatedNoteLoading.value = true;
    aiRelatedNoteMessage.value = '';
    aiCategorizedNoteLoading.value = true;
    aiSuggestionList.value = aiSuggestion.value.toIconTextActionModelList();

    aiIllustration.value = '';
    aiIllustrationLoading.value = true;
    fromOrganizeResultToContentList();
  }

  Future<void> smartOrganize() async {
    final running = _smartOrganizeFuture;
    final active = _activeOperation;
    if (running != null && active != null && _isCurrent(active)) {
      return running;
    }

    reset();
    final identity = _captureOperation();
    _activeOperation = identity;
    final operation = _runSmartOrganize(identity);
    _smartOrganizeFuture = operation;
    try {
      await operation;
    } finally {
      if (identical(_smartOrganizeFuture, operation) && _isCurrent(identity)) {
        _smartOrganizeFuture = null;
      }
    }
  }

  Future<void> _runSmartOrganize(_OperationIdentity operation) async {
    try {
      final validation = await _aiGateway.validateNote(operation.noteId);
      if (!_isCurrent(operation)) return;
      aiValidateResult.value = validation;
      aiValidateLoading.value = false;
      if (validation.meaningful != true) {
        _finishDownstreamLoading(operation);
        return;
      }

      final recordId = _normalizedId(validation.recordId);
      if (recordId == null) {
        aiOrganizeMessage.value = '智能整理记录无效，请稍后重试';
        aiRelatedNoteMessage.value = '缺少整理记录，无法加载相关备忘录';
        _finishDownstreamLoading(operation);
        return;
      }
      _activeRecordId = recordId;
      final recordOperation = operation.withRecordId(recordId);
      _activeOperation = recordOperation;
      if (!_isCurrent(recordOperation)) return;

      await _startAiOrganize(
        recordId: recordId,
        operation: recordOperation,
      );
      if (!_isCurrent(recordOperation)) return;
      unawaited(_startAiRelatedNote(
        recordId: recordId,
        operation: recordOperation,
      ));
      await Future.wait<void>([
        _startAiSuggestion(recordId: recordId, operation: recordOperation),
        _startAiRelatedLink(recordId: recordId, operation: recordOperation),
        _startAiRelatedTitle(recordId: recordId, operation: recordOperation),
        _startAiIllustration(recordId: recordId, operation: recordOperation),
        _startAiCategorizedNote(recordId: recordId, operation: recordOperation),
      ], eagerError: false);
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiValidateResult.value = AiValidateModel(
        meaningful: false,
        result: '智能整理暂时不可用，请稍后重试',
      );
      _finishDownstreamLoading(operation);
    } finally {
      if (_isCurrent(operation)) aiValidateLoading.value = false;
    }
  }

  void _finishDownstreamLoading(_OperationIdentity operation) {
    if (!_isCurrent(operation)) return;
    aiOrganizeLoading.value = false;
    aiSuggestionLoading.value = false;
    aiRelatedLinkLoading.value = false;
    aiRelatedTitleLoading.value = false;
    aiRelatedNoteLoading.value = false;
    aiIllustrationLoading.value = false;
    aiCategorizedNoteLoading.value = false;
  }

  Future<void> _startAiRelatedTitle({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      final results = await _aiGateway.relatedTitle(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiRelatedTitleList.value = results;
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiRelatedTitleList.clear();
    } finally {
      if (_isCurrent(operation)) aiRelatedTitleLoading.value = false;
    }
  }

  Future<void> _startAiRelatedNote({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    if (!_isCurrent(operation) || recordId.trim().isEmpty) return;
    try {
      final results = await _aiGateway.relatedNote(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiRelatedNoteList.value = results;
      aiRelatedNoteMessage.value = results.isEmpty ? '暂无相关备忘录' : '';
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiRelatedNoteList.clear();
      aiRelatedNoteMessage.value = '相关备忘录加载失败，请稍后重试';
    } finally {
      if (_isCurrent(operation)) aiRelatedNoteLoading.value = false;
    }
  }

  Future<void> _startAiCategorizedNote({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      final results = await _aiGateway.categorizedNote(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiCategorizedNoteList.value = results;
      aiCategorizedNoteMessage.value = '';
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiCategorizedNoteList.clear();
      aiCategorizedNoteMessage.value = '信息归类失败，请重试；原文未修改';
    } finally {
      if (_isCurrent(operation)) aiCategorizedNoteLoading.value = false;
    }
  }

  Future<void> _startAiIllustration({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      final result = await _aiGateway.illustration(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiIllustration.value = result ?? '';
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiIllustration.value = '';
    } finally {
      if (_isCurrent(operation)) aiIllustrationLoading.value = false;
    }
  }

  Future<void> _startAiRelatedLink({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      final results = await _aiGateway.relatedLink(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiRelatedLinkList.value = results;
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiRelatedLinkList.clear();
    } finally {
      if (_isCurrent(operation)) aiRelatedLinkLoading.value = false;
    }
  }

  Future<void> retryCategories() async {
    final operation = _activeOperation;
    final recordId = _activeRecordId;
    if (operation == null ||
        recordId == null ||
        !_isCurrent(operation) ||
        aiCategorizedNoteLoading.value) return;
    aiCategorizedNoteLoading.value = true;
    aiCategorizedNoteMessage.value = '';
    await _startAiCategorizedNote(recordId: recordId, operation: operation);
  }

  Future<void> retryOrganize() async {
    final operation = _activeOperation;
    final recordId = _activeRecordId;
    if (operation == null ||
        recordId == null ||
        !_isCurrent(operation) ||
        aiOrganizeLoading.value) return;
    aiOrganizeLoading.value = true;
    aiOrganizeMessage.value = '';
    await _startAiOrganize(recordId: recordId, operation: operation);
  }

  Future<void> _startAiOrganize({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      final result = await _aiGateway.organizeNote(recordId: recordId);
      if (!_isCurrent(operation)) return;
      aiOrganizeResult.value = result;
      fromOrganizeResultToContentList();
      final hasOrganizedContent =
          (aiOrganizeResult.value.title ?? '').trim().isNotEmpty ||
              (aiOrganizeResult.value.bodyText ?? '').trim().isNotEmpty ||
              contentList.isNotEmpty;
      if (!hasOrganizedContent) {
        aiOrganizeMessage.value = '未生成可用的整理内容';
      }
    } catch (error) {
      if (!_isCurrent(operation)) return;
      aiOrganizeResult.value = AiOrganizeModel();
      contentList.clear();
      aiOrganizeMessage.value = error is DioException &&
              (error.type == DioExceptionType.receiveTimeout ||
                  error.type == DioExceptionType.connectionTimeout)
          ? '整理等待超时，原文未修改，请点击重试'
          : '智能整理失败，原文未修改，请点击重试';
    } finally {
      if (_isCurrent(operation)) aiOrganizeLoading.value = false;
    }
  }

  Future<void> _startAiSuggestion({
    required String recordId,
    required _OperationIdentity operation,
  }) async {
    try {
      await for (final data in _aiGateway.suggestion(recordId: recordId)) {
        if (!_isCurrent(operation)) return;
        aiSuggestionStr.value += data;
      }
      if (!_isCurrent(operation)) return;
      if (!parseAiSuggestion(aiSuggestionStr.value)) {
        aiSuggestionMessage.value = '暂无可用的智能建议';
      }
    } catch (_) {
      if (!_isCurrent(operation)) return;
      aiSuggestionMessage.value = '智能建议生成失败，请稍后重试';
      aiSuggestionList.clear();
      aiSuggestionStr.value = '';
    } finally {
      if (_isCurrent(operation)) aiSuggestionLoading.value = false;
    }
  }

  _OperationIdentity _captureOperation({String? recordId}) =>
      _OperationIdentity(
        token: _operationToken,
        note: note.value,
        noteId: _normalizedId(note.value.id),
        recordId: recordId,
      );

  bool _isCurrent(_OperationIdentity operation) =>
      !isClosed &&
      identical(operation.token, _operationToken) &&
      identical(operation.note, note.value) &&
      operation.noteId == _normalizedId(note.value.id) &&
      (operation.recordId == null || operation.recordId == _activeRecordId);

  static String? _normalizedId(String? value) {
    final normalized = value?.trim();
    return normalized == null || normalized.isEmpty ? null : normalized;
  }

  Future<bool> setAutoOrganize(bool enabled) async {
    if (autoOrganizeSaving.value) return false;
    autoOrganizeSaving.value = true;
    try {
      await _persistAutoOrganize(enabled);
      if (isClosed) return false;
      autoOrganize.value = enabled;
      return true;
    } finally {
      if (!isClosed) autoOrganizeSaving.value = false;
    }
  }

  void deleteAiSuggestion() {
    aiSuggestion.value = AiSuggestionModel();
    aiSuggestionList.value = aiSuggestion.value.toIconTextActionModelList();
  }

  void saveAllAiSuggestion() {
    for (final item in aiSuggestionList) {
      if (!contentList.any((existing) => existing.content == item.content)) {
        contentList.add(item);
      }
    }
    aiSuggestionList.clear();
  }

  void saveOneAiSuggestion(IconTextActionModel item) {
    if (contentList
        .where((element) => element.content == item.content)
        .isEmpty) {
      contentList.add(item);
      aiSuggestionList.remove(item);
      Toast.success("保留成功");
    } else {
      Toast.error("已存在");
    }
  }

  final moduleSavePending = false.obs;

  Future<bool> _saveModule(NoteModulePayload module) async {
    if (moduleSavePending.value || replacing.value) return false;
    final operation = _captureOperation();
    moduleSavePending.value = true;
    try {
      final saved = await NoteApi.addNoteModule(note.value, module);
      if (!_isCurrent(operation)) return false;
      note.value = saved;
      Toast.success('保存成功');
      return true;
    } catch (_) {
      if (_isCurrent(operation)) Toast.error('保存失败，请重试；原内容未被替换');
      return false;
    } finally {
      if (!isClosed) moduleSavePending.value = false;
    }
  }

  Future<bool> saveAiIllustration() async {
    if (aiIllustration.value.isEmpty) return false;
    return _saveModule(AIPictureModule(imageUrl: aiIllustration.value));
  }

  Future<bool> saveQuestionAnswer(QuestionAnswerModule module) async {
    if (module.questionAnswerItems?.isNotEmpty != true) return false;
    return _saveModule(module);
  }

  Future<bool> saveScene() async {
    final scene = aiOrganizeResult.value.userScenario ?? '';
    if (scene.isEmpty) return false;
    return _saveModule(SceneModule(title: scene, content: scene));
  }

  Future<bool> saveCategory() async {
    final items = aiCategorizedNoteList
        .expand((category) => category.items ?? [])
        .toList();
    if (items.isEmpty) {
      Toast.info('暂无可保存的归类信息');
      return false;
    }
    return _saveModule(KeyValuePairModule(
        sourceNoteId: note.value.id,
        keyValueItems: items
            .map((e) => KeyValuePairItem(key: e.key, value: e.value))
            .toList()));
  }
}

abstract class SmartOrganizeAiGateway {
  Future<AiValidateModel> validateNote(String? noteId);
  Future<AiOrganizeModel> organizeNote({String? recordId});
  Stream<String> suggestion({String? recordId});
  Future<List<AiRelatedLinkModel>> relatedLink({String? recordId});
  Future<List<AiRelatedTitleModel>> relatedTitle({String? recordId});
  Future<List<AiRelatedNoteModel>> relatedNote({required String recordId});
  Future<String?> illustration({String? recordId});
  Future<List<AiCategorizedNoteModel>> categorizedNote({String? recordId});
}

class DefaultSmartOrganizeAiGateway implements SmartOrganizeAiGateway {
  const DefaultSmartOrganizeAiGateway();

  @override
  Future<AiValidateModel> validateNote(String? noteId) =>
      AiApi.aiValidateNote(noteId);

  @override
  Future<AiOrganizeModel> organizeNote({String? recordId}) =>
      AiApi.aiOrganizeNote(recordId: recordId);

  @override
  Stream<String> suggestion({String? recordId}) =>
      AiApi.aiSuggestion(recordId: recordId);

  @override
  Future<List<AiRelatedLinkModel>> relatedLink({String? recordId}) =>
      AiApi.aiRelatedLink(recordId: recordId);

  @override
  Future<List<AiRelatedTitleModel>> relatedTitle({String? recordId}) =>
      AiApi.aiRelatedTitle(recordId: recordId);

  @override
  Future<List<AiRelatedNoteModel>> relatedNote({required String recordId}) =>
      AiApi.aiRelatedNote(recordId: recordId);

  @override
  Future<String?> illustration({String? recordId}) =>
      AiApi.aiIllustration(recordId: recordId);

  @override
  Future<List<AiCategorizedNoteModel>> categorizedNote({String? recordId}) =>
      AiApi.aiCategorizedNote(recordId: recordId);
}

class _OperationIdentity {
  const _OperationIdentity({
    required this.token,
    required this.note,
    required this.noteId,
    this.recordId,
  });

  final Object token;
  final NoteModel note;
  final String? noteId;
  final String? recordId;

  _OperationIdentity withRecordId(String value) => _OperationIdentity(
        token: token,
        note: note,
        noteId: noteId,
        recordId: value,
      );
}
