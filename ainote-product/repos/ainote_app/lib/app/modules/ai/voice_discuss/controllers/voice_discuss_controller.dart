import 'dart:async';

import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../models/voice_discussion_models.dart';
import '../services/voice_discussion_api.dart';
import '../services/voice_platform_bridge.dart';

class VoiceDiscussController extends GetxController {
  VoiceDiscussController({
    VoiceDiscussionGateway? gateway,
    VoicePlatformGateway? platform,
    NoteModel? note,
  })  : gateway = gateway ?? VoiceDiscussionApi(),
        platform = platform ?? MethodChannelVoicePlatformBridge(),
        _providedNote = note;

  final VoiceDiscussionGateway gateway;
  final VoicePlatformGateway platform;
  final NoteModel? _providedNote;

  final draftController = TextEditingController();
  final summaryController = TextEditingController();
  final messages = <VoiceDiscussionMessage>[].obs;
  final capabilities = Rxn<VoiceCapabilities>();
  final isListening = false.obs;
  final isSending = false.obs;
  final isSpeaking = false.obs;
  final isSummaryLoading = false.obs;
  final isSummarySaving = false.obs;
  final summarySaved = false.obs;
  final savedNote = Rxn<NoteModel>();
  final errorText = RxnString();

  NoteModel? note;
  String? _saveRequestId;
  String? _saveRequestSummary;
  int _requestGeneration = 0;
  int _voiceSessionGeneration = 0;
  int _ttsGeneration = 0;
  int _summaryGeneration = 0;
  StreamSubscription<VoiceRecognitionEvent>? _recognitionSubscription;
  StreamSubscription<bool>? _speakingSubscription;

  String? get noteId {
    final value = note?.id?.trim();
    return value == null || value.isEmpty ? null : value;
  }

  String get noteTitle {
    final value = note?.title?.trim();
    return value == null || value.isEmpty ? '当前笔记' : value;
  }

  bool get canSummarize =>
      messages.any((message) =>
          message.role == VoiceDiscussionRole.assistant &&
          message.delivery == VoiceMessageDelivery.sent) &&
      !isSending.value;

  @override
  void onInit() {
    super.onInit();
    note = _providedNote ?? _noteFromArguments(Get.arguments);
    _recognitionSubscription =
        platform.recognitionEvents.listen(_handleRecognitionEvent);
    _speakingSubscription = platform.speakingEvents.listen((speaking) {
      isSpeaking.value = speaking;
    });
    unawaited(refreshCapabilities());
  }

  NoteModel? _noteFromArguments(dynamic arguments) {
    if (arguments is NoteModel) return arguments;
    if (arguments is Map && arguments['note'] is NoteModel) {
      return arguments['note'] as NoteModel;
    }
    return null;
  }

  Future<void> refreshCapabilities() async {
    final generation = _voiceSessionGeneration;
    final current = await platform.capabilities();
    if (_isCurrentVoiceSession(generation)) {
      capabilities.value = current;
    }
  }

  Future<void> startListening() async {
    final generation = ++_voiceSessionGeneration;
    errorText.value = null;
    try {
      await stopSpeaking();
      if (!_isCurrentVoiceSession(generation)) return;
      var current = await platform.capabilities();
      if (!_isCurrentVoiceSession(generation)) return;
      capabilities.value = current;
      if (current.speechAvailable && !current.microphonePermission) {
        final granted = await platform.requestMicrophonePermission();
        if (!_isCurrentVoiceSession(generation)) return;
        current = await platform.capabilities();
        if (!_isCurrentVoiceSession(generation)) return;
        capabilities.value = current;
        if (!granted) {
          errorText.value = '未获得麦克风权限，识别文字仍可手动输入';
          return;
        }
      }
      if (!current.canListen) {
        errorText.value = current.reason ??
            (current.microphonePermission
                ? '系统没有可用的语音识别服务，可直接输入文字'
                : '请在系统设置中允许麦克风权限，或直接输入文字');
        return;
      }
      await platform.startListening();
      if (!_isCurrentVoiceSession(generation)) return;
      isListening.value = true;
    } catch (error) {
      if (!_isCurrentVoiceSession(generation)) return;
      isListening.value = false;
      errorText.value = _messageFor(error, fallback: '无法启动语音识别或请求麦克风权限');
    }
  }

