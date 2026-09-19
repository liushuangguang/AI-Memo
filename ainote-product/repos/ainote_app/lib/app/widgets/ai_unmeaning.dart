// AI理解信息中...
// 网络连接存在问题
// 对不起，您记录的信息我无法理解，故不做任何改写
// 对不起，您记录的信息可能无意义，故不做任何优化
// 您输入的备忘录可能有意义但我不能理解，帮您找到相关搜索结果参考
import 'package:ainote_app/app/widgets/ai_action_text.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';

class AiUnmeaning extends StatelessWidget {
  final String? text;
  final Widget? child;

  const AiUnmeaning({super.key, this.text, this.child});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 36.0),
          child: Image.asset(
            Assets.imagesAiGeneratingPic,
            width: 220,
            height: 220,
          ),
        ),
        text != null
            ? Text(
                text!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                ),
              )
            : const AiDoing(),
        child != null ? child! : const SizedBox(),
      ],
    );
  }
}
