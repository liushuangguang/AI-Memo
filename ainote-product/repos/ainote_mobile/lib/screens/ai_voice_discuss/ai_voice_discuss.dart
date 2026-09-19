import 'dart:async';
import 'dart:convert';
import 'dart:ui';

import 'package:ainote/models/message_model.dart';
import 'package:ainote/services/api_service.dart';
import 'package:animate_do/animate_do.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:get/get.dart';

import '../../models/note_model.dart';
import '../../models/todo_model.dart';
import '../../models/voice_todo_msg_model.dart';
import '../../services/local_note_service.dart';
import '../widgets/todo_result_item.dart';
import 'ai_voice_btn_group.dart';
import 'message/message_failed.dart';
import 'message/message_list.dart';
import 'message/message_logic.dart';
import 'message/message_thinking.dart';

class AiVoiceDiscuss extends StatefulWidget {
  final NoteModel? noteModel;
  final Future<bool> Function(List<TodoModel> todo)? onSaveTodo;
  final Future<bool> Function(String text)? onSaveNote;

  const AiVoiceDiscuss(
      {super.key, this.onSaveTodo, this.onSaveNote, this.noteModel});

  @override
  State<AiVoiceDiscuss> createState() => _AiVoiceDiscussState();
}

class _AiVoiceDiscussState extends State<AiVoiceDiscuss> {
  MessageLogic messageLogic = Get.put(MessageLogic());

  final flutterTts = FlutterTts();

  Future<void> initTextToSpeech() async {
    await flutterTts.setSharedInstance(true);

    flutterTts.setStartHandler(() {
      print('[ai speaking start]');
      messageLogic.setIsSpeaking(true);
    });

    flutterTts.setCompletionHandler(() {
      print('[ai speaking completed]');
      messageLogic.setIsSpeaking(false);
    });

    flutterTts.setCancelHandler(() {
      print('[ai speaking cancelled]');
      messageLogic.setIsSpeaking(false);
    });

    flutterTts.setPauseHandler(() {
      print('[ai speaking paused]');
      messageLogic.setIsSpeaking(false);
    });

    flutterTts.setContinueHandler(() {
      print('[ai speaking continued]');
      messageLogic.setIsSpeaking(true);
    });

    flutterTts.setErrorHandler((msg) {
      print('[ai speaking failed]');
      messageLogic.setIsSpeaking(false);
    });
  }

  Future<void> systemSpeak(String content) async {
    await flutterTts.speak(content);
    messageLogic.setIsSpeaking(true);
  }

  Future<void> stopSpeak() async {
    var result = await flutterTts.stop();
    if (result == 1) {
      messageLogic.setIsSpeaking(false);
    }
  }

  String audioSrc =
      'https://dl.solahangs.com/Music/1403/02/H/128/Hiphopologist%20-%20Shakkak%20%28128%29.mp3';
  bool isFileAudio = false;

  List<Widget> messages = [];

  final ScrollController _scrollController = ScrollController();
  bool _isUserScrolling = false;

