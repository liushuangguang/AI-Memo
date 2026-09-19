import 'package:flutter/material.dart';

import '../../../ui/retry_btn.dart';
import 'message_item.dart';

class MessageFailed extends StatelessWidget {
  final VoidCallback? onTap;

  const MessageFailed({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const MessageItem(
          message: '生成失败',
          isAiMessage: true,
        ),
        RetryBtn(onTap: onTap),
      ],
    );
  }
}


