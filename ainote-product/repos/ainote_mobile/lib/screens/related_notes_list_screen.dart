import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../constants/app_colors.dart';
import '../constants/app_images.dart';
import '../models/note_model.dart';
import '../models/type_model.dart';
import '../utils/utils.dart';
import 'ai_generate_screen.dart';

class RelatedNotesListScreen extends StatefulWidget {
  final List<NoteModel> relatedNotes;
  final List<TypeEntry> noteType;
  final Function()? onNoteUpdated;
  final Function()? onLocalNoteUpdated;
  const RelatedNotesListScreen({super.key, required this.relatedNotes, required this.noteType, this.onNoteUpdated, this.onLocalNoteUpdated});

  @override
  State<RelatedNotesListScreen> createState() => _RelatedNotesListScreenState();
}

class _RelatedNotesListScreenState extends State<RelatedNotesListScreen> {
  List<bool> _expands = [];
  int _selectedIndex = 0;

  @override
  void initState() {
    super.initState();
    _expands = List.generate(widget.relatedNotes.length, (index) => false);
  }

  Widget _noteSheet() {
    final note = widget.relatedNotes[_selectedIndex];
    return SafeArea(
      child: Container(
        height: 640,
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(20),
            topRight: Radius.circular(20),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 36.0, bottom: 24, left: 24, right: 24),
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12.0),
                        child: Text(
                          getNoteTitle(note.noteAnalysisContent ?? ""),
                          style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF12102F),
                          ),
                        ),
                      ),
                      MarkdownBody(
                        data: getMDShowingString(getNoteBody(note.noteAnalysisContent ?? "")),
                      )
                    ],
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  Navigator.push(
                    context,
                    CupertinoPageRoute(builder: (_) => AIGenerateScreen(
                      note: note,
                      onNoteUpdated: () {
                        if (widget.onNoteUpdated != null) {
                          widget.onNoteUpdated!();
                        }
                      },
                      onLocalNoteUpdated: () {
                        if (widget.onLocalNoteUpdated != null) {
                          widget.onLocalNoteUpdated!();
                        }
                      },
                      noteTypes: widget.noteType,
                    )),
                  );
                },
                child: Container(
                  height: 54,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.all(Radius.circular(8)),
                    color: Color(0xFF12102F)
                  ),
                  child: Center(
                    child: Text(
                      '跳转查看',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFFFDFCFF),
                      ),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12,),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  Navigator.pop(context);
                },
                child: Container(
                  height: 54,
                  decoration: BoxDecoration(
                      borderRadius: BorderRadius.all(Radius.circular(8)),
                      color: Color(0xFFDFDEE8)
                  ),
                  child: Center(
                    child: Text(
                      '关闭',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF12102F),
                      ),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10,),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Color(0xFFF3F5F8).withOpacity(0.9),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 10.0),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  Row(
                    children: [
                      GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTap: () {
                          Navigator.pop(context);
                        },
                        child: Padding(
                          padding: const EdgeInsets.only(left: 16.0),
                          child: Image.asset(
                            AppImages.backBtn.path,
                            width: 28,
                            height: 28,
                          ),
                        ),
                      ),
                      const Spacer(),
                    ],
                  ),

                  Text(
                    '相关备忘录',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      color: Color(0xFF12102F),
                    ),
                  )
                ],
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: ListView.separated(
                  padding: EdgeInsets.zero,
                  shrinkWrap: true,
                    itemBuilder: (context, index) {
                    final note = widget.relatedNotes[index];
                      return Column(
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(left: 8.0, right: 8, bottom: 8),
                            child: GestureDetector(
                              behavior: HitTestBehavior.translucent,
                              onTap: () {
                                setState(() {
                                  _selectedIndex = index;
                                });
                                showCupertinoModalBottomSheet(
                                  context: context,
                                  builder: (context) {
                                    return _noteSheet();
                                  },
                                );
                              },
                              child: Container(
                                height: 88,
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: Color(0xFFEBEBEB)),
                                  color: Colors.white,
                                ),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text(
                                            getNoteTitle(note.noteAnalysisContent ?? "").isEmpty ? "无标题" : getNoteTitle(note.noteAnalysisContent ?? ""),
                                            style: TextStyle(
                                              color: Color(0xFF12102F),
                                              fontSize: 16,
                                              fontWeight: FontWeight.w500,
                                              height: 1,
                                              decoration: TextDecoration.none,
                                            ),
                                            maxLines: 1,
                                          ),
                                          Text(
                                            getNoteBody(note.noteAnalysisContent ?? "") ,
                                            maxLines: 1,
                                            style: TextStyle(
                                              color: Color(0xFF4C4A5C),
                                              fontSize: 14,
                                              fontWeight: FontWeight.w400,
                                              height: 1,
                                              decoration: TextDecoration.none,
                                            ),
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ],
                                      ),
                                    )
                                  ],
                                ),
                              ),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8.0),
                            child: GestureDetector(
                              behavior: HitTestBehavior.translucent,
                              onTap: () {
                                setState(() {
                                  _expands[index] = !_expands[index];
                                });
                              },
                              child: Row(
                                children: [
                                  Padding(
                                    padding: const EdgeInsets.only(right: 8.0),
                                    child: Text(
                                      '${note.hitTags?.length ?? 0}个共同的标签',
                                      style: TextStyle(
                                        color: Color(0xFF4C4A5C),
                                        fontSize: 14,
                                        fontWeight: FontWeight.w400,
                                        height: 1,
                                        decoration: TextDecoration.none,
                                      ),
                                    ),
                                  ),
                                  Image.asset(
                                    _expands[index] ? AppImages.expandedIcon.path : AppImages.notExpandedIcon.path,
                                    width: 17,
                                    height: 17,
                                  ),
                                ],
                              ),
                            ),
                          ),
                          Visibility(
                            visible: _expands[index],
                            child: Padding(
                              padding: const EdgeInsets.only(left: 12.0, right: 12, top: 8),
                              child: ListView.separated(
                                physics: NeverScrollableScrollPhysics(),
                                shrinkWrap: true,
                                padding: EdgeInsets.zero,
                                  itemBuilder: (context, index) {
                                    final tag = note.hitTags?[index];
                                    return Row(
                                      children: [
                                        Padding(
                                          padding: const EdgeInsets.only(right: 8.0),
                                          child: Image.asset(
                                            AppImages.dotIcon.path,
                                            width: 14,
                                            height: 20,
                                          ),
                                        ),
                                        Text(
                                          tag ?? "",
                                          style: TextStyle(
                                            color: Color(0xFF4C4A5C),
                                            fontSize: 14,
                                            fontWeight: FontWeight.w700,
                                            height: 1,
                                            decoration: TextDecoration.none,
                                          ),
                                        ),
                                      ],
                                    );
                                  },
                                  separatorBuilder: (context, index) => const SizedBox(width: 4),
                                  itemCount: note.hitTags?.length ?? 0,
                              ),
                            ),
                          )
                        ],
                      );
                    },
                    separatorBuilder: (context, index) => const SizedBox(height: 16),
                    itemCount: widget.relatedNotes.length,
                ),
              ),
            )
          ],
        ),
      ),
    );
  }
}