  Future<void> stopListening() async {
    final generation = ++_voiceSessionGeneration;
    try {
      await platform.stopListening();
    } finally {
      if (_isCurrentVoiceSession(generation)) {
        isListening.value = false;
      }
    }
  }

  Future<void> cancelListening() async {
    final generation = ++_voiceSessionGeneration;
    try {
      await platform.cancelListening();
    } finally {
      if (_isCurrentVoiceSession(generation)) {
        isListening.value = false;
      }
    }
  }

  bool _isCurrentVoiceSession(int generation) =>
      !isClosed && generation == _voiceSessionGeneration;

  void _handleRecognitionEvent(VoiceRecognitionEvent event) {
    if (event.isError) {
      isListening.value = false;
      if (event.errorCode == 'cancelled') return;
      errorText.value = _speechErrorMessage(event.errorCode!);
      return;
    }
    if (event.text.isNotEmpty) {
      draftController.value = TextEditingValue(
        text: event.text,
        selection: TextSelection.collapsed(offset: event.text.length),
      );
    }
    if (event.isFinal) {
      isListening.value = false;
    }
  }

  String _speechErrorMessage(String code) {
    switch (code) {
      case 'microphone_permission_required':
        return '请在系统设置中允许麦克风权限，识别文字仍可手动输入';
      case 'no_match':
        return '没有识别到清晰内容，可重试或直接编辑文字';
      case 'recognizer_busy':
        return '语音识别器正忙，请稍后重试';
      case 'speech_service_unavailable':
        return '系统语音识别服务不可用，可直接输入文字';
      default:
        return '语音识别失败，可重试或直接输入文字';
    }
  }

  Future<void> sendDraft() async {
    final text = draftController.text.trim();
    if (text.isEmpty) {
      errorText.value = '请先说话或输入要讨论的内容';
      return;
    }
    draftController.clear();
    await _send(text);
  }

  Future<void> _send(String text, {int? retryIndex}) async {
    final id = noteId;
    if (id == null) {
      errorText.value = '请先保存当前笔记，再开始讨论';
      return;
    }
    if (isSending.value) return;

    errorText.value = null;
    final int userIndex;
    final List<VoiceDiscussionMessage> history;
    if (retryIndex == null) {
      history = _successfulHistory(messages);
      messages.add(VoiceDiscussionMessage(
        role: VoiceDiscussionRole.user,
        content: text,
        delivery: VoiceMessageDelivery.pending,
      ));
      userIndex = messages.length - 1;
    } else {
      if (retryIndex < 0 || retryIndex >= messages.length) return;
      history = _successfulHistory(messages.take(retryIndex));
      messages[retryIndex] =
          messages[retryIndex].copyWith(delivery: VoiceMessageDelivery.pending);
      userIndex = retryIndex;
    }

    isSending.value = true;
    final generation = ++_requestGeneration;
    try {
      final reply = await gateway.respond(
        noteId: id,
        message: text,
        history: history,
      );
      if (generation != _requestGeneration || isClosed) return;
      messages[userIndex] =
          messages[userIndex].copyWith(delivery: VoiceMessageDelivery.sent);
      messages.add(VoiceDiscussionMessage(
        role: VoiceDiscussionRole.assistant,
        content: reply,
      ));
    } on VoiceDiscussionCancelledException catch (error) {
      if (generation != _requestGeneration || isClosed) return;
      messages[userIndex] = messages[userIndex]
          .copyWith(delivery: VoiceMessageDelivery.cancelled);
      errorText.value = error.message;
    } catch (error) {
      if (generation != _requestGeneration || isClosed) return;
      messages[userIndex] =
          messages[userIndex].copyWith(delivery: VoiceMessageDelivery.failed);
      errorText.value = _messageFor(error, fallback: '回复失败，请重试');
    } finally {
      if (generation == _requestGeneration && !isClosed) {
        isSending.value = false;
      }
    }
  }

  Future<void> retryMessage(int index) async {
    if (index < 0 || index >= messages.length || !messages[index].canRetry) {
      return;
    }
    await _send(messages[index].content, retryIndex: index);
  }

