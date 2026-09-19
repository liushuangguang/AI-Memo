import 'dart:convert';

import 'package:ainote_app/app/data/models/content_assist_model.dart';
import 'package:ainote_app/app/data/models/icon_text_action_model.dart';
import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/widgets/ai_result_item.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/widgets/select_checked_item.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/widgets/todo_result_item.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/utils/quill_editor.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/app/widgets/quill/extensions/quill_embed_ext.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class AiAssistResult extends StatefulWidget {
  final ContentAssistResultModel result;
  final String originContent;
  final List selectedItems;

  const AiAssistResult({
    super.key,
    required this.result,
    required this.selectedItems,
    required this.originContent,
  });

  @override
  State<AiAssistResult> createState() => _AiAssistResultState();
}

class _AiAssistResultState extends State<AiAssistResult> {
  final noteEditLogic = Get.find<NoteEditController>();
  bool loading = true;
  String content = '';
  List items = [];
  List<TodoModel> todos = [];

  void _showDetail() {
    var primaryColor = Theme.of(context).primaryColor;
    showCupertinoModalBottomSheet(
      context: context,
      enableDrag: false,
      topRadius: Radius.circular(20.w),
      builder: (context) => Container(
        padding: EdgeInsets.all(24.w),
        color: Colors.white,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '辅写的文案',
              style: TextStyle(
                  fontSize: 18,
                  color: primaryColor,
                  fontWeight: FontWeight.w700),
              textAlign: TextAlign.left,
            ),
            const SizedBox(
              height: 12,
            ),
            ConstrainedBox(
              constraints: BoxConstraints(
                // 获取屏幕高度
                maxHeight:
                    MediaQuery.of(context).size.height - (kToolbarHeight + 200),
              ),
              child: SingleChildScrollView(
                child: Text(
                  widget.originContent,
                  style: TextStyle(
                      fontSize: 14, height: 24 / 14, color: primaryColor),
                  textAlign: TextAlign.left,
                ),
              ),
            ),
            Container(
              height: 102,
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              child: PrimaryBtn(
                text: '关闭',
                onPressed: () {
                  Navigator.pop(context);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _onSaveTodo() {
    for (var e in todos) {
      var d = jsonEncode(IconTextActionModel(
        id: e.id,
        content: e.content,
        done: e.done,
        scheduledAt: e.scheduledAt,
        description: e.description,
      ).toJson());
      noteEditLogic.quillLogic.quillController.insertTodoBlock(data: d);
    }

    Toast.success('保存成功');
  }

  void _onSaveContent() async {
    // noteEditLogic.quillLogic.quillController.appendMarkdown(content);
    var delta = convertMarkdownToDelta(content);
    noteEditLogic.quillLogic.quillController.replaceText(
        noteEditLogic.quillLogic.quillController.document.length - 1,
        1,
        delta,
        null);
    Toast.success('保存成功');
  }

  @override
  void initState() {
    super.initState();

    content = widget.result.rewrittenContent;
    todos = widget.result.todos;

    // 超时处理
    if (loading) {
      Future.delayed(const Duration(microseconds: 5000), () {
        setState(() {
          loading = false;
        });
      });
    }
  }

  @override
  void didUpdateWidget(AiAssistResult oldWidget) {
    super.didUpdateWidget(oldWidget);

    var curContent = widget.result.rewrittenContent;

    if (widget.result.rewrittenContent.isEmpty) {
      Future.delayed(const Duration(microseconds: 1000), () {
        if (mounted) {
          setState(() => loading = true);
        }
      });
    }

    if (widget.result.rewrittenContent != oldWidget.result.rewrittenContent) {
      setState(() {
        loading = true;
        content = curContent;
      });
    }

    if (widget.result.rewrittenContent.isNotEmpty &&
        widget.result.rewrittenContent == oldWidget.result.rewrittenContent) {
      setState(() {
        content = curContent;
        loading = false;
      });

      Future.delayed(const Duration(microseconds: 1000), () {
        setState(() {
          items = widget.selectedItems;
          todos = widget.result.todos;
        });
      });
    }
  }

  @override
  void dispose() {
    loading = true;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
        bottom: false,
        child: SizedBox(
            // margin: const EdgeInsets.only(top: 40.0),
            // height: MediaQuery.of(context).size.height - 40,
            height: MediaQuery.of(context).size.height - 260,
            width: MediaQuery.of(context).size.width,
            // color: const Color(0xFFF3F5F8),
            child: SingleChildScrollView(
                child: Padding(
              padding:
                  const EdgeInsets.only(top: 0, left: 0, right: 0, bottom: 20),
              child: Column(
                children: [
                  ResultItem(
                    loading: loading,
                    title: '选中内容辅写',
                    titleColor: const Color(0xFF624E00),
                    backgroundColor: const Color(0xFFFFFDF4),
                    header: Row(children: [
                      ResultItemHeader(
                        title: '选中内容辅写',
                        iconPath: Assets.imagesBlackCheckedIcon,
                      ),
                      const Spacer(),
                      ResultItemAction(
                        actionText: '存入正文',
                        afterActionText: '存入成功',
                        onSave: _onSaveContent,
                      ),
                    ]),
                    body: ContentSelectedResultBody(
                      content: widget.result.rewrittenContent,
                      items: widget.selectedItems,
                    ),
                  ),
                  SizedBox(
                    height: todos.isNotEmpty ? 16 : 0,
                  ),
                  TodoResultItem(todos: todos, onSave: _onSaveTodo),
                  SizedBox(
                    height: widget.originContent.isNotEmpty ? 16 : 0,
                  ),
                  Visibility(
                    visible: widget.originContent.isNotEmpty,
                    child: ResultItem(
                      loading: loading,
                      title: '辅写的文案',
                      header: Row(children: [
                        const ResultItemHeader(
                          title: '辅写的文案',
                        ),
                        const Spacer(),
                        GestureDetector(
                          onTap: _showDetail,
                          child: const Text(
                            '查看全部',
                            style: TextStyle(
                              color: Color(0xFF3A51FF),
                              fontSize: 14,
                              height: 24 / 14,
                              decoration: TextDecoration.none,
                            ),
                          ),
                        ),
                      ]),
                      text: widget.originContent,
                      ellipsis: true,
                    ),
                  ),
                  const SizedBox(
                    height: 16,
                  ),
                  ResultItem(
                    loading: loading,
                    title: '提示',
                    header: const Row(children: [
                      ResultItemHeader(
                        title: '提示',
                      ),
                    ]),
                    text: "由于AI能力的限制，对于过长的内容，AI内容辅助生成的内容可能会缺失",
                  ),
                ],
              ),
            ))));
  }
}

class ContentSelectedResultBody extends StatelessWidget {
  final String content;
  final List<dynamic> items;

  const ContentSelectedResultBody(
      {super.key, required this.content, required this.items});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Wrap(
          spacing: 8.0,
          runSpacing: 8.0,
          children: [
            for (var item in items) SelectCheckedItem(text: item.toString()),
          ],
        ),
        const SizedBox(
          height: 12,
        ),
        MarkdownBody(
          data: content,
        )
      ],
    );
  }
}
