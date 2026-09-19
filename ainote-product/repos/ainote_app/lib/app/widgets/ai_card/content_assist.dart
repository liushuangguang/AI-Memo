import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/constants.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/ai_assist_content.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class ContentAssist extends StatelessWidget {
  const ContentAssist({super.key});

  @override
  Widget build(BuildContext context) {
    void showDialog([String? direction]) {
      showCupertinoModalBottomSheet(
          context: context,
          enableDrag: false,
          useRootNavigator: true,
          builder: (context) {
            return AiAssistContent(noSelectedContentText: true, initialDirection: direction);
          });
    }

    return CardContainer(
      onTap: () => showDialog(),
      bgColor: MyColors.cardBgBlue,
      borderColor: MyColors.cardBorderBlue,
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('内容辅写',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16.sp,
                  color: MyColors.colorBlue)),
          IconButton(
              padding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              onPressed: () => showDialog(),
              icon: Icon(Icons.arrow_forward_ios_rounded,
                  color: MyColors.colorBlue, size: 16.sp))
        ],
      ),
      content: SizedBox(
        height: 80.w,
        child: ListView.separated(
          itemBuilder: (context, index) {
            var item = contentAssistListChildren[index];
            return GestureDetector(
              onTap: () => showDialog(item['title'] as String),
              child: Container(
                  width: 72.w,
                  height: 80.w,
                  decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8),
                      border: null),
                  child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        SizedBox(
                            width: 36.w,
                            height: 36.w,
                            child:
                                Image.asset(item['icon'], fit: BoxFit.contain)),
                        SizedBox(height: 3.w),
                        Text(item['title'],
                            style: TextStyle(
                              fontSize: item['title'].length > 4 ? 10 : 12,
                              fontWeight: FontWeight.bold,
                              color: const Color(0xFF12102F),
                              decoration: TextDecoration.none,
                            ))
                      ])),
            );
          },
          itemCount: contentAssistListChildren.length,
          scrollDirection: Axis.horizontal,
          separatorBuilder: (BuildContext context, int index) {
            return SizedBox(width: 8.w);
          },
        ),
      ),
    );
  }
}
