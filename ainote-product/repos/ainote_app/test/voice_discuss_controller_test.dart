import 'dart:async';

import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/controllers/voice_discuss_controller.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/models/voice_discussion_models.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/services/voice_discussion_api.dart';
import 'package:ainote_app/app/modules/ai/voice_discuss/services/voice_platform_bridge.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    Get.testMode = true;
  });

  tearDown(Get.reset);

  test('recognized text stays editable and successful turns form next history',
      () async {
    final gateway = _FakeGateway();
    final platform = _FakePlatform();
    final controller = VoiceDiscussController(
      gateway: gateway,
      platform: platform,
      note: NoteModel(id: 'note-1', title: '产品想法'),
    );
    controller.onInit();

    platform.emitRecognition('原始识别', isFinal: true);
    await Future<void>.delayed(Duration.zero);
    controller.draftController.text = '编辑后的问题';
    await controller.sendDraft();
    controller.draftController.text = '第二个问题';
    await controller.sendDraft();

    expect(gateway.messages, ['编辑后的问题', '第二个问题']);
    expect(gateway.histories.first, isEmpty);
    expect(
        gateway.histories.last.map((item) => item.content), ['编辑后的问题', '回复 1']);
    expect(controller.messages.length, 4);
    expect(controller.messages.last.content, '回复 2');

    controller.onClose();
  });

  test('failed send remains visible and can be retried', () async {
    final gateway = _FakeGateway()..failNext = true;
    final controller = VoiceDiscussController(
      gateway: gateway,
      platform: _FakePlatform(),
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();
    controller.draftController.text = '请重试';

    await controller.sendDraft();
    expect(controller.messages.single.delivery, VoiceMessageDelivery.failed);
    await controller.retryMessage(0);

    expect(controller.messages.first.delivery, VoiceMessageDelivery.sent);
    expect(controller.messages.last.role, VoiceDiscussionRole.assistant);
    expect(gateway.messages, ['请重试', '请重试']);

    controller.onClose();
  });

  test('summary is previewed before explicit save and returns refreshed note',
      () async {
    final gateway = _FakeGateway();
    final controller = VoiceDiscussController(
      gateway: gateway,
      platform: _FakePlatform(),
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();
    controller.messages.assignAll(const [
      VoiceDiscussionMessage(role: VoiceDiscussionRole.user, content: '讨论内容'),
      VoiceDiscussionMessage(
          role: VoiceDiscussionRole.assistant, content: '讨论回复'),
    ]);

    expect(await controller.generateSummaryPreview(), isTrue);
    expect(controller.summarySaved.value, isFalse);
    expect(gateway.saveCalls, 0);
    controller.summaryController.text = '用户编辑后的总结';
    expect(await controller.saveSummary(), isTrue);

    expect(gateway.savedSummary, '用户编辑后的总结');
    expect(gateway.saveCalls, 1);
    expect(controller.summarySaved.value, isTrue);
    expect(controller.savedNote.value?.id, 'note-1');

    controller.onClose();
  });

  test('unavailable native speech reports error while text send remains usable',
      () async {
    final platform = _FakePlatform(
      capabilities: VoiceCapabilities.unavailable('没有语音服务'),
    );
    final gateway = _FakeGateway();
    final controller = VoiceDiscussController(
      gateway: gateway,
      platform: platform,
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();

    await controller.startListening();
    expect(controller.errorText.value, '没有语音服务');
    controller.draftController.text = '文字仍能发送';
    await controller.sendDraft();
    expect(gateway.messages, ['文字仍能发送']);

    controller.onClose();
  });

  test('cancelled native recognition does not surface as a failure', () async {
    final platform = _FakePlatform();
    final controller = VoiceDiscussController(
      gateway: _FakeGateway(),
      platform: platform,
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();

    await controller.startListening();
    platform.emitError('cancelled');
    await Future<void>.delayed(Duration.zero);

    expect(controller.isListening.value, isFalse);
    expect(controller.errorText.value, isNull);
    controller.onClose();
  });

  test('microphone permission is requested before listening', () async {
    final platform = _FakePlatform(
      capabilities: const VoiceCapabilities(
        speechAvailable: true,
        microphonePermission: false,
        ttsAvailable: true,
      ),
    )..permissionGrantResult = true;
    final controller = VoiceDiscussController(
      gateway: _FakeGateway(),
      platform: platform,
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();

    await controller.startListening();

    expect(platform.permissionRequests, 1);
    expect(controller.isListening.value, isTrue);
    controller.onClose();
  });

  test('late capabilities completion after close never starts listening',
      () async {
    final platform = _FakePlatform()..holdCapabilities = true;
    final controller = VoiceDiscussController(
      gateway: _FakeGateway(),
      platform: platform,
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();

    final start = controller.startListening();
    await Future<void>.delayed(Duration.zero);
    controller.onClose();
    platform.completeCapabilities();
    await start;

    expect(platform.startListeningCalls, 0);
  });

  test('late TTS start completion cannot override stop', () async {
    final platform = _FakePlatform()..holdSpeak = true;
    final controller = VoiceDiscussController(
      gateway: _FakeGateway(),
      platform: platform,
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();

    final speaking = controller.speakMessage('朗读内容');
    await Future<void>.delayed(Duration.zero);
    await controller.stopSpeaking();
    platform.completeSpeak();
    await speaking;

    expect(platform.speakCalls, 1);
    expect(controller.isSpeaking.value, isFalse);
    controller.onClose();
  });

  test('ambiguous summary retry keeps payload-bound request id', () async {
    final gateway = _FakeGateway()..failSaveNext = true;
    final controller = VoiceDiscussController(
      gateway: gateway,
      platform: _FakePlatform(),
      note: NoteModel(id: 'note-1'),
    );
    controller.onInit();
    controller.summaryController.text = '原总结';

    expect(await controller.saveSummary(), isFalse);
    controller.summaryController.text = '修改后的总结';
    expect(await controller.saveSummary(), isFalse);
    expect(gateway.saveCalls, 1);
    expect(controller.errorText.value, contains('恢复为上次提交的总结'));

    controller.summaryController.text = '原总结';
    expect(await controller.saveSummary(), isTrue);
    expect(gateway.saveRequestIds[0], gateway.saveRequestIds[1]);
    controller.onClose();
  });

  test('disposed method-channel bridge rejects every outgoing API', () async {
    const channel = MethodChannel('ainote/test_voice_bridge_disposed');
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    final calls = <MethodCall>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return null;
    });
    final bridge = MethodChannelVoicePlatformBridge(channel: channel);

    bridge.dispose();
    await Future<void>.delayed(Duration.zero);
    final callsAfterDispose = calls.length;

    final attempts = <Future<Object?> Function()>[
      bridge.capabilities,
      bridge.requestMicrophonePermission,
      bridge.startListening,
      bridge.stopListening,
      bridge.cancelListening,
      () => bridge.speak('文本'),
      bridge.stopSpeaking,
    ];
    for (final attempt in attempts) {
      await expectLater(attempt(), throwsA(isA<VoicePlatformException>()));
    }
    expect(calls, hasLength(callsAfterDispose));
    messenger.setMockMethodCallHandler(channel, null);
  });

  test('late summary preview and save do not write after close', () async {
    final previewGateway = _FakeGateway()..holdPreview = true;
    final previewController = VoiceDiscussController(
      gateway: previewGateway,
      platform: _FakePlatform(),
      note: NoteModel(id: 'note-preview'),
    );
    previewController.onInit();
    previewController.messages.assignAll(const [
      VoiceDiscussionMessage(
          role: VoiceDiscussionRole.assistant, content: '已讨论'),
    ]);
    final preview = previewController.generateSummaryPreview();
    await Future<void>.delayed(Duration.zero);
    previewController.onClose();
    previewGateway.completePreview();
    expect(await preview, isFalse);

    final saveGateway = _FakeGateway()..holdSave = true;
    final saveController = VoiceDiscussController(
      gateway: saveGateway,
      platform: _FakePlatform(),
      note: NoteModel(id: 'note-save'),
    );
    saveController.onInit();
    saveController.summaryController.text = '待保存总结';
    final save = saveController.saveSummary();
    await Future<void>.delayed(Duration.zero);
    saveController.onClose();
    saveGateway.completeSave();
    expect(await save, isFalse);
  });
}

class _FakeGateway implements VoiceDiscussionGateway {
  final messages = <String>[];
  final histories = <List<VoiceDiscussionMessage>>[];
  bool failNext = false;
  bool cancelled = false;
  int saveCalls = 0;
  String? savedSummary;
  bool failSaveNext = false;
  bool holdPreview = false;
  bool holdSave = false;
  final saveRequestIds = <String>[];
  Completer<String>? _previewCompleter;
  Completer<void>? _saveCompleter;

  @override
  void cancelActive() {
    cancelled = true;
  }

  @override
  Future<String> previewSummary({
    required String noteId,
    required List<VoiceDiscussionMessage> history,
  }) {
    if (!holdPreview) return Future.value('总结预览');
    return (_previewCompleter ??= Completer<String>()).future;
  }

  void completePreview() {
    holdPreview = false;
    _previewCompleter?.complete('迟到总结');
    _previewCompleter = null;
  }

  @override
  Future<String> respond({
    required String noteId,
    required String message,
    required List<VoiceDiscussionMessage> history,
  }) async {
    messages.add(message);
    histories.add(List.of(history));
    if (failNext) {
      failNext = false;
      throw const VoiceDiscussionApiException('失败');
    }
    return '回复 ${messages.length}';
  }

  @override
  Future<VoiceSummarySaveResult> saveSummary({
    required String noteId,
    required String summary,
    required String saveRequestId,
  }) async {
    saveCalls++;
    savedSummary = summary;
    saveRequestIds.add(saveRequestId);
    if (failSaveNext) {
      failSaveNext = false;
      throw const VoiceDiscussionApiException('保存结果未知');
    }
    if (holdSave) {
      await (_saveCompleter ??= Completer<void>()).future;
    }
    return VoiceSummarySaveResult(
      noteId: noteId,
      saveRequestId: saveRequestId,
      alreadySaved: false,
      note: NoteModel(id: noteId, title: '已更新'),
    );
  }

  void completeSave() {
    holdSave = false;
    _saveCompleter?.complete();
    _saveCompleter = null;
  }
}

class _FakePlatform implements VoicePlatformGateway {
  _FakePlatform({VoiceCapabilities? capabilities})
      : capabilitiesValue = capabilities ??
            const VoiceCapabilities(
              speechAvailable: true,
              microphonePermission: true,
              ttsAvailable: true,
            );

  VoiceCapabilities capabilitiesValue;
  bool permissionGrantResult = false;
  int permissionRequests = 0;
  int startListeningCalls = 0;
  int speakCalls = 0;
  bool holdCapabilities = false;
  bool holdSpeak = false;
  Completer<VoiceCapabilities>? _capabilitiesCompleter;
  Completer<void>? _speakCompleter;
  final recognitionController =
      StreamController<VoiceRecognitionEvent>.broadcast();
  final speakingController = StreamController<bool>.broadcast();

  void emitRecognition(String text, {required bool isFinal}) {
    recognitionController.add(
      VoiceRecognitionEvent(text: text, isFinal: isFinal),
    );
  }

  void emitError(String errorCode) {
    recognitionController.add(
      VoiceRecognitionEvent(text: '', isFinal: true, errorCode: errorCode),
    );
  }

  @override
  Future<VoiceCapabilities> capabilities() {
    if (!holdCapabilities) return Future.value(capabilitiesValue);
    return (_capabilitiesCompleter ??= Completer<VoiceCapabilities>()).future;
  }

  void completeCapabilities() {
    holdCapabilities = false;
    _capabilitiesCompleter?.complete(capabilitiesValue);
    _capabilitiesCompleter = null;
  }

  @override
  Future<bool> requestMicrophonePermission() async {
    permissionRequests++;
    if (permissionGrantResult) {
      capabilitiesValue = VoiceCapabilities(
        speechAvailable: capabilitiesValue.speechAvailable,
        microphonePermission: true,
        ttsAvailable: capabilitiesValue.ttsAvailable,
      );
    }
    return permissionGrantResult;
  }

  @override
  Future<void> cancelListening() async {}

  @override
  Future<void> startListening() async {
    startListeningCalls++;
  }

  @override
  Future<void> stopListening() async {}

  @override
  Future<void> speak(String text) {
    speakCalls++;
    if (!holdSpeak) return Future.value();
    return (_speakCompleter ??= Completer<void>()).future;
  }

  void completeSpeak() {
    holdSpeak = false;
    _speakCompleter?.complete();
    _speakCompleter = null;
  }

  @override
  Future<void> stopSpeaking() async {}

  @override
  Stream<VoiceRecognitionEvent> get recognitionEvents =>
      recognitionController.stream;

  @override
  Stream<bool> get speakingEvents => speakingController.stream;

  @override
  void dispose() {
    recognitionController.close();
    speakingController.close();
  }
}
