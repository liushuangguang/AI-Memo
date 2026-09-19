import 'package:ainote_app/app/data/constants.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_module/note_module_payload.dart';
import 'package:ainote_app/app/data/models/note_module/text_data_module.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';
import 'package:ainote_app/app/widgets/secondary_btn.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:share_plus/share_plus.dart';

import '../../generated/assets.dart';
import '../config/theme/my_colors.dart';
import '../data/stores/my_secure_storage.dart';

void shareNote({required String noteId}) async {
  final deviceId = await MySecureStorage.getDeviceId();

  if (deviceId.isEmpty || noteId.isEmpty) {
    Toast.info("分享失败，请稍后再试");
    return;
  }

  Share.share(
      '【AI备忘录】用过吗，看看我刚写的内容 $shareNoteUrl?noteId=$noteId&deviceId=$deviceId');

  Share.share('【AI备忘录】用过吗，看看我刚写的内容 $shareDownloadUrl');
}

void showNoteActionSheet(context,
    {required Function onDelete, required String noteId}) {
  showCustomModalBottomSheet(
      context: context,
      builder: (build) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                shareNote(noteId: noteId);
              },
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 16.w),
                child: Row(
                  children: [
                    Padding(
                      padding: EdgeInsets.only(right: 12.w),
                      child: Icon(Icons.share),
                    ),
                    Text(
                      '分享',
                      style: TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 16.sp,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Divider(height: 1, color: Color(0xFFDEDEDE)),
            GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                confirmDelete(context, onDelete: onDelete);
              },
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 16.w),
                child: Row(
                  children: [
                    Padding(
                      padding: EdgeInsets.only(right: 12.w),
                      child: SvgPicture.asset(
                        Assets.imagesDelete,
                        width: 24.w,
                        height: 24.w,
                      ),
                    ),
                    Text(
                      '删除',
                      style: TextStyle(
                        color: MyColors.colorDeleteRed,
                        fontWeight: FontWeight.w500,
                        fontSize: 16.sp,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
      containerWidget:
          (BuildContext context, Animation<double> animation, Widget child) {
        return Container(
          margin: EdgeInsets.all(15.w),
          padding: EdgeInsets.all(16.w),
          constraints: BoxConstraints(minHeight: 40.w),
          decoration: BoxDecoration(
              color: Colors.white, borderRadius: BorderRadius.circular(20.w)),
          child: child,
        );
      });
}

void confirmDelete(context, {required Function onDelete}) {
  showCupertinoModalBottomSheet(
      context: context,
      builder: (buildContext) {
        return SafeArea(
          child: Container(
            padding: EdgeInsets.only(
                top: 32.w, left: 16.w, right: 16.w, bottom: 32.w),
            color: Colors.white,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '确定删除吗？',
                  style:
                      TextStyle(fontSize: 20.sp, fontWeight: FontWeight.w700),
                  textAlign: TextAlign.center,
                ),
                12.verticalSpace,
                PrimaryBtn(
                  text: '删除',
                  backgroundColor: MyColors.colorDeleteRed,
                  onPressed: () {
                    onDelete();
                    Toast.success("删除成功");
                    Navigator.pop(buildContext);
                  },
                ),
                12.verticalSpace,
                SecondaryBtn(
                  text: '取消',
                  onPressed: () {
                    Navigator.pop(buildContext);
                  },
                )
              ],
            ),
          ),
        );
      });
}

// 将TextDataModule内容同步到Note
void setTextDataToQuill(NoteModel note, MyQuillController quillLogic,
    Function(dynamic textDataModule)? fn) {
  TextDataModule? textDataModule = findNoteModuleByType<TextDataModule>(
      note.modules, NoteModuleType.TEXT_DATA, TextDataModule.fromJson);
  if (textDataModule is TextDataModule) {
    note.content = textDataModule.content;
    fn?.call(textDataModule);
  }

  String content = note.content ?? '';
  quillLogic.setQuillDocumentFromJsonString(content);
}

// 将Note内容同步到TextDataModule
void setQuillToTextData(NoteModel note, MyQuillController quillLogic,
    Function(dynamic textDataModule)? fn) {
  var (index, textDataModule) = findNoteModuleByTypeAndIndex(
      note.modules, NoteModuleType.TEXT_DATA, TextDataModule.fromJson);

  if (textDataModule is TextDataModule) {
    textDataModule.content = note.content;
    textDataModule.title = note.title;
    note.modules?[index ?? 0] = textDataModule.toJson();
    fn?.call(textDataModule);
  }

  String content = note.content ?? '';
  quillLogic.setQuillDocumentFromJsonString(content);
}

void showCommonBottomSheet({
  required BuildContext context,
  String? title,
  Widget? leading,
  required Widget content,
  Widget? trailing,
  VoidCallback? onSave,
  bool? expanded = true,
  Color? backgroundColor,
}) {
  final child = SafeArea(
    child: Container(
      constraints: BoxConstraints(maxHeight: Get.height - 200),
      decoration: BoxDecoration(
        color: backgroundColor ?? Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(16.w),
          topRight: Radius.circular(16.w),
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.w),
            child: ListTile(
              contentPadding: EdgeInsets.all(0),
              leading: leading,
              title: Row(
                children: [
                  Expanded(
                    child: Text(
                      title ?? '',
                      style: TextStyle(
                          fontSize: 16.sp, fontWeight: FontWeight.w700),
                      textAlign: TextAlign.left,
                    ),
                  ),
                ],
              ),
              trailing: trailing ??
                  Visibility(
                    visible: onSave != null,
                    child: TextButton(
                      onPressed: onSave,
                      child: Text('保存',
                          style: TextStyle(
                              fontSize: 14, color: MyColors.colorBlue)),
                    ),
                  ),
            ),
          ),
          Container(
            margin: EdgeInsets.symmetric(horizontal: 16.w),
            height: 1.w,
            width: Get.width,
            color: Color(0xFFE1E1E1),
          ),
          expanded == true
              ? Expanded(
                  child: content,
                )
              : content,
        ],
      ),
    ),
  );

  // showCupertinoModalBottomSheet(
  //   context: context,
  //   expand: false,
  //   enableDrag: false,
  //   useRootNavigator: true,
  //   builder: (_) {
  //     return child;
  //   },
  // );

  SmartDialog.show(
    alignment: Alignment.bottomCenter,
    builder: (_) => child,
  );
}
