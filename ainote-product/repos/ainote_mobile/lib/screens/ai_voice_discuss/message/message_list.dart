import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'message_logic.dart';

class MessageList extends StatelessWidget {
  MessageLogic messageLogic = Get.put(MessageLogic());

  @override
  Widget build(BuildContext context) {
    return GetBuilder<MessageLogic>(
      builder: (logic) {
        return Column(
            mainAxisAlignment: MainAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              ...logic.messageWidgets,
              // MessageItem(message: '帮我记录下，我明天和小明去吃烧烤', avatar: Image.network('https://cdn-icons-png.flaticon.com/512/149/149071.png', width: 36),),
              //
              // VoiceMessageView(
              //   controller: VoiceController(
              //     audioSrc: audioSrc,
              //     maxDuration: const Duration(seconds: 10),
              //     isFile: isFileAudio,
              //     onComplete: () {
              //       /// do something on complete
              //     },
              //     onPause: () {
              //       /// do something on pause
              //     },
              //     onPlaying: () {
              //       /// do something on playing
              //     },
              //     onError: (err) {
              //       /// do somethin on error
              //     },
              //   ),
              //   innerPadding: 12,
              //   cornerRadius: 20,
              // ),
            ]);
      },
    );
  }
}
