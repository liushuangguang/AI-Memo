import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';
import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/icon_text_action.dart';
import 'package:ainote_app/app/widgets/loading_text.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:skeletonizer/skeletonizer.dart';

class AiSuggestion extends StatefulWidget {
  final String rawStr;
  final List<IconTextActionModel>? list;
  final AiSuggestionModel? data;
  final bool? loading;
  final bool? disabled;
  final bool? extra;
  final VoidCallback? onSaveAll;
  final VoidCallback? onDelete;
  final Function(IconTextActionModel item)? onSaveOne;

  const AiSuggestion({
    super.key,
    this.data,
    required this.rawStr,
    this.loading,
    this.disabled,
    this.onSaveAll,
    this.onDelete,
    this.onSaveOne,
    this.list,
    this.extra,
  });

  @override
  State<AiSuggestion> createState() => _AiSuggestionState();
}

class _AiSuggestionState extends State<AiSuggestion> {
  final expandableController = ExpandableController();
  bool expandable = true;

  void toggleExpandable() {
    expandableController.expanded = !expandableController.expanded;
    setState(() {
      expandable = expandableController.expanded;
    });
  }

  @override
  void initState() {
    super.initState();
    expandableController.expanded = expandable;
  }

  @override
  void dispose() {
    expandableController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // List<IconTextActionModel> list = widget.data.toIconTextActionModelList();
    var list = widget.list ?? [];

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ExpandablePanel(
          controller: expandableController,
          header: Row(
            children: [
              Text('AI建议',
                  style:
                      TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w700)),
              Spacer(),
              Transform.translate(
                  offset: Offset(4.w, 0),
                  child: Row(children: [
                    if (widget.onSaveAll != null) ...[
                      TextButton(
                          onPressed:
                              widget.loading == true ? null : widget.onSaveAll,
                          child: Row(
                            children: [
                              SvgPicture.asset(Assets.imagesListUp),
                              4.horizontalSpace,
                              Text('全部保留', style: TextStyle(fontSize: 12.sp)),
                            ],
                          )),
                      SizedBox(
                        height: 20.w,
                        child: VerticalDivider(
                            indent: 4.w,
                            endIndent: 4.w,
                            thickness: 1.w,
                            color: MyColors.verticalLineColor),
                      ),
                      TextButton(
                          onPressed:
                              widget.loading == true ? null : toggleExpandable,
                          child: Row(
                            children: [
                              Icon(
                                  expandable
                                      ? Icons.visibility_off
                                      : Icons.visibility,
                                  color: MyColors.primaryColor.withOpacity(0.7),
                                  size: 16.w),
                              4.horizontalSpace,
                              Text(expandable ? '隐藏' : '显示',
                                  style: TextStyle(fontSize: 12.sp)),
                            ],
                          )),
                      if (widget.onDelete != null) ...[
                        SizedBox(
                          height: 20.w,
                          child: VerticalDivider(
                              indent: 4.w,
                              endIndent: 4.w,
                              thickness: 1.w,
                              color: MyColors.verticalLineColor),
                        ),
                        TextButton(
                            onPressed: widget.loading == true
                                ? null
                                : () {
                                    confirmDelete(context, onDelete: () {
                                      widget.onDelete?.call();
                                    });
                                  },
                            child: SvgPicture.asset(Assets.imagesDeleteBlack)),
                      ]
                    ],
                  ]))
            ],
          ),
          collapsed: SizedBox.shrink(),
          expanded: Column(
            children: [
              if (widget.loading == true && widget.rawStr.isEmpty)
                LoadingText(text: "等待正文优化完成"),
              Skeletonizer(
                enabled: widget.loading == true && widget.rawStr.isEmpty,
                enableSwitchAnimation: true,
                child: Column(children: [
                  if (widget.rawStr.isNotEmpty)
                    Padding(
                      padding: EdgeInsets.symmetric(vertical: 12.0.w),
                      child: Text(
                        widget.rawStr,
                        style: TextStyle(
                          color: MyColors.thirdColor,
                          fontSize: 14.sp,
                        ),
                        textAlign: TextAlign.left,
                      ),
                    ),
                  if (widget.rawStr.isEmpty) ...[
                    ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemBuilder: (context, index) {
                        IconTextActionModel item = list[index];
                        return IconTextAction(
                          key: ValueKey('${item.id}$index'),
                          highlight: false,
                          disabled: widget.disabled,
                          data: item,
                          extra: widget.extra == true
                              ? TextButton(
                                  onPressed: () {
                                    widget.onSaveOne?.call(item);
                                  },
                                  child: Text(
                                    '保留',
                                    style: TextStyle(
                                        fontSize: 12.sp,
                                        color: MyColors.primaryColor
                                            .withOpacity(0.7)),
                                  ))
                              : null,
                        );
                      },
                      itemCount: list.length,
                    ),
                  ]
                ]),
              ),
            ],
          ),
          theme: ExpandableThemeData(
            hasIcon: false,
            tapHeaderToExpand: false,
          ),
        ),
        16.verticalSpace,
      ],
    );
  }
}
