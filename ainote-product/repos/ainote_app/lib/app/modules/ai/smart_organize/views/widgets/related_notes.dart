import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/ai_related_note_model.dart';
import 'package:ainote_app/app/widgets/ai_card/card_container.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class SmartRelatedNotes extends StatelessWidget {
  final List<AiRelatedNoteModel> notes;
  final bool loading;
  final String message;

  const SmartRelatedNotes(
      {super.key,
      required this.notes,
      required this.loading,
      required this.message});

  String _displayTitle(AiRelatedNoteModel note) {
    final title = note.title?.trim();
    return title == null || title.isEmpty ? '未命名备忘录' : title;
  }

  void _showDetail(BuildContext context, AiRelatedNoteModel note) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(20.w, 20.w, 20.w, 28.w),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(_displayTitle(note),
                style: TextStyle(fontSize: 20.sp, fontWeight: FontWeight.bold)),
            16.verticalSpace,
            Text(note.content ?? '',
                style: TextStyle(fontSize: 16.sp, height: 1.5)),
            if ((note.reason ?? '').trim().isNotEmpty) ...[
              16.verticalSpace,
              Text('关联理由：${note.reason}',
                  style:
                      TextStyle(color: MyColors.thirdColor, fontSize: 14.sp)),
            ],
          ]),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final visibleNotes = notes.where((note) => note.isUsable).toList();
    final body = loading
        ? const Center(
            child: SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2)))
        : visibleNotes.isEmpty
            ? Padding(
                padding: EdgeInsets.symmetric(vertical: 8.w),
                child: Text(message.isEmpty ? '暂无相关备忘录' : message,
                    style:
                        TextStyle(color: MyColors.thirdColor, fontSize: 14.sp)))
            : Column(
                children: visibleNotes
                    .map((note) => InkWell(
                          onTap: () => _showDetail(context, note),
                          borderRadius: BorderRadius.circular(8.w),
                          child: ConstrainedBox(
                            constraints: BoxConstraints(minHeight: 48.w),
                            child: Padding(
                              padding: EdgeInsets.symmetric(vertical: 7.w),
                              child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(_displayTitle(note),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: TextStyle(
                                            fontSize: 16.sp,
                                            fontWeight: FontWeight.w600)),
                                    4.verticalSpace,
                                    Text(note.content ?? '',
                                        maxLines: 2,
                                        overflow: TextOverflow.ellipsis,
                                        style: TextStyle(fontSize: 14.sp)),
                                    if ((note.reason ?? '').trim().isNotEmpty)
                                      Text('关联理由：${note.reason}',
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(
                                              fontSize: 12.sp,
                                              color: MyColors.thirdColor)),
                                  ]),
                            ),
                          ),
                        ))
                    .toList());
    return CardContainer(
      bgColor: MyColors.cardBgWhite,
      borderColor: MyColors.cardBorderWhite,
      title: Text('相关备忘录',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16.sp)),
      content: body,
    );
  }
}
