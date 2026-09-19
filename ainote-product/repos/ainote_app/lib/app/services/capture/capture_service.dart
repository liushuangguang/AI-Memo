import 'dart:async';
import 'dart:io';

import 'package:ainote_app/app/api/api_urls.dart';
import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/stores/my_secure_storage.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:dio/dio.dart';
import 'package:dio_smart_retry/dio_smart_retry.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart' hide FormData, MultipartFile;

class CaptureCandidate {
  const CaptureCandidate({
    required this.id,
    required this.source,
    this.uri,
    this.localPath,
    this.displayName,
    this.mimeType,
    this.recognizedText,
  });

  final String id;
  final String source;
  final String? uri;
  final String? localPath;
  final String? displayName;
  final String? mimeType;
  final String? recognizedText;

  factory CaptureCandidate.fromMap(Map<Object?, Object?> map) {
    String? read(String key) {
      final value = map[key];
      return value is String && value.isNotEmpty ? value : null;
    }

    return CaptureCandidate(
      id: read('id') ?? '',
      source: read('source') ?? 'unknown',
      uri: read('uri'),
      localPath: read('localPath'),
      displayName: read('displayName'),
      mimeType: read('mimeType'),
      recognizedText: read('recognizedText'),
    );
  }
}

/// Coordinates screenshot, Android share-sheet, and manual image capture.
///
/// Share/manual/history candidates stay behind Flutter confirmation. Only
/// newly detected screenshots covered by the separate native consent can run
/// through the background OCR/upload pipeline.
class CaptureService with WidgetsBindingObserver {
  CaptureService._();

  static final CaptureService instance = CaptureService._();
  static const MethodChannel _channel = MethodChannel('ainote/capture');

  final ValueNotifier<bool> watchEnabled = ValueNotifier<bool>(false);
  final ValueNotifier<bool> autoAvailable = ValueNotifier<bool>(false);
  final ValueNotifier<bool> autoEnabled = ValueNotifier<bool>(false);
  final ValueNotifier<Map<Object?, Object?>?> autoJob =
      ValueNotifier<Map<Object?, Object?>?>(null);
  final ValueNotifier<bool> busy = ValueNotifier<bool>(false);
  String? lastWatchMessage;
  String? lastAutoMessage;
  bool _initialized = false;
  bool _observerAttached = false;
  Future<void>? _initializeFuture;
  bool _dialogVisible = false;
  bool _drainInFlight = false;
  bool _drainScheduled = false;
  bool _openingAutoNote = false;

  Future<void> initialize() {
    if (_initialized) {
      return _refreshInitialized();
    }
    final pending = _initializeFuture;
    if (pending != null) return pending;
    final future = _initializeOnce();
    _initializeFuture = future;
    return future.whenComplete(() {
      if (identical(_initializeFuture, future)) _initializeFuture = null;
    });
  }

  Future<void> _initializeOnce() async {
    if (!_observerAttached) {
      WidgetsBinding.instance.addObserver(this);
      _observerAttached = true;
    }
    _channel.setMethodCallHandler(_onNativeCall);
    await _channel.invokeMapMethod<Object?, Object?>(
      'provisionCaptureIdentity',
      <String, Object?>{'deviceId': await MySecureStorage.getDeviceId()},
    );
    await _refreshStatus();
    _initialized = true;
    _scheduleDrain();
    _scheduleOpenCompletion();
  }

  Future<void> _refreshInitialized() async {
    await _refreshStatus();
    _scheduleDrain();
    _scheduleOpenCompletion();
  }

