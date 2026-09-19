import 'dart:convert';
import 'dart:async';

import 'package:ainote_app/app/data/models/ai_analysis_record_model.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_note_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/content_assist_model.dart';
import 'package:ainote_app/app/data/models/complete_info_model.dart';
import 'package:ainote_app/app/utils/json_format.dart';
import 'package:flutter/foundation.dart';
import 'package:dio/dio.dart';
import 'package:dio_smart_retry/dio_smart_retry.dart';

import 'api_urls.dart';
import 'my_dio.dart';

Stream<String> decodeAiSuggestionLines(Stream<List<int>> bytes) =>
    utf8.decoder.bind(bytes).transform(const LineSplitter()).map(trimData);

class AiApi {
  // A blocking workflow can spend 90s in Dify followed by 90s in the
  // configured fallback. The normal 30s HTTP budget is too short for it.
  // Do not automatically replay billable generation on a slow response.
  static Options _generationOptions() => Options(
        connectTimeout: const Duration(seconds: 15),
        sendTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 200),
      )..disableRetry = true;

  static AiValidateModel _validatedNote(dynamic envelope) {
    final raw = envelope is Map ? envelope['data'] : null;
    if (raw is! Map) throw const FormatException('Invalid note validation');
    final result = AiValidateModel.fromJson(Map<String, dynamic>.from(raw));
    if (result.meaningful == null ||
        (result.meaningful == true && result.recordId?.trim().isNotEmpty != true)) {
      throw const FormatException('Incomplete note validation');
    }
    return result;
  }

  static Future<AiAnalysisRecordModel?> noteAnalysisRecordLatest(
      String? id) async {
    final resp = await MyDio.get(
      ApiUrls.noteAnalysisRecordLatest,
      queryParameters: {"noteId": id},
    );
    AiAnalysisRecordModel? respData;
    try {
      respData = AiAnalysisRecordModel.fromJson(resp.data);
    } catch (_) {}
    return respData;
  }

  static Future<AiValidateModel> aiValidateNote(String? id) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiValidateNote,
      data: {"id": id},
      options: _generationOptions(),
    );
    return _validatedNote(resp.data);
  }

  // aiValidateNote --> recordId
  static Future<AiOrganizeModel> aiOrganizeNote({String? recordId}) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiOrganizeNote,
      data: {"recordId": recordId},
      options: _generationOptions(),
    );
    final raw = resp.data is Map ? resp.data['data'] : null;
    if (raw is! Map) throw const FormatException('Invalid organized note');
    final result = AiOrganizeModel.fromJson(Map<String, dynamic>.from(raw));
    if (result.bodyText?.trim().isNotEmpty != true) {
      throw const FormatException('Organized note has no body');
    }
    return result;
  }

  // aiValidateNote -->  recordId --> aiOrganizeNote
  static Stream<String> aiSuggestion({
    String? recordId,
  }) async* {
    final Stream<Uint8List> resp = await MyDio.postStream(
      ApiUrls.aiSuggestion,
      data: {"recordId": recordId},
      options: _generationOptions(),
    );
    await for (final item in decodeAiSuggestionLines(resp)) {
      yield item;
    }
  }

  static Future<Stream<Uint8List>?> aiSuggestionStream({
    String? recordId,
  }) async {
    try {
      final Stream<Uint8List> resp = await MyDio.postStream(
        ApiUrls.aiSuggestion,
        data: {"recordId": recordId},
        options: _generationOptions(),
      );
      return resp;
    } catch (e) {}
    return null;
  }

  // aiValidateNote --> recordId
  static Future<List<AiRelatedTitleModel>> aiRelatedTitle(
      {String? recordId, String? text}) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiRelatedTitle,
      data: {"recordId": recordId, "specificContent": text},
      options: _generationOptions(),
    );
    List<AiRelatedTitleModel> aiRelatedTitleList = [];
    try {
      aiRelatedTitleList = resp.data['data']
          .map<AiRelatedTitleModel>((e) => AiRelatedTitleModel.fromJson(e))
          .toList();
    } catch (_) { throw const FormatException('Invalid related titles'); }

    // Fetch detail on demand. Background prefetch used to replay billable work
    // on every refresh and leak unhandled errors when the provider was slow.
    return aiRelatedTitleList;
  }

  // aiValidateNote --> recordId
  static Future<List<AiRelatedLinkModel>> aiRelatedLink(
      {String? recordId, String? text}) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiRelatedLink,
      data: {"recordId": recordId, "specificContent": text},
      options: _generationOptions(),
    );

    List<AiRelatedLinkModel> aiRelatedLinkModelList = [];
    try {
      aiRelatedLinkModelList = resp.data['data']
          .map<AiRelatedLinkModel>((e) => AiRelatedLinkModel.fromJson(e))
          .toList();
    } catch (_) { throw const FormatException('Invalid related links'); }
    return aiRelatedLinkModelList;
  }

  static Future<List<AiRelatedNoteModel>> aiRelatedNote({
    required String recordId,
  }) async {
    final normalizedRecordId = recordId.trim();
    if (normalizedRecordId.isEmpty) {
      throw ArgumentError.value(recordId, 'recordId', 'must not be blank');
    }
    final options = Options(
      connectTimeout: const Duration(seconds: 15),
      sendTimeout: const Duration(seconds: 15),
      // The backend applies a 75-second end-to-end semantic-analysis limit.
      // Leave a small transport margin while keeping retries disabled below.
      receiveTimeout: const Duration(seconds: 80),
    )..disableRetry = true;
    final resp = await MyDio.postJSON(
      ApiUrls.aiRelatedNote,
      data: {'recordId': normalizedRecordId},
      options: options,
    );
    final rawItems = resp.data is Map ? resp.data['data'] : null;
    if (rawItems is! List) {
      throw const FormatException('Invalid relatedNotes response');
    }
    final items = <AiRelatedNoteModel>[];
    for (final rawItem in rawItems) {
      if (rawItem is! Map) continue;
      try {
        final item =
            AiRelatedNoteModel.fromJson(Map<String, dynamic>.from(rawItem));
        if (item.isUsable) items.add(item);
      } catch (_) {
        // A mixed response may contain malformed entries. Preserve any usable
        // entries, but reject a wholly malformed non-empty response below.
      }
    }
    if (rawItems.isNotEmpty && items.isEmpty) {
      throw const FormatException(
        'Invalid relatedNotes items: non-empty data contained no usable note',
      );
    }
    return items;
  }

  // aiValidateNote --> recordId
  static Future<bool?> aiAutoAnalysis({
    String? noteId,
  }) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiAutoAnalysis,
      data: {"noteId": noteId},
    );

    try {
      return resp.data['data'];
    } catch (e) {}
    return null;
  }

  // aiValidateNote --> recordId
  static var aiRelatedInfoCached = <String, dynamic>{};

  static Future<String> aiRelatedInfo(String? text) async {
    if (aiRelatedInfoCached[text] != null) return aiRelatedInfoCached[text];
    final resp = await MyDio.postJSON(
      ApiUrls.aiRelatedInfo,
      queryParameters: {"textContent": text},
      options: _generationOptions(),
    );

    try {
      String result = resp.data['data'];
      aiRelatedInfoCached[text ?? ''] = result;
      return result;
    } catch (_) { throw const FormatException('Invalid related detail'); }
  }

  // aiValidateNote --> recordId
  static Future<List<AiCategorizedNoteModel>> aiCategorizedNote({
    String? recordId,
  }) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiCategorizedNote,
      data: {"recordId": recordId},
      options: _generationOptions(),
    );
    List<AiCategorizedNoteModel> aiCategorizedNoteList = [];
    try {
      aiCategorizedNoteList = resp.data['data']
          .map<AiCategorizedNoteModel>((e) =>
              AiCategorizedNoteModel.fromJson(e['categorizedNoteModule']))
          .toList();
    } catch (_) { throw const FormatException('Invalid categories'); }

    return aiCategorizedNoteList;
  }

  // aiValidateNote --> recordId
  static Future<String?> aiIllustration(
      {String? recordId, String? text}) async {
    final resp = await MyDio.postJSON(
      ApiUrls.aiIllustration,
      data: {"recordId": recordId, "specificContent": text},
      options: _generationOptions(),
    );
    try {
      return resp.data['data'];
    } catch (e) {}
    return null;
  }

  // contentAssist 1
  static Future<AiValidateModel> validateContentAssist(String? id) async {
    final resp = await MyDio.postJSON(
      ApiUrls.contentAssistValidate,
      data: {"id": id},
      options: _generationOptions(),
    );
    return _validatedNote(resp.data);
  }

  // contentAssist 2
  static Future<ContentAssistDirectionModel> getAssistantDirection(
      String? recordId) async {
    final resp = await MyDio.postJSON(
      ApiUrls.selectedAssistantDirection,
      data: {"noteId": recordId},
      options: _generationOptions(),
    );
    ContentAssistDirectionModel contentAssistDirectionModel =
        ContentAssistDirectionModel();
    try {
      contentAssistDirectionModel =
          ContentAssistDirectionModel.fromJson(resp.data['data']);
    } catch (e) {}
    return contentAssistDirectionModel;
  }

  // contentAssist 3
  static Future<ContentAssistResultModel> startRewriteContent(
      {String? recordId,
      List<String>? assistDirections,
      String? selectedContent}) async {
    final trimmedSelectedContent = selectedContent?.trim();
    final resp = await MyDio.postJSON(
      ApiUrls.rewriteContent,
      options: _generationOptions(),
      data: {
        "recordId": recordId,
        'assistDirections': assistDirections,
        "selectedContent": trimmedSelectedContent?.isEmpty == true
            ? null
            : trimmedSelectedContent
      },
    );
    final result = ContentAssistResultModel.fromJson(resp.data['data']);
    if (result.rewrittenContent.trim().isEmpty) {
      throw const FormatException('Empty rewrite result');
    }
    return result;
  }

  static Future<List<CompleteInfoModel>> completeInfo(String content) async {
    final trimmedContent = content.trim();
    if (trimmedContent.isEmpty) return [];

    final resp = await MyDio.postJSON(
      ApiUrls.completeInfo,
      data: {'content': trimmedContent},
      options: _generationOptions(),
    );
    final responseData = resp.data;
    final dynamic rawItems = responseData is List
        ? responseData
        : responseData is Map
            ? responseData['data']
            : null;
    if (rawItems is! List) {
      throw const FormatException('Invalid completeInfo response');
    }

    final items = rawItems
        .whereType<Map>()
        .map((item) =>
            CompleteInfoModel.fromJson(Map<String, dynamic>.from(item)))
        .where((item) => item.isUsable)
        .toList();
    if (rawItems.isNotEmpty && items.isEmpty) {
      throw const FormatException('Invalid completeInfo items');
    }
    return items;
  }
}
