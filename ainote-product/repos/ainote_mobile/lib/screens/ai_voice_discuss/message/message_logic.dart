import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../models/message_model.dart';
import '../../../models/note_model.dart';

class MessageLogic extends GetxController {
  NoteModel? noteModel = NoteModel();
  List<MessageModel> messageModelList = [];
  List<Widget> messageWidgets = [];

  // 0 初始状态，1 说话中 2 聆听中
  int aiStatus = 0;

  bool isSpeaking = false;

  String summary = '';
  bool summaryLoading = false;

  void setSummaryLoading(bool loading) {
    summaryLoading = loading;
    update();
  }

  void setSummary(String summary) {
    this.summary = summary;
    update();
  }

  void setNoteModel(NoteModel? noteModel) {
    noteModel = noteModel;
    update();
  }

  void addMessage(MessageModel message) {
    messageModelList.add(message);
    messageWidgets.add(message.toWidget());
    update();
  }

  void addMessageWidget(Widget message) {
    messageWidgets.add(message);
    update();
  }

  void removeMessageWidget(Widget message) {
    messageWidgets.remove(message);
    update();
  }

  void popMessageWidget() {
    messageWidgets.removeLast();
    update();
  }

  void removeMessageWidgetAt(int index) {
    messageWidgets.removeAt(index);
    update();
  }

  void clearMessage() {
    messageWidgets.clear();
    messageModelList.clear();
    update();
  }

  void setAiStatus(int status) {
    aiStatus = status;
    update();
  }

  void setIsSpeaking(bool isSpeaking) {
    this.isSpeaking = isSpeaking;
    setAiStatus(isSpeaking ? 1 : 2);
    update();
  }
}