  Future<void> _refreshStatus() async {
    final status =
        await _channel.invokeMapMethod<Object?, Object?>('getStatus');
    watchEnabled.value = status?['watchEnabled'] == true;
    autoAvailable.value = status?['autoAvailable'] == true;
    autoEnabled.value = status?['autoEnabled'] == true;
    lastAutoMessage = status?['autoMessage'] is String
        ? status!['autoMessage']! as String
        : null;
    autoJob.value = status?['autoJob'] is Map
        ? Map<Object?, Object?>.from(status!['autoJob']! as Map)
        : null;
    if (!watchEnabled.value && status?['message'] is String) {
      lastWatchMessage = status!['message']! as String;
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) return;
    unawaited(_refreshStatus().then((_) => _scheduleDrain()).catchError((_) {
      _scheduleDrain();
    }));
    _scheduleOpenCompletion();
  }

  Future<bool> setWatchEnabled(bool enabled) async {
    await initialize();
    if (!enabled) {
      await _channel.invokeMethod<void>('stopScreenshotWatch');
      watchEnabled.value = false;
      autoEnabled.value = false;
      lastWatchMessage = null;
      return true;
    }
    final permissionResult = await _channel.invokeMapMethod<Object?, Object?>(
      'requestWatchPermissions',
    );
    lastWatchMessage = permissionResult?['message'] is String
        ? permissionResult!['message']! as String
        : null;
    if (permissionResult?['granted'] != true) {
      watchEnabled.value = false;
      return false;
    }
    await _channel.invokeMethod<void>('startScreenshotWatch');
    watchEnabled.value = true;
    lastWatchMessage = null;
    return true;
  }

  Future<bool> setAutoGenerationEnabled(bool enabled) async {
    await initialize();
    final status = await _channel.invokeMapMethod<Object?, Object?>(
      'setAutoGenerationEnabled',
      <String, Object?>{'enabled': enabled},
    );
    autoAvailable.value = status?['autoAvailable'] == true;
    autoEnabled.value = status?['autoEnabled'] == true;
    lastAutoMessage = status?['autoMessage'] is String
        ? status!['autoMessage']! as String
        : null;
    return autoEnabled.value == enabled;
  }

  Future<bool> retryAutoJob() async {
    final id = autoJob.value?['id'];
    if (id is! String || id.isEmpty) return false;
    final retried = await _channel.invokeMethod<bool>(
      'retryAutoCandidate',
      <String, Object?>{'id': id},
    );
    await _refreshStatus();
    return retried == true;
  }

  Future<void> pickImage() async {
    await initialize();
    final value = await _channel.invokeMapMethod<Object?, Object?>('pickImage');
    if (value == null) return;
    _scheduleDrain();
  }

  Future<dynamic> _onNativeCall(MethodCall call) async {
    if (call.method == 'captureError' && call.arguments is Map) {
      final error = Map<Object?, Object?>.from(call.arguments as Map);
      final context = Get.context;
      if (context != null && context.mounted) {
        _showMessage(error['message'] is String
            ? error['message']! as String
            : '无法读取分享的图片，请重新选择');
        await _channel.invokeMethod<bool>('ackPendingError');
      }
      return;
    }
    if (call.method == 'captureCandidate' ||
        call.method == 'autoCaptureUpdated') {
      unawaited(_refreshStatus().catchError((_) {}));
      _scheduleDrain();
      _scheduleOpenCompletion();
    }
  }

