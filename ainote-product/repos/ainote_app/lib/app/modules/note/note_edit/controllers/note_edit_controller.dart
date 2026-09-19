import 'dart:async';
import 'dart:convert';

import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/data/models/ai_analysis_record_model.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/note_module/note_module_payload.dart';
import 'package:ainote_app/app/data/models/note_module/text_data_module.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:ainote_app/app/data/models/note_module/scene_module.dart';
import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/ai_assist_content.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_category.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_illustration.dart';
import 'package:ainote_app/app/widgets/note_card/note_source_image.dart';
import 'package:ainote_app/app/widgets/note_card/note_merge_sources.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_qa.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_situation.dart';
import 'package:ainote_app/app/widgets/quill/embeds/todo_embed.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';
import 'package:ainote_app/app/widgets/todo_list.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../../../../api/note.dart';
import '../../../../data/models/note_model.dart';
import '../../../../utils/alarm.dart';
import '../../../../utils/date_format.dart';
import '../../../../utils/quill_editor.dart';

// 笔记创建/编辑/预览
enum NoteEditMode {
  edit,
  create,
  preview,
}

typedef AnalysisRecordLatestLoader = Future<AiAnalysisRecordModel?> Function(
  String noteId,
);
typedef PersistQuickCompletion = Future<bool> Function();
typedef StartQuickCompletionAnalysis = Future<bool> Function();

Future<bool> persistQuickCompletionThenStart({
  required PersistQuickCompletion persist,
  required StartQuickCompletionAnalysis startAnalysis,
}) async {
  if (!await persist()) return false;
  return startAnalysis();
}

class NoteEditController extends GetxController {
  NoteEditController({
    AnalysisRecordLatestLoader? analysisRecordLatestLoader,
    SmartOrganizeController? smartOrganizeController,
  })  : _analysisRecordLatestLoader = analysisRecordLatestLoader ??
            ((noteId) => AiApi.noteAnalysisRecordLatest(noteId)),
        smartOrganizeLogic =
            smartOrganizeController ?? Get.put(SmartOrganizeController());

  final AnalysisRecordLatestLoader _analysisRecordLatestLoader;
  int _latestRequestGeneration = 0;
  int _liveOperationGeneration = 0;
  int _nextLiveOperationToken = 0;
  int? _activeLiveOperationToken;
  String? _activeLiveNoteId;
  String? _activeLiveContent;
  String? _lastEditorContent;
  bool _applyingSavedContent = false;
  int _noteNavigationGeneration = 0;
  Future<bool>? _saveInFlight;
  bool _leaving = false;

  final titleController = TextEditingController();

  final quillTag = 'note-edit';
  late MyQuillController quillLogic;

  final noteOnlyView = false.obs;
  final noteHasSaved = false.obs;

  final aiAnalysisVisible = false.obs;
  final aiAnalysisDone = false.obs;
  final aiAnalysisTime =
      formatDate(DateTime.now(), DateFormatStr.noYearNoSecond).obs;

  final note = NoteModel.empty().obs;
  final isFromTheme = false.obs;
  final isThemeLoading = false.obs;

  final editorToolVisible = false.obs;
  final bottomToolVisible = true.obs;
  final showQuickComplete = false.obs;

  final toolBarItems = [].obs;
  final todoList = <ToDoItem>[].obs;

  // 智能整理
  final analysisRecord = AiAnalysisRecordModel.mock().obs;
  final SmartOrganizeController smartOrganizeLogic;

  // ai 智能整理保存的module
  final aiModuleVisible = false.obs;

  final aiCardList = <Widget>[].obs;

