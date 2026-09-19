import 'note_module_payload.dart';

class NoteModuleRequest {
  // '笔记id'
  String? id;

  // '笔记模块id'
  String? moduleId;

  // '笔记模块'
  NoteModulePayload? module;

  NoteModuleRequest({this.id, this.moduleId, this.module});
}