  void _scheduleOpenCompletion() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_openCompletedAutoNote());
    });
  }

  Future<void> _openCompletedAutoNote() async {
    if (_openingAutoNote ||
        WidgetsBinding.instance.lifecycleState != AppLifecycleState.resumed ||
        Get.context == null) {
      return;
    }
    _openingAutoNote = true;
    try {
      final completion = await _channel.invokeMapMethod<Object?, Object?>(
        'peekAutoCompletion',
      );
      final noteId = completion?['noteId'];
      if (noteId is! String || noteId.isEmpty) return;
      final response =
          await MyDio.postJSON(ApiUrls.noteQueryDetailById(noteId));
      final payload = response.data;
      if (payload is! Map ||
          payload['code'] != 200 ||
          payload['data'] is! Map) {
        throw StateError('无法读取刚生成的备忘录');
      }
      final note = NoteModel.fromJson(
        Map<String, dynamic>.from(payload['data'] as Map),
      );
      if (note.id != noteId) throw StateError('返回的备忘录与生成结果不一致');
      final acknowledged = await _channel.invokeMethod<bool>(
        'ackAutoCompletion',
        <String, Object?>{'noteId': noteId},
      );
      if (acknowledged != true) throw StateError('无法确认已生成的备忘录');
      await Get.toNamed(
        Routes.NOTE_EDIT,
        arguments: note,
        parameters: <String, String>{'mode': NoteEditMode.preview.name},
      );
    } catch (_) {
      _showMessage('备忘录已生成，但暂时无法打开；可从通知或截图设置中重试');
    } finally {
      _openingAutoNote = false;
    }
  }

  void _scheduleDrain() {
    if (_drainScheduled || _drainInFlight) return;
    _drainScheduled = true;
    WidgetsBinding.instance.scheduleFrame();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _drainScheduled = false;
      unawaited(_drain());
    });
  }

  Future<void> _drain() async {
    if (_drainInFlight || _dialogVisible) return;
    if (WidgetsBinding.instance.lifecycleState != AppLifecycleState.resumed) {
      return;
    }
    final activeContext = Get.context;
    if (activeContext == null || !activeContext.mounted) return;

    _drainInFlight = true;
    var terminal = false;
    String? activeCandidateId;
    try {
      final pendingError = await _channel.invokeMapMethod<Object?, Object?>(
        'peekPendingError',
      );
      if (pendingError?['message'] is String) {
        _showMessage(pendingError!['message']! as String);
        await _channel.invokeMethod<bool>('ackPendingError');
      }
      final pending = await _channel.invokeMapMethod<Object?, Object?>(
        'peekPendingCandidate',
      );
      if (pending == null) return;
      var candidate = CaptureCandidate.fromMap(pending);
      activeCandidateId = candidate.id;
      if ((candidate.localPath == null ||
              !File(candidate.localPath!).existsSync()) &&
          candidate.uri != null) {
        final materialized = await _channel.invokeMapMethod<Object?, Object?>(
          'materializeUri',
          <String, Object?>{
            'id': candidate.id,
            'uri': candidate.uri,
            'source': candidate.source,
          },
        );
        if (materialized == null) return;
        candidate = CaptureCandidate.fromMap(materialized);
      }
      final path = candidate.localPath;
      if (path == null || !File(path).existsSync()) {
        terminal = await _offerDiscard(
          candidate.id,
          '图片副本暂时无法读取。你可以稍后重试，或跳过此图片继续处理队列',
        );
        return;
      }

      final context = Get.context;
      if (context == null || !context.mounted) return;
      _dialogVisible = true;
      final shouldUpload = await showDialog<bool>(
        context: context,
        barrierDismissible: false,
        builder: (dialogContext) => AlertDialog(
          title: Text(_candidateTitle(candidate.source)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 260),
                  child: Image.file(File(path), fit: BoxFit.contain),
                ),
              ),
              const SizedBox(height: 12),
              const Text('确认后才会在设备上识别文字，并上传图片生成备忘录。'),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('忽略'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('识别并生成'),
            ),
          ],
        ),
      );
      _dialogVisible = false;

      if (shouldUpload != true) {
        terminal = await _ackCandidate(candidate.id);
        return;
      }
      busy.value = true;
      final recognized = await _channel.invokeMapMethod<Object?, Object?>(
        'recognizeCandidate',
        <String, Object?>{'id': candidate.id},
      );
      if (recognized == null) throw StateError('图片文字识别失败');
      candidate = CaptureCandidate.fromMap(recognized);
      if (candidate.recognizedText?.trim().isEmpty ?? true) {
        throw StateError('图片中没有识别到可读文字');
      }
      final note = await _upload(candidate);
      terminal = await _ackCandidate(candidate.id);
      if (!terminal) throw StateError('备忘录已生成，但本地图片状态清理失败');
      await Get.toNamed(
        Routes.NOTE_EDIT,
        arguments: note,
        parameters: <String, String>{'mode': NoteEditMode.preview.name},
      );
    } on PlatformException catch (error) {
      final message = error.message ?? '图片处理失败';
      if (error.code == 'no_text' || error.code == 'image_unavailable') {
        terminal = await _offerDiscard(activeCandidateId, message);
      } else {
        _showMessage('$message，图片已保留，可稍后重试');
      }
    } on DioException catch (error) {
      final data = error.response?.data;
      _showMessage(data is Map && data['message'] is String
          ? data['message'] as String
          : '上传失败，图片已保留，请检查网络后重试');
    } catch (error) {
      _showMessage(error is StateError
          ? '${error.message}，图片已保留，可稍后重试'
          : '图片处理失败，已保留，可稍后重试');
    } finally {
      _dialogVisible = false;
      busy.value = false;
      _drainInFlight = false;
      if (terminal) _scheduleDrain();
    }
  }

  Future<NoteModel> _upload(CaptureCandidate candidate) async {
    final path = candidate.localPath!;
    _showMessage('正在识别图片并生成备忘录…');
    final mimeType = candidate.mimeType == 'image/png'
        ? DioMediaType('image', 'png')
        : DioMediaType('image', 'jpeg');
    final response = await MyDio.dio.post(
      ApiUrls.noteCreateImage,
      options: Options(
          connectTimeout: const Duration(seconds: 15),
          sendTimeout: const Duration(seconds: 60),
          receiveTimeout: const Duration(seconds: 200))
        ..disableRetry = true,
      data: FormData.fromMap(<String, Object?>{
        'file': await MultipartFile.fromFile(
          path,
          filename: File(path).uri.pathSegments.last,
          contentType: mimeType,
        ),
        'recognizedText': candidate.recognizedText,
        'requestId': candidate.id,
      }),
    );
    final payload = response.data;
    if (payload is! Map || payload['code'] != 200 || payload['data'] is! Map) {
      throw StateError(payload is Map && payload['message'] is String
          ? payload['message'] as String
          : '图片识别失败');
    }
    final note = NoteModel.fromJson(
      Map<String, dynamic>.from(payload['data'] as Map),
    );
    if (note.id == null || note.id!.isEmpty) {
      throw StateError('服务端没有返回已创建的备忘录');
    }
    return note;
  }

  Future<bool> _ackCandidate(String id) async =>
      await _channel.invokeMethod<bool>(
        'ackCandidate',
        <String, Object?>{'id': id},
      ) ==
      true;

  Future<bool> _offerDiscard(String? id, String message) async {
    final context = Get.context;
    if (id == null || context == null || !context.mounted) {
      _showMessage('$message，图片已保留');
      return false;
    }
    final discard = await showDialog<bool>(
          context: context,
          barrierDismissible: false,
          builder: (dialogContext) => AlertDialog(
            title: const Text('无法处理这张图片'),
            content: Text('$message\n\n只有你明确跳过后，应用才会删除本地副本并处理下一张。'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(false),
                child: const Text('稍后重试'),
              ),
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(true),
                child: const Text('跳过此图片'),
              ),
            ],
          ),
        ) ??
        false;
    return discard && await _ackCandidate(id);
  }

  String _candidateTitle(String source) => switch (source) {
        'screenshot' => '发现新截图',
        'share' => '从其他应用接收图片',
        _ => '使用所选图片生成备忘录',
      };

  void _showMessage(String message) {
    final context = Get.context;
    if (context == null || !context.mounted) return;
    ScaffoldMessenger.maybeOf(context)?.showSnackBar(
      SnackBar(content: Text(message)),
    );
  }
}
