import 'dart:convert';

import 'package:ainote/models/note_model.dart';

import '../utils/utils.dart';
import 'categorized_note_model.dart';
import 'content_assist_model.dart';

class AIHistoryModel {
  int? id;
  String? rawNote;
  int? noteAnalysisId;
  int? version;
  String? organizedNoteText;
  /// analysisType: 1-一键整理，2-内容辅助，3-语音讨论
  int? analysisType;
  String? analysisTypeText;
  List<CategorizedNoteModel>? categorizedNotes;
  List<NoteModel>? relatedNotes;
  String? productRecommendations;
  String? imageLink;
  ContentAssistResultModel? assistedContent;
  String? createdAt;
  String? updatedAt;
  bool? empty;

  AIHistoryModel({
    this.id,
    this.rawNote,
    this.noteAnalysisId,
    this.version,
    this.organizedNoteText,
    this.analysisType,
    this.categorizedNotes,
    this.relatedNotes,
    this.productRecommendations,
    this.imageLink,
    this.assistedContent,
    this.createdAt,
    this.updatedAt,
    this.empty,
    this.analysisTypeText,
  });

  factory AIHistoryModel.fromRawString(String rawString) {
    if(rawString.startsWith('data:')) {
      rawString = rawString.substring(5);
    }
    var result = AIHistoryModel();
    try {
      var json = jsonDecode(rawString);
      result = AIHistoryModel.fromJson(json);
    } catch (e) {
      print('Error decoding JSON: $e');
    }
    return result;
  }

  factory AIHistoryModel.fromJson(Map<String, dynamic> theJson) {
    dynamic jsonCategorizedResponse = theJson['categorizedNotes'] == null
        ? null
        : json.decode(theJson['categorizedNotes']);

    List<CategorizedNoteModel>? categorizedNotesList;
    if (jsonCategorizedResponse is List) {
      categorizedNotesList = jsonCategorizedResponse
          .map((item) => CategorizedNoteModel.fromJson(item))
          .toList();
    } else if (jsonCategorizedResponse is Map<String, dynamic>) {
      categorizedNotesList = [CategorizedNoteModel.fromJson(jsonCategorizedResponse)];
    } else {
      categorizedNotesList = null;
    }

    //for related notes
    dynamic jsonRelatedResponse = theJson['relatedNotes'] == null
        ? null
        : json.decode(theJson['relatedNotes']);

    List<NoteModel>? relatedNotesList;
    if (jsonRelatedResponse is List) {
      relatedNotesList = jsonRelatedResponse
          .map((item) => NoteModel.fromJson(item))
          .toList();
    } else if (jsonRelatedResponse is Map<String, dynamic>) {
      relatedNotesList = [NoteModel.fromJson(jsonRelatedResponse)];
    } else {
      relatedNotesList = null;
    }

    int analysisType = theJson['analysisType'].toInt();
    String analysisTypeText = aiTypeToText(analysisType);
    return AIHistoryModel(
      id: theJson['id'] as int?,
      rawNote: theJson['rawNote'] as String?,
      noteAnalysisId: theJson['noteAnalysisId'] as int?,
      version: theJson['version'] as int?,
      organizedNoteText: theJson['organizedNoteText'] as String?,
      analysisType: theJson['analysisType'] as int?,
      categorizedNotes: categorizedNotesList,
      relatedNotes: relatedNotesList,
      productRecommendations: theJson['productRecommendations'] as String?,
      imageLink: theJson['imageLink'] as String?,
      assistedContent: ContentAssistResultModel.fromRawString(theJson['assistedContent']),
      createdAt: formatDateTime(theJson['createdAt']),
      updatedAt: formatDateTime(theJson['updatedAt']),
      empty: theJson['empty'] as bool?,
      analysisTypeText: analysisTypeText,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'rawNote': rawNote,
      'noteAnalysisId': noteAnalysisId,
      'version': version,
      'organizedNoteText': organizedNoteText,
      'analysisType': analysisType,
      'categorizedNotes': categorizedNotes?.map((e) => e.toJson()).toList(),
      'relatedNotes': relatedNotes?.map((e) => e.toJson()).toList(),
    };
  }

  static AIHistoryModel create(int type) {
    return AIHistoryModel(
      analysisType: type,
    );
  }
}

List<AIHistoryModel> formatRawHistData(str) {
  var list = str.split('data:');
  list.removeAt(0);
  List<AIHistoryModel> result = [];
  list.forEach((element) {
    element = element.trim();
    try {
      var json = jsonDecode(element);
      result.add(AIHistoryModel.fromJson(json));
    } catch (e) {
      print('Error decoding JSON: $e');
    }
  });
  return result;
}