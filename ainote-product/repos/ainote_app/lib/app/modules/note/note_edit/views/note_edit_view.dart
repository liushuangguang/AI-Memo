import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/ai_assist_content.dart';
import 'package:ainote_app/app/modules/home/controllers/home_controller.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/edit_tool_bar.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/note_auto_save.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/note_header.dart';
import 'package:ainote_app/app/utils/date_format.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/utils/quill_delta_replacements.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/dot_grid_container.dart';
import 'package:ainote_app/app/widgets/save_before_leaving.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_editor.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'package:get/get.dart';

import 'package:flutter_quill/flutter_quill.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:skeletonizer/skeletonizer.dart';

class NoteEditView extends GetView<NoteEditController> {
  const NoteEditView({super.key});

  @override
  Widget build(BuildContext context) {
    final homeLogic = Get.find<HomeController>();
    final logic = Get.put(NoteEditController());
    final viewInsetsBottom = MediaQuery.of(context).viewInsets.bottom;

    Future<bool> saveBeforeLeaving() async {
      if (!await logic.prepareToLeave() || !context.mounted) return false;
      FocusManager.instance.primaryFocus?.unfocus();
      if (logic.note.value.id?.isNotEmpty == true)
        homeLogic.updateNote(logic.note.value);
      return true;
    }

    return SaveBeforeLeaving(
        save: saveBeforeLeaving,
        child: Stack(
          children: [
            Container(
              width: double.infinity,
              height: double.infinity,
              decoration: BoxDecoration(
                  color: MyColors.pageBackgroundBlackColor,
                  image: DecorationImage(
                      image: AssetImage(Assets.imagesStarsBg),
                      repeat: ImageRepeat.repeat,
                      fit: BoxFit.cover)),
            ),
            Scaffold(
              resizeToAvoidBottomInset: false,
              backgroundColor: Colors.transparent,
              appBar: AppBar(
                systemOverlayStyle: SystemUiOverlayStyle(
                  statusBarColor: Colors.transparent,
                  statusBarIconBrightness: Brightness.light,
                  statusBarBrightness: Brightness.light,
                ),
                backgroundColor: Colors.transparent,
                iconTheme: IconThemeData(color: Colors.white),
                centerTitle: true,
                leading: IconButton(
                  onPressed: () => Navigator.of(context).maybePop(),
                  icon: Icon(Icons.arrow_back_ios),
                ),
                actions: [
                  QuillToolbarHistoryButton(
                    controller: logic.quillLogic.quillController,
                    isUndo: true,
                    options: QuillToolbarHistoryButtonOptions(
                      childBuilder: (options, extraOptions) {
                        return IconButton(
                          onPressed: () {
                            extraOptions.onPressed?.call();
                          },
                          icon: SvgPicture.asset(
                            Assets.editToolBarUndo,
                            theme: SvgTheme(
                                currentColor:
                                    logic.quillLogic.quillController.hasUndo
                                        ? MyColors.editToolBarActiveColor
                                        : MyColors.editToolBarInActiveColor),
                          ),
                        );
                      },
                    ),
                  ),
                  QuillToolbarHistoryButton(
                    controller: logic.quillLogic.quillController,
                    isUndo: false,
                    options: QuillToolbarHistoryButtonOptions(
                      childBuilder: (options, extraOptions) {
                        return IconButton(
                          onPressed: () {
                            extraOptions.onPressed?.call();
                          },
                          icon: SvgPicture.asset(
                            Assets.editToolBarRedo,
                            theme: SvgTheme(
                                currentColor:
                                    logic.quillLogic.quillController.hasRedo
                                        ? MyColors.editToolBarActiveColor
                                        : MyColors.editToolBarInActiveColor),
                          ),
                        );
                      },
                    ),
                  ),
                  Obx(() {
                    return Visibility(
                      visible: !logic.noteHasSaved.value,
                      child: QuillToolbarHistoryButton(
                        controller: logic.quillLogic.quillController,
                        isUndo: true,
                        options: QuillToolbarHistoryButtonOptions(
                          childBuilder: (options, extraOptions) {
                            var canSave = !logic
                                    .quillLogic.quillIsEmpty.value &&
                                (logic.quillLogic.quillController.hasRedo ||
                                    logic.quillLogic.quillController.hasUndo);
                            return IconButton(
                              onPressed: () {
                                if (canSave) {
                                  logic.quillLogic.quillFocusNode.unfocus();
                                  Future.microtask(logic.saveNote);
                                }
                              },
                              icon: SvgPicture.asset(
                                Assets.editToolBarDone,
                                theme: SvgTheme(
                                    currentColor: canSave
                                        ? MyColors.editToolBarActiveColor
                                        : MyColors.editToolBarInActiveColor),
                              ),
                            );
                          },
                        ),
                      ),
                    );
                  }),
                  Obx(() {
                    return Visibility(
                      visible: logic.noteHasSaved.value,
                      child: IconButton(
                        onPressed: () => showNoteActionSheet(context,
                            noteId: logic.note.value.id!, onDelete: () {
                          logic.deleteNote();
                          Future.microtask(() {
                            Get.back();
                          });
                        }),
                        icon: SvgPicture.asset(
                          Assets.editToolBarMore,
                        ),
                      ),
                    );
                  }),
                ],
              ),
              body: SafeArea(
                  child: Column(
                children: [
                  Obx(() => Visibility(
                        visible: logic.showQuickComplete.value &&
                            logic.noteHasSaved.value,
                        child: Padding(
                          padding: const EdgeInsets.only(
                              left: 16.0, right: 16.0, bottom: 12),
                          child: Container(
                            height: 46.w,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(12),
                              color: Colors.white,
                            ),
                            padding: EdgeInsets.all(12),
                            child: Row(
                              children: [
                                Padding(
                                  padding: const EdgeInsets.only(right: 12.0),
                                  child: Image.asset(
                                    Assets.imagesWriteDocIcon,
                                    width: 20.w,
                                    height: 20.w,
                                  ),
                                ),
                                Text(
                                  '帮您快速完善备忘录内容',
                                  style: TextStyle(
                                      fontSize: 14.sp,
                                      color: MyColors.primaryColor,
                                      fontWeight: FontWeight.w500),
                                ),
                                Spacer(),
                                Padding(
                                  padding: const EdgeInsets.only(right: 24.0),
                                  child: GestureDetector(
                                    onTap: () {
                                      logic.showQuickComplete.value = false;
                                    },
                                    child: Text(
                                      '忽略',
                                      style: TextStyle(
                                          fontSize: 14.sp,
                                          color: MyColors.colorBlue,
                                          fontWeight: FontWeight.w400),
                                    ),
                                  ),
                                ),
                                GestureDetector(
                                  onTap: () async {
                                    logic.showQuickComplete.value = false;
                                    final noteContent = logic
                                        .quillLogic.quillController.document
                                        .toPlainText();
                                    if (noteContent.trim().isEmpty) {
                                      Toast.info('请先输入需要完善的内容');
                                      return;
                                    }

                                    Map<String, String>? result;
                                    try {
                                      final completionItems =
                                          await AiApi.completeInfo(noteContent);
                                      if (completionItems.isEmpty) {
                                        Toast.info('当前内容无需补充');
                                        return;
                                      }
                                      final routeResult = await Get.toNamed(
                                          '/ai-additional',
                                          arguments: {
                                            'noteContent': noteContent,
                                            'toAdditional': completionItems
                                                .map((item) => item.vaguePhrase)
                                                .toList(),
                                            'toAdditionalsOptions':
                                                completionItems
                                                    .map((item) => item.options)
                                                    .toList(),
                                          });
                                      if (routeResult is Map) {
                                        result = routeResult.map((key, value) =>
                                            MapEntry(key.toString(),
                                                value.toString()));
                                      }
                                    } catch (_) {
                                      logic.showQuickComplete.value = true;
                                      Toast.error('信息补充暂时不可用，请稍后重试');
                                      return;
                                    }

                                    if (result != null) {
                                      final replacementResult =
                                          replaceVaguePhrasesInDelta(
                                        logic
                                            .quillLogic.quillController.document
                                            .toDelta(),
                                        result,
                                      );
                                      if (replacementResult.replacementCount ==
                                          0) {
                                        Toast.info('未在当前内容中找到待补充文字');
                                        return;
                                      }
                                      logic.quillLogic.quillController
                                          .document = Document.fromDelta(
                                        replacementResult.delta,
                                      );
                                      logic.setNoteHasSaved(false);
                                      final started = await logic
                                          .saveQuickCompletionAndStartSmartOrganize();
                                      if (!started) {
                                        logic.setQuillIsReadOnly(false);
                                        Toast.error('补充内容保存失败，请稍后重试');
                                        return;
                                      }
                                      Toast.success('补充成功，开始智能整理');
                                    }
                                  },
                                  child: Text(
                                    '开始',
                                    style: TextStyle(
                                        fontSize: 14.sp,
                                        color: MyColors.colorBlue,
                                        fontWeight: FontWeight.w400),
                                  ),
                                )
                              ],
                            ),
                          ),
                        ),
                      )),
                  Stack(
                    children: [
                      if (logic.note.value.title != null &&
                          logic.note.value.title!.isNotEmpty)
                        Padding(
                          padding: EdgeInsets.symmetric(horizontal: 16.0.w),
                          child: NoteHeader(),
                        ),
                      if (logic.isThemeLoading.value)
                        Padding(
                          padding: EdgeInsets.symmetric(horizontal: 16.0.w),
                          child: NoteHeader(
                            isThemeLoading: true,
                            disabled: true,
                          ),
                        ),
                      Column(
                        mainAxisSize: MainAxisSize.max,
                        children: [
                          GestureDetector(
                            onTap: () {
                              logic.setQuillIsReadOnly(false);
                              logic.quillLogic.quillFocusNode.requestFocus();
                            },
                            child: logic.isThemeLoading.value
                                ? Container(
                                    width: double.infinity,
                                    height: viewInsetsBottom > 0
                                        ? 250.w
                                        : logic.aiModuleVisible.value
                                            ? Get.height - 450.w
                                            : Get.height - 280.w - 55.w,
                                    margin: EdgeInsets.only(
                                        left: 16.w,
                                        right: 16.w,
                                        bottom: 8.w,
                                        top: (logic.note.value.title != null &&
                                                    logic.note.value.title!
                                                        .isNotEmpty) ||
                                                logic.isThemeLoading.value
                                            ? 46.w
                                            : 0),
                                    decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(12),
                                      color: MyColors.editorBackgroundColor,
                                    ),
                                    child: Skeletonizer(
                                      enabled: true,
                                      enableSwitchAnimation: true,
                                      child: Padding(
                                        padding: EdgeInsets.all(16.w),
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: List.generate(
                                              5,
                                              (index) => Padding(
                                                    padding: EdgeInsets.only(
                                                        bottom: 16.w),
                                                    child: Container(
                                                      height: 28.w,
                                                      width: double.infinity,
                                                      decoration: BoxDecoration(
                                                        color: MyColors
                                                            .skeletonColor,
                                                        borderRadius:
                                                            BorderRadius
                                                                .circular(12),
                                                      ),
                                                    ),
                                                  )),
                                        ),
                                      ),
                                    ),
                                  )
                                : DotGridContainer(
                                    centered: false,
                                    constraints: BoxConstraints(
                                        minHeight: 250.w,
                                        maxHeight: viewInsetsBottom > 0
                                            ? 250.w
                                            : logic.aiModuleVisible.value
                                                ? Get.height - 450.w
                                                : Get.height - 280.w),
                                    margin: EdgeInsets.only(
                                        left: 16.w,
                                        right: 16.w,
                                        bottom: 8.w,
                                        top: (logic.note.value.title != null &&
                                                    logic.note.value.title!
                                                        .isNotEmpty) ||
                                                logic.isThemeLoading.value
                                            ? 46.w
                                            : 0),
                                    child: MyQuillEditor(
                                      autoFocus: !logic.noteOnlyView.value,
                                      tag: logic.quillTag,
                                      onFormatText: () {
                                        logic.setEditorToolVisible(true);
                                      },
                                      onContentAssist: () {
                                        if (logic.quillLogic.selectedText.value
                                            .isEmpty) {
                                          Toast.info('请先选中文本');
                                          return;
                                        }
                                        showCupertinoModalBottomSheet(
                                            context: Get.context!,
                                            enableDrag: false,
                                            useRootNavigator: true,
                                            builder: (context) {
                                              return AiAssistContent(
                                                  selectedContentText: logic
                                                      .quillLogic
                                                      .selectedText
                                                      .value);
                                            });
                                      },
                                    )),
                          ),
                          if (logic.note.value.updatedAt != '')
                            Padding(
                              padding: EdgeInsets.symmetric(horizontal: 16.0.w),
                              child: Row(
                                  mainAxisAlignment: MainAxisAlignment.start,
                                  children: [
                                    SvgPicture.asset(Assets.imagesDate),
                                    4.horizontalSpace,
                                    Text(
                                        parseApiDateString(
                                            logic.note.value.updatedAt!),
                                        style: TextStyle(
                                            fontSize: 10.sp,
                                            color: MyColors.colorWhite
                                                .withOpacity(0.5)))
                                  ]),
                            ),
                          12.w.verticalSpace,
                        ],
                      ),
                      Obx(() {
                        return Visibility(
                          visible: viewInsetsBottom <= 0 &&
                              logic.aiModuleVisible.value &&
                              logic.aiCardList.isNotEmpty,
                          child: Positioned(
                            bottom: 0,
                            left: 0,
                            right: 0,
                            child: SingleChildScrollView(
                              physics: const BouncingScrollPhysics(),
                              scrollDirection: Axis.horizontal,
                              child: Padding(
                                padding:
                                    EdgeInsets.only(left: 16.w, right: 4.w),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.start,
                                  mainAxisSize: MainAxisSize.min,
                                  children: logic.aiCardList,
                                ),
                              ),
                            ),
                          ),
                        );
                      }),
                    ],
                  ),
                ],
              )),
              bottomNavigationBar: Padding(
                padding: EdgeInsets.only(bottom: viewInsetsBottom),
                child: Obx(() => logic.isFromTheme.value &&
                        logic.isThemeLoading.value
                    ? Padding(
                        padding: EdgeInsets.only(
                            left: 16.0,
                            right: 16.0,
                            bottom: MediaQuery.of(context).padding.bottom + 10),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              decoration: BoxDecoration(
                                color: MyColors.chipBgPurple,
                                borderRadius: BorderRadius.circular(20),
                              ),
                              padding: EdgeInsets.symmetric(
                                  horizontal: 12.w, vertical: 2.w),
                              child: Text(
                                '5篇相关备忘录',
                                style: TextStyle(
                                    fontSize: 12.sp,
                                    fontWeight: FontWeight.w700,
                                    color: MyColors.chipColorPurple),
                              ),
                            ),
                            10.w.verticalSpace,
                            Stack(
                              children: [
                                Padding(
                                  padding: EdgeInsets.only(
                                      left: 32.0, right: 32.0, top: 15.0),
                                  child: Container(
                                    padding: EdgeInsets.all(16.w),
                                    decoration: BoxDecoration(
                                      color: MyColors.darkCardBgColor3,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Row(
                                      children: [
                                        Image.asset(Assets.imagesNoteSampleIcon,
                                            width: 32.w, height: 32.w),
                                        12.horizontalSpace,
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment:
                                                CrossAxisAlignment.start,
                                            children: [
                                              Text('调理肠胃的日常食物',
                                                  style: TextStyle(
                                                      fontSize: 12.sp,
                                                      fontWeight:
                                                          FontWeight.w700,
                                                      color:
                                                          MyColors.colorWhite)),
                                              Text(
                                                  '全谷物：如燕麦、糙米、全麦面包等，它们富含纤维，有助于促进肠道蠕动。',
                                                  maxLines: 1,
                                                  overflow:
                                                      TextOverflow.ellipsis,
                                                  style: TextStyle(
                                                    fontSize: 10.sp,
                                                    fontWeight: FontWeight.w400,
                                                    color: Colors.white,
                                                  ))
                                            ],
                                          ),
                                        )
                                      ],
                                    ),
                                  ),
                                ),
                                Padding(
                                  padding: EdgeInsets.only(
                                      left: 10.0, right: 10.0, top: 9.0),
                                  child: Container(
                                    padding: EdgeInsets.all(16.w),
                                    decoration: BoxDecoration(
                                      color: MyColors.darkCardBgColor2,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Row(
                                      children: [
                                        Image.asset(Assets.imagesNoteSampleIcon,
                                            width: 32.w, height: 32.w),
                                        12.horizontalSpace,
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment:
                                                CrossAxisAlignment.start,
                                            children: [
                                              Text('调理肠胃的日常食物',
                                                  style: TextStyle(
                                                      fontSize: 12.sp,
                                                      fontWeight:
                                                          FontWeight.w700,
                                                      color:
                                                          MyColors.colorWhite)),
                                              Text(
                                                  '全谷物：如燕麦、糙米、全麦面包等，它们富含纤维，有助于促进肠道蠕动。',
                                                  maxLines: 1,
                                                  overflow:
                                                      TextOverflow.ellipsis,
                                                  style: TextStyle(
                                                    fontSize: 10.sp,
                                                    fontWeight: FontWeight.w400,
                                                    color: Colors.white,
                                                  ))
                                            ],
                                          ),
                                        )
                                      ],
                                    ),
                                  ),
                                ),
                                Container(
                                  padding: EdgeInsets.all(16.w),
                                  decoration: BoxDecoration(
                                    color: MyColors.darkCardBgColor,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Row(
                                    children: [
                                      Image.asset(Assets.imagesNoteSampleIcon,
                                          width: 32.w, height: 32.w),
                                      12.horizontalSpace,
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text('调理肠胃的日常食物',
                                                style: TextStyle(
                                                    fontSize: 12.sp,
                                                    fontWeight: FontWeight.w700,
                                                    color:
                                                        MyColors.colorWhite)),
                                            Text(
                                                '全谷物：如燕麦、糙米、全麦面包等，它们富含纤维，有助于促进肠道蠕动。',
                                                maxLines: 1,
                                                overflow: TextOverflow.ellipsis,
                                                style: TextStyle(
                                                  fontSize: 10.sp,
                                                  fontWeight: FontWeight.w400,
                                                  color: Colors.white,
                                                ))
                                          ],
                                        ),
                                      )
                                    ],
                                  ),
                                ),
                              ],
                            )
                          ],
                        ),
                      )
                    : EditToolBar()),
              ),
            ),
            NoteAutoSave(
              onBackground: logic.startAiAutoAnalysis,
            ),
          ],
        ));
  }
}
