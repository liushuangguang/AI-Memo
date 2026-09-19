import 'package:flutter/material.dart';
import 'package:loading_indicator/loading_indicator.dart';

import 'message_item.dart';

class MessageThinking extends StatelessWidget {
  const MessageThinking({super.key});

  @override
  Widget build(BuildContext context) {
    return const MessageItem(
      message: '',
      isAiMessage: true,
      content: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: 20,
            height: 20,
            child: LoadingIndicator(
              indicatorType: Indicator.lineSpinFadeLoader,
              colors: [Colors.white],
              strokeWidth: 1,
            ),
          ),
          SizedBox(width: 8),
          Text(
            '思考中...',
            style: TextStyle(
                color: Colors.white,
                fontSize: 16,
                fontWeight: FontWeight.w400),
          )
        ],
      ),
    );
  }
}