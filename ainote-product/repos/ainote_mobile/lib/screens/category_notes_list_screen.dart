import 'dart:convert';

import 'package:ainote/models/note_model.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_quill/markdown_quill.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

import '../constants/app_colors.dart';
import '../constants/app_images.dart';
import '../models/categorized_note_model.dart';
import '../models/type_model.dart';
import '../services/api_service.dart';
import '../utils/utils.dart';
import 'ai_generate_screen.dart';

class CategoryNotesListScreen extends StatefulWidget {
  final List<CategorizedNoteModel> categorizedNotes;
  final List<TypeEntry> noteType;
  final Function()? onNoteUpdated;
  final Function()? onLocalNoteUpdated;
  const CategoryNotesListScreen({super.key, required this.categorizedNotes, this.onNoteUpdated, this.onLocalNoteUpdated, required this.noteType});

  @override
  State<CategoryNotesListScreen> createState() => _CategoryNotesListScreenState();
}

class _CategoryNotesListScreenState extends State<CategoryNotesListScreen> {
  int _selectedIndex = 0;
  NoteModel? _showingNote;

  Widget _noteSheet() {
    String body = _showingNote?.noteAnalysisContent ?? "";

    final deltaToMd = DeltaToMarkdown();

    List<dynamic> decodedList;
    try {
      decodedList = jsonDecode(body);
    } catch (e) {
      decodedList = [];
    }

    String mdContent = "";

    if (decodedList.isNotEmpty) {
      final delta = Delta.fromJson(decodedList);
      mdContent = deltaToMd.convert(delta);
    } else {
      mdContent = body;
    }
    final note = _showingNote;
    if (note == null) {
      return Container();
    }
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
                          note.title ?? "",
                          style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF12102F),
                          ),
                        ),
                      ),
                      MarkdownBody(
                        data: getMDShowingString(mdContent),
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
                  // Navigator.push(
                  //   context,
                  //   CupertinoPageRoute(builder: (_) => AIGenerateScreen(
                  //     note: note,
                  //     onNoteUpdated: () {
                  //       if (widget.onNoteUpdated != null) {
                  //         widget.onNoteUpdated!();
                  //       }
                  //     },
                  //     onLocalNoteUpdated: () {
                  //       if (widget.onLocalNoteUpdated != null) {
                  //         widget.onLocalNoteUpdated!();
                  //       }
                  //     },
                  //     noteTypes: widget.noteType,
                  //   )),
                  // );
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

  _getNoteDetail(int index) async {
    final note = widget.categorizedNotes[index];
    final noteId = note.noteAnalysisId.toString();
    final detailedNote = await APIService().getNoteDetail(noteId);

    if (detailedNote != null) {
      setState(() {
        _showingNote = detailedNote;
      });
    }
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
                    '信息归类',
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
                    final note = widget.categorizedNotes[index];
                    return Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(left: 8.0, right: 8),
                          child: GestureDetector(
                            behavior: HitTestBehavior.translucent,
                            onTap: () async {
                              await _getNoteDetail(index);
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
                                          getCategorizedTitle(note.noteText ?? ""),
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
                                          getCategorizedContent(note.noteText ?? ""),
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
                      ],
                    );
                  },
                  separatorBuilder: (context, index) => const SizedBox(height: 16),
                  itemCount: widget.categorizedNotes.length,
                ),
              ),
            )
          ],
        ),
      ),
    );
  }
}
