import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:ainote_app/app/modules/ai/ai_additional/controllers/ai_additional_controller.dart';

class NoteAdditionalSheet extends StatefulWidget {
  final List<String> toAdditional;
  final List<List<String>> toAdditionalsOptions;
  final Function(String) onOptionSelected;
  const NoteAdditionalSheet(
      {super.key,
      required this.toAdditional,
      required this.toAdditionalsOptions,
      required this.onOptionSelected});

  @override
  State<NoteAdditionalSheet> createState() => _NoteAdditionalSheetState();
}

class _NoteAdditionalSheetState extends State<NoteAdditionalSheet> {
  int currentIndex = 0;
  int? currentOptionIndex;
  late final PageController _pageController;
  final TextEditingController _customInputController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: currentIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    _customInputController.dispose();
    super.dispose();
  }

  void _showCustomInputDialog() {
    final pageIndex = currentIndex;
    var submitted = false;
    _customInputController.text = Get.find<AiAdditionalController>().getCustomInput(pageIndex) ?? '';
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: MyColors.pageBackgroundBlackColor,
      builder: (context) {
        return Padding(
          padding:
              EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 8.w),
            color: MyColors.pageBackgroundBlackColor,
            child: Row(
              children: [
                Expanded(
                  child: Container(
                    height: 38.h,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8.w),
                    ),
                    child: TextField(
                      controller: _customInputController,
                      autofocus: true,
                      style: TextStyle(
                        fontSize: 14.sp,
                        color: MyColors.primaryColor,
                      ),
                      decoration: InputDecoration(
                        contentPadding: EdgeInsets.only(
                            left: 12.w, right: 12.w, bottom: 6.w),
                        hintText: '请输入自定义选项',
                        hintStyle: TextStyle(
                          color: MyColors.thirdColor,
                          fontSize: 14.sp,
                        ),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                ),
                8.horizontalSpace,
                GestureDetector(
                  onTap: () {
                    _customInputController.clear();
                    Navigator.pop(context);
                  },
                  child: Container(
                    width: 64.w,
                    height: 38.h,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8.w),
                    ),
                    child: Center(
                      child: Text(
                        '取消',
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: MyColors.colorBlue,
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                    ),
                  ),
                ),
                8.horizontalSpace,
                GestureDetector(
                  onTap: () {
                    final input = _customInputController.text.trim();
                    if (!submitted && input.isNotEmpty) {
                      submitted = true;
                      widget.onOptionSelected(input);
                      final controller = Get.find<AiAdditionalController>();
                      controller.updateCustomInput(
                        pageIndex,
                        input,
                      );
                      Navigator.pop(context);
                      _customInputController.clear();
                    }
                  },
                  child: Container(
                    width: 64.w,
                    height: 38.h,
                    decoration: BoxDecoration(
                      color: MyColors.colorBlue,
                      borderRadius: BorderRadius.circular(8.w),
                    ),
                    child: Center(
                      child: Text(
                        '确定',
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: Colors.white,
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildOptionItem(int pageIndex, int optionIndex) {
    final options = widget.toAdditionalsOptions[pageIndex].take(4).toList();
    if (optionIndex == options.length) {
      return Column(
        children: [
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 12.w),
            child: GetBuilder<AiAdditionalController>(builder: (controller) {
              final selectedOption = controller.getSelectedOption(pageIndex);
              final customInput = controller.getCustomInput(pageIndex);
              return GestureDetector(
                key: ValueKey('completion-manual-$pageIndex'),
                onTap: _showCustomInputDialog,
                child: Container(
                  width: double.infinity,
                  height: 38.h,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8.w),
                    border: Border.all(
                      color:
                          selectedOption == customInput && customInput != null
                              ? Colors.black
                              : Colors.transparent,
                      width: 1.w,
                    ),
                  ),
                  padding: EdgeInsets.symmetric(horizontal: 12.w),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: [
                      Container(
                        constraints: BoxConstraints(minHeight: 23.h),
                        decoration: BoxDecoration(
                          color: MyColors.lightBlueColor,
                          borderRadius: BorderRadius.circular(4.w),
                        ),
                        padding: EdgeInsets.symmetric(
                            horizontal: 4.w, vertical: 2.w),
                        child: Center(
                          child: Text(
                            '手动输入',
                            style: TextStyle(
                              fontSize: 12.sp,
                              color: MyColors.colorBlue,
                            ),
                          ),
                        ),
                      ),
                      10.horizontalSpace,
                      Expanded(child: Text(
                        customInput ?? '请输入',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: customInput != null
                              ? MyColors.primaryColor
                              : MyColors.thirdColor,
                          fontWeight: FontWeight.w400,
                        ),
                      )),
                    ],
                  ),
                ),
              );
            }),
          ),
          if (optionIndex < 3) 8.verticalSpace,
        ],
      );
    }

    return Column(
      children: [
        Padding(
          padding: EdgeInsets.symmetric(horizontal: 12.w),
          child: GetBuilder<AiAdditionalController>(
            builder: (controller) {
              final selectedOption = controller.getSelectedOption(pageIndex);
              return GestureDetector(
                onTap: () {
                  widget.onOptionSelected(options[optionIndex]);
                  controller.updateSelectedOption(
                      pageIndex, options[optionIndex]);
                },
                child: Container(
                  width: double.infinity,
                  constraints: BoxConstraints(minHeight: 38.h),
                  padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 8.h),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8.w),
                    border: Border.all(
                      color: selectedOption == options[optionIndex]
                          ? Colors.black
                          : Colors.transparent,
                      width: 1.w,
                    ),
                  ),
                  child: Center(
                    child: Text(
                      options[optionIndex],
                      style: TextStyle(
                        fontSize: 14.sp,
                        color: MyColors.primaryColor,
                        fontWeight: FontWeight.w400,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
        if (optionIndex < 3) 8.verticalSpace,
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20.w),
        color: MyColors.pageBackgroundColor,
      ),
      padding: EdgeInsets.symmetric(vertical: 10.w),
      child: GestureDetector(
        behavior: HitTestBehavior.translucent,
        child: SingleChildScrollView(child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.w),
              child: Row(
                children: [
                  Padding(
                    padding: EdgeInsets.only(right: 12.w),
                    child: Image.asset(
                      Assets.imagesWriteDocIcon,
                      width: 24.w,
                      height: 24.w,
                    ),
                  ),
                  Expanded(child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'AI信息补充',
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: MyColors.primaryColor,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        '帮您快速完善备忘录内容，进而更准确的理解内容',
                        style: TextStyle(
                          fontSize: 12.sp,
                          color: MyColors.thirdColor,
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                    ],
                  ))
                ],
              ),
            ),
            12.verticalSpace,
            Divider(
              color: MyColors.dividerColor,
              height: 1.h,
            ),
            12.verticalSpace,
            Row(
              children: [
                12.horizontalSpace,
                AnimatedOpacity(
                  duration: Duration(milliseconds: 200),
                  opacity: currentIndex > 0 ? 1.0 : 0.0,
                  child: GestureDetector(
                    onTap: currentIndex > 0
                        ? () {
                            setState(() {
                              currentIndex--;
                            });
                            _pageController.animateToPage(
                              currentIndex,
                              duration: Duration(milliseconds: 300),
                              curve: Curves.easeInOut,
                            );
                          }
                        : null,
                    child: Image.asset(
                      Assets.imagesChevLeftIcon,
                      width: 24.w,
                      height: 24.w,
                    ),
                  ),
                ),
                Expanded(
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(child: Text(
                            '"${widget.toAdditional[currentIndex]}"是指',
                            style: TextStyle(
                                fontSize: 14.sp,
                                color: MyColors.primaryColor,
                                fontWeight: FontWeight.w500),
                          )),
                          Container(
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(4.w),
                            ),
                            padding: EdgeInsets.symmetric(
                                horizontal: 4.w, vertical: 2.w),
                            child: Text(
                              '${currentIndex + 1}/${widget.toAdditional.length}',
                              style: TextStyle(
                                  fontSize: 12.sp,
                                  color: MyColors.thirdColor,
                                  fontWeight: FontWeight.w400),
                            ),
                          )
                        ],
                      ),
                      12.verticalSpace,
                      SizedBox(
                        height: 38.h * 4 + 8.w * 3 + 10.w,
                        child: PageView.builder(
                          onPageChanged: (index) {
                            setState(() {
                              currentIndex = index;
                            });
                            Get.find<AiAdditionalController>()
                                .updateSelectedIndex(index);
                          },
                          controller: _pageController,
                          itemCount: widget.toAdditional.length,
                          itemBuilder: (context, pageIndex) {
                            return ListView(
                              padding: EdgeInsets.zero,
                              children: List.generate(
                                  widget.toAdditionalsOptions[pageIndex].take(4).length + 1,
                                  (index) =>
                                      _buildOptionItem(pageIndex, index)),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
                AnimatedOpacity(
                  duration: Duration(milliseconds: 200),
                  opacity:
                      currentIndex < widget.toAdditional.length - 1 ? 1.0 : 0.0,
                  child: GestureDetector(
                    onTap: currentIndex < widget.toAdditional.length - 1
                        ? () {
                            setState(() {
                              currentIndex++;
                            });
                            _pageController.animateToPage(
                              currentIndex,
                              duration: Duration(milliseconds: 300),
                              curve: Curves.easeInOut,
                            );
                          }
                        : null,
                    child: Image.asset(
                      Assets.imagesChevRightIcon,
                      width: 24.w,
                      height: 24.w,
                    ),
                  ),
                ),
                12.horizontalSpace,
              ],
            ),
            12.verticalSpace,
            Text(
              '提示：模糊的语义，会让AI服务您的准确性大打折扣',
              style: TextStyle(
                  fontSize: 12.sp,
                  color: MyColors.thirdColor,
                  fontWeight: FontWeight.w400),
            )
          ],
        )),
      ),
    );
  }
}