  @override
  void onInit() {
    super.onInit();
    var fromTheme = Get.parameters['isFromTheme'] ?? 'false';
    isFromTheme.value = fromTheme == 'true';
    var themeLoadingParam = Get.parameters['isThemeLoading'] ?? 'false';
    isThemeLoading.value = themeLoadingParam == 'true';

    quillLogic = Get.put(MyQuillController(), tag: quillTag);

    toolBarItems.value = [
      {
        'id': 'assist',
        'label': 'AI辅写',
        'type': 'custom', // 自定义
        'icon': Assets.editToolBarAssist,
        'activeIcon': Assets.editToolBarAssistActive,
        'disabled': false,
        'onTap': () {
          showCupertinoModalBottomSheet(
              context: Get.context!,
              enableDrag: false,
              useRootNavigator: true,
              builder: (context) {
                return AiAssistContent(
                    selectedContentText: quillLogic.selectedText.value);
              });
        }
      },
      {
        'id': 'image',
        'label': '图片',
        'type': 'editor', // 编辑器插件
        'icon': Assets.editToolBarImg,
        'activeIcon': Assets.editToolBarImgActive,
        'disabled': false,
        'onTap': () {}
      },
      {
        'id': 'text',
        'label': '文字',
        'type': 'editor', // 编辑器插件
        'icon': Assets.editToolBarText,
        'activeIcon': Assets.editToolBarText,
        'disabled': false,
        'onTap': () {
          setEditorToolVisible(!editorToolVisible.value);
        }
      },
      {
        'id': 'todo',
        'label': '待办',
        'type': 'editor', // 编辑器插件
        'icon': Assets.editToolBarTodo,
        'activeIcon': Assets.editToolBarTodoActive,
        'disabled': false,
        'onTap': () {
          showTodoModal(
            Get.context,
            onConfirm: (todo) {
              final todoJsonString = jsonEncode(todo);
              insertQuillCustomEmbedNode(
                  quillLogic.quillController, TodoEmbed(todoJsonString));
            },
          );
        }
      },
    ];

    final mode = Get.parameters['mode'];
    switch (mode) {
      case 'edit':
        noteOnlyView.value = false;
        break;
      case 'create':
        noteOnlyView.value = false;
        aiAnalysisDone.value = false;
        aiAnalysisVisible.value = false;
        break;
      case 'preview':
        noteOnlyView.value = true;
        Future.delayed(Duration(milliseconds: 10), () {
          setQuillIsReadOnly(true);
          setNoteHasSaved(true);
        });
        break;
    }

    quillLogic.quillController.addListener(handleQuillChange);

    final NoteModel? initialNote = Get.arguments;
    if (initialNote is NoteModel) {
      setNote(initialNote);
      getAiAnalysisRecordLatest();
    }
  }

  @override
  void onClose() {
    _noteNavigationGeneration++;
    _invalidateAnalysisLifecycle();
    quillLogic.quillController.removeListener(handleQuillChange);

    titleController.dispose();
    Alarm().dispose();
    analysisRecord.close();
    smartOrganizeLogic.dispose();
    super.onClose();
  }

  void updateNoteType(NoteTypeModel? value) {
    note.value.noteType = value?.id;
    NoteApi.updateNote(note.value);
  }

  void setEditorToolVisible(bool visible) {
    editorToolVisible.value = visible;
    if (visible) {
      setQuillIsReadOnly(false);
    }
  }

  void setBottomToolVisible(bool visible) {
    bottomToolVisible.value = visible;
  }

  void setNote(NoteModel data) {
    _noteNavigationGeneration++;
    _invalidateAnalysisLifecycle();
    note.value = data;
    _applyingSavedContent = true;
    try {
      setTextDataToQuill(note.value, quillLogic, null);
      _lastEditorContent = getQuillJsonString(quillLogic.quillController);
    } finally {
      _applyingSavedContent = false;
    }
    titleController.text = note.value.title!;

    aiCardList.value = generateAiCardList();
    aiModuleVisible.value = (data.noteThemeIds?.isNotEmpty ?? false) ||
        (data.imageUrl?.trim().isNotEmpty ?? false) ||
        (data.modules?.any((element) {
              return element?['noteModuleType'] !=
                  NoteModuleType.TEXT_DATA.toString().split('.').last;
            }) ??
            false);
    smartOrganizeLogic.setNote(note.value);
    setNoteHasSaved(_normalizedId(data.id) != null);
    update();
  }

  /// Refresh persisted modules without discarding the active AI preview.
  /// Used only for a save to this same note, never for navigation.
  void applySavedNote(NoteModel data, {bool replaceText = false}) {
    if (data.id != note.value.id) return;
    final preserveDraft = !replaceText && !noteHasSaved.value;
    if (preserveDraft) {
      data.title = titleController.text;
      data.content = getQuillJsonString(quillLogic.quillController);
      for (final module in data.modules ?? []) {
        if (module is Map && module['noteModuleType'] == 'TEXT_DATA') {
          module['content'] = data.content;
          module['title'] = data.title;
        }
      }
    }
    note.value = data;
    if (!preserveDraft) {
      _applyingSavedContent = true;
      try {
        setTextDataToQuill(data, quillLogic, null);
        _lastEditorContent = getQuillJsonString(quillLogic.quillController);
      } finally {
        _applyingSavedContent = false;
      }
      titleController.text = data.title ?? '';
    }
    aiCardList.value = generateAiCardList();
    aiModuleVisible.value = (data.noteThemeIds?.isNotEmpty ?? false) ||
        (data.imageUrl?.trim().isNotEmpty ?? false) ||
        (data.modules
                ?.any((element) => element?['noteModuleType'] != 'TEXT_DATA') ??
            false);
    setNoteHasSaved(!preserveDraft);
    update();
  }

