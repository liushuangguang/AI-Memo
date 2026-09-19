import 'dart:convert';
import 'dart:io';

import 'package:ainote/models/ai_generate_model.dart';
import 'package:ainote/models/categorized_note_model.dart';
import 'package:ainote/models/note_model.dart';
import 'package:ainote/services/keychain_service.dart';
import 'package:flutter/services.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:http/http.dart' as http;
import 'package:http/http.dart';
import 'package:path/path.dart';
import 'package:http_parser/http_parser.dart';
import 'package:mime_type/mime_type.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../constants/constants.dart';
import '../models/ai_history_model.dart';
import '../models/content_assist_model.dart';
import '../models/message_model.dart';
import '../models/type_model.dart';
import '../utils/utils.dart';

class APIService {
  static const String baseUrl = 'https://aifunc.top';

  Stream<String> getAIGenerate(String noteId, String version) async* {
    final url = Uri.parse('$baseUrl/note/analysis/organizedNoteText');
    final request = http.Request('POST', url);

    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
    });

    request.body = jsonEncode({
      'id': noteId,
      'version': version,
    });

    var streamedResponse = await request.send();

    await for (var line in streamedResponse.stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())) {
      yield line;
    }
  }

  Future<NoteModel?> getNoteDetail(String noteId) async {
    final endpoint = "/note/$noteId";
    try {
      final response = await HttpManager.get(endpoint);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        final json = jsonDecode(decodedResponse);
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<List<AIHistoryModel>?> getAIHistory(String noteId) async {
    final endpoint = "/note/analysis/history/all?noteAnalysisId=$noteId";
    try {
      final response = await HttpManager.post(endpoint);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        return parseData(
            decodedResponse, (json) => AIHistoryModel.fromJson(json));
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<NoteModel?> deleteNote(String noteId) async {
    final endpoint = "/note/delete/$noteId";
    try {
      final response = await HttpManager.delete(endpoint);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<NoteModel?> updateNote(
      String title, String content, String noteId, String noteType,
      [Map<String, dynamic>? talkSnapshot]) async {
    final endpoint = "/note/update/$noteId";
    final body = {
      'rawNote': content,
      'noteType': noteType,
      'title': title,
      'talkSnapshot': talkSnapshot
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<NoteModel?> createNote(
      String title, String content, String noteType) async {
    const endpoint = "/note/create";
    final body = {
      'rawNote': content,
      'noteType': noteType,
      'title': title,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<List<NoteModel>> getRelatedNotes(String noteId, String version) async {
    final endpoint = "/note/analysis/relatedNotes";
    final body = {
      'id': noteId,
      'version': version,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        return parseData(decodedResponse, (json) => NoteModel.fromJson(json));
      } else {
        print('请求失败: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      print('请求失败: $e');
      return [];
    }
  }

  Future<List<CategorizedNoteModel>> getCategorizedNotes(
      String noteId, String version) async {
    final endpoint = "/note/analysis/categorizedNotes";
    final body = {
      'id': noteId,
      'version': version,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        return parseData(
            decodedResponse, (json) => CategorizedNoteModel.fromJson(json));
      } else {
        print('请求失败: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      print('请求失败: $e');
      return [];
    }
  }

  Future<AIGuessModel?> getGuessed(String noteId, String version) async {
    final endpoint = "/note/analysis/relationalInformation";
    final body = {
      'id': noteId,
      'version': version,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        return AIGuessModel.fromJson(jsonDecode(decodedResponse));
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<String?> getImageLink(String noteId, String version) async {
    final endpoint = "/note/analysis/imageLink";
    final body = {
      'id': noteId,
      'version': version,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print("请求返回的数据:" + decodedResponse);
        return decodedResponse;
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<List<AILinkModel>?> getAILinks(String noteId, String version) async {
    final endpoint = "/note/analysis/relatedLinks";
    final body = {
      'id': noteId,
      'version': version,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        return parseData(decodedResponse, (json) => AILinkModel.fromJson(json));
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<NoteModel?> appendToNote(String noteId, String content) async {
    print('appendToNote $noteId $content');

    final endpoint = "/note/appendToOriginalText";
    final body = {
      'id': noteId,
      'toAppendText': content,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<List<TypeModel>?> getTypeDict(String typeName) async {
    final endpoint = "/dict/list";
    final body = {
      'typeNames': [typeName],
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        final jsonResponse = jsonDecode(decodedResponse);
        return (jsonResponse as List)
            .map((e) => TypeModel.fromJson(e as Map<String, dynamic>))
            .toList();
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  Future<List<NoteModel>?> getNoteList(
      {String? keyword = '', String? noteType, int? page = 1}) async {
    final endpoint = "/note/list";
    final body = {
      'keyword': keyword,
      'noteType': noteType,
      'page': page,
      'size': 10,
      'ascending': false,
      'sortBy': 'id',
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        final jsonResponse = jsonDecode(decodedResponse);
        return (jsonResponse as List)
            .map((e) => NoteModel.fromJson(e as Map<String, dynamic>))
            .toList();
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  static Future<ContentAssistDirectionModel?> getAssistanceDirection(
      int? noteId) async {
    const endpoint = "/note/analysis/selectedAssistanceDirection";
    final body = {
      'id': noteId,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        final jsonResponse = jsonDecode(decodedResponse);
        return ContentAssistDirectionModel.fromJson(jsonResponse);
      } else {
        print('getAssistanceDirection 请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('getAssistanceDirection 请求失败: $e');
      return null;
    }
  }

  static Stream<String> getAssistanceResult(
      {NoteModel? noteModel,
      required List<dynamic> assistDirections,
      String? selectedTitle,
      String? selectedContent}) async* {
    final url = Uri.parse('$baseUrl/note/analysis/assistedContent');
    final request = http.Request('POST', url);

    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
    });

    request.body = jsonEncode({
      'id': noteModel?.id,
      'assistDirections': assistDirections,
      'version': noteModel?.version,
      'selectedTitle': selectedTitle ?? '',
      'selectedContent': selectedContent ?? '',
    });

    var streamedResponse = await request.send();

    await for (var line in streamedResponse.stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())) {
      yield line;
    }
  }

  /// 创建笔记分析历史记录
  /// analysisType: 1-整理笔记，2-辅助内容，3-讨论
  static Future<AIHistoryModel> createNoteAnalysisHistory(
      int noteAnalysisId, int analysisType) async {
    const endpoint = "/note/analysis/history/create";
    final body = {
      'noteAnalysisId': noteAnalysisId,
      'analysisType': analysisType,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        final jsonResponse = jsonDecode(decodedResponse);
        return AIHistoryModel.fromJson(jsonResponse);
      } else {
        print('createNoteAnalysisHistory 请求失败: ${response.statusCode}');
        return AIHistoryModel();
      }
    } catch (e) {
      print('createNoteAnalysisHistory 请求失败: $e');
      return AIHistoryModel();
    }
  }

  /// 获取笔记分析历史记录
  static Future<List<AIHistoryModel>> getNoteAnalysisHistoryAll(
      int noteAnalysisId) async {
    String endpoint =
        "/note/analysis/history/all?noteAnalysisId=$noteAnalysisId";

    try {
      final response = await HttpManager.post(endpoint);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        print(decodedResponse);
        List<AIHistoryModel> _historyList = formatRawHistData(decodedResponse);

        print('_historyList jsonResponse${_historyList}');

        return _historyList;
      } else {
        print('getNoteAnalysisHistoryAll 请求失败: ${response.statusCode}');
        return [];
      }
    } catch (e) {
      print('getNoteAnalysisHistoryAll 请求失败: $e');
      return [];
    }
  }

  /// 获取笔记分析历史记录
  static Stream<String> getNoteAnalysisHistoryById(
      {required int noteAnalysisId,
      required int version,
      required int analysisType}) async* {
    final url = Uri.parse(
        '$baseUrl/note/analysis/history/specific?noteAnalysisId=$noteAnalysisId&version=$version&analysisType=$analysisType');
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final request = http.Request('GET', url);

    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
      'device-id': deviceId,
    });

    var streamedResponse = await request.send();

    await for (var line in streamedResponse.stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())) {
      yield line;
    }
  }

  /// 查看某个note的最新AI分析历史记录
  static Stream<String> getNoteAnalysisHistoryLatest(
      {required int noteAnalysisId,
      required int version,
      required int analysisType}) async* {
    final url = Uri.parse(
        '$baseUrl/note/analysis/history/latest?noteAnalysisId=$noteAnalysisId&analysisType=$analysisType');
    final request = http.Request('GET', url);
    final deviceId = await KeyChainService().getDeviceId() ?? "";

    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
      'device-id': deviceId,
    });

    var streamedResponse = await request.send();

    await for (var line in streamedResponse.stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())) {
      yield line;
    }
  }

  //创建图片笔记
  static Future<String?> createImageNote({required String filePath}) async {
    const endpoint = "/note/createImageNote";
    try {
      // String tempPath = '${(await getTemporaryDirectory()).path}/${const Uuid().v4()}${extension(filePath)}';
      // // print("==== tempPath ==== " + tempPath);
      // var compressFile = await FlutterImageCompress.compressAndGetFile(
      //   filePath, tempPath,
      //   minHeight: 1,
      //   minWidth: 1,
      //   quality: 88,
      // );
      // print("==== compressFile.path ==== " + (compressFile?.path??""));

      final StreamedResponse response =
          await HttpManager.uploadFile(endpoint, File(filePath));
      if (response.statusCode == 200) {
        var bytes = await response.stream.toBytes();
        final body = utf8.decode(bytes);
        return body;
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  /// 识别PCM音频，返回识别文字结果
  static Future<String> recognizeSpeechToText(
      {required String filePath}) async {
    final file = File(filePath);
    final fileName = basename(file.path);
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final request =
        http.MultipartRequest('POST', Uri.parse('$baseUrl/audio/recognize'))
          ..files.add(await http.MultipartFile.fromPath(
            'file',
            file.path,
            filename: fileName,
          ));
    request.headers.addAll({
      'device-id': deviceId,
    });
    try {
      final response = await request.send();
      if (response.statusCode == 200) {
        var responseBody = await response.stream.bytesToString();
        print('recognizeSpeechToText: ${responseBody.replaceAll('data:', '')}');
        return responseBody.replaceAll('data:', '');
      } else {
        return '';
      }
    } catch (e) {
      print('recognizeSpeechToText error: $e');
      return '';
    }
  }

  /// 识别PCM音频，返回识别文字结果
  static Future<String> recognizeAssetFileSpeechToText(
      {required String fileName}) async {
    final assetFilePath = 'assets/media/$fileName'; // 替换为实际文件路径
    final bytes = await rootBundle
        .load(assetFilePath)
        .then((value) => value.buffer.asUint8List());

    final request =
        http.MultipartRequest('POST', Uri.parse('$baseUrl/audio/recognize'))
          ..files.add(http.MultipartFile.fromBytes(
            'file',
            bytes,
            filename: fileName,
          ));
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
      'device-id': deviceId,
    });

    try {
      final response = await request.send();
      if (response.statusCode == 200) {
        var responseBody = await response.stream.bytesToString();
        print(
            'recognizeAssetFileSpeechToText: ${responseBody.replaceAll('data:', '')}');
        return responseBody.replaceAll('data:', '');
      } else {
        return '';
      }
    } catch (e) {
      print('recognizeAssetFileSpeechToText error: $e');
      return '';
    }
  }

  /// 识别PCM音频，返回识别文字结果
  static Future<String> recognizeSpeechStreamToText(
      {required String fileName}) async {
    final assetFilePath = 'assets/media/$fileName'; // 替换为实际文件路径
    final bytes = await rootBundle
        .load(assetFilePath)
        .then((value) => value.buffer.asUint8List());

    final request =
        http.MultipartRequest('POST', Uri.parse('$baseUrl/audio/recognize'))
          ..files.add(http.MultipartFile.fromBytes(
            'file',
            bytes,
            filename: fileName,
          ));
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
      'device-id': deviceId,
    });

    try {
      final response = await request.send();
      if (response.statusCode == 200) {
        var responseBody = await response.stream.bytesToString();
        print(
            'recognizeAssetFileSpeechToText: ${responseBody.replaceAll('data:', '')}');
        return responseBody.replaceAll('data:', '');
      } else {
        return '';
      }
    } catch (e) {
      print('recognizeAssetFileSpeechToText error: $e');
      return '';
    }
  }

  /// 获得Bot针对语音文本笔记回复
  static Stream<String> getOrganizeToDoListNote(String msg) async* {
    final url = Uri.parse('$baseUrl/audio/getOrganizeToDoListNote');
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final body = {'sessionId': 0, 'deviceId': deviceId, 'messageContent': msg};
    final request = http.Request('POST', url);

    request.body = json.encode(body);
    request.headers.addAll({
      'accept': 'text/event-stream',
      'Content-Type': 'application/json',
      'device-id': deviceId,
    });

    var streamedResponse = await request.send();

    await for (var line in streamedResponse.stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())) {
      print('getOrganizeToDoListNote resp: $line');
      yield line;
    }
  }

  /// 手动保存语音讨论笔记内容-历史记录
  static Future<NoteModel?> updateNoteDiscussAudioHistory(
    int id,
    List<MessageModel> messages,
  ) async {
    // final deviceId = await KeyChainService().getDeviceId() ?? "";
    final endpoint = "/audio/updateNoteDiscussAudio";

    print('saveOrUpdateNoteDiscussAudio ${jsonDecode(jsonEncode(messages))}');

    final body = {
      // 'deviceId': deviceId,
      'id': id,
      // type 0-AI 1-用户
      'talkSnapshot': {'list': jsonDecode(jsonEncode(messages))},
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        print('saveOrUpdateNoteDiscussAudio $json');
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  /// 语音讨论完毕之后 ai 总结
  static Future<String> getNoteDiscussAudioAiSummary(
    int noteId,
    List<dynamic> content,
  ) async {
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    const url = "$baseUrl/audio/noteDiscussAudioAiSummary";
    final body = jsonEncode(content);
    print('saveOrUpdateNoteDiscussAudio $body');
    try {
      final response = await http.post(
        Uri.parse(url),
        headers: {
          'device-id': deviceId,
          'Content-Type': 'application/json; charset=utf-8',
        },
        body: body,
      );
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        print('saveOrUpdateNoteDiscussAudio $json');
        return json['总结内容'] ?? '';
      } else {
        print('请求失败: ${response.statusCode}');
        return '';
      }
    } catch (e) {
      print('请求失败: $e');
      return '';
    }
  }

  /// 获取笔记详情需要查询会议的语音讨论
  static Future<NoteModel?> getNoteDiscussAudioDetailById(
    int noteId,
    String content,
  ) async {
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final endpoint = "/audio/getNoteDiscussAudio";
    final body = {
      'sessionId': noteId,
      'deviceId': deviceId,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        String decodedResponse = utf8.decode(response.bodyBytes);
        final json = jsonDecode(decodedResponse);
        print('saveOrUpdateNoteDiscussAudio $json');
        return NoteModel.fromJson(json);
      } else {
        print('请求失败: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('请求失败: $e');
      return null;
    }
  }

  /// 删除语音讨论
  static Future<void> deleteNoteDiscussAudioById(
    int noteId,
  ) async {
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final endpoint = "/audio/deleteNoteDiscussAudio";
    final body = {
      'sessionId': noteId,
      'deviceId': deviceId,
    };
    try {
      final response = await HttpManager.post(endpoint, body: body);
      if (response.statusCode == 200) {
        return;
      } else {
        print('请求失败: ${response.statusCode}');
        return;
      }
    } catch (e) {
      print('请求失败: $e');
      return;
    }
  }
}

class HttpManager {
  static Future<http.Response> get(String endpoint,
      {Map<String, String>? params}) async {
    var uri = Uri.parse('$baseUrl$endpoint').replace(queryParameters: params);
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final response = await http.get(uri, headers: {
      'device-id': deviceId,
    });
    print("request: $uri");
    return _handleResponse(response);
  }

  static Future<http.Response> post(String endpoint,
      {Map<String, dynamic>? body}) async {
    var uri = Uri.parse('$baseUrl$endpoint');
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    final response = await http.post(uri, body: json.encode(body), headers: {
      'device-id': deviceId,
      'Content-Type': 'application/json; charset=utf-8',
    });
    print("request: $uri");
    return _handleResponse(response);
  }

  static Future<StreamedResponse> uploadFile(String endpoint, File file,
      {Map<String, dynamic>? body}) async {
    var uri = Uri.parse('$baseUrl$endpoint');
    final deviceId = await KeyChainService().getDeviceId() ?? "";
    print("deviceId ==== " + deviceId);

    var request = http.MultipartRequest('POST', uri);
    request.headers["device-id"] = deviceId;
    // request.fields.addAll({"device-id":deviceId});
    var mimeType = mime(file.path);
    var multipartFile = await http.MultipartFile.fromPath(
      'file',
      file.path,
      contentType: MediaType.parse(mimeType ?? "multipart/form-data"),
    );

    request.files.add(multipartFile);

    StreamedResponse streamedResponse = await request.send();
    if (streamedResponse.statusCode >= 200 &&
        streamedResponse.statusCode < 300) {
      return streamedResponse;
    } else {
      var bytes = await streamedResponse.stream.toBytes();
      final body = utf8.decode(bytes);
      throw Exception('HTTP请求失败，状态码: ${streamedResponse.statusCode},错误信息：$body');
    }
  }

  static Future<http.Response> delete(String endpoint) async {
    var uri = Uri.parse('$baseUrl$endpoint');
    final response = await http.delete(uri);
    print("request: $uri");
    return _handleResponse(response);
  }

  static http.Response _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return response;
    } else {
      throw Exception('HTTP请求失败，状态码: ${response.statusCode}');
    }
  }
}
