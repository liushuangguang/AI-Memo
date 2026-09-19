import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/views/widgets/ai_suggestion.dart';
import 'package:ainote_app/app/widgets/my_stream_builder.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

class AiSuggestionStream extends StatelessWidget {
  const AiSuggestionStream({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<SmartOrganizeController>();
    return Obx(() {
      return MyStreamBuilder(
        stream: Stream.fromIterable(['']),
        beforeBuilder: () => AiSuggestion(
          data: AiSuggestionModel.mock(),
          loading: true,
          rawStr: '',
        ),
        builder: (String msg) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('AI建议',
                  textAlign: TextAlign.left,
                  style:
                      TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w700)),
              16.verticalSpace,
              Text(
                msg,
                style: TextStyle(
                  color: MyColors.thirdColor,
                  fontSize: 14.sp,
                ),
                textAlign: TextAlign.left,
              ),
              16.verticalSpace,
            ],
          );
        },
        afterBuilder: (String msg) {
          try {
            return AiSuggestion(
              data: controller.aiSuggestion.value,
              rawStr: '',
            );
          } catch (e) {}
          return Text(
            msg,
            style: TextStyle(
              color: MyColors.thirdColor,
              fontSize: 14.sp,
            ),
            textAlign: TextAlign.left,
          );
        },
        onDone: controller.parseAiSuggestion,
      );
    });
  }
}
