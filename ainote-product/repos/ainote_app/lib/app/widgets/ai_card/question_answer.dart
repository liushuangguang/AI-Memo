import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_related_link_model.dart';
import 'package:ainote_app/app/data/models/ai_related_title_model.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:ainote_app/app/utils/helper.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:ainote_app/app/widgets/dashed_line.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/app/widgets/secondary_btn.dart';
import 'package:ainote_app/app/widgets/web_view_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:skeletonizer/skeletonizer.dart';
import './related_title_detail.dart';

class QuestionAnswer extends StatefulWidget {
  final bool loading;
  final bool? noBg;
  final bool? usePage;
  final Color? itemBgColor;
  final List<AiRelatedLinkModel> relatedLinkList;
  final List<AiRelatedTitleModel> relatedTitleList;
  final Function(QuestionAnswerModule q)? onSave;
  final EdgeInsetsGeometry? padding;

  const QuestionAnswer(
      {super.key,
      required this.relatedLinkList,
      required this.relatedTitleList,
      required this.loading,
      this.onSave,
      this.itemBgColor,
      this.noBg,
      this.usePage,
      this.padding});

  @override
  State<QuestionAnswer> createState() => _QuestionAnswerState();
}

class _QuestionAnswerState extends State<QuestionAnswer> {
  @override
  Widget build(BuildContext context) {
    var _relatedLinkList = widget.relatedLinkList;
    var _relatedTitleList = widget.relatedTitleList;
    if (widget.loading) {
      _relatedLinkList = [];
      _relatedTitleList = List.generate(
          3,
          (index) => AiRelatedTitleModel(
                title: generateMockString(20),
              ));
    }

    void handleViewDetail(AiRelatedLinkModel relatedLink) {
      if (relatedLink.snippet != null && relatedLink.snippet!.isNotEmpty) {
        showCupertinoModalBottomSheet(
            context: context,
            builder: (_) {
              return SafeArea(
                child: Container(
                  padding: EdgeInsets.only(
                      top: 32.w, left: 16.w, right: 16.w, bottom: 32.w),
                  color: Colors.white,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        relatedLink.linkName ?? '',
                        style: TextStyle(
                            fontSize: 20.sp, fontWeight: FontWeight.w700),
                        textAlign: TextAlign.center,
                      ),
                      12.verticalSpace,
                      Container(
                        constraints:
                            BoxConstraints(maxHeight: Get.height - 360.h),
                        child: SingleChildScrollView(
                          child: Text(
                            relatedLink.snippet ?? '',
                            style: const TextStyle(fontSize: 14),
                          ),
                        ),
                      ),
                      12.verticalSpace,
                      PrimaryBtn(
                        text: '查看原文',
                        onPressed: () {
                          jumpToWebView(context, relatedLink);
                        },
                      ),
                      12.verticalSpace,
                      SecondaryBtn(
                        text: '关闭',
                        onPressed: () {
                          Navigator.pop(context);
                        },
                      )
                    ],
                  ),
                ),
              );
            });
      }
    }

    return CardContainer(
        padding: widget.padding,
        needHighlight: false,
        bgColor: widget.noBg == true ? Colors.transparent : MyColors.cardBgRed,
        borderColor:
            widget.noBg == true ? Colors.transparent : MyColors.cardBorderRed,
        title: Text('猜你想看',
            style: TextStyle(
                color: MyColors.cardTitleRed,
                fontWeight: FontWeight.bold,
                fontSize: 16.sp)),
        content: Skeletonizer(
          enabled: widget.loading,
          enableSwitchAnimation: true,
          child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                ..._relatedTitleList.map((relatedTitle) => GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      if (widget.usePage == true) {
                        jumpToDetail(context, relatedTitle);
                      } else {
                        openRelatedTitleBottomSheet(context, relatedTitle);
                      }
                    },
                    child: LinkItem(
                      bgColor: widget.itemBgColor,
                      relatedTitle: relatedTitle,
                      onTap: () {
                        if (widget.usePage == true) {
                          jumpToDetail(context, relatedTitle);
                        } else {
                          openRelatedTitleBottomSheet(context, relatedTitle);
                        }
                      },
                    ))),
                if (_relatedLinkList.isNotEmpty && _relatedTitleList.isNotEmpty)
                  DashedLine(
                    color: MyColors.cardBorderRed,
                    padding: EdgeInsets.only(
                        top: 4.w, bottom: 12.w, left: 4.w, right: 4.w),
                  ),
                ..._relatedLinkList.map((relatedLink) => GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      if (widget.usePage == true) {
                        jumpToWebView(context, relatedLink);
                      } else {
                        openRelatedLinkBottomSheet(context, relatedLink);
                      }
                    },
                    child: LinkItem(
                      bgColor: widget.itemBgColor,
                      relatedLink: relatedLink,
                      onTap: () {
                        if (widget.usePage == true) {
                          jumpToWebView(context, relatedLink);
                        } else {
                          openRelatedLinkBottomSheet(context, relatedLink);
                        }
                      },
                    ))),
              ]),
        ));
  }

  void openRelatedTitleBottomSheet(
      BuildContext context, AiRelatedTitleModel relatedTitle) {
    showCommonBottomSheet(
        context: context,
        title: relatedTitle.title ?? '',
        leading: Text(
          relatedTitle.emoji ?? '',
          style: TextStyle(fontSize: 20.sp, fontWeight: FontWeight.w700),
        ),
        content:
            RelatedTitleDetail(text: relatedTitle.title ?? '', noHeader: true),
        onSave: widget.onSave != null
            ? () {
                String? detail = AiApi.aiRelatedInfoCached[relatedTitle.title];
                if (detail == null) {
                  Toast.info('AI努力解析中');
                  return;
                }
                widget.onSave?.call(QuestionAnswerModule(
                    title: relatedTitle.title ?? '',
                    description: '',
                    questionAnswerItems: [
                      QuestionAnswerItem(
                          question:
                              '${relatedTitle.emoji} ${relatedTitle.title}',
                          answer: detail)
                    ]));
              }
            : null);
  }

  void openRelatedLinkBottomSheet(
      BuildContext context, AiRelatedLinkModel relatedLink) {
    showCommonBottomSheet(
        context: context,
        title: relatedLink.linkName ?? '',
        leading: Image.network(
          relatedLink.icon ?? '',
          width: 20.w,
          height: 20.w,
          frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
            if (frame == null) {
              return const SizedBox.shrink();
            }
            return child;
          },
          errorBuilder: (context, error, stackTrace) {
            return SizedBox.shrink();
          },
        ),
        content: WebViewPage(noHeader: true, url: relatedLink.link ?? ''),
        onSave: widget.onSave != null
            ? () {
                widget.onSave?.call(QuestionAnswerModule(
                    title: relatedLink.linkName ?? '',
                    description: '',
                    questionAnswerItems: [
                      QuestionAnswerItem(
                        question: relatedLink.linkName ?? '',
                        answer: relatedLink.snippet ?? '',
                      ),
                      QuestionAnswerItem(
                        question: relatedLink.icon ?? '',
                        answer: relatedLink.link ?? '',
                      )
                    ]));
              }
            : null);
  }

  void jumpToWebView(BuildContext context, AiRelatedLinkModel relatedLink) {
    String link = relatedLink.link ?? '';
    if (link.isEmpty) {
      Toast.info("暂无相关链接");
      return;
    }
    if (!Uri.parse(link).isAbsolute ||
        !Uri.parse(link).scheme.startsWith('http')) {
      Toast.info("无效的链接");
      return;
    }
    try {
      Get.to(() => WebViewPage(
            url: link,
            title: relatedLink.linkName,
          ));
    } catch (e) {
      Toast.error("导航失败: $e");
    }
  }

  void jumpToDetail(BuildContext context, AiRelatedTitleModel relatedTitle) {
    String text = "${relatedTitle.emoji} ${relatedTitle.title}";
    try {
      Get.to(() => RelatedTitleDetail(
            text: text,
          ));
    } catch (e) {
      Toast.error("导航失败: $e");
    }
  }
}

