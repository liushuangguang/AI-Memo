import 'package:ainote_app/app/api/note_theme.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:ainote_app/app/modules/note/note_list1/bindings/ai_theme_list_repository.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/theme_card_widget.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/theme_related_list.dart';
import 'package:ainote_app/app/widgets/new_theme_sheet.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:loading_more_list/loading_more_list.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class AiThemeList extends StatefulWidget {
  final AIThemeListRepository aiThemeListRepository;

  const AiThemeList({
    super.key,
    required this.aiThemeListRepository,
  });

  @override
  State<AiThemeList> createState() => _AiThemeListState();
}

class _AiThemeListState extends State<AiThemeList> {
  late AIThemeListRepository _repository;

  @override
  void initState() {
    super.initState();
    _repository = widget.aiThemeListRepository;
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
              Text(
                '自定义主题',
                style: TextStyle(
                  fontSize: 16.sp,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Spacer(),
              GestureDetector(
                onTap: () async {
                  NoteThemeModel? createdTheme;
                  await showCupertinoModalBottomSheet<void>(
                    context: context,
                    enableDrag: false,
                    useRootNavigator: true,
                    builder: (context) {
                      return NewThemeSheet(
                        onThemeCreated: (content, desc) async {
                          createdTheme = await NoteThemeApi.createTheme(
                            theme: content,
                            description: desc,
                          );
                        },
                      );
                    },
                  );
                  if (!mounted || createdTheme == null) return;
                  await _repository.refresh();
                  if (!mounted) return;
                  await showDialog<void>(
                    context: context,
                    barrierDismissible: false,
                    useSafeArea: false,
                    builder: (_) => ThemeRelatedList(theme: createdTheme!),
                  );
                  await _repository.refresh();
                },
                child: Text(
                  '新增主题',
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: MyColors.colorBlue,
                  ),
                ),
              )
            ],
          ),
        ),
        Expanded(
          child: LoadingMoreList(
            ListConfig<NoteThemeModel>(
              itemBuilder: (
                context,
                item,
                index,
              ) {
                return ThemeCardWidget(
                  theme: item,
                  isCustom: true,
                  onDeleted: () async {
                    await _repository.refresh();
                  },
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
