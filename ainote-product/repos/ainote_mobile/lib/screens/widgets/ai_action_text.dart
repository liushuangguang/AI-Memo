// AI理解信息中...
// 网络连接存在问题
// 对不起，您记录的信息我无法理解，故不做任何改写
// 对不起，您记录的信息可能无意义，故不做任何优化
// 您输入的备忘录可能有意义但我不能理解，帮您找到相关搜索结果参考

import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';

import '../../constants/app_images.dart';

class AiActionText extends StatelessWidget {
  final String? text;
  final Widget? child;

  const AiActionText({super.key, this.text, this.child});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 36.0),
          child: Image.asset(
            AppImages.aiGeneratingPic.path,
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
            : const SizedBox.shrink(),
        child != null ? child! : const SizedBox.shrink(),
      ],
    );
  }
}

class AiDoing extends StatelessWidget {
  const AiDoing({super.key});

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'AI理解信息中',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: primaryColor,
            decoration: TextDecoration.none,
          ),
        ),
        const SizedBox(width: 4),
        LoadingAnimationWidget.progressiveDots(
          color: primaryColor,
          size: 18,
        )
      ],
    );
  }
}
