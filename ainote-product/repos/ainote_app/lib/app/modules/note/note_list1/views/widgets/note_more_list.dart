import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:loading_more_list/loading_more_list.dart';

import '../../bindings/note_list_repository.dart';
import 'note_card_widget.dart';

class NoteMoreList extends StatelessWidget {
  final NoteListBaseRepository noteRepository;

  const NoteMoreList({
    super.key,
    required this.noteRepository,
  });

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: noteRepository.refresh,
      child: LoadingMoreList(
        ListConfig<NoteModel>(
          itemBuilder: (
            context,
            item,
            index,
          ) {
            return NoteCardWidget(
              note: item,
            );
          },
          sourceList: noteRepository,
          indicatorBuilder: (context, status) => SizedBox(
            height: 40.h,
            child: Center(
              child: getLoadStatusWidget(status, noteRepository.length),
            ),
          ),
          padding: const EdgeInsets.all(0.0),
        ),
      ),
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
