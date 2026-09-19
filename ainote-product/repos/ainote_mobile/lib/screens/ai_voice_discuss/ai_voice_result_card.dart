import 'package:ainote/constants/app_images.dart';
import 'package:ainote/screens/ai_voice_discuss/ai_voice_discuss.dart';
import 'package:ainote/ui/secondary_btn.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../../ui/primary_btn.dart';
import 'message/message_list.dart';

class AiVoiceResultCard extends StatelessWidget {
  final VoidCallback? onDelete;
  const AiVoiceResultCard({super.key, this.onDelete});

  @override
  Widget build(BuildContext context) {
    var primaryColor = Theme.of(context).primaryColor;

    void _handleDelete() {
      showCupertinoModalBottomSheet(
          context: context,
          enableDrag: false,
          topRadius: const Radius.circular(20),
          builder: (context) {
            return Container(
                padding: const EdgeInsets.all(24),
                color: const Color(0xFFF3F5F8),
                child: Column(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const Text(
                        '确定删除语音讨论内容吗？',
                        style: TextStyle(
                            fontSize: 18,
                            color: Color(0xFF12102F),
                            fontWeight: FontWeight.w700),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(
                        height: 12,
                      ),
                      Container(
                        height: 74,
                        width: double.infinity,
                        padding: const EdgeInsets.only(bottom: 12),
                        margin: const EdgeInsets.only(top: 24),
                        child: PrimaryBtn(
                          text: '删除',
                          onPressed: () {
                            onDelete?.call();
                            Navigator.pop(context);
                          },
                          backgroundColor: const Color(0xFFCE3A54),
                        ),
                      ),
                      const SizedBox(
                        height: 12,
                      ),
                      Container(
                        height: 74,
                        width: double.infinity,
                        padding: const EdgeInsets.only(bottom: 12),
                        child: SecondaryBtn(
                          onPressed: () {
                            Navigator.pop(context);
                          },
                          text: '取消',
                        ),
                      ),
                    ]));
          });
    }

    void _showDetail() {
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
                  '语音讨论',
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
                child: SingleChildScrollView(child: MessageList()),
              ),
              const SizedBox(
                height: 20,
              ),
              SizedBox(
                height: 54,
                width: double.infinity,
                child: SecondaryBtn(
                  text: '关闭',
                  onPressed: () {
                    Navigator.pop(context);
                  },
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Container(
        height: 55,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: const Color(0xFFE2E2E2), width: 1),
        ),
        child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Icon(
                    Icons.mic_rounded,
                    size: 24,
                  ),
                  // SizedBox(width: 8),
                  Text(
                    '语音讨论',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  // TextButton(
                  //     onPressed: () {},
                  //     child: const Text(
                  //       '存入正文',
                  //       style: TextStyle(
                  //           color: Color(0xFF3A51FF),
                  //           fontSize: 16,
                  //           fontWeight: FontWeight.w500),
                  //     )),
                  const VerticalDivider(
                    thickness: 1,
                    indent: 4,
                    endIndent: 4,
                    color: Color(0xFFE2E2E2),
                  ),
                  IconButton(
                      onPressed: _handleDelete,
                      icon: SvgPicture.asset(AppImages.deleteBlackIcon.path)),
                  const VerticalDivider(
                    thickness: 1,
                    indent: 4,
                    endIndent: 4,
                    color: Color(0xFFE2E2E2),
                  ),
                  IconButton(
                      onPressed: _showDetail,
                      icon: SvgPicture.asset(
                        AppImages.openFullIcon.path,
                      )),
                ],
              ),
            ]));
  }
}