  void setAiAnalysisVisible(bool visible) {
    aiAnalysisVisible.value = visible;
  }

  void setNoteHasSaved(bool hasSaved) {
    noteHasSaved.value = hasSaved;
  }

  void setNoteOnlyView(bool onlyView) {
    noteOnlyView.value = onlyView;
    setQuillIsReadOnly(onlyView);
  }

  void setQuillIsReadOnly(bool readOnly) {
    quillLogic.setQuillIsReadOnly(readOnly);
    update();
  }

  void toggleQuillIsReadOnly() {
    setQuillIsReadOnly(!quillLogic.quillIsReadOnly.value);
  }

  void deleteNote() {
    NoteApi.deleteNote(note.value.id!);
    Get.back();
  }

  Future<bool> prepareToLeave() async {
    if (_leaving || isClosed) return false;
    _leaving = true;
    try {
      if (_normalizedId(note.value.id) == null &&
          quillLogic.quillController.document.toPlainText().trim().isEmpty)
        return true;
      if (!await saveNote(true) || isClosed) {
        if (!isClosed) Toast.error('保存未完成，请重试；当前内容仍在编辑页');
        return false;
      }
      return true;
    } finally {
      _leaving = false;
    }
  }

  Future<bool> saveNote([bool silent = false]) {
    if (isClosed) return Future.value(false);
    if (_saveInFlight != null) return _saveInFlight!;
    final future = _persistNote(silent);
    _saveInFlight = future;
    return future.whenComplete(() {
      if (identical(_saveInFlight, future)) _saveInFlight = null;
    });
  }

  Future<bool> _persistNote(bool silent) async {
    setQuillIsReadOnly(true);
    note.value.content = getQuillJsonString(quillLogic.quillController);
    note.value.title = getNoteTitle(quillLogic.quillController);
    if (noteHasSaved.value == true) return true;
    if (quillLogic.quillController.document.toPlainText().trim().isEmpty) {
      setQuillIsReadOnly(false);
      if (!silent) {
        Toast.info("请输入内容");
      }
      return false;
    }
    final generation = _noteNavigationGeneration;
    final originalId = note.value.id;
    final savedContent = note.value.content;
    var saved = NoteModel.fromJson(jsonDecode(jsonEncode(note.value.toJson())));
    try {
      if (note.value.id != null && note.value.id!.isNotEmpty) {
        final raw = (saved.modules ?? [])
            .whereType<Map>()
            .where((module) => module['noteModuleType'] == 'TEXT_DATA')
            .firstOrNull;
        if (raw == null) {
          throw StateError('Text data module is unavailable');
        }
        final module = TextDataModule.fromJson(Map<String, dynamic>.from(raw))
          ..content = saved.content
          ..title = saved.title;
        saved = await NoteApi.updateNoteModule(saved, module);
      } else {
        saved = await NoteApi.createNote(saved);
      }
      if (_normalizedId(saved.id) == null ||
          (_normalizedId(originalId) != null && saved.id != originalId)) {
        throw StateError('Saved note identity is invalid');
      }
    } catch (_) {
      if (!isClosed && generation == _noteNavigationGeneration)
        setQuillIsReadOnly(false);
      if (!silent && !isClosed && generation == _noteNavigationGeneration) {
        Toast.error("保存失败, 请稍后重试");
      }
      return false;
    }
    if (isClosed ||
        generation != _noteNavigationGeneration ||
        originalId != note.value.id) return false;
    if (savedContent != getQuillJsonString(quillLogic.quillController)) {
      // Preserve a newly created ID without overwriting a newer draft.
      if (_normalizedId(originalId) == null) {
        note.value.id = saved.id;
        // Bring back the new text-module identity while retaining the newer
        // editor draft, so the next save updates instead of creating again.
        applySavedNote(saved);
      }
      setNoteHasSaved(false);
      setQuillIsReadOnly(false);
      return false;
    }
    note.value = saved;
    setNoteHasSaved(true);
    aiAnalysisDone.value = false;
    if (smartOrganizeLogic.autoOrganize.value && !_leaving) {
      unawaited(startSmartOrganize());
    }
    if (!silent) {
      Alarm().playVibrator();
      Toast.success("保存成功");
      showQuickComplete.value = true;
    }
    return true;
  }

