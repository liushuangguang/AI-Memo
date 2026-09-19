import 'package:ainote_app/app/api/note_theme.dart';
import 'package:ainote_app/app/utils/note_image.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/theme_related_list.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/delete_theme_sheet.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class ThemeCardWidget extends StatelessWidget {
  final NoteModel? note;
  final NoteThemeModel? theme;
  final bool isCustom;
  final Future<void> Function()? onDeleted;

  const ThemeCardWidget({
    super.key,
    this.note,
    this.theme,
    this.isCustom = false,
    this.onDeleted,
  }) : assert((note == null) != (theme == null));

  Widget _labelWidget(int labelType) {
    // 标签Widget，需要修改
    String lableText = "";
    if (labelType == 1) {
      lableText = "置顶";
    } else if (labelType == 2) {
      lableText = "自动整理中";
    } else if (labelType == 3) {
      lableText = "已整理";
    }
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(5),
        color:
            labelType == 1 ? MyColors.pageBackgroundColor : Color(0xffE6E9FF),
      ),
      padding: EdgeInsets.symmetric(horizontal: 4.w, vertical: 2.h),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Visibility(
            visible: labelType == 2,
            child: CupertinoActivityIndicator(
              radius: 6.0,
              animating: true,
              color: Colors.blue,
            ),
          ),
          Text(
            lableText,
            style: TextStyle(
              color: labelType == 1 ? MyColors.thirdColor : MyColors.colorBlue,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (theme != null) return _buildCustomTheme(context, theme!);
    final currentNote = note!;
    var noteContent = deltaStringToText(currentNote.content ?? '');
    // 去除 json 相关字符串
    noteContent = noteContent.replaceAll(RegExp(r'[{}\n#-*]'), '');

    return GestureDetector(
      onTap: () => Get.toNamed(Routes.NOTE_EDIT,
          arguments: currentNote,
          parameters: {'mode': NoteEditMode.preview.name}),
      child: Column(
        children: [
          Padding(
            padding: EdgeInsets.zero,
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(5),
              ),
              margin: EdgeInsets.symmetric(horizontal: 15.w, vertical: 5.h),
              padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 8.h),
              child: Padding(
                padding: EdgeInsets.zero,
                child: Row(
                  children: [
                    Visibility(
                      visible: currentNote.imageUrl != null,
                      child: Container(
                        width: 54.w,
                        height: 54.h,
                        margin: EdgeInsets.only(right: 8.w),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(5),
                        ),
                        child: Image.network(
                          noteImageUrl(currentNote.imageUrl ?? ""),
                          headers: noteImageHeaders(currentNote.imageUrl ?? ""),
                          width: 54.w,
                          height: 54.w,
                          fit: BoxFit.contain,
                          errorBuilder: (context, error, stackTrace) =>
                              Container(),
                        ),
                      ),
                    ),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  currentNote.title ?? "无标题",
                                  style: TextStyle(
                                      fontSize: 16.sp,
                                      fontWeight: FontWeight.w700),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Visibility(
                                visible: false, //这里要改成标签类型
                                child: _labelWidget(currentNote.noteType ?? 0),
                              ),
                            ],
                          ),
                          8.verticalSpace,
                          Text(
                            noteContent ?? "(空)",
                            style: TextStyle(
                              fontSize: 14.sp,
                              color: (noteContent ?? "").isEmpty
                                  ? MyColors.thirdColor
                                  : MyColors.secondaryColor,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          )
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCustomTheme(BuildContext context, NoteThemeModel currentTheme) {
    final latest = currentTheme.latestMerge;
    final imageUrl = latest?.imageUrls.isNotEmpty == true
        ? latest!.imageUrls.first
        : null;
    return GestureDetector(
      onTap: currentTheme.mergedNoteId == null
          ? () => _openCandidates(context, currentTheme)
          : () => _openNote(currentTheme.mergedNoteId!),
      onLongPress: () {
        showCupertinoModalBottomSheet<void>(
          context: context,
          enableDrag: false,
          useRootNavigator: true,
          builder: (_) => DeleteThemeSheet(
            onDelete: () async {
              try {
                await NoteThemeApi.deleteTheme(currentTheme.id);
                await onDeleted?.call();
              } catch (_) {
                Toast.error('主题删除失败，请重试');
              }
            },
          ),
        );
      },
      child: Container(
        margin: EdgeInsets.symmetric(horizontal: 15.w, vertical: 5.h),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 12.h),
              child: Row(
                children: [
                  if (imageUrl != null)
                    Padding(
                      padding: EdgeInsets.only(right: 10.w),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(5),
                        child: Image.network(
                          noteImageUrl(imageUrl),
                          headers: noteImageHeaders(imageUrl),
                          width: 54.w,
                          height: 54.w,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) =>
                              const SizedBox.shrink(),
                        ),
                      ),
                    ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          currentTheme.theme,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        8.verticalSpace,
                        Text(
                          latest?.mergedContentPreview.isNotEmpty == true
                              ? latest!.mergedContentPreview
                              : currentTheme.description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 14.sp,
                            color: MyColors.secondaryColor,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            if (currentTheme.mergeHistory.isEmpty)
              Padding(
                padding: EdgeInsets.fromLTRB(12.w, 0, 12.w, 12.h),
                child: Text(
                  '尚未生成合并备忘录',
                  style: TextStyle(fontSize: 12.sp, color: MyColors.thirdColor),
                ),
              )
            else
              ExpansionTile(
                tilePadding: EdgeInsets.symmetric(horizontal: 12.w),
                title: Text(
                  '合并记录 ${currentTheme.mergeHistory.length}',
                  style: TextStyle(fontSize: 12.sp, color: MyColors.thirdColor),
                ),
                children: currentTheme.mergeHistory.reversed
                    .map((history) => _historyTile(history))
                    .toList(growable: false),
              ),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: () => _openCandidates(context, currentTheme),
                icon: const Icon(Icons.auto_awesome, size: 16),
                label: Text(currentTheme.mergeHistory.isEmpty
                    ? '开始整合'
                    : '继续整合'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _historyTile(NoteThemeMergeHistoryModel history) {
    final date = history.mergedAt;
    final dateText = date == null
        ? ''
        : '${date.month.toString().padLeft(2, '0')}/'
            '${date.day.toString().padLeft(2, '0')} '
            '${date.hour.toString().padLeft(2, '0')}:'
            '${date.minute.toString().padLeft(2, '0')}';
    return Padding(
      padding: EdgeInsets.fromLTRB(12.w, 0, 12.w, 12.h),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$dateText ${history.mergedTitle}'.trim(),
            style: TextStyle(fontSize: 13.sp, fontWeight: FontWeight.w600),
          ),
          SizedBox(height: 6.h),
          Wrap(
            spacing: 6.w,
            runSpacing: 4.h,
            children: history.sources
                .map(
                  (source) => ActionChip(
                    label: Text(source.title),
                    onPressed: () => _openNote(source.noteId),
                  ),
                )
                .toList(growable: false),
          ),
        ],
      ),
    );
  }

  Future<void> _openNote(String noteId) async {
    try {
      final target = await NoteThemeApi.getNote(noteId);
      await Get.toNamed(
        Routes.NOTE_EDIT,
        arguments: target,
        parameters: {'mode': NoteEditMode.preview.name},
      );
    } catch (_) {
      Toast.error('备忘录打开失败，可能已被删除');
    }
  }

  Future<void> _openCandidates(
      BuildContext context, NoteThemeModel currentTheme) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      useSafeArea: false,
      builder: (_) => ThemeRelatedList(theme: currentTheme),
    );
    await onDeleted?.call();
  }
}
