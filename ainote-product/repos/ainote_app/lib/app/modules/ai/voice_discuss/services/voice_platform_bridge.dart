import 'dart:async';

import 'package:flutter/services.dart';

class VoiceCapabilities {
  const VoiceCapabilities({
    required this.speechAvailable,
    required this.microphonePermission,
    required this.ttsAvailable,
    this.reason,
  });

  final bool speechAvailable;
  final bool microphonePermission;
  final bool ttsAvailable;
  final String? reason;

  bool get canListen => speechAvailable && microphonePermission;

  factory VoiceCapabilities.unavailable(String reason) => VoiceCapabilities(
        speechAvailable: false,
        microphonePermission: false,
        ttsAvailable: false,
        reason: reason,
      );
}

class VoiceRecognitionEvent {
  const VoiceRecognitionEvent({
    required this.text,
    required this.isFinal,
    this.errorCode,
  });

  final String text;
  final bool isFinal;
  final String? errorCode;

  bool get isError => errorCode != null;
}

abstract class VoicePlatformGateway {
  Stream<VoiceRecognitionEvent> get recognitionEvents;
  Stream<bool> get speakingEvents;

  Future<VoiceCapabilities> capabilities();
  Future<bool> requestMicrophonePermission();
  Future<void> startListening();
  Future<void> stopListening();
  Future<void> cancelListening();
  Future<void> speak(String text);
  Future<void> stopSpeaking();
  void dispose();
}

class MethodChannelVoicePlatformBridge implements VoicePlatformGateway {
  MethodChannelVoicePlatformBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel('ainote/voice_bridge'),
        _sessionId = ++_nextSessionId {
    _activeBridge = this;
    _channel.setMethodCallHandler(_handleNativeCall);
  }

  static int _nextSessionId = 0;
  static MethodChannelVoicePlatformBridge? _activeBridge;
  final MethodChannel _channel;
  final int _sessionId;
  final _recognitionController =
      StreamController<VoiceRecognitionEvent>.broadcast();
  final _speakingController = StreamController<bool>.broadcast();
  bool _disposed = false;

  @override
  Stream<VoiceRecognitionEvent> get recognitionEvents =>
      _recognitionController.stream;

  @override
  Stream<bool> get speakingEvents => _speakingController.stream;

  Future<void> _handleNativeCall(MethodCall call) async {
    if (_disposed) return;
    final arguments = call.arguments;
    final data = arguments is Map
        ? Map<String, dynamic>.from(arguments)
        : const <String, dynamic>{};
    if (data['sessionId'] != _sessionId) return;
    if (call.method == 'onSpeechEvent') {
      _recognitionController.add(VoiceRecognitionEvent(
        text: data['text']?.toString() ?? '',
        isFinal: data['final'] == true,
        errorCode: data['errorCode']?.toString(),
      ));
    } else if (call.method == 'onTtsEvent') {
      _speakingController.add(data['speaking'] == true);
    }
  }

  @override
  Future<VoiceCapabilities> capabilities() async {
    _throwIfDisposed();
    try {
      final raw = await _channel.invokeMapMethod<String, dynamic>(
          'capabilities', _sessionArguments());
      if (raw == null) {
        return VoiceCapabilities.unavailable('系统未返回语音能力');
      }
      return VoiceCapabilities(
        speechAvailable: raw['speechAvailable'] == true,
        microphonePermission: raw['microphonePermission'] == true,
        ttsAvailable: raw['ttsAvailable'] == true,
        reason: raw['reason']?.toString(),
      );
    } on MissingPluginException {
      return VoiceCapabilities.unavailable('当前安装包尚未注册语音桥');
    } on PlatformException catch (error) {
      return VoiceCapabilities.unavailable(error.message ?? '无法检测系统语音能力');
    }
  }

  @override
  Future<bool> requestMicrophonePermission() async {
    _throwIfDisposed();
    try {
      return await _channel.invokeMethod<bool>(
              'requestMicrophonePermission', _sessionArguments()) ??
          false;
    } on MissingPluginException {
      throw const VoicePlatformException('当前安装包尚未注册语音桥');
    } on PlatformException catch (error) {
      throw VoicePlatformException(error.message ?? error.code);
    }
  }

  @override
  Future<void> startListening() => _invoke('startListening');

  @override
  Future<void> stopListening() => _invoke('stopListening');

  @override
  Future<void> cancelListening() => _invoke('cancelListening');

  @override
  Future<void> speak(String text) => _invoke('speak', {'text': text});

  @override
  Future<void> stopSpeaking() => _invoke('stopSpeaking');

  Future<void> _invoke(String method, [Map<String, dynamic>? arguments]) async {
    _throwIfDisposed();
    try {
      await _channel.invokeMethod<void>(
        method,
        _sessionArguments(arguments),
      );
    } on MissingPluginException {
      throw const VoicePlatformException('当前安装包尚未注册语音桥');
    } on PlatformException catch (error) {
      throw VoicePlatformException(error.message ?? error.code);
    }
  }

  void _throwIfDisposed() {
    if (_disposed) {
      throw const VoicePlatformException('语音会话已关闭');
    }
  }

  Map<String, dynamic> _sessionArguments([Map<String, dynamic>? arguments]) =>
      <String, dynamic>{'sessionId': _sessionId, ...?arguments};

  @override
  void dispose() {
    if (_disposed) return;
    _disposed = true;
    unawaited(_channel
        .invokeMethod<void>('dispose', _sessionArguments())
        .catchError((_) {}));
    if (identical(_activeBridge, this)) {
      _activeBridge = null;
      _channel.setMethodCallHandler(null);
    }
    unawaited(_recognitionController.close());
    unawaited(_speakingController.close());
  }
}

class VoicePlatformException implements Exception {
  const VoicePlatformException(this.message);
  final String message;

  @override
  String toString() => message;
}