  Future<bool> saveQuickCompletionAndStartSmartOrganize() {
    return persistQuickCompletionThenStart(
      persist: () => saveNote(true),
      startAnalysis: () async {
        if (_normalizedId(note.value.id) == null) return false;
        unawaited(startSmartOrganize());
        return true;
      },
    );
  }

  // 获取最新的 AI 智能整理结果
  Future<void> getAiAnalysisRecordLatest() async {
    final noteId = _normalizedId(note.value.id);
    if (noteId == null || _activeLiveNoteId == noteId) return;

    final requestGeneration = ++_latestRequestGeneration;
    final liveGeneration = _liveOperationGeneration;
    AiAnalysisRecordModel? res;
    try {
      res = await _analysisRecordLatestLoader(noteId);
    } catch (_) {
      if (!_canApplyLatest(
        noteId: noteId,
        requestGeneration: requestGeneration,
        liveGeneration: liveGeneration,
      )) {
        return;
      }

      // A failed cache lookup must not escape as an unhandled async error.
      // Treat it like a cache miss and let the guarded live-analysis path
      // settle its own success or retry state for the still-active note.
      aiAnalysisVisible.value = true;
      aiAnalysisDone.value = false;
      await startSmartOrganize();
      return;
    }
    if (!_canApplyLatest(
      noteId: noteId,
      requestGeneration: requestGeneration,
      liveGeneration: liveGeneration,
    )) {
      return;
    }

    final recordNoteId = _normalizedId(res?.noteId);
    if (res == null ||
        isMockData(res.id) ||
        recordNoteId == null ||
        recordNoteId != noteId) {
      aiAnalysisVisible.value = true;
      aiAnalysisDone.value = false;
      await startSmartOrganize();
      return;
    }

    analysisRecord.value = res;
    // Loading read-only AI metadata must never mark an active text draft saved.
    aiAnalysisDone.value = true;
    aiAnalysisVisible.value = true;
    if (res.updatedAt != null) {
      aiAnalysisTime.value =
          parseApiDateString(res.updatedAt!, DateFormatStr.noYearNoSecond);
    }

    smartOrganizeLogic.setAnalysisRecord(res);
  }

  // 开始智能整理
  Future<void> startSmartOrganize() async {
    final noteId = _normalizedId(note.value.id);
    if (noteId == null) return;
    if (_activeLiveNoteId == noteId && _activeLiveContent == note.value.content)
      return;

    _latestRequestGeneration++;
    final liveGeneration = ++_liveOperationGeneration;
    final liveToken = ++_nextLiveOperationToken;
    _activeLiveOperationToken = liveToken;
    _activeLiveNoteId = noteId;
    _activeLiveContent = note.value.content;
    aiAnalysisDone.value = false;
    setAiAnalysisVisible(true);
    smartOrganizeLogic.setNote(note.value);

    try {
      await smartOrganizeLogic.smartOrganize();
    } catch (e) {
      if (!_canApplyLive(noteId, liveGeneration)) return;
      Toast.error("智能整理失败, 请重试");

      aiAnalysisDone.value = false;
      return;
    } finally {
      if (_activeLiveOperationToken == liveToken) {
        _activeLiveOperationToken = null;
        _activeLiveNoteId = null;
        _activeLiveContent = null;
      }
    }

    if (!_canApplyLive(noteId, liveGeneration)) return;
    aiAnalysisDone.value =
        smartOrganizeLogic.aiValidateResult.value.meaningful == true &&
            smartOrganizeLogic.aiOrganizeMessage.value.isEmpty;
    aiAnalysisTime.value =
        formatDate(DateTime.now(), DateFormatStr.noYearNoSecond);
  }

  bool _canApplyLatest({
    required String noteId,
    required int requestGeneration,
    required int liveGeneration,
  }) =>
      _normalizedId(note.value.id) == noteId &&
      _latestRequestGeneration == requestGeneration &&
      _liveOperationGeneration == liveGeneration &&
      _activeLiveNoteId != noteId;

  bool _canApplyLive(String noteId, int liveGeneration) =>
      _normalizedId(note.value.id) == noteId &&
      _liveOperationGeneration == liveGeneration;

  void _invalidateAnalysisLifecycle() {
    _latestRequestGeneration++;
    _liveOperationGeneration++;
    _activeLiveOperationToken = null;
    _activeLiveNoteId = null;
    _activeLiveContent = null;
  }

