import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:ainote_app/app/utils/json_format.dart';

class AiAnalysisRecordModel {
  String? id;
  String? noteId;
  String? rawNote;
  int? version;
  String? overallReview;

  String? createdAt;
  String? updatedAt;

  List<TodoModel>? todoList;
  List<String>? tag;

  AiOrganizeModel? organizedNote;
  AiSuggestionModel? aiSuggestion;
  List<AiRelatedLinkModel>? relatedLinkList;
  List<AiRelatedTitleModel>? relatedTitleList;

  String? aiIllustration;
  String? aiAutoAnalysis;

  List<AiCategorizedNoteModel>? categorizedNote;

  AiAnalysisRecordModel({
    this.id,
    this.noteId,
    this.rawNote,
    this.version,
    this.overallReview,
    this.createdAt,
    this.updatedAt,
    this.todoList,
    this.tag,
    this.organizedNote,
    this.aiSuggestion,
    this.relatedLinkList,
    this.relatedTitleList,
    this.aiIllustration,
    this.categorizedNote,
    this.aiAutoAnalysis,
  });

  factory AiAnalysisRecordModel.empty() => AiAnalysisRecordModel();

  factory AiAnalysisRecordModel.mock() => AiAnalysisRecordModel(
        id: generateMockString(2),
        noteId: generateMockString(2),
        rawNote: generateMockString(20),
        version: -1,
        overallReview: generateMockString(10),
        createdAt: generateMockString(10),
        updatedAt: generateMockString(10),
        todoList: [TodoModel.mock()],
        tag: [generateMockString(5), generateMockString(5)],
        organizedNote: AiOrganizeModel.mock(),
        aiSuggestion: AiSuggestionModel.mock(),
        relatedLinkList: [AiRelatedLinkModel.mock(), AiRelatedLinkModel.mock()],
        relatedTitleList: [
          AiRelatedTitleModel.mock(),
          AiRelatedTitleModel.mock()
        ],
        aiIllustration: '',
        aiAutoAnalysis: '',
        categorizedNote: [
          AiCategorizedNoteModel(),
        ],
      );

  factory AiAnalysisRecordModel.fromJson(Map<String, dynamic> json) {
    AiSuggestionModel? aiSuggestion =
        parseAiSuggestionModelFromJsonString(json['aiSuggestion']);
    List<TodoModel>? todoList = [];
    try {
      todoList = json['todoList'] != null
          ? (json['todoList'] as List)
              .map<TodoModel>((i) => TodoModel.fromJson(i))
              .toList()
          : null;
    } catch (e) {}

    AiOrganizeModel? organizedNote;
    try {
      organizedNote = json['organizedNote'] != null
          ? AiOrganizeModel.fromJson(json['organizedNote'])
          : null;
    } catch (e) {}

    List<AiRelatedLinkModel> relatedLinkList = [];
    try {
      relatedLinkList = json['relatedLink'] != null
          ? json['relatedLink']
              .map<AiRelatedLinkModel>((e) => AiRelatedLinkModel.fromJson(e))
              .toList()
          : [];
    } catch (e) {}

    List<AiRelatedTitleModel> relatedTitleList = [];
    try {
      relatedTitleList = json['relatedTitle'] != null
          ? json['relatedTitle']
              .map<AiRelatedTitleModel>((e) => AiRelatedTitleModel.fromJson(e))
              .toList()
          : [];
    } catch (e) {}

    return AiAnalysisRecordModel(
      id: json['id'],
      noteId: json['noteId'],
      rawNote: json['rawNote'],
      version: json['version'],
      overallReview: json['overallReview'],
      createdAt: json['createdAt'],
      updatedAt: json['updatedAt'],
      todoList: todoList,
      tag: json['tag'] != null ? List<String>.from(json['tag']) : null,
      organizedNote: organizedNote,
      aiSuggestion: aiSuggestion,
      relatedLinkList: relatedLinkList,
      relatedTitleList: relatedTitleList,
      aiIllustration: json['aiIllustration'],
      aiAutoAnalysis: json['aiAutoAnalysis'],
      categorizedNote: json['categorizedNote']
          ?.map<AiCategorizedNoteModel>(
              (e) => AiCategorizedNoteModel.fromJson(e))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'noteId': noteId,
      'rawNote': rawNote,
      'version': version,
      'overallReview': overallReview,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'todoList': todoList?.map((x) => x.toJson()).toList(),
      'tag': tag,
      'organizedNote': organizedNote?.toJson(),
      'aiSuggestion': aiSuggestion?.toJson(),
      'relatedLinkList': relatedLinkList?.toList(),
      'relatedTitleList': relatedTitleList?.toList(),
      'aiIllustration': aiIllustration,
      'categorizedNote': categorizedNote?.map((x) => x.toJson()).toList(),
    };
  }
}
