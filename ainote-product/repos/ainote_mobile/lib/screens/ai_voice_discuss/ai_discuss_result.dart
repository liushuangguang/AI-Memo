import 'package:ainote/constants/app_images.dart';
import 'package:ainote/screens/ai_voice_discuss/ai_voice_result_card.dart';
import 'package:ainote/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../../models/todo_model.dart';
import '../../ui/primary_btn.dart';
import '../../ui/secondary_btn.dart';
import 'ai_voice_result_content.dart';
import 'message/message_logic.dart';

class AiDiscussResult extends StatefulWidget {
  final VoidCallback? onDelete;
  final Function(String summary) onSaveSummaryToNote;
  final Future<bool> Function({required List<TodoModel> todo, required String note})? onSaveAll;

  const AiDiscussResult({super.key, this.onDelete, this.onSaveAll, required this.onSaveSummaryToNote});

  @override
  State<AiDiscussResult> createState() => _AiDiscussResultState();
}

class _AiDiscussResultState extends State<AiDiscussResult> {
  MessageLogic messageLogic = Get.put(MessageLogic());
  bool hasSaved = false;
  String savedSummary = '';

  void _getSummary() async {
    messageLogic.setSummary('');
    messageLogic.setSummaryLoading(true);
    var result = await APIService.getNoteDiscussAudioAiSummary(messageLogic.noteModel?.id ?? 0, messageLogic.messageModelList);
    messageLogic.setSummary(result);
    messageLogic.setSummaryLoading(false);
  }

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Color primaryColor = Theme.of(context).primaryColor;

    void _handleSaveSummary() {
      setState(() {
        hasSaved = true;
        savedSummary = messageLogic.summary;
      });
      Navigator.of(context).pop();
    }

    void _handleAiVoiceSummary() async {
      _getSummary();
      showCupertinoModalBottomSheet(
        context: context,
        enableDrag: false,
        topRadius: const Radius.circular(20),
        builder: (context) => Container(
          padding: const EdgeInsets.all(24),
          color: const Color(0xFFF3F5F8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Align(
                alignment: Alignment.center,
                child: Text(
                  'AI总结',
                  style: TextStyle(
                      fontSize: 18,
                      color: primaryColor,
                      fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(
                height: 12,
              ),
              ConstrainedBox(
                constraints: BoxConstraints(
                  // 获取屏幕高度
                  maxHeight: MediaQuery.of(context).size.height -
                      (kToolbarHeight + 220),
                ),
                child: GetBuilder<MessageLogic>(builder: (logic) => AiVoiceResultContent(
                  loading: logic.summaryLoading,
                  content: logic.summary,
                  onTap: _getSummary,
                )),
              ),
              const SizedBox(
                height: 20,
              ),
              SizedBox(
                height: 54,
                width: double.infinity,
                child: PrimaryBtn(
                  text: '保存',
                  onPressed: _handleSaveSummary,
                ),
              ),
              const SizedBox(
                height: 12,
              ),
              SizedBox(
                height: 54,
                width: double.infinity,
                child: SecondaryBtn(
                  text: '不保存',
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                ),
              ),
            ],
          ),
        ),
      );
    }

    void _handleSaveAll() async {
      widget.onSaveAll?.call(
        todo: [],
        note: '',
      );
    }

    return SafeArea(
      bottom: false,
      child: Container(
          margin: const EdgeInsets.only(top: 40.0),
          child: Scaffold(
            backgroundColor: Colors.transparent,
            body: Container(
              height: MediaQuery.of(context).size.height - 40,
              width: MediaQuery.of(context).size.width,
              color: const Color(0xFFF3F5F8),
              child: SingleChildScrollView(
                child: Padding(
                    padding: const EdgeInsets.only(
                        top: 16, left: 16, right: 16, bottom: 220),
                    child: Column(children: [
                      AiVoiceResultCard(onDelete: widget.onDelete),
                      Visibility(
                        visible: !hasSaved,
                        child: TextButton(
                            onPressed: _handleAiVoiceSummary,
                            child: const Text(
                              '总结语音讨论内容',
                              style: TextStyle(
                                  color: Color(0xFF3A51FF),
                                  fontSize: 16,
                                  fontWeight: FontWeight.w500),
                            )),
                      ),
                      const SizedBox(height: 16),
                      Visibility(
                        visible: hasSaved,
                        child: AiVoiceResultContent(
                          onSave: widget.onSaveSummaryToNote,
                          onTap: _handleAiVoiceSummary,
                          content: savedSummary,),
                      ),
                    ])),
              ),
            ),
            floatingActionButtonLocation:
                FloatingActionButtonLocation.centerDocked,
            floatingActionButton: InkWell(
              onTap: () {
                _handleSaveAll();
              },
              child: Container(
                width: 160,
                height: 48,
                margin: const EdgeInsets.only(bottom: 150),
                decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0xFFD9D9D9),
                        blurRadius: 12,
                        offset: Offset(0, 4),
                      ),
                    ]),
                child:
                    Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                  SvgPicture.asset(AppImages.aiVoiceFileSaveIcon.path),
                  const SizedBox(width: 8),
                  const Text(
                    '全部存入正文',
                    style: TextStyle(
                        fontSize: 16,
                        color: Color(0xFF3A51FF),
                        fontWeight: FontWeight.w500),
                  )
                ]),
              ),
            ),
          )),
    );
  }
}
