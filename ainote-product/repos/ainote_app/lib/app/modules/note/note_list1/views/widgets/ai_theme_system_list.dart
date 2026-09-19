import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/note/note_list1/bindings/ai_theme_system_list_repository.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/note_card_widget.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/theme_card_widget.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:loading_more_list/loading_more_list.dart';

import '../../bindings/ai_theme_system_list_repository.dart';

class AiThemeSystemList extends StatefulWidget {
  final AIThemeSystemListRepository aiThemeSystemListRepository;

  const AiThemeSystemList({
    super.key,
    required this.aiThemeSystemListRepository,
  });

  @override
  State<AiThemeSystemList> createState() => _AiThemeSystemListState();
}

class _AiThemeSystemListState extends State<AiThemeSystemList> {
  late AIThemeSystemListRepository _repository;

  @override
  void initState() {
    super.initState();
    _repository = widget.aiThemeSystemListRepository;
    _repository.loadData();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
          child: Row(
            children: [
              Image(
                  image: AssetImage(Assets.imagesAiThemeSystemIcon),
                  width: 20.w,
                  height: 20.w),
              SizedBox(width: 12.w),
              Text(
                '系统主题',
                style: TextStyle(
                  fontSize: 16.sp,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: LoadingMoreList(
            ListConfig<NoteModel>(
              itemBuilder: (
                context,
                item,
                index,
              ) {
                return ThemeCardWidget(
                  note: item,
                  isCustom: false,
                );
              },
              sourceList: _repository,
              indicatorBuilder: (context, status) => SizedBox(
                height: 40.h,
                child: Center(
                  child: getLoadStatusWidget(status, _repository.length),
                ),
              ),
              padding: const EdgeInsets.all(0.0),
            ),
          ),
        ),
      ],
    );
  }

  getLoadStatusWidget(IndicatorStatus status, int length) {
    switch (status) {
      case IndicatorStatus.loadingMoreBusying:
        return CupertinoActivityIndicator(
          color: MyColors.colorBlue,
        );
      case IndicatorStatus.noMoreLoad:
        if (length < 5) {
          return SizedBox.shrink();
        }
        return Text('没有更多数据了',
            style: TextStyle(fontSize: 14.sp, color: MyColors.colorGrey));
      case IndicatorStatus.empty:
        return Text('暂无任何备忘录',
            style: TextStyle(color: MyColors.colorGrey, fontSize: 14.sp));
      default:
        return CupertinoActivityIndicator(
          color: MyColors.colorBlue,
        );
    }
  }
}
