import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/views/widgets/ai_suggestion.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/note_header.dart';
import 'package:ainote_app/app/widgets/ai_unmeaning.dart';
import 'package:ainote_app/app/widgets/dashed_line.dart';
import 'package:ainote_app/app/widgets/dot_grid_container.dart';
import 'package:ainote_app/app/widgets/icon_text_action.dart';
import 'package:ainote_app/app/widgets/loading_text.dart';
import 'package:ainote_app/app/widgets/ai_card/ai_illustration.dart';
import 'package:ainote_app/app/widgets/ai_card/auto_sort.dart';
import 'package:ainote_app/app/widgets/ai_card/content_assist.dart';
import 'package:ainote_app/app/widgets/ai_card/question_answer.dart';
import 'package:ainote_app/app/widgets/ai_card/note_enrichment.dart';
import 'package:ainote_app/app/widgets/ai_card/voice_discuss.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'package:get/get.dart';
import 'package:skeletonizer/skeletonizer.dart';

import '../controllers/smart_organize_controller.dart';
import 'widgets/related_notes.dart';

class SmartOrganizeView extends StatelessWidget {
  const SmartOrganizeView({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<SmartOrganizeController>();
    final noteEditLogic = Get.find<NoteEditController>();

    void handleSaveNote({bool replaceText = false}) async {
      noteEditLogic.applySavedNote(controller.note.value,
          replaceText: replaceText);
      // noteEditLogic.saveNote(true);
      Future.delayed(Duration(milliseconds: 10), () {
        SmartDialog.dismiss();
      });
      Future.delayed(Duration(milliseconds: 10), () {
        noteEditLogic.setQuillIsReadOnly(true);
      });
    }

    void handleReplaceAll() async {
      try {
        if (!await controller.replaceAllNote()) return;
        handleSaveNote(replaceText: true);
        Get.back();
      } catch (_) {
        Toast.error('替换失败，请重试；未清空原备忘录');
      }

      // The server's persisted modules are the source of truth. setNote above renders
      // them and resets the preview; rebuilding from that reset preview erased text.
    }

    void handleSaveAiIllustration() async {
      if (await controller.saveAiIllustration()) handleSaveNote();
    }

    void handleSaveQA(q) async {
      if (await controller.saveQuestionAnswer(q)) handleSaveNote();
    }

    void handleSaveScene() async {
      if (await controller.saveScene()) handleSaveNote();
    }

    void handleSaveCategory() async {
      if (await controller.saveCategory()) handleSaveNote();
    }

    AutoSort currentAutoSort() => AutoSort(
          list: controller.aiCategorizedNoteList.toList(),
          loading: controller.aiCategorizedNoteLoading.value ||
              controller.aiOrganizeLoading.value,
          onSaveCategory: handleSaveCategory,
          onSaveScene: handleSaveScene,
          scene: controller.aiOrganizeResult.value.userScenario,
          tags: controller.aiOrganizeResult.value.tag ?? const [],
          reason: controller.aiOrganizeResult.value.memoClassification,
          errorMessage: [
            controller.aiOrganizeMessage.value,
            controller.aiCategorizedNoteMessage.value
          ].where((message) => message.isNotEmpty).join('\n'),
          onRetry: () {
            if (controller.aiOrganizeMessage.isNotEmpty)
              controller.retryOrganize();
            if (controller.aiCategorizedNoteMessage.isNotEmpty)
              controller.retryCategories();
          },
          liveState: currentAutoSort,
        );

    Widget buildBody() {
      if (controller.aiValidateLoading.value == true) {
        return const AiUnmeaning();
      }
      if (controller.aiValidateLoading.value == false &&
          controller.aiValidateResult.value.meaningful == false) {
        return ListView(
          children: [
            48.verticalSpace,
            AiUnmeaning(text: controller.aiValidateResult.value.result),
            24.verticalSpace,
            // AdLink(),
            // AdLink(),
            // AdLink(),
          ],
        );
      }
      return ListView(
        children: [
          Stack(
            children: [
              NoteHeader(
                  disabled: true,
                  title: controller.aiOrganizeResult.value.title,
                  loading: controller.aiOrganizeLoading.value),
              DotGridContainer(
                  centered: false,
                  constraints: BoxConstraints(minHeight: 240.w),
                  margin: EdgeInsets.only(top: 46.w),
                  padding: EdgeInsets.only(
                      top: 8.w, bottom: 60.w, left: 12.w, right: 12.w),
                  child: ListView(
                    controller: controller.listViewController,
                    physics: const NeverScrollableScrollPhysics(),
                    shrinkWrap: true,
                    children: [
                      if (controller.aiOrganizeLoading.value == true)
                        LoadingText(text: "正文优化中"),
                      Skeletonizer(
                        enabled: controller.aiOrganizeLoading.value,
                        enableSwitchAnimation: true,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            MarkdownBody(
                              data:
                                  controller.aiOrganizeResult.value.bodyText ??
                                      '',
                            ),
                            IconTextActionList(
                                disabled: true,
                                list: controller.contentList,
                                loading: controller.aiOrganizeLoading.value),
                          ],
                        ),
                      ),
                      if (!controller.aiOrganizeLoading.value &&
                          controller.aiOrganizeMessage.isNotEmpty)
                        Padding(
                          padding: EdgeInsets.symmetric(vertical: 8.w),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                controller.aiOrganizeMessage.value,
                                style: TextStyle(
                                    color: MyColors.thirdColor,
                                    fontSize: 14.sp),
                              ),
                              TextButton(
                                onPressed: controller.retryOrganize,
                                child: const Text('重试正文整理'),
                              ),
                            ],
                          ),
                        ),
                      Visibility(
                          visible: controller.aiSuggestionList.isNotEmpty,
                          child: Column(children: [
                            16.verticalSpace,
                            DashedLine(),
                            16.verticalSpace,
                            AiSuggestion(
                              disabled: false,
                              rawStr: controller.aiSuggestionStr.value,
                              loading: controller.aiSuggestionLoading.value,
                              list: controller.aiSuggestionList,
                              onDelete: controller.deleteAiSuggestion,
                              onSaveAll: controller.saveAllAiSuggestion,
                              onSaveOne: controller.saveOneAiSuggestion,
                            ),
                          ]))
                    ],
                  )),
              Positioned(
                  bottom: 12.w,
                  left: 0,
                  right: 0,
                  child: PrimaryBtn(
                      onPressed: controller.replacing.value ||
                              controller.aiOrganizeLoading.value ||
                              controller.aiSuggestionLoading.value == true ||
                              controller.aiOrganizeMessage.isNotEmpty
                          ? null
                          : handleReplaceAll,
                      margin: EdgeInsets.symmetric(horizontal: 12.w),
                      decoration: BoxDecoration(
                        color: controller.aiSuggestionLoading.value == true ||
                                controller.aiOrganizeMessage.isNotEmpty
                            ? MyColors.primaryColor.withOpacity(0.5)
                            : null,
                        shape: BoxShape.rectangle,
                        borderRadius: BorderRadius.circular(24.w),
                      ),
                      width: double.infinity,
                      height: 48.w,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.center,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SvgPicture.asset(Assets.imagesStar, width: 22.w),
                          8.horizontalSpace,
                          Text(
                            '一键替换',
                            style: TextStyle(
                              color: MyColors.colorWhite,
                              fontSize: 16.sp,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      )))
            ],
          ),
          16.verticalSpace,
          QuestionAnswer(
            loading: controller.aiRelatedLinkLoading.value ||
                controller.aiRelatedTitleLoading.value,
            relatedLinkList: controller.aiRelatedLinkList,
            relatedTitleList: controller.aiRelatedTitleList,
            onSave: handleSaveQA,
          ),
          SmartRelatedNotes(
            notes: controller.aiRelatedNoteList,
            loading: controller.aiRelatedNoteLoading.value,
            message: controller.aiRelatedNoteMessage.value,
          ),
          NoteEnrichment(
            note: controller.note.value,
            recordId: controller.aiValidateResult.value.recordId,
            autoLoad: !controller.aiOrganizeLoading.value &&
                controller.aiOrganizeResult.value.bodyText?.trim().isNotEmpty ==
                    true,
            onReplaced: (saved) {
              if (controller.note.value.id != saved.id ||
                  noteEditLogic.note.value.id != saved.id) return;
              controller.note.value = saved;
              noteEditLogic.applySavedNote(saved, replaceText: true);
              Toast.success('回顾已替换正文，截图和其他卡片已保留');
            },
            onSaved: (saved) {
              if (controller.note.value.id != saved.id ||
                  noteEditLogic.note.value.id != saved.id) return;
              controller.note.value = saved;
              noteEditLogic.applySavedNote(saved);
            },
          ),
          VoiceDiscuss(note: controller.note.value),
          // RelatedNote(title: "信息归类", list: []),
          // RelatedNote(
          //     title: "相关备忘录",
          //     list: controller.aiCategorizedNoteList,
          //     loading: controller.aiCategorizedNoteLoading.value),
          currentAutoSort(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Flexible(flex: 3, child: ContentAssist()),
              SizedBox(width: 16),
              Flexible(
                  flex: 2,
                  child: AiIllustration(
                    img: controller.aiIllustration.value,
                    loading: controller.aiIllustrationLoading.value,
                    onSave: handleSaveAiIllustration,
                  )),
            ],
          ),
          if (!controller.aiSuggestionLoading.value &&
              controller.aiSuggestionList.isEmpty &&
              controller.aiSuggestionMessage.isNotEmpty)
            Padding(
              padding: EdgeInsets.only(top: 8.w),
              child: Text(
                controller.aiSuggestionMessage.value,
                style: TextStyle(color: MyColors.thirdColor, fontSize: 14.sp),
              ),
            )
        ],
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Column(
          children: [
            Text('智能整理',
                style: TextStyle(fontWeight: FontWeight.w700, fontSize: 20.sp)),
            Obx(() {
              return Text(
                noteEditLogic.aiAnalysisTime.value,
                style: TextStyle(
                    fontSize: 12.sp,
                    color: MyColors.primaryColor.withOpacity(0.5)),
              );
            }),
          ],
        ),
        centerTitle: true,
        leading: GestureDetector(
            onTap: () {
              Navigator.of(context).pop();
            },
            child: const Icon(Icons.close_rounded)),
        actions: [
          Text(
            '自动整理',
            style: TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w400),
          ),
          Obx(() {
            return Transform.scale(
              scale: 0.8,
              child: Switch(
                  value: controller.autoOrganize.value,
                  onChanged: controller.autoOrganizeSaving.value
                      ? null
                      : noteEditLogic.setAutoOrganize,
                  trackOutlineColor:
                      WidgetStateProperty.all(Colors.transparent),
                  inactiveThumbColor: MyColors.colorWhite.withOpacity(0.2),
                  inactiveTrackColor: Colors.grey.withOpacity(0.5),
                  activeColor: MyColors.colorWhite,
                  activeTrackColor: MyColors.colorGreen),
            );
          }),
          8.horizontalSpace,
        ],
      ),
      body: SafeArea(
        child: Container(
          padding: EdgeInsets.symmetric(horizontal: 16.w),
          width: double.infinity,
          child: Obx(() {
            return buildBody();
          }),
        ),
      ),
    );
  }
}
