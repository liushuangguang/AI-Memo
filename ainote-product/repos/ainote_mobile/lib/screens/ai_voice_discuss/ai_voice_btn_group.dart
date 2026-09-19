import 'dart:async';

import 'package:ainote/screens/ai_voice_discuss/recorder_wave.dart';
import 'package:ainote/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:get/get.dart';
import 'package:lottie/lottie.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../../constants/app_images.dart';
import '../../constants/lottie_data.dart';
import 'ai_voice_act_btn.dart';
import 'ai_voice_recorder.dart';
import 'message/message_logic.dart';

const List<String> aiStatusTexts = ['', '说话中', '聆听中'];

class AiVoiceBtnGroup extends StatelessWidget {
  final bool disabled;
  final VoidCallback onStop;
  final Future<void> Function(String msg) onMessage;

  const AiVoiceBtnGroup({
    super.key,
    required this.onMessage,
    required this.onStop,
    required this.disabled,
  });

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    // void handleStop(String path) async {
    //   var result = await APIService.recognizeSpeechToText(filePath: path);
    //   // var result = await APIService.recognizeAssetFileSpeechToText(fileName: 'test.pcm');
    //   await onMessage(result);
    // }

    void handleStop() async {
      if (disabled) {
        Fluttertoast.showToast(msg: '正在识别中');
      } else {
        onStop();
      }
    }

    void _handleTextInput() {
      handleStop();
      if (disabled) {
        return;
      }
      showModalBottomSheet(
          context: context,
          builder: (context) => Container(
                padding: EdgeInsets.only(
                  left: 16.0,
                  right: 16.0,
                  top: 16.0,
                  bottom: MediaQuery.of(context).viewInsets.bottom,
                ),
                decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(16.0),
                      topRight: Radius.circular(16.0),
                    )),
                child: TextField(
                  decoration: const InputDecoration(
                      hintText: '请输入问题', border: InputBorder.none),
                  autofocus: true,
                  // 监听 enter
                  onEditingComplete: () {
                    Navigator.pop(context);
                  },
                  onSubmitted: (text) {
                    if (text.isNotEmpty) {
                      onMessage(text);
                    } else {
                      EasyLoading.showToast('请输入内容');
                    }
                  },
                ),
              ));
    }

    void _showMicroDialog() {
      handleStop();
      if (disabled) {
        return;
      }
      showCupertinoModalBottomSheet(
          topRadius: const Radius.circular(40),
          context: context,
          backgroundColor: Colors.transparent,
          builder: (context) => Container(
                padding: const EdgeInsets.all(24),
                color: Colors.black.withOpacity(0.66),
                child: Column(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      RecorderWave(
                        onStop: onMessage,
                      ),
                    ]),
              ));
    }

    return GetBuilder<MessageLogic>(
      init: MessageLogic(),
      builder: (logic) {
        var isSpeaking = logic.isSpeaking;
        var aiStatus = logic.aiStatus;
        return Container(
          margin: const EdgeInsets.only(left: 16, right: 16, bottom: 16),
          height: 110,
          width: double.infinity,
          decoration: BoxDecoration(
              color: primaryColor,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                    color: const Color(0xFF414141).withOpacity(0.25),
                    offset: const Offset(0, 4),
                    blurRadius: 24,
                    spreadRadius: 0),
              ]),
          child: Flex(
              direction: Axis.horizontal,
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.only(left: 16),
                  child: Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SvgPicture.asset(
                          AppImages.aiVoiceTalkIcon.path,
                          height: 68,
                        ),
                        // if (aiStatus == 2)
                        //   Lottie.asset(LottieData.mic.path,
                        //       height: 68,
                        //       width: 68,
                        //       repeat: true,
                        //       filterQuality: FilterQuality.high),
                        if (aiStatus == 1)
                          Lottie.asset(LottieData.play.path,
                              height: 44,
                              width: 44,
                              repeat: true,
                              filterQuality: FilterQuality.high),
                        const SizedBox(width: 8),
                        Visibility(
                          visible: false,
                          child: Text(
                            // _paused ? '文本对话' : aiStatusTexts[aiStatus],
                            aiStatusTexts[aiStatus],
                            style: const TextStyle(
                                color: Colors.white,
                                fontSize: 20,
                                fontWeight: FontWeight.w700),
                          ),
                        )
                      ]),
                ),
                SizedBox(
                  width: MediaQuery.of(context).size.width - 200,
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Padding(
                      padding: const EdgeInsets.only(right: 16),
                      child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            if (isSpeaking) ...[
                              AiVoiceActBtn(
                                onTap: handleStop,
                                iconData: Icons.stop_rounded,
                                color: const Color(0xFF3A51FF),
                                text: '打断',
                              ),
                              const SizedBox(width: 8),
                              const VerticalDivider(
                                color: Color.fromRGBO(255, 255, 255, 0.3),
                                thickness: 1,
                                indent: 45,
                                endIndent: 45,
                              ),
                              const SizedBox(width: 8),
                            ],
                            AiVoiceActBtn(
                              onTap: () {
                                Navigator.pop(context);
                              },
                              icon: SvgPicture.asset(
                                AppImages.aiVoiceOffIcon.path,
                                width: 48,
                              ),
                              color: const Color(0xFFF5222D),
                              text: '结束',
                            ),
                            const SizedBox(width: 4),
                            const VerticalDivider(
                              color: Color.fromRGBO(255, 255, 255, 0.3),
                              thickness: 1,
                              indent: 45,
                              endIndent: 45,
                            ),
                            const SizedBox(width: 4),
                            AiVoiceActBtn(
                              onTap: _showMicroDialog,
                              icon: const Icon(
                                Icons.mic,
                                size: 26,
                                color: Color(0xFFFFFFFF),
                              ),
                              color: const Color(0xFF3A51FF),
                              text: '语音',
                            ),
                            const SizedBox(width: 4),
                            const VerticalDivider(
                              color: Color.fromRGBO(255, 255, 255, 0.3),
                              thickness: 1,
                              indent: 45,
                              endIndent: 45,
                            ),
                            const SizedBox(width: 4),
                            AiVoiceActBtn(
                              onTap: _handleTextInput,
                              icon: const Icon(
                                Icons.text_fields_rounded,
                                size: 26,
                                color: Color(0xFFFFFFFF),
                              ),
                              color: const Color(0xFF3A51FF),
                              text: '文本',
                            ),
                          ]),
                    ),
                  ),
                ),
              ]),
        );
      },
    );
  }
}
