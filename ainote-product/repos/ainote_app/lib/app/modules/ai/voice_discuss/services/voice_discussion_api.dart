import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:dio/dio.dart';

import '../models/voice_discussion_models.dart';

abstract class VoiceDiscussionGateway {
  Future<String> respond({
    required String noteId,
    required String message,
    required List<VoiceDiscussionMessage> history,
  });

  Future<String> previewSummary({
    required String noteId,
    required List<VoiceDiscussionMessage> history,
  });

  Future<VoiceSummarySaveResult> saveSummary({
    required String noteId,
    required String summary,
    required String saveRequestId,
  });

  void cancelActive();
}

class VoiceDiscussionApi implements VoiceDiscussionGateway {
  static const _root = '/v2/voice-discussion';
  CancelToken? _activeCancelToken;

  @override
  Future<String> respond({
    required String noteId,
    required String message,
    required List<VoiceDiscussionMessage> history,
  }) async {
    final response = await _post(
      '$_root/respond',
      data: {
        'noteId': noteId,
        'message': message,
        'history': history.map((message) => message.toJson()).toList(),
      },
    );
    return _requiredString(response, 'reply');
  }

  @override
  Future<String> previewSummary({
    required String noteId,
    required List<VoiceDiscussionMessage> history,
  }) async {
    final response = await _post(
      '$_root/summary/preview',
      data: {
        'noteId': noteId,
        'history': history.map((message) => message.toJson()).toList(),
      },
    );
    return _requiredString(response, 'summary');
  }

  @override
  Future<VoiceSummarySaveResult> saveSummary({
    required String noteId,
    required String summary,
    required String saveRequestId,
  }) async {
    final data = await _post(
      '$_root/summary/save',
      data: {
        'noteId': noteId,
        'summary': summary,
        'saveRequestId': saveRequestId,
      },
    );
    return VoiceSummarySaveResult(
      noteId: _requiredString(data, 'noteId'),
      saveRequestId: _requiredString(data, 'saveRequestId'),
      alreadySaved: data['alreadySaved'] == true,
      note: NoteModel.fromJson(_requiredMap(data, 'note')),
    );
  }

  Future<Map<String, dynamic>> _post(
    String path, {
    required Map<String, dynamic> data,
  }) async {
    final token = CancelToken();
    _activeCancelToken = token;
    try {
      final response = await MyDio.dio.post<dynamic>(
        path,
        data: data,
        cancelToken: token,
      );
      final envelope = response.data;
      if (envelope is! Map || envelope['code'] != 200) {
        throw const VoiceDiscussionApiException('语音讨论服务返回了无效结果');
      }
      final payload = envelope['data'];
      if (payload is! Map) {
        throw const VoiceDiscussionApiException('语音讨论服务没有返回内容');
      }
      return Map<String, dynamic>.from(payload);
    } on DioException catch (error) {
      if (CancelToken.isCancel(error)) {
        throw const VoiceDiscussionCancelledException();
      }
      final status = error.response?.statusCode;
      if (status == 401 || status == 403) {
        throw const VoiceDiscussionApiException('当前账号不能访问这条笔记');
      }
      if (status == 404) {
        throw const VoiceDiscussionApiException('当前笔记不存在');
      }
      if (status == 400) {
        throw const VoiceDiscussionApiException('讨论内容无效或过长');
      }
      if (status == 409) {
        throw const VoiceDiscussionApiException('笔记正文已在其他位置更新，请退出讨论刷新后重试');
      }
      if (status == 503) {
        throw const VoiceDiscussionApiException('AI 讨论服务暂时不可用');
      }
      throw const VoiceDiscussionApiException('网络异常，请稍后重试');
    } finally {
      if (identical(_activeCancelToken, token)) {
        _activeCancelToken = null;
      }
    }
  }

  String _requiredString(Map<String, dynamic> data, String key) {
    final value = data[key];
    if (value is! String || value.trim().isEmpty) {
      throw const VoiceDiscussionApiException('语音讨论服务返回了空内容');
    }
    return value.trim();
  }

  Map<String, dynamic> _requiredMap(Map<String, dynamic> data, String key) {
    final value = data[key];
    if (value is! Map) {
      throw const VoiceDiscussionApiException('语音讨论服务没有返回更新后的笔记');
    }
    return Map<String, dynamic>.from(value);
  }

  @override
  void cancelActive() {
    final token = _activeCancelToken;
    if (token != null && !token.isCancelled) {
      token.cancel('user_cancelled');
    }
  }
}

class VoiceDiscussionApiException implements Exception {
  const VoiceDiscussionApiException(this.message);
  final String message;

  @override
  String toString() => message;
}

class VoiceDiscussionCancelledException extends VoiceDiscussionApiException {
  const VoiceDiscussionCancelledException() : super('本次回复已取消');
}