class LinkItem extends StatelessWidget {
  final AiRelatedLinkModel? relatedLink;
  final AiRelatedTitleModel? relatedTitle;
  final VoidCallback onTap;
  final Color? bgColor;
  final Color? titleColor;
  final Widget? iconWidget;

  const LinkItem(
      {super.key,
      this.relatedLink,
      this.relatedTitle,
      required this.onTap,
      this.bgColor,
      this.titleColor,
      this.iconWidget});

  @override
  Widget build(BuildContext context) {
    String? title;
    String? icon;
    if (relatedLink != null) {
      title = relatedLink?.linkName;
      icon = relatedLink?.icon;
    }
    if (relatedTitle != null) {
      title = '${relatedTitle?.emoji} ${relatedTitle?.title}';
    }
    return Container(
      padding: EdgeInsets.only(left: 12.w, top: 4.w, bottom: 4.w),
      margin: EdgeInsets.only(bottom: 12.h),
      decoration: BoxDecoration(
        color: bgColor ?? MyColors.colorWhite,
        borderRadius: BorderRadius.circular(8.w),
      ),
      child: ListTile(
        contentPadding: EdgeInsets.all(0),
        title: Row(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            if (iconWidget != null) ...[
              iconWidget!,
              8.horizontalSpace,
            ],
            if (icon != null && iconWidget == null) ...[
              Image.network(
                icon,
                width: 20.w,
                height: 20.w,
                frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
                  if (frame == null) {
                    return const SizedBox.shrink();
                  }
                  return child;
                },
                errorBuilder: (context, error, stackTrace) {
                  return SizedBox.shrink();
                },
              ),
              8.horizontalSpace,
            ],
            Expanded(
              child: Text(title ?? '',
                  style: TextStyle(
                      color: titleColor,
                      fontWeight: FontWeight.w400,
                      fontSize: 14.sp)),
            ),
          ],
        ),
        // subtitle: Text(subTitle ?? '', style: TextStyle(fontSize: 12.sp)),
        trailing: IconButton(
            onPressed: onTap,
            icon: Icon(Icons.arrow_forward_ios_rounded,
                color: MyColors.primaryColor, size: 16.sp)),
      ),
    );
  }
}
