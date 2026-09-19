import 'package:ainote_app/app/widgets/note_additional_sheet.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/widgets/dot_grid_container.dart';
import 'package:ainote_app/generated/assets.dart';

import '../controllers/ai_additional_controller.dart';

class AiAdditionalView extends GetView<AiAdditionalController> {
  final String noteContent;
  final List<String> toAdditional;
  final List<List<String>> toAdditionalsOptions;
  const AiAdditionalView(
      {Key? key,
      required this.noteContent,
      required this.toAdditional,
      required this.toAdditionalsOptions})
      : super(key: key);

  List<TextSpan> _buildTextSpans(
      String content, List<String> highlights, int selectedIndex) {
    List<TextSpan> spans = [];
    String remainingText = content;
    if (highlights.isEmpty) return [TextSpan(text: content)];
    String selectedHighlight = highlights[selectedIndex.clamp(0, highlights.length - 1)];

    while (remainingText.isNotEmpty) {
      String? nextHighlight;
      var nextOffset = remainingText.length;
      for (final candidate in highlights.where((word) => word.isNotEmpty)) {
        final offset = remainingText.indexOf(candidate);
        if (offset >= 0 && (offset < nextOffset ||
            (offset == nextOffset && candidate.length > (nextHighlight?.length ?? 0)))) {
          nextOffset = offset;
          nextHighlight = candidate;
        }
      }
      if (nextHighlight != null) {
          final highlight = nextHighlight;
          final startIndex = nextOffset;

          if (startIndex > 0) {
            spans.add(TextSpan(text: remainingText.substring(0, startIndex)));
          }

          int highlightIndex = highlights.indexOf(highlight);
          String? optionForHighlight =
              controller.getSelectedOption(highlightIndex);

          // 判断是否有选中的选项来决定颜色
          Color decorationColor =
              optionForHighlight != null ? Colors.green : Colors.red;
          Color? backgroundColor = highlight == selectedHighlight
              ? (optionForHighlight != null
                  ? Colors.green.withOpacity(0.2)
                  : Colors.red.withOpacity(0.2))
              : null;

          spans.add(
            TextSpan(
              text: optionForHighlight ??
                  remainingText.substring(
                      startIndex, startIndex + highlight.length),
              style: TextStyle(
                decoration: TextDecoration.combine([TextDecoration.underline]),
                decorationColor: decorationColor,
                decorationStyle: TextDecorationStyle.wavy,
                backgroundColor: backgroundColor,
              ),
            ),
          );

          remainingText =
              remainingText.substring(startIndex + highlight.length);
      } else {
        spans.add(TextSpan(text: remainingText));
        break;
      }
    }

    return spans;
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Container(
          width: double.infinity,
          height: double.infinity,
          decoration: BoxDecoration(
              color: MyColors.pageBackgroundBlackColor,
              image: DecorationImage(
                  image: AssetImage(Assets.imagesStarsBg),
                  repeat: ImageRepeat.repeat,
                  fit: BoxFit.cover)),
        ),
        Scaffold(
          backgroundColor: Colors.transparent,
          appBar: AppBar(
            backgroundColor: Colors.transparent,
            iconTheme: IconThemeData(color: Colors.white),
            centerTitle: true,
            leading: GestureDetector(
              onTap: () => Get.back(),
              child: Container(
                padding: EdgeInsets.all(12.w),
                child: Image.asset(Assets.imagesXmarkIcon),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () {
                  if (controller.hasSelectedOptions()) {
                    final replacements =
                        controller.getReplacedContent(toAdditional);
                    Get.back(result: replacements);
                  } else {
                    Get.back();
                  }
                },
                child: Text(
                  '完成',
                  style: TextStyle(
                    color: MyColors.editToolBarActiveColor,
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ),
            ],
          ),
          body: SafeArea(
            bottom: false,
            child: Column(
              children: [
                Expanded(
                  child: DotGridContainer(
                    centered: false,
                    margin:
                        EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.w),
                    padding: EdgeInsets.all(12.w),
                    child: SingleChildScrollView(
                      child: Obx(() => RichText(
                            text: TextSpan(
                              children: _buildTextSpans(noteContent,
                                  toAdditional, controller.selectedIndex),
                              style: TextStyle(
                                color: MyColors.primaryColor,
                                fontSize: 14.sp,
                              ),
                            ),
                          )),
                    ),
                  ),
                ),
                20.verticalSpace,
                SizedBox(
                  height: 380.w,
                  child: NoteAdditionalSheet(
                    toAdditional: toAdditional,
                    toAdditionalsOptions: toAdditionalsOptions,
                    onOptionSelected: (option) {
                      controller.handleOptionSelected(option);
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
