import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_categorized_note_model.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:ainote_app/app/widgets/card_stack/card_stack_swiper.dart';
import 'package:ainote_app/app/widgets/dashed_line.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:skeletonizer/skeletonizer.dart';

import '../../../generated/assets.dart';

class AutoSort extends StatelessWidget {
  final List<AiCategorizedNoteModel> list; // 信息分类
  final bool loading; // 信息分类是否正在加载
  final VoidCallback? onSaveCategory; // 保存分类
  final VoidCallback? onSaveScene; // 保存情景记录
  final bool? hasVectored; // 是否向量化
  final String? scene; // 情景记录
  final String? reason; // 归类原因
  final List<String> tags;
  final String? errorMessage;
  final VoidCallback? onRetry;
  final AutoSort Function()? liveState;
  const AutoSort({
    super.key,
    required this.list,
    required this.loading,
    this.onSaveCategory,
    this.onSaveScene,
    this.hasVectored,
    this.scene,
    this.reason,
    this.tags = const [],
    this.errorMessage,
    this.onRetry,
    this.liveState,
  });

  Widget buildDetails(BuildContext context) {
    return Container(
        color: MyColors.pageBackgroundColor,
        padding: EdgeInsets.only(top: 32.w, left: 16.w, right: 16.w),
        constraints: BoxConstraints(maxHeight: 700.h),
        child: SingleChildScrollView(
          child: SafeArea(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                Text(
                  '自动整理',
                  style:
                      TextStyle(fontWeight: FontWeight.bold, fontSize: 22.sp),
                ),
                24.verticalSpace,
                if (loading) const Text('正在生成标签和归类，请稍候…'),
                if (tags.isNotEmpty) ...[
                  AutoSortItem(
                      title: '标签',
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 6,
                        children:
                            tags.map((tag) => Chip(label: Text(tag))).toList(),
                      )),
                  14.verticalSpace,
                ],
                if (errorMessage?.isNotEmpty == true) ...[
                  Text(errorMessage!),
                  if (onRetry != null)
                    TextButton(
                        onPressed: loading ? null : onRetry,
                        child: const Text('重试整理')),
                ],
                if (hasVectored == true) ...[
                  AutoSortItem(
                      title: "相关备忘录关系网",
                      child: Container(
                        padding: EdgeInsets.symmetric(
                            horizontal: 8.w, vertical: 6.h),
                        decoration: BoxDecoration(
                          color: Color(0xff45b670).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(4.r),
                        ),
                        child: Skeletonizer(
                          enabled: loading,
                          enableSwitchAnimation: true,
                          child: Center(
                            child: Text('已向量化该备忘录，成功关联备忘录关系网',
                                style: TextStyle(
                                  color: loading ? null : MyColors.colorGreen,
                                  fontWeight: FontWeight.bold,
                                )),
                          ),
                        ),
                      )),
                  14.verticalSpace,
                ],
                AutoSortItem(
                    onSave: loading ? null : onSaveCategory,
                    title: "信息归类",
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          padding: EdgeInsets.symmetric(
                              horizontal: 8.w, vertical: 6.h),
                          decoration: BoxDecoration(
                            color: Color(0xff45b670).withOpacity(0.1),
                            borderRadius: BorderRadius.circular(4.r),
                          ),
                          child: Skeletonizer(
                            enabled: loading,
                            enableSwitchAnimation: true,
                            child: Center(
                              child: Text(
                                  list.isEmpty
                                      ? '暂无可用的归类信息'
                                      : '发现 ${list.length} 类信息，可查看并保存',
                                  style: TextStyle(
                                    color: loading ? null : MyColors.colorGreen,
                                    fontWeight: FontWeight.bold,
                                  )),
                            ),
                          ),
                        ),
                        8.verticalSpace,
                        Skeletonizer(
                          enabled: loading,
                          enableSwitchAnimation: true,
                          child: RichText(
                              text: TextSpan(
                                  text: '归类原因：',
                                  style: TextStyle(
                                    color: MyColors.primaryColor,
                                    fontSize: 14.sp,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  children: [
                                TextSpan(
                                    text: reason ?? '暂无',
                                    style: TextStyle(
                                      color: MyColors.secondaryColor,
                                      fontSize: 14.sp,
                                      fontWeight: FontWeight.w400,
                                    ))
                              ])),
                        ),
                        8.verticalSpace,
                        CardStackSwiper(
                          list: loading == true
                              ? [
                                  AiCategorizedNoteModel.mock(),
                                  AiCategorizedNoteModel.mock(),
                                  AiCategorizedNoteModel.mock(),
                                ]
                              : list,
                          loading: loading,
                          childBuilder: (BuildContext context, item) {
                            return Row(
                              mainAxisAlignment: MainAxisAlignment.start,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Expanded(
                                    child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(item.key ?? '',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        textAlign: TextAlign.left,
                                        softWrap: false,
                                        style: TextStyle(
                                            fontSize: 16.sp,
                                            fontWeight: FontWeight.w500)),
                                    SizedBox(height: 8.h),
                                    Text(item.value ?? '',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        textAlign: TextAlign.left,
                                        softWrap: false,
                                        style: TextStyle(
                                            fontSize: 14.sp,
                                            fontWeight: FontWeight.w400)),
                                  ],
                                ))
                              ],
                            );
                          },
                        )
                      ],
                    )),
                14.verticalSpace,
                AutoSortItem(
                    onSave: loading ? null : onSaveScene,
                    title: "情景记录",
                    child: Skeletonizer(
                      enabled: loading,
                      enableSwitchAnimation: true,
                      child: Text(scene ?? '',
                          textAlign: TextAlign.left,
                          style: TextStyle(
                            color: MyColors.secondaryColor,
                            fontWeight: FontWeight.w400,
                          )),
                    )),
                16.verticalSpace,
              ],
            ),
          ),
        ));
  }

  @override
  Widget build(BuildContext context) {
    void showDialog() {
      showCupertinoModalBottomSheet(
        context: context,
        enableDrag: false,
        useRootNavigator: true,
        builder: (context) => Material(
            child: liveState == null
                ? buildDetails(context)
                : Obx(() => liveState!().buildDetails(context))),
      );
    }

    return CardContainer(
      onTap: showDialog,
      bgColor: MyColors.cardBgCyan,
      borderColor: MyColors.cardBorderCyan,
      title: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text('自动整理',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16.sp)),
          IconButton(
              padding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
              onPressed: () {
                showDialog();
              },
              icon: Icon(Icons.arrow_forward_ios_rounded,
                  color: MyColors.primaryColor, size: 16.sp))
        ],
      ),
      content: Text('自动整理，根据笔记内容自动生成标签，方便查找。',
          style: TextStyle(
              fontWeight: FontWeight.w400,
              fontSize: 14.sp,
              color: MyColors.primaryColor.withOpacity(0.5))),
    );
  }
}

class AutoSortItem extends StatelessWidget {
  final String title;
  final Widget? child;
  final VoidCallback? onSave;

  const AutoSortItem({super.key, required this.title, this.child, this.onSave});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
      decoration: BoxDecoration(
        color: MyColors.colorWhite,
        borderRadius: BorderRadius.circular(12.r),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Image.asset(
                Assets.imagesNoteBook,
                width: 20.w,
                fit: BoxFit.fitWidth,
              ),
              8.horizontalSpace,
              Text(
                title,
                style: TextStyle(fontSize: 16.sp, fontWeight: FontWeight.bold),
              ),
              Spacer(),
              Visibility(
                visible: onSave != null,
                child: TextButton(
                  onPressed: onSave,
                  child: Text('保存',
                      style: TextStyle(
                          color: MyColors.colorBlue,
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold)),
                ),
              )
            ],
          ),
          8.verticalSpace,
          DashedLine(color: MyColors.dashedLineColor),
          8.verticalSpace,
          Visibility(
              visible: child != null,
              child: Padding(
                padding: EdgeInsets.only(
                  top: 8.0.w,
                ),
                child: child!,
              ))
        ],
      ),
    );
  }
}