  void updateMsgList(Widget widget) {
    setState(() {
      messages.add(widget);
    });

    if (!_isUserScrolling) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 500),
        curve: Curves.easeOut,
      );
    }
  }

  @override
  void initState() {
    super.initState();
    initTextToSpeech();
    messageLogic.clearMessage();
    messageLogic.setNoteModel(widget.noteModel);

    _scrollController.addListener(() {
      // 监听滚动位置的变化
      if (_scrollController.position.userScrollDirection ==
          ScrollDirection.reverse) {
        // 用户向上滚动
        _isUserScrolling = true;
      } else if (_scrollController.position.userScrollDirection ==
          ScrollDirection.forward) {
        // 用户向下滚动
        _isUserScrolling = true;
      }

      if (_scrollController.offset >=
          _scrollController.position.maxScrollExtent - 50) {
        // 滚动到底部
        _isUserScrolling = false;
      }
    });

    Timer(const Duration(milliseconds: 500), () {
      var isFirstUse = LocalNoteService.instance.getIsFirstUseVoice();
      String content = isFirstUse
          ? '你好，我是你的ai语音助手，想和我讨论些什么呢？'
              '\n你可以使用语音输入或者文本输入和我进行对话哦~'
          : '你好，想和我讨论些什么呢？';
      systemSpeak(content);
      messageLogic.addMessage(MessageModel(
        content: content,
        msgType: 0,
        userType: 0,
      ));
    });
  }

  void _scrollToBottom() {
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent + 80,
      duration: const Duration(milliseconds: 1000),
      curve: Curves.easeOut,
    );
  }

  String _lastMsg = '';
  bool _hasFailed = false;

  void _handleFailed() {
    messageLogic.addMessageWidget(MessageFailed(onTap: () {
      _hasFailed = false;
      messageLogic.popMessageWidget();
      _getAiResponse(_lastMsg);
    }));
    _hasFailed = true;
  }

  bool _hasEmpty = false;

  void _popEmptyMsg() {
    if (_hasEmpty) {
      messageLogic.popMessageWidget();
    }
    _hasEmpty = false;
  }

  Future<void> _handleTextMsg(String msg) async {
    _popEmptyMsg();
    _hasEmpty = msg.isEmpty;
    _lastMsg = msg;
    var content = msg.isEmpty ? '哎呀，你说的话我没有听清楚，你可以在重复一次么~' : msg;
    if (msg.isEmpty) {
      systemSpeak(content);
    }
    messageLogic.addMessage(MessageModel(
      content: content,
      msgType: 0,
      userType: msg.isEmpty ? 1 : 2,
    ));
    if (msg.isEmpty) {
      return;
    }
    await _getAiResponse(msg);
  }

  bool isThinking = false;

  Future<void> _getAiResponse(String msg) async {
    setState(() {
      isThinking = true;
    });
    messageLogic.addMessageWidget(const MessageThinking());
    _scrollToBottom();
    var res = APIService.getOrganizeToDoListNote(msg);

    var rawString = '';
    await for (var line in res) {
      rawString += line.replaceAll("data:", "");
      print('rawString: $rawString');
    }

    var result = VoiceTodoMsgModel.fromRawString(rawString);

    messageLogic.popMessageWidget();

    bool isAllEmpty = true;
    if (result.message != null && result.message!.isNotEmpty) {
      isAllEmpty = false;
      systemSpeak(result.message!);
      messageLogic.addMessage(MessageModel(
        content: result.message!,
        msgType: 0,
        userType: 1,
      ));
    }

    if (result.note != null && result.note!.isNotEmpty) {
      isAllEmpty = false;
      messageLogic.addMessage(
        MessageModel(
            content: result.note!,
            msgType: 2,
            onTap: () {
              widget.onSaveNote?.call(result.note!);
            }),
      );
    }

    if (result.todos != null && result.todos!.isNotEmpty) {
      messageLogic.addMessageWidget(
        FadeInUp(
          duration: const Duration(milliseconds: 500),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: TodoResultItem(
                todos: result.todos!,
                onSave: (todos) {
                  widget.onSaveTodo?.call(todos);
                  return Future(() => true);
                }),
          ),
        ),
      );

      isAllEmpty = false;
      // messageLogic.addMessage(MessageModel(
      //   content: jsonEncode(result.todos),
      //   msgType: 3,
      // ));
    }

    if (isAllEmpty) {
      _handleFailed();
    }

    Future.delayed(const Duration(milliseconds: 500), _scrollToBottom);

    setState(() {
      isThinking = false;
    });
  }

  @override
  void deactivate() {
    messages.clear();
    super.deactivate();
  }

  @override
  void dispose() {
    messages.clear();
    _scrollController.dispose();
    flutterTts.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 10.0, sigmaY: 10.0),
      child: Scaffold(
        backgroundColor: const Color.fromRGBO(243, 245, 248, 0.5),
        appBar: AppBar(
          title: Text('语音讨论',
              style: TextStyle(
                  color: primaryColor,
                  fontWeight: FontWeight.w700,
                  fontSize: 20)),
          centerTitle: true,
          leading: GestureDetector(
              onTap: () {
                Navigator.of(context).pop();
              },
              child: const Icon(Icons.close_rounded)),
        ),
        body: Padding(
          padding:
              const EdgeInsets.only(left: 16, right: 16, bottom: 150, top: 32),
          child: SingleChildScrollView(
            controller: _scrollController,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 150),
              child: MessageList(),
            ),
          ),
        ),
        floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
        floatingActionButton: Stack(children: [
          Padding(
            padding: const EdgeInsets.only(top: 48),
            child: AiVoiceBtnGroup(
              disabled: isThinking,
              onMessage: _handleTextMsg,
              onStop: stopSpeak,
            ),
          ),
        ]),
      ),
    );
  }
}