  String? _normalizedId(String? value) {
    final normalized = value?.trim();
    return normalized == null || normalized.isEmpty ? null : normalized;
  }

  Future<void> setAutoOrganize(bool enabled) async {
    try {
      if (!await smartOrganizeLogic.setAutoOrganize(enabled)) return;
      if (enabled) {
        Toast.info('已开启：保存后自动生成整理结果，原文不会自动替换');
        await startAiAutoAnalysis(force: true);
      }
    } catch (_) {
      Toast.error('自动整理设置保存失败，请重试');
    }
  }

  // Use the same observable analysis path for automatic and manual runs.
  // A background API acknowledgement alone never means AI completed.
  Future<void> startAiAutoAnalysis({bool force = false}) async {
    if (!smartOrganizeLogic.autoOrganize.value) return;
    if (!noteHasSaved.value || _normalizedId(note.value.id) == null) {
      await saveNote(true); // Successful saves trigger the enabled analysis.
      return;
    }
    if (_activeLiveNoteId != null || (!force && aiAnalysisDone.value)) return;
    await startSmartOrganize();
  }

  void retryAnalysis() {
    aiAnalysisDone.value = false;
    startSmartOrganize();
  }

  void handleQuillChange() {
    if (_applyingSavedContent) return;
    final content = getQuillJsonString(quillLogic.quillController);
    // Cursor/selection/focus changes are not edits and must not invalidate AI.
    if (content == _lastEditorContent) return;
    _lastEditorContent = content;
    invalidateAnalysisForDraft();
    quillLogic.setQuillIsReadOnly(false);
    quillLogic.quillFocusNode.requestFocus();
  }

  void invalidateAnalysisForDraft() {
    _invalidateAnalysisLifecycle();
    smartOrganizeLogic.reset();
    aiAnalysisDone.value = false;
    aiAnalysisVisible.value = false;
    noteHasSaved.value = false;
  }

  void deleteModule(String? moduleId) async {
    await NoteApi.deleteNoteModule(note.value.id, moduleId);
    note.value.modules?.removeWhere((module) => module['moduleId'] == moduleId);
    aiCardList.value = generateAiCardList();
    smartOrganizeLogic.setNote(note.value);
  }

  List<Widget> generateAiCardList() {
    var modules = note.value.modules ?? [];
    List<Widget> list = [];
    final sourceImage = note.value.imageUrl?.trim();
    final sourceImages = <String>{};
    if (sourceImage != null && sourceImage.isNotEmpty) {
      list.add(NoteSourceImage(url: sourceImage));
      sourceImages.add(sourceImage);
    }
    if (note.value.noteThemeIds?.isNotEmpty == true &&
        note.value.id?.isNotEmpty == true) {
      list.add(NoteMergeSources(
          key: ValueKey('merge-sources-${note.value.id}'),
          noteId: note.value.id!));
    }
    for (int i = modules.length - 1; i >= 0; i--) {
      var module = modules[i];
      if (module['noteModuleType'] ==
          NoteModuleType.AI_PICTURE.toString().split('.').last) {
        if (module['title'] == '来源备忘录图片') {
          final url = (module['imageUrl'] as String?)?.trim();
          if (url != null && url.isNotEmpty && sourceImages.add(url))
            list.add(NoteSourceImage(url: url, label: '来源图片'));
          continue;
        }
        // AI配图
        list.add(NoteCardIllustration(
            img: module['imageUrl'],
            onDelete: () => deleteModule(module['moduleId'])));
      }
      if (module['noteModuleType'] ==
          NoteModuleType.QUESTION_ANSWER.toString().split('.').last) {
        // AI问答
        list.add(NoteCardQa(
            data: QuestionAnswerModule.fromJson(module),
            onDelete: () => deleteModule(module['moduleId'])));
      }
      if (module['noteModuleType'] ==
          NoteModuleType.SCENARIO_RECORD.toString().split('.').last) {
        // 情景记录
        list.add(NoteCardSituation(
            data: SceneModule.fromJson(module),
            onDelete: () => deleteModule(module['moduleId'])));
      }
      if (module['noteModuleType'] ==
          NoteModuleType.KEY_VALUE_PAIR.toString().split('.').last) {
        // 信息归类
        list.add(NoteCardCategory(
            data: AiCategorizedNoteModel.fromJson(module),
            onDelete: () => deleteModule(module['moduleId'])));
      }
    }
    return list;
  }
}
