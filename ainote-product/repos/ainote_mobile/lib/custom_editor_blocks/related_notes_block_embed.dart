import 'dart:convert';

import 'package:ainote/classes/dashed_line_painter.dart';
import 'package:ainote/classes/material_transparent_route.dart';
import 'package:ainote/constants/app_images.dart';
import 'package:ainote/models/editor_todo_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/markdown_quill.dart';
import 'package:markdown/markdown.dart' as md;

import '../models/note_model.dart';
import '../screens/related_notes_list_screen.dart';


class RelatedNotesBlockEmbed extends CustomBlockEmbed {
  const RelatedNotesBlockEmbed(String value) : super(noteType, value);

  static const String noteType = 'related';

  static RelatedNotesBlockEmbed fromJsonString(String jsonString) =>
      RelatedNotesBlockEmbed(jsonString);

  String get jsonString => data;
}

class RelatedNotesEmbedBuilder extends EmbedBuilder {

  @override
  String get key => RelatedNotesBlockEmbed.noteType;

  @override
  Widget build(
      BuildContext context,
      QuillController controller,
      Embed node,
      bool readOnly,
      bool inline,
      TextStyle textStyle,
      ) {
    final notes = RelatedNotesBlockEmbed(node.value.data).jsonString;
    final json = jsonDecode(notes);
    final related = (json as List).map((e) => NoteModel.fromJson(e as Map<String, dynamic>)).toList();
    return Padding(
      padding: const EdgeInsets.only(top: 16.0),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 24.0),
            child: DashedLine(),
          ),
          Container(
            decoration: BoxDecoration(
              color: Color(0xFFFFFFFF),
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.all(8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.only(bottom: 8.0),
                  child: Row(
                    children: [
                      Text(
                        "相关备忘录",
                        style: TextStyle(
                          color: Color(0xFF12102F),
                          fontSize: 24,
                          fontWeight: FontWeight.w700,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
                      ),
                      const Spacer(),
                      GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTap: () {
                          final index = getEmbedNode(controller, controller.selection.start).offset;
                          final len = controller.document.length;
                          controller.document.delete(index, len);
                        },
                        child: Container(
                          width: 56,
                          height: 35,
                          child: Center(
                            child: Text(
                              "删除",
                              style: TextStyle(
                                color: Color(0xFF3A51FF),
                                fontSize: 16,
                                fontWeight: FontWeight.w500,
                                height: 1,
                                decoration: TextDecoration.none,
                              ),
                            ),
                          ),
                        ),
                      )
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 8.0),
                  child: Image.asset(
                    AppImages.hDash.path,
                    height: 1,
                    color: Color(0xFFE1E1E1),
                  ),
                ),
                Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(4),
                    color: Color(0xFF45B670).withOpacity(0.1),
                  ),
                  height: 32,
                  child: Center(
                    child: Text(
                      related.isEmpty ? "没有找到任何相关的备忘录" : "为您匹配到${related.length}篇相关备忘录",
                      style: TextStyle(
                        color: Color(0xFF009D4F),
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        height: 1,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ),
                ),

                Visibility(
                  visible: related.isNotEmpty,
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      if (related.isNotEmpty) {
                        Navigator.of(context).push(MaterialTransparentRoute(
                            builder: (BuildContext context) => RelatedNotesListScreen(
                              relatedNotes: related,
                              noteType: [],
                              onNoteUpdated: () {

                              },
                              onLocalNoteUpdated: () {

                              },
                            )));
                      }
                    },
                    child: Padding(
                      padding: const EdgeInsets.only(top: 8.0),
                      child: Stack(
                        children: [
                          Visibility(
                            visible: related.length > 2,
                            child: Padding(
                              padding: const EdgeInsets.only(
                                  top: 20.0, left: 36, right: 36),
                              child: Container(
                                height: 88,
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: Color(0xFFEBEBEB)),
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                          Visibility(
                            visible: related.length > 1,
                            child: Padding(
                              padding: const EdgeInsets.only(
                                  top: 10.0, left: 14, right: 14),
                              child: Container(
                                height: 88,
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: Color(0xFFEBEBEB)),
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                          Container(
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
                                        related.isEmpty
                                            ? ""
                                            : related.first.title ?? "",
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
                                        related.isEmpty
                                            ? ""
                                            : related.first.noteAnalysisContent ?? "",
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
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}



