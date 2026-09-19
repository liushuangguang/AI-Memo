import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:dio/dio.dart';

class NoteThemeApi {
  static const String _base = '/note/theme';

  static Future<NoteThemeModel> createTheme({
    required String theme,
    required String description,
  }) async {
    final title = theme.trim();
    final detail = description.trim();
    if (title.isEmpty || title.length > 120 || detail.length > 1000) {
      throw ArgumentError('主题或描述长度无效');
    }
    final response = await MyDio.postJSON(
      '$_base/create',
      data: {'theme': title, 'description': detail},
      options: Options(extra: {'ro_disable_retry': true}),
    );
    return NoteThemeModel.fromJson(_dataMap(response));
  }

  static Future<List<NoteThemeModel>> listThemes({
    int page = 1,
    int size = 20,
  }) async {
    final response = await MyDio.postJSON('$_base/pagination', data: {
      'page': page,
      'size': size.clamp(1, 50),
      'ascending': false,
      'sortBy': 'updatedAt',
    });
    final data = response.data is Map ? response.data['data'] : null;
    final content = data is Map ? data['content'] : null;
    if (content == null) return const [];
    if (content is! List) throw const FormatException('主题列表格式错误');
    return content
        .whereType<Map>()
        .map((item) => NoteThemeModel.fromJson(Map<String, dynamic>.from(item)))
        .where((item) => item.id.isNotEmpty && item.theme.isNotEmpty)
        .toList(growable: false);
  }

  static Future<List<NoteThemeCandidateModel>> candidates(
      String themeId) async {
    final id = _requiredId(themeId, '主题');
    final response = await MyDio.postJSON(
      '$_base/$id/candidates',
      options: Options(
        extra: {'ro_disable_retry': true},
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 100),
      ),
    );
    final data = response.data is Map ? response.data['data'] : null;
    if (data == null) return const [];
    if (data is! List) throw const FormatException('候选备忘录格式错误');
    return data
        .whereType<Map>()
        .map((item) => NoteThemeCandidateModel.fromJson(
            Map<String, dynamic>.from(item)))
        .where((item) => item.isUsable)
        .toList(growable: false);
  }

  static Future<NoteThemeMergeResultModel> merge({
    required String themeId,
    required List<String> sourceNoteIds,
    required String idempotencyKey,
  }) async {
    final id = _requiredId(themeId, '主题');
    final ids = sourceNoteIds.map((item) => item.trim()).toList(growable: false);
    if (ids.length < 2 ||
        ids.length > 20 ||
        ids.any((item) => item.isEmpty) ||
        ids.toSet().length != ids.length) {
      throw ArgumentError('每次请选择2至20条备忘录');
    }
    final key = idempotencyKey.trim();
    if (key.length < 8 || key.length > 128) {
      throw ArgumentError('合并请求标识无效');
    }
    final response = await MyDio.postJSON(
      '$_base/$id/merge',
      data: {
        'sourceNoteIds': ids,
        'selectionConfirmed': true,
        'idempotencyKey': key,
      },
      options: Options(
        // Transport retry is safe because the backend idempotency key is stable,
        // but the screen owns explicit retry and its user-facing state.
        extra: {'ro_disable_retry': true},
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 120),
      ),
    );
    return NoteThemeMergeResultModel.fromJson(_dataMap(response));
  }

  static Future<NoteModel> getNote(String noteId) async {
    final id = _requiredId(noteId, '备忘录');
    final response = await MyDio.get('/v2/note/$id');
    if (response.data is! Map) throw const FormatException('备忘录格式错误');
    return NoteModel.fromJson(Map<String, dynamic>.from(response.data as Map));
  }

  static Future<List<NoteThemeMergeHistoryModel>> mergeHistory(String noteId) async {
    final id = _requiredId(noteId, '备忘录');
    final response = await MyDio.get('/v2/note/$id/merge-history');
    final data = response.data is Map ? response.data['data'] : null;
    if (data is! List) throw const FormatException('合并来源格式错误');
    return data.whereType<Map>().map((item) => NoteThemeMergeHistoryModel.fromJson(
      Map<String, dynamic>.from(item))).where((history) => history.mergedNoteId == id).toList();
  }

  static Future<void> deleteTheme(String themeId) async {
    await MyDio.delete('$_base/delete/${_requiredId(themeId, '主题')}');
  }

  static Map<String, dynamic> _dataMap(Response response) {
    final envelope = response.data;
    final data = envelope is Map ? envelope['data'] : null;
    if (data is! Map) throw const FormatException('服务端返回格式错误');
    return Map<String, dynamic>.from(data);
  }

  static String _requiredId(String value, String label) {
    final id = value.trim();
    if (id.isEmpty || id.length > 160 || id.contains('/')) {
      throw ArgumentError('$label ID无效');
    }
    return id;
  }
}
