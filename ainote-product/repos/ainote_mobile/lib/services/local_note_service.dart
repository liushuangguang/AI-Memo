import 'dart:convert';

import 'package:ainote/models/note_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LocalNoteService {
  LocalNoteService._privateConstructor();

  static final LocalNoteService instance =
      LocalNoteService._privateConstructor();

  SharedPreferences? _prefs;

  Future<void> init() async {
    try {
      _prefs = await SharedPreferences.getInstance();
    } catch (e) {
      print("SharedPreferences init error: $e");
    }
  }

  List<NoteModel> getAllNotes() {
    final notesJson = _prefs?.getString('notes');
    if (notesJson == null || notesJson.isEmpty) {
      return [];
    }

    final List<dynamic> noteList = json.decode(notesJson);
    return noteList.map((notesJson) => NoteModel.fromJson(notesJson)).toList();
  }

  void saveAllNotes(List<NoteModel> notes) {
    _prefs?.setString('notes', json.encode(notes));
  }

  void add(NoteModel note) {
    final notes = getAllNotes();
    notes.add(note);
    _prefs?.setString('notes', json.encode(notes));
  }

  void remove(NoteModel note) {
    final notes = getAllNotes();
    notes.removeWhere((element) => element.id == note.id);
    _prefs?.setString('notes', json.encode(notes));
  }

  void update(String noteId, String title, String content) {
    final notes = getAllNotes();
    final index =
        notes.indexWhere((element) => element.id.toString() == noteId);
    if (index == -1) {
      return;
    }
    notes[index].title = title;
    notes[index].noteAnalysisContent = content;
    _prefs?.setString('notes', json.encode(notes));
  }

  bool getIsFirstUseVoice() {
    var isFirstUseVoice = _prefs?.getBool('isFirstUseVoice') ?? true;
    if (isFirstUseVoice) {
      setIsFirstUseVoice(false);
    }
    return isFirstUseVoice;
  }

  void setIsFirstUseVoice(bool isFirstUseVoice) {
    _prefs?.setBool('isFirstUseVoice', isFirstUseVoice);
  }
}