  void cancelResponse() {
    gateway.cancelActive();
  }

  List<VoiceDiscussionMessage> _successfulHistory(
          Iterable<VoiceDiscussionMessage> source) =>
      source
          .where((message) => message.delivery == VoiceMessageDelivery.sent)
          .toList(growable: false);

  Future<void> speakMessage(String text) async {
    final generation = ++_ttsGeneration;
    try {
      final current = await platform.capabilities();
      if (!_isCurrentTts(generation)) return;
      capabilities.value = current;
      if (!current.ttsAvailable) {
        errorText.value = current.reason ?? '系统文字朗读服务不可用';
        return;
      }
      await platform.speak(text);
      if (!_isCurrentTts(generation)) return;
      isSpeaking.value = true;
    } catch (error) {
      if (!_isCurrentTts(generation)) return;
      isSpeaking.value = false;
      errorText.value = _messageFor(error, fallback: '无法朗读本条回复');
    }
  }

  Future<void> stopSpeaking() async {
    final generation = ++_ttsGeneration;
    try {
      await platform.stopSpeaking();
    } catch (_) {
      // Stopping is best-effort; the user must still be able to type/listen.
    } finally {
      if (_isCurrentTts(generation)) isSpeaking.value = false;
    }
  }

  bool _isCurrentTts(int generation) =>
      !isClosed && generation == _ttsGeneration;

  Future<bool> generateSummaryPreview() async {
    final id = noteId;
    final history = _successfulHistory(messages);
    if (id == null || history.isEmpty || isSummaryLoading.value) return false;
    errorText.value = null;
    isSummaryLoading.value = true;
    final generation = ++_summaryGeneration;
    summarySaved.value = false;
    _saveRequestId = null;
    _saveRequestSummary = null;
    try {
      final summary = await gateway.previewSummary(
        noteId: id,
        history: history,
      );
      if (!_isCurrentSummary(generation)) return false;
      summaryController.text = summary;
      return true;
    } catch (error) {
      if (!_isCurrentSummary(generation)) return false;
      errorText.value = _messageFor(error, fallback: '总结生成失败，请重试');
      return false;
    } finally {
      if (_isCurrentSummary(generation)) isSummaryLoading.value = false;
    }
  }

  Future<bool> saveSummary() async {
    final id = noteId;
    final summary = summaryController.text.trim();
    if (id == null || summary.isEmpty || isSummarySaving.value) return false;
    errorText.value = null;
    if (_saveRequestId != null && _saveRequestSummary != summary) {
      errorText.value = '上次保存结果尚未确认。请恢复为上次提交的总结并重试，确认后再修改';
      return false;
    }
    isSummarySaving.value = true;
    final generation = ++_summaryGeneration;
    if (_saveRequestId == null) {
      _saveRequestId =
          '${DateTime.now().microsecondsSinceEpoch}_${id.hashCode.abs()}';
      _saveRequestSummary = summary;
    }
    try {
      final result = await gateway.saveSummary(
        noteId: id,
        summary: summary,
        saveRequestId: _saveRequestId!,
      );
      if (!_isCurrentSummary(generation)) return false;
      savedNote.value = result.note;
      summarySaved.value = true;
      return true;
    } catch (error) {
      if (!_isCurrentSummary(generation)) return false;
      errorText.value = _messageFor(error, fallback: '总结保存失败，请重试');
      return false;
    } finally {
      if (_isCurrentSummary(generation)) isSummarySaving.value = false;
    }
  }

  bool _isCurrentSummary(int generation) =>
      !isClosed && generation == _summaryGeneration;

  String _messageFor(Object error, {required String fallback}) {
    if (error is VoiceDiscussionApiException) return error.message;
    if (error is VoicePlatformException) return error.message;
    return fallback;
  }

  @override
  void onClose() {
    _requestGeneration++;
    _voiceSessionGeneration++;
    _ttsGeneration++;
    _summaryGeneration++;
    gateway.cancelActive();
    unawaited(_recognitionSubscription?.cancel());
    unawaited(_speakingSubscription?.cancel());
    platform.dispose();
    draftController.dispose();
    summaryController.dispose();
    super.onClose();
  }
}
