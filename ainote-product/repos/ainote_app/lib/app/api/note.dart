import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:ainote_app/app/data/models/ai_organize_model.dart';
import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/data/models/note_module/note_module_payload.dart';
import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/utils/date_format.dart';
import 'package:ainote_app/app/data/models/note_module/text_data_module.dart';

import '../data/models/note_model.dart';
import 'api_urls.dart';
import 'my_dio.dart';

class NoteApi {
  // Same-note mutations execute in order so a slower old response cannot hide
  // modules saved by a newer action. Different notes remain independent.
  static final Map<String, Future<void>> _writes = {};
  static Future<Response<dynamic>> _postMutation(String path,
      {required Map<String, dynamic> data}) {
    final snapshot = jsonDecode(jsonEncode(data)) as Map<String, dynamic>;
    final id = snapshot['id'] as String?;
    if (id == null || id.isEmpty) return MyDio.postJSON(path, data: snapshot);
    final previous = _writes[id] ?? Future<void>.value();
    final result = previous.then((_) => MyDio.postJSON(path, data: snapshot));
    final tail =
        result.then<void>((_) {}, onError: (Object _, StackTrace __) {});
    _writes[id] = tail;
    tail.then((_) {
      if (identical(_writes[id], tail)) _writes.remove(id);
    });
    return result;
  }

  /// Replace just the text module; source images, tasks and other modules stay.
  static Future<NoteModel> replaceTextBody(NoteModel note, String delta) async {
    final rawModule = (note.modules ?? [])
        .whereType<Map>()
        .where((item) => item['noteModuleType'] == 'TEXT_DATA')
        .firstOrNull;
    if (rawModule == null ||
        rawModule['moduleId'] == null ||
        delta.trim().isEmpty) {
      throw StateError('Text module unavailable');
    }
    final module =
        TextDataModule.fromJson(Map<String, dynamic>.from(rawModule));
    module.content = delta;
    final response = await _postMutation(ApiUrls.noteModuleUpdate, data: {
      'id': note.id,
      'moduleId': module.moduleId,
      'module': module.toJson(),
    });
    return NoteModel.fromJson(response.data['data']);
  }

  static Future<NoteModel> createNote(NoteModel note) async {
    var resp = await MyDio.postJSON(
      ApiUrls.noteCreate,
      data: {
        "title": note.title,
        "content": note.content,
        "dimension": note.dimension,
        "noteType": note.noteType
      },
    );

    return NoteModel.fromJson(resp.data['data']);
  }

  // todo itemId
  static updateNoteModuleItem(NoteModel note, NoteModulePayload module,
      Map<String, dynamic> item) async {
    await _postMutation(
      ApiUrls.noteModuleItemUpdate,
      data: {
        "id": note.id,
        "moduleId": module.moduleId,
        "itemId": item['itemId'],
        "module": module.toJson()
      },
    );
  }

  static Future<NoteModel> updateNoteModule(
      NoteModel note, NoteModulePayload module) async {
    final response = await _postMutation(
      ApiUrls.noteModuleUpdate,
      data: {
        "id": note.id,
        "moduleId": module.moduleId,
        "module": module.toJson()
      },
    );
    return NoteModel.fromJson(response.data['data']);
  }

  static Future<NoteModel> addNoteModule(
      NoteModel note, NoteModulePayload module) async {
    var resp = await _postMutation(
      ApiUrls.noteModuleAdd,
      data: {"id": note.id, "module": module.toJson()},
    );

    return NoteModel.fromJson(resp.data['data']);
  }

  static updateNote(NoteModel note) async {
    await _postMutation(
      ApiUrls.noteUpdate,
      data: {"id": note.id, "title": note.title, "noteType": note.noteType},
    );
  }

  static Future<NoteModel> deleteNoteModule(
      String? noteId, String? moduleId) async {
    var resp = await _postMutation(
      ApiUrls.noteModuleDelete,
      data: {"id": noteId, "moduleId": moduleId},
    );

    return NoteModel.fromJson(resp.data['data']);
  }

  static getNoteDetail(String id) async {
    await MyDio.postJSON(
      ApiUrls.noteQueryDetailById(id),
    );
  }

  static deleteNote(String id) async {
    await MyDio.delete(
      ApiUrls.noteDeleteById(id),
    );
  }

  static Future<List<NoteModel>?> getNoteList({
    int page = 1,
    int size = 10,
    bool ascending = false,
    String sortBy = 'id', // 'id' | 'updatedAt'
    int? noteType,
    String? keyword,
    int? dimension = 0, // 维度， 0：用户维度，1：系统维度
  }) async {
    try {
      final resp = await MyDio.postJSON(ApiUrls.noteQueryList, data: {
        "keyword": keyword,
        "noteType": noteType,
        "page": page,
        "size": size,
        "ascending": ascending,
        "sortBy": sortBy,
        "dimension": 0
      });
      if (resp.data['data'] == null) {
        return null;
      }
      return resp.data['data']?['content']
          .map<NoteModel>((e) => NoteModel.fromJson(e))
          .toList();
    } catch (_) {
      return null;
    }
  }

  static getAllNote() async {
    final resp = await MyDio.postJSON(ApiUrls.todoQueryAll, data: {
      "keyword": "",
      "noteType": 0,
      "page": 1,
      "size": 10,
      "ascending": false,
      "sortBy": "id",
      "dimension": 0
    });
  }

  static Future<List<NoteTypeModel>> getNoteTypeList(
      [String typeName = "noteType"]) async {
    try {
      final resp = await MyDio.postJSON(ApiUrls.noteTypeDict, data: {
        'typeNames': [typeName],
      });

      var entryList = NodeTypeDictModel.fromJson(resp.data[0]).entryList;
      return entryList;
    } catch (_) {
      return [];
    }
  }

  static Future<NoteModel> replaceNote(
      {required NoteModel note,
      required AiOrganizeModel organizedNote,
      required List<IconTextActionModel> list}) async {
    var arr = [];
    for (var item in list) {
      if (item.emoji != null) {
        arr.add({
          "noteItemType": "AI_RECOMMENDATION",
          "suggestion": item.content ?? '',
          "emoji": item.emoji ?? '',
        });
      } else {
        arr.add({
          "noteItemType": "BACKLOG",
          "content": item.content,
          "description": item.description ?? '',
          "scheduledAt": parseToApiDateString(item.scheduledAt) ?? '',
          "needNotify": item.scheduledAt != null
        });
      }
    }
    var resp = await _postMutation(
      ApiUrls.noteReplace,
      data: {
        "id": note.id,
        "title": organizedNote.title ?? note.title,
        "organizedNote": organizedNote.bodyText,
        "noteModuleItems": arr
      },
    );

    return NoteModel.fromJson(resp.data['data']);
  }
}
