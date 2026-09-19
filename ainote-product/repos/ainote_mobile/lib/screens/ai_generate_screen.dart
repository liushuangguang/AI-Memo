import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:ainote/classes/app_local_settings.dart';
import 'package:ainote/constants/app_colors.dart';
import 'package:ainote/constants/app_images.dart';
import 'package:ainote/constants/constants.dart';
import 'package:ainote/custom_editor_blocks/related_notes_block_embed.dart';
import 'package:ainote/custom_editor_blocks/todo_block_embed.dart';
import 'package:ainote/custom_editor_blocks/todo_list_embed.dart';
import 'package:ainote/models/ai_generate_model.dart';
import 'package:ainote/models/ai_history_model.dart';
import 'package:ainote/models/editor_todo_model.dart';
import 'package:ainote/models/stream_section.dart';
import 'package:ainote/screens/related_notes_list_screen.dart';
import 'package:ainote/screens/widgets/ai_action_text.dart';
import 'package:ainote/screens/ai_assist_content/ai_assist_content.dart';
import 'package:ainote/screens/ai_assist_content/ai_assist_result.dart';
import 'package:ainote/services/api_service.dart';
import 'package:ainote/services/keychain_service.dart';
import 'package:ainote/services/local_note_service.dart';
import 'package:ainote/sheets/todo_sheet.dart';
import 'package:ainote/utils/utils.dart';
import 'package:any_link_preview/any_link_preview.dart';
import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:favicon/favicon.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:flutter_quill_extensions/flutter_quill_extensions.dart';

// import 'package:flutter_quill_extensions/models/config/image/toolbar/image_configurations.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:get/get.dart';
import 'package:loading_indicator/loading_indicator.dart';
import 'package:markdown_quill/markdown_quill.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:markdown/markdown.dart' as md;
import 'package:markdown_quill/markdown_quill.dart';
import 'package:share_plus/share_plus.dart';

import '../classes/material_transparent_route.dart';
import '../components/web_view_page.dart';
import '../custom_editor_blocks/divider_block_embed.dart';
import '../custom_editor_blocks/voice_discuss_card_embed.dart';
import '../models/categorized_note_model.dart';
import '../models/content_assist_model.dart';
import '../models/note_model.dart';
import '../models/todo_model.dart';
import '../models/type_model.dart';
import 'ai_voice_discuss/ai_discuss_result.dart';
import 'ai_voice_discuss/ai_voice_discuss.dart';
import 'ai_voice_discuss/ai_voice_result_card.dart';
import 'ai_voice_discuss/message/message_logic.dart';
import 'category_notes_list_screen.dart';

class AIGenerateScreen extends StatefulWidget {
  final NoteModel? note;
  final List<TypeEntry> noteTypes;
  final Function()? onNoteUpdated;
  final Function()? onLocalNoteUpdated;
  final String? toCreateNoteTypeId;

  AIGenerateScreen({
    super.key,
    this.note,
    this.onNoteUpdated,
    required this.noteTypes,
    this.onLocalNoteUpdated,
    this.toCreateNoteTypeId,
  });

  @override
  State<AIGenerateScreen> createState() => _AIGenerateScreenState();
}

class _AIGenerateScreenState extends State<AIGenerateScreen> {
  NoteModel? _note;
  int _selectedTabIndex = 0;
  bool _isAIIn = false;
  bool _isContentGenerating = false;
  bool _isContentGenerated = false;
  bool _isRelatedGenerating = false;
  bool _isRelatedGenerated = false;
  bool _isCategoryGenerating = false;
  bool _isCategoryGenerated = false;
  bool _isGuessedGenerated = false;
  bool _isContentTextGenerated = false;
  bool _isTodoGenerated = false;
  bool _isGuessGenerated = false;
  bool _isAILinkGenerated = false;
  bool _isAIImageLinkGenerated = false;
  bool _isSceneGenerated = false;
  bool _isSuggestionsGenerated = false;
  String _content = '';
  AIGenerateModel? _aiGenerated;
  bool _isContentSaved = false;
  bool _showContentSaveSuccess = false;
  bool _isSuggestionSaved = false;
  bool _showSuggestionSaveSuccess = false;
  bool _isGuessedSaved = false;
  bool _showGuessedSavedSuccess = false;
  bool _isTodoSaved = false;
  bool _showTodoSaveSuccess = false;
  bool _isGuessSaved = false;
  bool _showGuessSaveSuccess = false;
  bool _isSceneSaved = false;
  bool _showSceneSaveSuccess = false;
  bool _isRelatedSaved = false;
  bool _showRelatedSaveSuccess = false;
  bool _isCategorySaved = false;
  bool _showCategorySaveSuccess = false;
  TextEditingController _titleTextController = TextEditingController();
  final _titleTextFocusNode = FocusNode();
  List<NoteModel> _relatedNotes = [];
  List<CategorizedNoteModel> _categorizedNotes = [];
  String? _currentNoteId;
  bool _isGenaratingError = true;
  final _controller = QuillController.basic();
  final _editorFocusNode = FocusNode();
  final _titleFocusNode = FocusNode();
  final _editorScrollController = ScrollController();
  bool _isFontStyleViewShowing = false;
  bool _isGeneratingToastShowing = false;
  bool _isGeneratingFirstCheck = false;
  bool _isGenerateFailed = true;
  int _generateFaildCate = 0;
  Timer? _updateTimer;
  bool _isUpdateSuccessToastShowing = false;
  final GlobalKey _noteTypeBtnkey = GlobalKey();
  String _selectedTitleText = '';
  String _selectedContentText = '';
  Offset? _noteTypeBtnPosition;
  int _selectedNoteTypeIndex = 0;
  Timer? _getRelatedNotesTimer;
  int _getRelatedNotesCount = 40;

  // 历史记录
  List<AIHistoryModel> _historyList = [];

  int _selectedHistoryIndex = 0;
  late AIHistoryModel _selectedHistory;
  bool _isNoteActionViewShowing = false;

  // 与analysisType保持一致
  // 1-一键整理，2-内容辅助，3-语音讨论
  int _selectedBottomTabIndex = 0;

  // 内容辅助相关
  String _originContent = '';
  List _aiAssistSelectedItems = [];

  MessageLogic messageLogic = Get.put(MessageLogic());

  ContentAssistResultModel _contentAssistResult =
      ContentAssistResultModel.fromRawString('');

  @override
  void initState() {
    super.initState();

    eventBus.on<String>().listen((event) {
      if (event == "app_paused") {
        if (mounted) {
          _updateNote();
        }
      }
    });

    _controller.onSelectionChanged = (TextSelection textSelection) {
      if (textSelection.isValid) {
        String fullText = _controller.plainTextEditingValue.text;
        String selectedText = removeNewLinesAndSpaces(
            textSelection.textInside(fullText).replaceAll('￼', ''));
        setState(() {
          _selectedTitleText = '';
          _selectedContentText = selectedText;
        });
      }
    };

    // 获取选中标题
    _titleTextController.addListener(() {
      final TextSelection selection = _titleTextController.selection;
      if (selection.isValid) {
        setState(() {
          _selectedContentText = '';
          _selectedTitleText = selection.textInside(_titleTextController.text);
        });
      }
    });

    if (widget.note != null) {
      _titleTextController.text = widget.note!.title ?? "";

      String body = widget.note!.noteAnalysisContent ?? "";

      List<dynamic> decodedList;
      try {
        decodedList = jsonDecode(body);
      } catch (e) {
        decodedList = [];
      }

      if (decodedList.isNotEmpty) {
        final delta = Delta.fromJson(decodedList);
        _controller.document = Document.fromDelta(delta);
      } else {
        final mdDocument = md.Document(encodeHtml: false);
        final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);
        final delta = mdToDelta.convert(
            (widget.note!.noteAnalysisContent ?? "").replaceAll("\n", "\n\n"));
        if (delta.isNotEmpty) {
          _controller.document = Document.fromDelta(delta);
        }
      }

      _note = widget.note;
      _getNoteDetail();
    }
    _startUpdateTimer();

    WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
      if (widget.note == null) {
        _editorFocusNode.requestFocus();
      }
    });
  }

  _getNoteDetail() async {
    final noteId = _note?.id.toString();
    if (noteId != null) {
      final detailedNote = await APIService().getNoteDetail(noteId);

      if (detailedNote != null) {
        detailedNote.convertToGenerated();
        setState(() {
          _note = detailedNote;
        });
      }
    }
  }

  @override
  void dispose() {
    _titleTextController.dispose();
    _titleTextFocusNode.dispose();
    _editorFocusNode.dispose();
    _titleFocusNode.dispose();
    _editorScrollController.dispose();
    _controller.dispose();
    _updateTimer?.cancel();
    _getRelatedNotesTimer?.cancel();
    super.dispose();
  }

  _startUpdateTimer() {
    // _updateTimer = Timer.periodic(Duration(seconds: 3), (timer) {
    //   if (_note != null) {
    //     _updateNote();
    //   }
    // });
  }

  Future<void> _updateNote() async {
    final title = _titleTextController.text;

    final allContent = _controller.document.toDelta().toJson();

    if (title.isEmpty && allContent.isEmpty) {
      return;
    }

    final allContentString = jsonEncode(allContent);

    if (_note == null) {
      final createdNote = await APIService().createNote(
          _titleTextController.text,
          allContentString,
          widget.toCreateNoteTypeId ?? "0");

      print('createdNote ${createdNote}');
      if (createdNote != null) {
        final detailedNote =
            await APIService().getNoteDetail(createdNote.id.toString());
        setState(() {
          _note = detailedNote ?? createdNote;
          _currentNoteId = createdNote.id.toString();
        });
        if (detailedNote != null) {
          detailedNote.convertToGenerated();
        }
        widget.onNoteUpdated?.call();
      }
    } else {
      LocalNoteService.instance.update(
          _note!.id.toString(), _titleTextController.text, allContentString);
      widget.onLocalNoteUpdated?.call();

      final updatedNote = await APIService().updateNote(
        _titleTextController.text,
        allContentString,
        _note!.id.toString(),
        _note!.noteType.toString(),
      );
      if (updatedNote != null) {
        widget.onNoteUpdated?.call();
      }
    }
  }

  _setToSections(StreamSection section) {
    setState(() {
      if (_aiGenerated!.all.contains("【备忘录标题】") &&
          _aiGenerated!.title == null) {
        section.isTitleSection = true;
        _aiGenerated!.title = "";
      }
      if (_aiGenerated!.all.contains("【用户记录意图/情景】") &&
          _aiGenerated!.scene == null) {
        section.isSceneSection = true;
        _aiGenerated!.scene = "";
      }
      if (_aiGenerated!.all.contains("【优化版正文】") &&
          _aiGenerated!.content == null) {
        section.isContentSection = true;
        _aiGenerated!.content = "";
      }
      if (_aiGenerated!.all.contains("【建议信息】") &&
          _aiGenerated!.suggestion == null) {
        section.isSuggestionsSection = true;
        _aiGenerated!.suggestion = "";
      }
      if (_aiGenerated!.all.contains("【标签】") && _aiGenerated!.tag == null) {
        section.isTagSection = true;
        _aiGenerated!.tag = "";
      }
      if (_aiGenerated!.all.contains("【时间计划】") && _aiGenerated!.todo == null) {
        section.isTodoSection = true;
        _aiGenerated!.todo = "";
      }
      if (_aiGenerated!.all.contains("【备忘录分类】") &&
          _aiGenerated!.category == null) {
        section.isCategorySection = true;
        _aiGenerated!.category = "";
      }
    });
  }

  _getStream(String content, String version) async {
    StreamSection section = StreamSection();

    await for (var line in APIService().getAIGenerate(content, version)) {
      final lineContent = line.replaceAll("data:", "");
      print(lineContent);
      setState(() {
        if (section.isTitleSection) {
          if (lineContent.contains("【")) {
            section.isTitleSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【备忘录标题】", "");
            _setToSections(section);
          } else {
            _aiGenerated!.title = _aiGenerated!.title! + lineContent;
          }
        } else if (section.isSceneSection) {
          if (lineContent.contains("【")) {
            section.isSceneSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【用户记录意图/情景】", "");
            _isSceneGenerated = true;
            _setToSections(section);
          } else {
            _aiGenerated!.scene = _aiGenerated!.scene! + lineContent;
          }
        } else if (section.isContentSection) {
          if (lineContent.contains("【")) {
            section.isContentSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【优化版正文】", "");
            _isContentTextGenerated = true;
            _setToSections(section);
          } else {
            _aiGenerated!.content = _aiGenerated!.content! + lineContent;
          }
        } else if (section.isSuggestionsSection) {
          if (lineContent.contains("【")) {
            section.isSuggestionsSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【建议信息】", "");
            _isSuggestionsGenerated = true;
            print(_aiGenerated!.suggestion);
            _setToSections(section);
          } else {
            _aiGenerated!.suggestion = _aiGenerated!.suggestion! + lineContent;
          }
        } else if (section.isTagSection) {
          if (lineContent.contains("【")) {
            section.isTagSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【标签】", "");
            _setToSections(section);
          } else {
            _aiGenerated!.tag = _aiGenerated!.tag! + lineContent;
          }
        } else if (section.isTodoSection) {
          if (lineContent.contains("【")) {
            section.isTodoSection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【时间计划】", "");
            _isTodoGenerated = true;
            _setToSections(section);
          } else {
            _aiGenerated!.todo = _aiGenerated!.todo! + lineContent;
          }
        } else if (section.isCategorySection) {
          if (lineContent.contains("【")) {
            section.isCategorySection = false;
            _aiGenerated!.all += lineContent;
            _aiGenerated!.all.replaceAll("【备忘录分类】", "");
            _setToSections(section);
          } else {
            _aiGenerated!.category = _aiGenerated!.category! + lineContent;
          }
        } else {
          _aiGenerated!.all += lineContent;

          _isGeneratingFirstCheck = false;

          if (_aiGenerated!.all.contains("对不起，您记录的")) {
            _isGenerateFailed = true;
            _generateFaildCate = 0;
          }
          if (_aiGenerated!.all.contains("您输入的备忘录可能有")) {
            _isGenerateFailed = true;
            _generateFaildCate = 1;
          }

          if (_aiGenerated!.all.contains("【备忘录标题】") &&
              _aiGenerated!.title == null) {
            section.isTitleSection = true;
            if (_aiGenerated!.all.split("【备忘录标题】").length > 1) {
              _aiGenerated!.title = _aiGenerated!.all.split("【备忘录标题】")[1];
            } else {
              _aiGenerated!.title = "";
            }
          }
          if (_aiGenerated!.all.contains("【用户记录意图/情景】") &&
              _aiGenerated!.scene == null) {
            section.isSceneSection = true;
            if (_aiGenerated!.all.split("【用户记录意图/情景】").length > 1) {
              _aiGenerated!.scene = _aiGenerated!.all.split("【用户记录意图/情景】")[1];
              _isSceneGenerated = true;
            } else {
              _aiGenerated!.scene = "";
            }
            _aiGenerated!.scene = "";
          }
          if (_aiGenerated!.all.contains("【优化版正文】") &&
              _aiGenerated!.content == null) {
            section.isContentSection = true;
            if (_aiGenerated!.all.split("【优化版正文】").length > 1) {
              _aiGenerated!.content = _aiGenerated!.all.split("【优化版正文】")[1];
              _isContentTextGenerated = true;
            } else {
              _aiGenerated!.content = "";
            }
            _aiGenerated!.content = "";
          }
          if (_aiGenerated!.all.contains("【建议信息】") &&
              _aiGenerated!.suggestion == null) {
            section.isSuggestionsSection = true;
            if (_aiGenerated!.all.split("【建议信息】").length > 1) {
              _aiGenerated!.suggestion = _aiGenerated!.all.split("【建议信息】")[1];
              _isSuggestionsGenerated = true;
            } else {
              _aiGenerated!.suggestion = "";
            }
            _aiGenerated!.suggestion = "";
          }
          if (_aiGenerated!.all.contains("【标签】") && _aiGenerated!.tag == null) {
            section.isTagSection = true;
            if (_aiGenerated!.all.split("【标签】").length > 1) {
              _aiGenerated!.tag = _aiGenerated!.all.split("【标签】")[1];
            } else {
              _aiGenerated!.tag = "";
            }
            _aiGenerated!.tag = "";
          }
          if (_aiGenerated!.all.contains("【时间计划】") &&
              _aiGenerated!.todo == null) {
            section.isTodoSection = true;
            if (_aiGenerated!.all.split("【时间计划】").length > 1) {
              _aiGenerated!.todo = _aiGenerated!.all.split("【时间计划】")[1];
              _isTodoGenerated = true;
            } else {
              _aiGenerated!.todo = "";
            }
            _aiGenerated!.todo = "";
          }
          if (_aiGenerated!.all.contains("【备忘录分类】") &&
              _aiGenerated!.category == null) {
            section.isCategorySection = true;
            if (_aiGenerated!.all.split("【备忘录分类】").length > 1) {
              _aiGenerated!.category = _aiGenerated!.all.split("【备忘录分类】")[1];
            } else {
              _aiGenerated!.category = "";
            }
            _aiGenerated!.category = "";
          }
        }
      });
    }

    if (_aiGenerated!.all.contains("您输入的备忘录可能有意义") &&
        _aiGenerated!.all.contains("http")) {
      setState(() {
        _aiGenerated!.links = extractLinks(_aiGenerated!.all);
      });
      _handleLinks();
    }

    print("done");

    setState(() {
      _isContentGenerating = false;
      _isContentGenerated = true;
      _isGeneratingToastShowing = false;
    });
  }

  _handleLinks() async {
    setState(() {
      if (_aiGenerated!.linkMetadatas == null) {
        _aiGenerated!.linkMetadatas = [];
      }
      if (_aiGenerated!.favIco == null) {
        _aiGenerated!.favIco = [];
      }
    });

    for (final link in _aiGenerated!.links!) {
      Metadata? metadata = await AnyLinkPreview.getMetadata(
        link: link,
        cache: const Duration(days: 1),
      );
      final iconUrl = await FaviconFinder.getBest(link);
      setState(() {
        _aiGenerated!.linkMetadatas!.add(metadata);
        _aiGenerated!.favIco!.add(iconUrl);
      });
    }
  }

  _getImageLink(String noteId, String version) async {
    final res = await APIService().getImageLink(noteId, version);
    if (res != null) {
      setState(() {
        _aiGenerated!.imageLink = res;
        _isAIImageLinkGenerated = true;
      });
    }
  }

  _getRelatedNotes(String noteId, String version) async {
    _getRelatedNotesTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_getRelatedNotesCount == 0) {
        _getRelatedNotesTimer?.cancel();
        return;
      }
      setState(() {
        _getRelatedNotesCount = _getRelatedNotesCount - 1;
      });
    });
    final relatedNotes = await APIService().getRelatedNotes(noteId, version);
    setState(() {
      _relatedNotes = relatedNotes;
      _isRelatedGenerating = false;
      _isRelatedGenerated = true;
      _getRelatedNotesTimer?.cancel();
    });
  }

  _getCategorizedNotes(String noteId, String version) async {
    final categorizedNotes =
        await APIService().getCategorizedNotes(noteId, version);
    setState(() {
      _categorizedNotes = categorizedNotes;
      _isCategoryGenerating = false;
      _isCategoryGenerated = true;
    });
  }

  _getGuessed(String noteId, String version) async {
    final guessed = await APIService().getGuessed(noteId, version);
    if (guessed != null) {
      setState(() {
        _aiGenerated!.aiGuess = guessed;
        _isGuessGenerated = true;
      });
    }
  }

  _getLinks(String noteId, String version) async {
    final links = await APIService().getAILinks(noteId, version);
    if (links != null) {
      setState(() {
        _aiGenerated!.aiLink = links;
        _isAILinkGenerated = true;
      });
    }
  }

  _startGenerate() async {
    FocusScope.of(context).unfocus();

    setState(() {
      _aiGenerated = AIGenerateModel(all: "");
      _relatedNotes = [];
      _categorizedNotes = [];
      _currentNoteId = null;
      _isGeneratingToastShowing = true;
      _isGeneratingFirstCheck = true;
      _isGenerateFailed = false;
      _selectedHistoryIndex = 0;
      _selectedBottomTabIndex = 1;
    });

    setState(() {
      _isContentGenerating = true;
      _isContentGenerated = false;
      _isRelatedGenerating = true;
      _isRelatedGenerated = false;
      _isCategoryGenerating = true;
      _isCategoryGenerated = false;
      _isGuessGenerated = false;
      _isAILinkGenerated = false;
      _isContentTextGenerated = false;
      _isTodoGenerated = false;
      _isSceneGenerated = false;
      _isSuggestionsGenerated = false;
      _isAIIn = true;
      _selectedTabIndex = 1;
      _isGenaratingError = false;
    });

    final isNew = _note == null;

    await _updateNote();

    AIHistoryModel? historyModel =
        await APIService.createNoteAnalysisHistory(_note!.id!, 1);

    _note?.version = historyModel?.version;

    final newAIHistory = AIHistoryModel.create(1);
    setState(() {
      _note!.insertAIHistory(newAIHistory);
      _note!.insertAIGenerate(_aiGenerated!);
    });

    if (_note == null) {
      setState(() {
        _isGenaratingError = true;
        _isContentGenerating = false;
        _isRelatedGenerating = false;
        _isCategoryGenerating = false;
      });
      return;
    }

    setState(() {
      _currentNoteId = _note!.id.toString();
      widget.onNoteUpdated?.call();
    });

    _getStream(_note!.id.toString(), isNew ? "1" : "0");

    _getImageLink(_note!.id.toString(), isNew ? "1" : "0");

    await Future.delayed(const Duration(seconds: 2));

    _getRelatedNotes(_note!.id.toString(), isNew ? "1" : "0");

    _getCategorizedNotes(_note!.id.toString(), isNew ? "1" : "0");

    _getHistoryList();

    _getGuessed(_note!.id.toString(), isNew ? "1" : "0");

    _getLinks(_note!.id.toString(), isNew ? "1" : "0");
  }

  Widget _generatingToast() {
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 300),
      opacity: _isGeneratingToastShowing ? 1 : 0,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.only(top: 30.0),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFF12102F),
              borderRadius: BorderRadius.circular(4),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF000000).withOpacity(0.15),
                  offset: const Offset(0, 0),
                  blurRadius: 20,
                ),
              ],
            ),
            padding: const EdgeInsets.symmetric(horizontal: 13.0, vertical: 10),
            child: const Text(
              "加速生成中，请稍等...",
              style: TextStyle(
                color: Color(0xFFFFFFFF),
                fontSize: 14,
                fontWeight: FontWeight.w500,
                height: 1,
                decoration: TextDecoration.none,
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _updateSuccessToast() {
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 300),
      opacity: _isUpdateSuccessToastShowing ? 1 : 0,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.only(top: 30.0),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFFFFFFFF),
              borderRadius: BorderRadius.circular(4),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF000000).withOpacity(0.15),
                  offset: const Offset(0, 0),
                  blurRadius: 20,
                ),
              ],
            ),
            padding: const EdgeInsets.symmetric(horizontal: 13.0, vertical: 10),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkSuccessIcon.path,
                    width: 12,
                    height: 12,
                  ),
                ),
                const Text(
                  "保存成功",
                  style: TextStyle(
                    color: Color(0xFF0FDA6C),
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    height: 1,
                    decoration: TextDecoration.none,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _generatingTitleView(String title, bool isPrimary) {
    return Container(
      height: 51,
      decoration: BoxDecoration(
        color: Color(isPrimary ? 0xFFFFFDF4 : 0xFFFFFFFF),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Row(
        children: [
          const Padding(
            padding: EdgeInsets.only(right: 8.0),
            child: SizedBox(
              width: 24,
              height: 24,
              child: LoadingIndicator(
                indicatorType: Indicator.lineSpinFadeLoader,
                colors: [Color(0xFF12102F)],
                strokeWidth: 1,
              ),
            ),
          ),
          Text(
            title,
            style: TextStyle(
              color: Color(isPrimary ? 0xFF624E00 : 0xFF12102F),
              fontSize: 24,
              fontWeight: FontWeight.w700,
              height: 1,
              decoration: TextDecoration.none,
            ),
          ),
        ],
      ),
    );
  }

  Widget _aiGenerateSuggestionView() {
    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Visibility(
                visible:
                    _selectedHistoryIndex == 0 ? _isSuggestionsGenerated : true,
                child: Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkBlackIcon.path,
                    width: 24,
                    height: 24,
                  ),
                ),
              ),
              Visibility(
                visible: _selectedHistoryIndex == 0
                    ? !_isSuggestionsGenerated
                    : false,
                child: const Padding(
                  padding: EdgeInsets.only(right: 8.0),
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: LoadingIndicator(
                      indicatorType: Indicator.lineSpinFadeLoader,
                      colors: [Color(0xFF12102F)],
                      strokeWidth: 1,
                    ),
                  ),
                ),
              ),
              const Text(
                "AI建议",
                style: TextStyle(
                  color: Color(0xFF12102F),
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  height: 1,
                  decoration: TextDecoration.none,
                ),
              ),
              const Spacer(),
              Visibility(
                visible: _selectedHistoryIndex == 0
                    ? _isSuggestionsGenerated &&
                        _aiGenerated?.suggestion != null
                    : showingGenerated?.suggestion != null,
                child: GestureDetector(
                  onTap: _isSuggestionSaved ? null : _saveSuggestion,
                  child: Visibility(
                    visible: !_isSuggestionSaved,
                    child: const Text(
                      "存入正文",
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
              ),
              Visibility(
                visible: _showSuggestionSaveSuccess,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0),
                      child: Image.asset(
                        AppImages.checkmarkSuccessIcon.path,
                        width: 16,
                        height: 16,
                      ),
                    ),
                    const Text(
                      "存入成功",
                      style: TextStyle(
                        color: Color(0xFF0FDA6C),
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        height: 1,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Visibility(
                  visible: _selectedHistoryIndex == 0
                      ? _isSuggestionsGenerated
                      : true,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 8.0),
                    child: Image.asset(
                      AppImages.hDash.path,
                      height: 1,
                      color: const Color(0xFFE1E1E1),
                    ),
                  ),
                ),
                Visibility(
                  visible: showingGenerated?.suggestion != null,
                  child: MarkdownBody(
                    data:
                        getMDShowingString(showingGenerated?.suggestion ?? ""),
                  ),
                ),
                Visibility(
                  visible: (showingGenerated?.suggestion ?? "") == "",
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(4),
                      color: const Color(0xFF45B670).withOpacity(0.1),
                    ),
                    height: 32,
                    child: Center(
                      child: Text(
                        _selectedHistoryIndex == 0
                            ? !_isSuggestionsGenerated
                                ? "正在生成"
                                : "未找到任何建议"
                            : "未找到任何建议",
                        style: const TextStyle(
                          color: Color(0xFF009D4F),
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
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

  Widget _aiGuessedView() {
    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Visibility(
                visible: _selectedHistoryIndex == 0 ? _isGuessGenerated : true,
                child: Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkBlackIcon.path,
                    width: 24,
                    height: 24,
                  ),
                ),
              ),
              Visibility(
                visible:
                    _selectedHistoryIndex == 0 ? !_isGuessGenerated : false,
                child: const Padding(
                  padding: EdgeInsets.only(right: 8.0),
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: LoadingIndicator(
                      indicatorType: Indicator.lineSpinFadeLoader,
                      colors: [Color(0xFF12102F)],
                      strokeWidth: 1,
                    ),
                  ),
                ),
              ),
              const Text(
                "猜你想看",
                style: TextStyle(
                  color: Color(0xFF12102F),
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  height: 1,
                  decoration: TextDecoration.none,
                ),
              ),
              const Spacer(),
              Visibility(
                visible: _selectedHistoryIndex == 0
                    ? _isGuessGenerated && _aiGenerated?.aiGuess != null
                    : showingGenerated?.aiGuess != null,
                child: GestureDetector(
                  onTap: _isGuessedSaved ? null : _saveGuessed,
                  child: Visibility(
                    visible: !_isGuessedSaved,
                    child: const Text(
                      "存入正文",
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
              ),
              Visibility(
                visible: _showGuessedSavedSuccess,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0),
                      child: Image.asset(
                        AppImages.checkmarkSuccessIcon.path,
                        width: 16,
                        height: 16,
                      ),
                    ),
                    const Text(
                      "存入成功",
                      style: TextStyle(
                        color: Color(0xFF0FDA6C),
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        height: 1,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Visibility(
                  visible:
                      _selectedHistoryIndex == 0 ? _isGuessGenerated : true,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 8.0),
                    child: Image.asset(
                      AppImages.hDash.path,
                      height: 1,
                      color: const Color(0xFFE1E1E1),
                    ),
                  ),
                ),
                Visibility(
                  visible: showingGenerated?.aiGuess != null,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(bottom: 16.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(bottom: 8.0),
                              child: Text(
                                "1.您可能需要的信息",
                                style: const TextStyle(
                                  color: Color(0xFF4C4A5C),
                                  fontSize: 16,
                                  fontWeight: FontWeight.w500,
                                  height: 1,
                                  decoration: TextDecoration.none,
                                ),
                              ),
                            ),
                            MarkdownBody(
                              data: getMDShowingString((showingGenerated
                                              ?.aiGuess?.relatedInfo ??
                                          "") ==
                                      ""
                                  ? "无"
                                  : showingGenerated?.aiGuess?.relatedInfo ??
                                      ""),
                            ),
                          ],
                        ),
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8.0),
                            child: Text(
                              "2.相关领域信息",
                              style: const TextStyle(
                                color: Color(0xFF4C4A5C),
                                fontSize: 16,
                                fontWeight: FontWeight.w500,
                                height: 1,
                                decoration: TextDecoration.none,
                              ),
                            ),
                          ),
                          MarkdownBody(
                            data: getMDShowingString(
                                showingGenerated?.aiGuess?.fieldInfo ?? ""),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                Visibility(
                  visible: showingGenerated?.aiGuess == null,
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(4),
                      color: const Color(0xFF45B670).withOpacity(0.1),
                    ),
                    height: 32,
                    child: Center(
                      child: Text(
                        _selectedHistoryIndex == 0
                            ? !_isGuessGenerated
                                ? "正在生成"
                                : "未找到任何建议"
                            : "未找到任何建议",
                        style: const TextStyle(
                          color: Color(0xFF009D4F),
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
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

  Widget _aiGenerateCategoryView() {
    final showingCategorizedNotes = (_selectedHistoryIndex == 0
            ? _categorizedNotes
            : _note?.noteAnalysisHistories?[_selectedHistoryIndex]
                .categorizedNotes) ??
        [];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
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
                Visibility(
                  visible:
                      _selectedHistoryIndex == 0 ? _isCategoryGenerated : true,
                  child: Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: Image.asset(
                      AppImages.checkmarkBlackIcon.path,
                      width: 24,
                      height: 24,
                    ),
                  ),
                ),
                Visibility(
                  visible: _selectedHistoryIndex == 0
                      ? !_isCategoryGenerated
                      : false,
                  child: const Padding(
                    padding: EdgeInsets.only(right: 8.0),
                    child: SizedBox(
                      width: 24,
                      height: 24,
                      child: LoadingIndicator(
                        indicatorType: Indicator.lineSpinFadeLoader,
                        colors: [Color(0xFF12102F)],
                        strokeWidth: 1,
                      ),
                    ),
                  ),
                ),
                const Text(
                  "信息归类",
                  style: TextStyle(
                    color: Color(0xFF12102F),
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    height: 1,
                    decoration: TextDecoration.none,
                  ),
                ),
                const Spacer(),
                Visibility(
                  visible: _selectedHistoryIndex == 0
                      ? _isCategoryGenerated && _categorizedNotes.isNotEmpty
                      : showingCategorizedNotes.isNotEmpty,
                  child: GestureDetector(
                    onTap: _isCategorySaved ? null : _saveCategory,
                    child: Visibility(
                      visible: !_isCategorySaved,
                      child: const Text(
                        "存入正文",
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
                ),
                Visibility(
                  visible: _showCategorySaveSuccess,
                  child: Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: Image.asset(
                          AppImages.checkmarkSuccessIcon.path,
                          width: 16,
                          height: 16,
                        ),
                      ),
                      const Text(
                        "存入成功",
                        style: TextStyle(
                          color: Color(0xFF0FDA6C),
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Image.asset(
              AppImages.hDash.path,
              height: 1,
              color: const Color(0xFFE1E1E1),
            ),
          ),

          Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(4),
              color: const Color(0xFF45B670).withOpacity(0.1),
            ),
            height: 32,
            child: Center(
              child: Text(
                _selectedHistoryIndex == 0
                    ? !_isCategoryGenerated
                        ? "正在生成"
                        : showingCategorizedNotes.isEmpty
                            ? "没有任何可归类的备忘录"
                            : "已自动为您归类信息到${showingCategorizedNotes.length}类备忘录中"
                    : showingCategorizedNotes.isEmpty
                        ? "没有任何可归类的备忘录"
                        : "已自动为您归类信息到${showingCategorizedNotes.length}类备忘录中",
                style: const TextStyle(
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
            visible: showingCategorizedNotes.isNotEmpty,
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                Navigator.of(context).push(MaterialTransparentRoute(
                    builder: (BuildContext context) => CategoryNotesListScreen(
                          categorizedNotes: showingCategorizedNotes,
                          noteType: widget.noteTypes,
                          onNoteUpdated: widget.onNoteUpdated,
                          onLocalNoteUpdated: widget.onLocalNoteUpdated,
                        )));
              },
              child: Padding(
                padding: const EdgeInsets.only(top: 12.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12.0),
                      child: Text(
                        showingCategorizedNotes.isEmpty
                            ? ""
                            : "归类原因：${getCategorizedReason(showingCategorizedNotes.first.noteText ?? "")}",
                        style: const TextStyle(
                          color: Color(0xFF4C4A5C),
                          fontSize: 14,
                          fontWeight: FontWeight.w400,
                          height: 1.4,
                          decoration: TextDecoration.none,
                        ),
                      ),
                    ),
                    Visibility(
                      visible: showingCategorizedNotes.isNotEmpty,
                      child: Stack(
                        children: [
                          Visibility(
                            visible: showingCategorizedNotes.length > 2,
                            child: Padding(
                              padding: const EdgeInsets.only(
                                  top: 20.0, left: 36, right: 36),
                              child: Container(
                                height: 88,
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                      color: const Color(0xFFEBEBEB)),
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                          Visibility(
                            visible: showingCategorizedNotes.length > 1,
                            child: Padding(
                              padding: const EdgeInsets.only(
                                  top: 10.0, left: 14, right: 14),
                              child: Container(
                                height: 88,
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(
                                      color: const Color(0xFFEBEBEB)),
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
                              border:
                                  Border.all(color: const Color(0xFFEBEBEB)),
                              color: Colors.white,
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(
                                        showingCategorizedNotes.isEmpty
                                            ? ""
                                            : getCategorizedTitle(
                                                showingCategorizedNotes
                                                        .first.noteText ??
                                                    ""),
                                        style: const TextStyle(
                                          color: Color(0xFF12102F),
                                          fontSize: 16,
                                          fontWeight: FontWeight.w500,
                                          height: 1,
                                          decoration: TextDecoration.none,
                                        ),
                                        maxLines: 1,
                                      ),
                                      Text(
                                        showingCategorizedNotes.isEmpty
                                            ? ""
                                            : getCategorizedContent(
                                                showingCategorizedNotes
                                                        .first.noteText ??
                                                    ""),
                                        maxLines: 1,
                                        style: const TextStyle(
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
                  ],
                ),
              ),
            ),
          ),

          // ListView.separated(
          //   physics: NeverScrollableScrollPhysics(),
          //   padding: EdgeInsets.zero,
          //   shrinkWrap: true,
          //   itemBuilder: (context, index) {
          //     return Column(
          //       crossAxisAlignment: CrossAxisAlignment.start,
          //       children: [
          //         Padding(
          //           padding: const EdgeInsets.only(bottom: 8.0),
          //           child: Text(
          //             "归类原因：${getCategorizedReason(_categorizedNotes[index].noteText ?? "")}",
          //             style: TextStyle(
          //               color: Color(0xFF4C4A5C),
          //               fontSize: 14,
          //               fontWeight: FontWeight.w400,
          //               height: 1.4,
          //               decoration: TextDecoration.none,
          //             ),
          //           ),
          //         ),
          //         Container(
          //           padding: const EdgeInsets.all(16),
          //           decoration: BoxDecoration(
          //             borderRadius: BorderRadius.circular(8),
          //             border: Border.all(color: Color(0xFFEBEBEB)),
          //           ),
          //           child: Row(
          //             children: [
          //               Expanded(
          //                 child: Column(
          //                   crossAxisAlignment: CrossAxisAlignment.start,
          //                   children: [
          //                     Padding(
          //                       padding: const EdgeInsets.only(bottom: 8.0),
          //                       child: Text(
          //                         getCategorizedTitle(
          //                             _categorizedNotes[index].noteText ?? ""),
          //                         style: TextStyle(
          //                           color: Color(0xFF12102F),
          //                           fontSize: 16,
          //                           fontWeight: FontWeight.w500,
          //                           height: 1,
          //                           decoration: TextDecoration.none,
          //                         ),
          //                       ),
          //                     ),
          //                     Text(
          //                       getCategorizedContent(
          //                           _categorizedNotes[index].noteText ?? ""),
          //                       maxLines: 1,
          //                       style: TextStyle(
          //                         color: Color(0xFF4C4A5C),
          //                         fontSize: 14,
          //                         fontWeight: FontWeight.w400,
          //                         height: 1,
          //                         decoration: TextDecoration.none,
          //                       ),
          //                       overflow: TextOverflow.ellipsis,
          //                     ),
          //                   ],
          //                 ),
          //               )
          //             ],
          //           ),
          //         ),
          //       ],
          //     );
          //   },
          //   separatorBuilder: (context, index) => const SizedBox(height: 8),
          //   itemCount: _categorizedNotes.isEmpty ? 0 : 1,
          // ),
        ],
      ),
    );
  }

  Widget _aiGenerateRelatedView() {
    final showingRelatedNotes = (_selectedHistoryIndex == 0
            ? _relatedNotes
            : _note
                ?.noteAnalysisHistories?[_selectedHistoryIndex].relatedNotes) ??
        [];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
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
                Visibility(
                  visible:
                      _selectedHistoryIndex == 0 ? _isRelatedGenerated : true,
                  child: Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: Image.asset(
                      AppImages.checkmarkBlackIcon.path,
                      width: 24,
                      height: 24,
                    ),
                  ),
                ),
                Visibility(
                  visible:
                      _selectedHistoryIndex == 0 ? !_isRelatedGenerated : false,
                  child: const Padding(
                    padding: EdgeInsets.only(right: 8.0),
                    child: SizedBox(
                      width: 24,
                      height: 24,
                      child: LoadingIndicator(
                        indicatorType: Indicator.lineSpinFadeLoader,
                        colors: [Color(0xFF12102F)],
                        strokeWidth: 1,
                      ),
                    ),
                  ),
                ),
                const Text(
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
                Visibility(
                  visible: _selectedHistoryIndex == 0
                      ? _isRelatedGenerated && _relatedNotes.isNotEmpty
                      : showingRelatedNotes.isNotEmpty,
                  child: GestureDetector(
                    onTap: _isRelatedSaved ? null : _saveRelated,
                    child: Visibility(
                      visible: !_isRelatedSaved,
                      child: const Text(
                        "存入正文",
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
                ),
                Visibility(
                  visible: _showRelatedSaveSuccess,
                  child: Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: Image.asset(
                          AppImages.checkmarkSuccessIcon.path,
                          width: 16,
                          height: 16,
                        ),
                      ),
                      const Text(
                        "存入成功",
                        style: TextStyle(
                          color: Color(0xFF0FDA6C),
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Image.asset(
              AppImages.hDash.path,
              height: 1,
              color: const Color(0xFFE1E1E1),
            ),
          ),
          Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(4),
              color: const Color(0xFF45B670).withOpacity(0.1),
            ),
            height: 32,
            child: Center(
              child: Text(
                _selectedHistoryIndex == 0
                    ? (_isRelatedGenerating
                        ? "大约需要${_getRelatedNotesCount}s，请稍等"
                        : _relatedNotes.isEmpty
                            ? "没有找到任何相关的备忘录"
                            : "为您匹配到${_relatedNotes.length}篇相关备忘录")
                    : showingRelatedNotes.isEmpty
                        ? "没有找到任何相关的备忘录"
                        : "为您匹配到${showingRelatedNotes.length}篇相关备忘录",
                style: const TextStyle(
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
            visible: showingRelatedNotes.isNotEmpty,
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                if (showingRelatedNotes.isNotEmpty) {
                  Navigator.of(context).push(MaterialTransparentRoute(
                      builder: (BuildContext context) => RelatedNotesListScreen(
                            relatedNotes: showingRelatedNotes,
                            noteType: widget.noteTypes,
                            onNoteUpdated: widget.onNoteUpdated,
                            onLocalNoteUpdated: widget.onLocalNoteUpdated,
                          )));
                }
              },
              child: Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: Stack(
                  children: [
                    Visibility(
                      visible: showingRelatedNotes.length > 2,
                      child: Padding(
                        padding: const EdgeInsets.only(
                            top: 20.0, left: 36, right: 36),
                        child: Container(
                          height: 88,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: const Color(0xFFEBEBEB)),
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                    Visibility(
                      visible: showingRelatedNotes.length > 1,
                      child: Padding(
                        padding: const EdgeInsets.only(
                            top: 10.0, left: 14, right: 14),
                        child: Container(
                          height: 88,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: const Color(0xFFEBEBEB)),
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
                        border: Border.all(color: const Color(0xFFEBEBEB)),
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
                                  showingRelatedNotes.isEmpty
                                      ? ""
                                      : getNoteTitle(showingRelatedNotes.first
                                                      .noteAnalysisContent ??
                                                  "")
                                              .isEmpty
                                          ? "无标题"
                                          : getNoteTitle(showingRelatedNotes
                                                  .first.noteAnalysisContent ??
                                              ""),
                                  style: const TextStyle(
                                    color: Color(0xFF12102F),
                                    fontSize: 16,
                                    fontWeight: FontWeight.w500,
                                    height: 1,
                                    decoration: TextDecoration.none,
                                  ),
                                  maxLines: 1,
                                ),
                                Text(
                                  showingRelatedNotes.isEmpty
                                      ? ""
                                      : getNoteBody(showingRelatedNotes
                                              .first.noteAnalysisContent ??
                                          ""),
                                  maxLines: 1,
                                  style: const TextStyle(
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

          // ListView.separated(
          //   physics: NeverScrollableScrollPhysics(),
          //   padding: EdgeInsets.zero,
          //   shrinkWrap: true,
          //   itemBuilder: (context, index) {
          //     return Column(
          //       crossAxisAlignment: CrossAxisAlignment.start,
          //       children: [
          //         Padding(
          //           padding: const EdgeInsets.only(bottom: 8.0),
          //           child: Text(
          //             "${index + 1}.相关原因：${(_relatedNotes[index].hitTags ?? []).map((e) => e).join("，")}",
          //             style: TextStyle(
          //               color: Color(0xFF4C4A5C),
          //               fontSize: 14,
          //               fontWeight: FontWeight.w400,
          //               height: 1.4,
          //               decoration: TextDecoration.none,
          //             ),
          //           ),
          //         ),
          //         Container(
          //           padding: const EdgeInsets.all(16),
          //           decoration: BoxDecoration(
          //             borderRadius: BorderRadius.circular(8),
          //             border: Border.all(color: Color(0xFFEBEBEB)),
          //           ),
          //           child: Row(
          //             children: [
          //               Expanded(
          //                 child: Column(
          //                   crossAxisAlignment: CrossAxisAlignment.start,
          //                   children: [
          //                     Padding(
          //                       padding: const EdgeInsets.only(bottom: 8.0),
          //                       child: Text(
          //                         getNoteTitle(_relatedNotes[index].noteAnalysisContent ?? "无标题"),
          //                         style: TextStyle(
          //                           color: Color(0xFF12102F),
          //                           fontSize: 16,
          //                           fontWeight: FontWeight.w500,
          //                           height: 1,
          //                           decoration: TextDecoration.none,
          //                         ),
          //                       ),
          //                     ),
          //                     Text(
          //                       getNoteBody(_relatedNotes[index].noteAnalysisContent ??
          //                           "") ,
          //                       maxLines: 1,
          //                       style: TextStyle(
          //                         color: Color(0xFF4C4A5C),
          //                         fontSize: 14,
          //                         fontWeight: FontWeight.w400,
          //                         height: 1,
          //                         decoration: TextDecoration.none,
          //                       ),
          //                       overflow: TextOverflow.ellipsis,
          //                     ),
          //                   ],
          //                 ),
          //               )
          //             ],
          //           ),
          //         ),
          //       ],
          //     );
          //   },
          //   separatorBuilder: (context, index) => const SizedBox(height: 8),
          //   itemCount: _relatedNotes.length,
          // ),
        ],
      ),
    );
  }

  Widget _aiGenerateSceneView() {
    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Visibility(
                visible: _selectedHistoryIndex == 0 ? _isSceneGenerated : true,
                child: Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkBlackIcon.path,
                    width: 24,
                    height: 24,
                  ),
                ),
              ),
              Visibility(
                visible:
                    _selectedHistoryIndex == 0 ? !_isSceneGenerated : false,
                child: const Padding(
                  padding: EdgeInsets.only(right: 8.0),
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: LoadingIndicator(
                      indicatorType: Indicator.lineSpinFadeLoader,
                      colors: [Color(0xFF12102F)],
                      strokeWidth: 1,
                    ),
                  ),
                ),
              ),
              const Text(
                "记录情景猜测",
                style: TextStyle(
                  color: Color(0xFF12102F),
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  height: 1,
                  decoration: TextDecoration.none,
                ),
              ),
              const Spacer(),
              Visibility(
                visible: _isSceneGenerated && _aiGenerated?.scene != null,
                child: GestureDetector(
                  onTap: _isSceneSaved ? null : _saveScene,
                  child: Visibility(
                    visible: !_isSceneSaved,
                    child: const Text(
                      "存入正文",
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
              ),
              Visibility(
                visible: _showSceneSaveSuccess,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0),
                      child: Image.asset(
                        AppImages.checkmarkSuccessIcon.path,
                        width: 16,
                        height: 16,
                      ),
                    ),
                    const Text(
                      "存入成功",
                      style: TextStyle(
                        color: Color(0xFF0FDA6C),
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        height: 1,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Visibility(
                  visible:
                      _selectedHistoryIndex == 0 ? _isSceneGenerated : true,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 8.0),
                    child: Image.asset(
                      AppImages.hDash.path,
                      height: 1,
                      color: const Color(0xFFE1E1E1),
                    ),
                  ),
                ),
                Visibility(
                  visible: showingGenerated?.scene != null,
                  child: MarkdownBody(
                    data: showingGenerated?.scene ?? "",
                    shrinkWrap: true,
                  ),
                ),
                Visibility(
                  visible: showingGenerated?.scene == null,
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(4),
                      color: const Color(0xFF45B670).withOpacity(0.1),
                    ),
                    height: 32,
                    child: Center(
                      child: Text(
                        _selectedHistoryIndex == 0
                            ? !_isSceneGenerated
                                ? "正在生成"
                                : "未找到任何情景猜测"
                            : "未找到任何情景猜测",
                        style: const TextStyle(
                          color: Color(0xFF009D4F),
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
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

  Widget _aiGenerateGuessView() {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
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
                Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkBlackIcon.path,
                    width: 24,
                    height: 24,
                  ),
                ),
                const Text(
                  "猜你想看",
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
                  onTap: _isGuessSaved ? null : _saveGuess,
                  child: Visibility(
                    visible: !_isGuessSaved,
                    child: const Text(
                      "存入正文",
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
                Visibility(
                  visible: _showGuessSaveSuccess,
                  child: Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: Image.asset(
                          AppImages.checkmarkSuccessIcon.path,
                          width: 16,
                          height: 16,
                        ),
                      ),
                      const Text(
                        "存入成功",
                        style: TextStyle(
                          color: Color(0xFF0FDA6C),
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                          height: 1,
                          decoration: TextDecoration.none,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Image.asset(
              AppImages.hDash.path,
              height: 1,
              color: const Color(0xFFE1E1E1),
            ),
          ),
          Visibility(
            visible: _aiGenerated?.guess != null,
            child: MarkdownBody(
              data: _aiGenerated?.guess ?? "",
              shrinkWrap: true,
            ),
          ),
        ],
      ),
    );
  }

  Widget _aiGenerateTodoView() {
    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Visibility(
                visible: showingGenerated?.todo != null,
                child: Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: Image.asset(
                    AppImages.checkmarkBlackIcon.path,
                    width: 24,
                    height: 24,
                  ),
                ),
              ),
              Visibility(
                visible: showingGenerated?.todo == null,
                child: const Padding(
                  padding: EdgeInsets.only(right: 8.0),
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: LoadingIndicator(
                      indicatorType: Indicator.lineSpinFadeLoader,
                      colors: [Color(0xFF12102F)],
                      strokeWidth: 1,
                    ),
                  ),
                ),
              ),
              const Text(
                "待办/计划",
                style: TextStyle(
                  color: Color(0xFF12102F),
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  height: 1,
                  decoration: TextDecoration.none,
                ),
              ),
              const Spacer(),
              Visibility(
                visible: showingGenerated?.todo != null,
                child: GestureDetector(
                  onTap: _isTodoSaved ? null : _saveTodo,
                  child: Visibility(
                    visible: !_isTodoSaved,
                    child: const Text(
                      "存入正文",
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
              ),
              Visibility(
                visible: _showTodoSaveSuccess,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0),
                      child: Image.asset(
                        AppImages.checkmarkSuccessIcon.path,
                        width: 16,
                        height: 16,
                      ),
                    ),
                    const Text(
                      "存入成功",
                      style: TextStyle(
                        color: Color(0xFF0FDA6C),
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        height: 1,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: Image.asset(
              AppImages.hDash.path,
              height: 1,
              color: const Color(0xFFE1E1E1),
            ),
          ),
          Visibility(
            visible: showingGenerated?.todo != null,
            child: MarkdownBody(
              data: (showingGenerated?.todo ?? "").replaceAll('- ', "\n- "),
              shrinkWrap: true,
            ),
          ),
          Visibility(
            visible: showingGenerated?.todo == null,
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(4),
                color: const Color(0xFF45B670).withOpacity(0.1),
              ),
              height: 32,
              child: Center(
                child: Text(
                  _selectedHistoryIndex == 0
                      ? !_isContentGenerated
                          ? "正在生成"
                          : "未生成任何待办"
                      : "未生成任何待办",
                  style: const TextStyle(
                    color: Color(0xFF009D4F),
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    height: 1,
                    decoration: TextDecoration.none,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _saveContent() async {
    setState(() {
      _isContentSaved = true;
      _showContentSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showContentSaveSuccess = false;
        _isContentSaved = false;
      });
    });

    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];

    if ((_note?.dimension ?? 0) == 0) {
      setState(() {
        _titleTextController.text = showingGenerated!.title ?? "";
      });
      if ((showingGenerated!.category ?? "").isNotEmpty) {
        final noteTypeId = widget.noteTypes
            .firstWhere((element) => element.name == showingGenerated.category)
            .id;
        setState(() {
          _note?.noteType = int.parse(noteTypeId);
          if ((showingGenerated.tag ?? "").isNotEmpty) {
            _note?.tags = showingGenerated.tag;
          }
          _selectedNoteTypeIndex = widget.noteTypes
              .indexWhere((element) => element.id == noteTypeId);
        });
      }
    }

    _appendToNote(
        (showingGenerated!.content ?? "").replaceAll("- ", "\n- "), null,
        imageUrl: showingGenerated.imageLink);
  }

  Future<void> _saveSuggestion() async {
    setState(() {
      _isSuggestionSaved = true;
      _showSuggestionSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showSuggestionSaveSuccess = false;
        _isSuggestionSaved = false;
      });
    });

    _appendToNote(
        (_aiGenerated!.suggestion ?? "").replaceAll("- ", "\n- "), "AI建议");
  }

  _appendToNote(String content, String? sectionTitle,
      {String? imageUrl}) async {
    final contentToAdd = sectionTitle == null
        ? "---\n\n$content\n"
        : "---\n\n### $sectionTitle\n$content\n";
    final mdDocument = md.Document(encodeHtml: false);
    final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);
    final delta = mdToDelta.convert(contentToAdd);
    _controller.replaceText(_controller.document.length - 1, 1, delta, null);
    if (null != imageUrl) {
      _controller.insertImageBlock(imageSource: imageUrl);
    }

    // final contentToAdd = sectionTitle == null ? "---\n\n$content\n" : "---\n\n### $sectionTitle\n$content\n";
    // final mdDocument = md.Document(encodeHtml: false);
    // final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);
    // final delta = mdToDelta.convert(contentToAdd);
    // final json = delta.toJson();
    //
    // final note = await APIService().appendToNote(_currentNoteId!, jsonEncode(json));
    //
    // final allNote = note?.noteAnalysisContent ?? "";
    // final noteBody = getNoteBody(allNote);
    //
    // final body = !note!.noteAnalysisContent!.contains("标题：") ? note.noteAnalysisContent ?? "" : getNoteBody(note.noteAnalysisContent ?? "");
    //
    //
    // if (note.noteAnalysisContent!.contains("标题：")) {
    //   List<dynamic> decodedList;
    //   try {
    //     decodedList = jsonDecode(body);
    //   } catch (e) {
    //     print('解析错误: $e');
    //     decodedList = [];
    //   }
    //
    //   if (decodedList.isNotEmpty) {
    //     final delta = Delta.fromJson(decodedList);
    //     if (delta.isNotEmpty) {
    //       _controller.document = Document.fromDelta(delta);
    //     }
    //   }
    //
    //
    // } else {
    //   final mdDocument = md.Document(encodeHtml: false);
    //   final mdToDelta = MarkdownToDelta(markdownDocument: mdDocument);
    //   final delta = mdToDelta.convert(note.noteAnalysisContent ?? "");
    //   if (delta.isNotEmpty) {
    //     _controller.document = Document.fromDelta(delta);
    //   }
    // }
    //
    _updateNote();
  }

  Future<void> _saveTodo() async {
    setState(() {
      _isTodoSaved = true;
      _showTodoSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showTodoSaveSuccess = false;
        _isTodoSaved = false;
      });
    });

    _appendToNote((_aiGenerated!.todo ?? "").replaceAll("- ", "\n- "), "待办/计划");
  }

  Future<void> _saveGuess() async {
    setState(() {
      _isGuessSaved = true;
      _showGuessSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showGuessSaveSuccess = false;
        _isGuessSaved = false;
      });
    });

    _appendToNote(_aiGenerated!.guess ?? "", "猜你想看");
  }

  Future<void> _saveScene() async {
    setState(() {
      _isSceneSaved = true;
      _showSceneSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showSceneSaveSuccess = false;
        _isSceneSaved = false;
      });
    });

    final theGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];

    _appendToNote("${theGenerated!.scene ?? ""}", "记录情景猜测");
  }

  Future<void> _saveGuessed() async {
    setState(() {
      _isGuessedSaved = true;
      _showGuessedSavedSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showGuessedSavedSuccess = false;
        _isGuessedSaved = false;
      });
    });

    final theGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];

    String guessedContent =
        "\n1. 1.您可能需要的信息\n\n" + (theGenerated?.aiGuess?.relatedInfo ?? "");
    guessedContent +=
        "\n\n2. 相关领域信息\n\n" + (theGenerated?.aiGuess?.fieldInfo ?? "");

    _appendToNote("$guessedContent", "猜你想看");
  }

  Future<void> _saveCategory() async {
    setState(() {
      _isCategorySaved = true;
      _showCategorySaveSuccess = true;
      // _noteCategory =
      //     getCategorizedTitle(_categorizedNotes.first.noteText ?? "");
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showCategorySaveSuccess = false;
        _isCategorySaved = false;
      });
    });

    _appendToNote(_categorizedNotes.first.noteText ?? "", "信息归类");
  }

  Future<void> _saveRelated() async {
    setState(() {
      _isRelatedSaved = true;
      _showRelatedSaveSuccess = true;
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _showRelatedSaveSuccess = false;
        _isRelatedSaved = false;
      });
    });

    // _appendToNote(_relatedNotes.map((e) => e.rawNote).join("\n"), "相关备忘录");

    final showingRelated = _selectedHistoryIndex == 0
        ? _relatedNotes
        : _note?.noteAnalysisHistories?[_selectedHistoryIndex].relatedNotes;

    final todoJsonString = jsonEncode(showingRelated);
    final block = BlockEmbed.custom(
      RelatedNotesBlockEmbed.fromJsonString(todoJsonString),
    );
    print(_controller.document.length);
    final lastIndex = _controller.document.length - 2;
    _controller.document.replace(lastIndex, 1, block);
  }

  Widget _aiGenerateResultView() {
    final showingGenerated = _selectedHistoryIndex == 0
        ? _aiGenerated
        : _note?.generatedList?[_selectedHistoryIndex];
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFFFFDF4),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.only(top: 8, left: 8, right: 8, bottom: 16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(
          children: [
            Visibility(
              visible:
                  _selectedHistoryIndex == 0 ? _isContentTextGenerated : true,
              child: Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: Image.asset(
                  AppImages.pinIcon.path,
                  width: 32,
                  height: 32,
                ),
              ),
            ),
            Visibility(
              visible:
                  _selectedHistoryIndex == 0 ? !_isContentTextGenerated : false,
              child: const Padding(
                padding: EdgeInsets.only(right: 8.0),
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: LoadingIndicator(
                    indicatorType: Indicator.lineSpinFadeLoader,
                    colors: [Color(0xFF12102F)],
                    strokeWidth: 1,
                  ),
                ),
              ),
            ),
            const Text(
              "正文整理",
              style: TextStyle(
                color: Color(0xFF624E00),
                fontSize: 24,
                fontWeight: FontWeight.w700,
                height: 1,
                decoration: TextDecoration.none,
              ),
            ),
            const Spacer(),
            Visibility(
              visible: _selectedHistoryIndex == 0
                  ? _isContentTextGenerated && _aiGenerated?.content != null
                  : showingGenerated?.content != null,
              child: GestureDetector(
                onTap: _isContentSaved ? null : _saveContent,
                child: Visibility(
                  visible: !_isContentSaved,
                  child: const Text(
                    "存入正文",
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
            ),
            Visibility(
              visible: _showContentSaveSuccess,
              child: Row(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: Image.asset(
                      AppImages.checkmarkSuccessIcon.path,
                      width: 16,
                      height: 16,
                    ),
                  ),
                  const Text(
                    "存入成功",
                    style: TextStyle(
                      color: Color(0xFF0FDA6C),
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                      height: 1,
                      decoration: TextDecoration.none,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        Visibility(
          visible: showingGenerated?.content != null,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Image.asset(
                  AppImages.hDash.path,
                  height: 1,
                ),
              ),
              Visibility(
                visible: showingGenerated?.title != null,
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 8.0),
                  child: Text(
                    showingGenerated?.title ?? "",
                    style: const TextStyle(
                      color: Color(0xFF12102F),
                      fontSize: 24,
                      fontWeight: FontWeight.w500,
                      height: 1,
                      decoration: TextDecoration.none,
                    ),
                  ),
                ),
              ),
              Visibility(
                visible: showingGenerated?.tag != null ||
                    showingGenerated?.category != null,
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 8.0),
                  child: Text(
                    (showingGenerated?.category ?? "") +
                        ((showingGenerated?.category ?? "").isNotEmpty
                            ? " / "
                            : "") +
                        (showingGenerated?.tag ?? "").replaceAll("；", " / "),
                    style: TextStyle(
                      color: const Color(0xFF12102F).withOpacity(0.5),
                      fontSize: 14,
                      fontWeight: FontWeight.w400,
                      height: 1,
                      decoration: TextDecoration.none,
                    ),
                  ),
                ),
              ),
              Visibility(
                visible: showingGenerated?.content != null,
                child: MarkdownBody(
                  data: getMDShowingString(showingGenerated?.content ?? ""),
                  shrinkWrap: true,
                ),
              ),
              Visibility(
                visible: showingGenerated?.imageLink != null,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: CachedNetworkImage(
                    imageUrl: showingGenerated?.imageLink ?? "",
                    width: double.infinity,
                    fit: BoxFit.fitWidth,
                    errorWidget: (context, url, error) => Image.asset(
                      AppImages.noImageIcon.path,
                      fit: BoxFit.cover,
                      width: 57,
                      height: 57,
                    ),
                  ),
                ),
              )
            ],
          ),
        ),
      ]),
    );
  }

  Widget _generatedErrorView() {
    return IgnorePointer(
      ignoring: !_isAIIn || _selectedTabIndex == 0 || !_isGenaratingError,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.only(top: 40.0),
          child: Container(
            height: MediaQuery.of(context).size.height - 40,
            width: MediaQuery.of(context).size.width,
            color: const Color(0xFFF3F5F8),
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 36.0),
                  child: Image.asset(
                    AppImages.aiGeneratingPic.path,
                    width: 240,
                    height: 240,
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.only(bottom: 40.0),
                  child: Text(
                    "网络连接存在问题",
                    style: TextStyle(
                      color: Color(0xFF180E25),
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      height: 1,
                    ),
                  ),
                ),
                GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: _startGenerate,
                  child: Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFFDFDEE8),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    height: 54,
                    width: MediaQuery.of(context).size.width - 32,
                    child: const Center(
                      child: Text(
                        "重新生成",
                        style: TextStyle(
                          color: Color(0xFF3A51FF),
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                          height: 1,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _generatingFirstCheckView() {
    return IgnorePointer(
      ignoring: !_isAIIn || _selectedTabIndex == 0 || !_isGenaratingError,
      child: const SafeArea(
        bottom: false,
        child: Padding(
          padding: EdgeInsets.only(top: 40.0),
          // child: Container(
          //   height: MediaQuery.of(context).size.height - 40,
          //   width: MediaQuery.of(context).size.width,
          //   color: Color(0xFFF3F5F8),
          //   child: Column(
          //     children: [
          //       Padding(
          //         padding: const EdgeInsets.symmetric(vertical: 36.0),
          //         child: Image.asset(
          //           AppImages.aiGeneratingPic.path,
          //           width: 220,
          //           height: 220,
          //         ),
          //       ),
          //       Text(
          //         "A理解信息中...",
          //         style: TextStyle(
          //           color: Color(0xFF180E25),
          //           fontSize: 18,
          //           fontWeight: FontWeight.w700,
          //           height: 1,
          //         ),
          //       )
          //     ],
          //   ),
          // ),
          child: AiDoing(),
        ),
      ),
    );
  }

  Widget _generateFailedView() {
    return IgnorePointer(
      ignoring: !_isAIIn || _selectedTabIndex == 0 || !_isGenerateFailed,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.only(top: 40.0),
          child: Container(
            height: MediaQuery.of(context).size.height - 40,
            width: MediaQuery.of(context).size.width,
            color: const Color(0xFFF3F5F8),
            child: SingleChildScrollView(
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 36.0),
                    child: Image.asset(
                      AppImages.aiGeneratingPic.path,
                      width: 220,
                      height: 220,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24.0),
                    child: Text(
                      _generateFaildCate == 0
                          ? "对不起，您记录的信息我无法理解，故不做任何优化"
                          : "您输入的备忘录可能有意义但我不能理解，帮您找到相关搜索结果参考",
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: Color(0xFF180E25),
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  Visibility(
                    visible: _generateFaildCate != 0,
                    child: Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(top: 24.0),
                          child: ListView.separated(
                            physics: const NeverScrollableScrollPhysics(),
                            padding: const EdgeInsets.symmetric(horizontal: 24),
                            shrinkWrap: true,
                            itemBuilder: (context, index) {
                              final meta = _aiGenerated!.linkMetadatas![index];
                              return GestureDetector(
                                behavior: HitTestBehavior.translucent,
                                onTap: () {
                                  Navigator.push(
                                    context,
                                    CupertinoPageRoute(
                                        builder: (_) => WebViewPage(
                                              url: meta?.url ?? "",
                                            )),
                                  );
                                },
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFFFFFFF),
                                    borderRadius: BorderRadius.circular(8),
                                    border: Border.all(
                                      color: const Color(0xFFEBEBEB),
                                      width: 1,
                                    ),
                                  ),
                                  padding: const EdgeInsets.all(16),
                                  child: Row(
                                    children: [
                                      Visibility(
                                        visible: meta?.image != null,
                                        child: Padding(
                                          padding: const EdgeInsets.only(
                                              right: 12.0),
                                          child: ClipRRect(
                                            borderRadius:
                                                BorderRadius.circular(8),
                                            child: CachedNetworkImage(
                                              imageUrl: meta?.image ?? "",
                                              fit: BoxFit.cover,
                                              width: 57,
                                              height: 57,
                                              errorWidget:
                                                  (context, url, error) =>
                                                      Image.asset(
                                                AppImages.noImageIcon.path,
                                                fit: BoxFit.cover,
                                                width: 57,
                                                height: 57,
                                              ),
                                            ),
                                          ),
                                        ),
                                      ),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Padding(
                                              padding: const EdgeInsets.only(
                                                  bottom: 10.0),
                                              child: Row(
                                                children: [
                                                  Expanded(
                                                    child: Padding(
                                                      padding:
                                                          const EdgeInsets.only(
                                                              right: 10.0),
                                                      child: Text(
                                                        meta?.title ?? "",
                                                        maxLines: 1,
                                                        style: const TextStyle(
                                                          color:
                                                              Color(0xFF4C4A5C),
                                                          fontSize: 14,
                                                          fontWeight:
                                                              FontWeight.w500,
                                                          height: 1,
                                                          decoration:
                                                              TextDecoration
                                                                  .none,
                                                        ),
                                                      ),
                                                    ),
                                                  ),
                                                  ClipRRect(
                                                    borderRadius:
                                                        BorderRadius.circular(
                                                            12),
                                                    child: CachedNetworkImage(
                                                      imageUrl:
                                                          (_aiGenerated?.favIco ??
                                                                          [])
                                                                      .length >
                                                                  index
                                                              ? _aiGenerated!
                                                                      .favIco![
                                                                          index]
                                                                      ?.url ??
                                                                  ""
                                                              : "",
                                                      fit: BoxFit.cover,
                                                      width: 24,
                                                      height: 24,
                                                    ),
                                                  ),
                                                ],
                                              ),
                                            ),
                                            Text(
                                              meta?.desc ?? "",
                                              maxLines: 1,
                                              style: const TextStyle(
                                                color: Color(0xFF4C4A5C),
                                                fontSize: 12,
                                                fontWeight: FontWeight.w400,
                                                height: 1,
                                                decoration: TextDecoration.none,
                                              ),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                            separatorBuilder: (context, index) =>
                                const SizedBox(height: 12),
                            itemCount:
                                (_aiGenerated?.linkMetadatas ?? []).length,
                          ),
                        ),
                        const SizedBox(
                          height: 160,
                        )
                      ],
                    ),
                  )
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _generatedView() {
    return IgnorePointer(
      ignoring: !_isAIIn || _selectedTabIndex == 0,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.only(top: 40.0),
          child: Container(
            height: MediaQuery.of(context).size.height - 40,
            width: MediaQuery.of(context).size.width,
            color: const Color(0xFFF3F5F8),
            child: SingleChildScrollView(
              child: Padding(
                padding: EdgeInsets.only(
                    left: 16.0,
                    right: 16,
                    bottom: MediaQuery.of(context).padding.bottom + 16),
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateResultView(),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateSuggestionView(),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateRelatedView(),
                    ),
                    // Visibility(
                    //   visible: _aiGenerated?.guess != null,
                    //   child: Padding(
                    //     padding: const EdgeInsets.only(top: 16.0),
                    //     child: _aiGenerateGuessView(),
                    //   ),
                    // ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGuessedView(),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateTodoView(),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateSceneView(),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16.0),
                      child: _aiGenerateCategoryView(),
                    ),
                    // Visibility(
                    //   visible:
                    //       _aiGenerated?.guess == null && !_isContentGenerated,
                    //   child: Padding(
                    //     padding: const EdgeInsets.only(top: 16.0),
                    //     child: _generatingTitleView("猜你想看", false),
                    //   ),
                    // ),
                    SizedBox(
                      height: (_note?.noteAnalysisHistories ?? []).isEmpty
                          ? 0
                          : 120,
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _bottomBar() {
    return Positioned(
      bottom: 16,
      left: 16,
      right: 16,
      child: SafeArea(
        child: Column(
          children: [
            Visibility(
              visible: _editorFocusNode.hasFocus || _titleFocusNode.hasFocus,
              child: Row(
                children: [
                  const Spacer(),
                  QuillToolbar(
                    configurations: const QuillToolbarConfigurations(),
                    child: Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12.0),
                          child: GestureDetector(
                            onTap: () {
                              showCupertinoModalBottomSheet(
                                context: context,
                                builder: (context) {
                                  return TodoSheet(
                                    todoContent: null,
                                    todoDate: null,
                                    onTodoCreated: (todoContent, todoDate) {
                                      final todo = EditorTodoModel(
                                        isChecked: false,
                                        item: todoContent ?? "",
                                        dateString: todoDate == null
                                            ? ""
                                            : formatDateTimeFromDateTime(
                                                todoDate,
                                                showYear: true),
                                      );
                                      final todoJsonString = jsonEncode(todo);
                                      final mdDocument =
                                          md.Document(encodeHtml: false);
                                      final mdToDelta = MarkdownToDelta(
                                          markdownDocument: mdDocument);
                                      final delta =
                                          mdToDelta.convert(todoJsonString);
                                      final block = BlockEmbed.custom(
                                        TodoBlockEmbed.fromDocument(
                                            Document.fromDelta(delta)),
                                      );
                                      final index =
                                          _controller.selection.baseOffset;
                                      final length =
                                          _controller.selection.extentOffset -
                                              index;
                                      _controller.replaceText(
                                          index, length, block, null);
                                    },
                                  );
                                },
                              );
                            },
                            child: Image.asset(
                              AppImages.todoBtn.path,
                              width: 48,
                              height: 48,
                            ),
                          ),
                        ),
                        GestureDetector(
                          onTap: () {
                            setState(() {
                              _isFontStyleViewShowing =
                                  !_isFontStyleViewShowing;
                            });
                          },
                          child: Padding(
                            padding: const EdgeInsets.only(bottom: 12.0),
                            child: Image.asset(
                              AppImages.textBtn.path,
                              width: 48,
                              height: 48,
                            ),
                          ),
                        ),
                        QuillToolbarImageButton(
                          controller: _controller,
                          options: QuillToolbarImageButtonOptions(
                            childBuilder: (options, extraOptions) {
                              return GestureDetector(
                                onTap: () {
                                  extraOptions.onPressed?.call();
                                },
                                child: Image.asset(
                                  AppImages.imageBtn.path,
                                  width: 48,
                                  height: 48,
                                ),
                              );
                            },
                          ),
                        ),
                      ],
                    ),
                  )
                ],
              ),
            ),
            Padding(
              padding:
                  EdgeInsets.only(top: _isFontStyleViewShowing ? 60 : 10.0),
              child: Container(
                width: MediaQuery.of(context).size.width - 32,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: const Color(0xFFE5E9EF),
                    width: 1,
                  ),
                ),
                padding:
                    const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
                child: Row(
                  children: [
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () async {
                        _unfocusAll();
                        _showVoiceDiscussDialog();
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 6.0),
                        child: Row(
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(right: 4.0),
                              child: Image.asset(
                                AppImages.micIcon.path,
                                width: 24,
                                height: 24,
                              ),
                            ),
                            const Text(
                              '语音讨论',
                              style: TextStyle(
                                color: Color(0xFF4C4A5C),
                                fontSize: 16,
                                fontWeight: FontWeight.w700,
                                height: 1,
                              ),
                            )
                          ],
                        ),
                      ),
                    ),
                    const Spacer(),
                    //a line
                    Container(
                      width: 1,
                      height: 16,
                      color: const Color(0xFFA9A9A9),
                    ),
                    const Spacer(),
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () async {
                        _unfocusAll();
                        if (_titleTextController.text.isEmpty &&
                            _controller.document
                                .toPlainText()
                                .replaceAll("\n", "")
                                .isEmpty) {
                          return;
                        }
                        _showContentAssistDialog();
                      },
                      child: AnimatedOpacity(
                        duration: const Duration(milliseconds: 200),
                        opacity: _titleTextController.text.isEmpty &&
                                _controller.document
                                    .toPlainText()
                                    .replaceAll("\n", "")
                                    .isEmpty
                            ? 0.5
                            : 1,
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 6.0),
                          child: Row(
                            children: [
                              Padding(
                                padding: const EdgeInsets.only(right: 4.0),
                                child: Image.asset(
                                  AppImages.editIcon.path,
                                  width: 24,
                                  height: 24,
                                  color: const Color(0xFF4C4A5C).withOpacity(
                                      _titleTextController.text.isEmpty &&
                                              _controller.document
                                                  .toPlainText()
                                                  .isEmpty
                                          ? 0.5
                                          : 1),
                                ),
                              ),
                              Text(
                                '内容辅助',
                                style: TextStyle(
                                  color: const Color(0xFF4C4A5C).withOpacity(
                                      _titleTextController.text.isEmpty &&
                                              _controller.document
                                                  .toPlainText()
                                                  .isEmpty
                                          ? 0.5
                                          : 1),
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                  height: 1,
                                ),
                              )
                            ],
                          ),
                        ),
                      ),
                    ),
                    const Spacer(),
                    //a line
                    Container(
                      width: 1,
                      height: 16,
                      color: const Color(0xFFA9A9A9),
                    ),
                    const Spacer(),
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () async {
                        _unfocusAll();
                        if (_titleTextController.text.isEmpty &&
                            _controller.document
                                .toPlainText()
                                .replaceAll("\n", "")
                                .isEmpty) {
                          return;
                        }
                        setState(() {
                          _startGenerate();
                        });
                      },
                      child: AnimatedOpacity(
                        duration: const Duration(milliseconds: 200),
                        opacity: _titleTextController.text.isEmpty &&
                                _controller.document
                                    .toPlainText()
                                    .replaceAll("\n", "")
                                    .isEmpty
                            ? 0.5
                            : 1,
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 6.0),
                          child: Row(
                            children: [
                              Padding(
                                padding: const EdgeInsets.only(right: 4.0),
                                child: Image.asset(
                                  AppImages.cateIcon.path,
                                  width: 24,
                                  height: 24,
                                  color: const Color(0xFF4C4A5C).withOpacity(
                                      _titleTextController.text.isEmpty &&
                                              _controller.document
                                                  .toPlainText()
                                                  .isEmpty
                                          ? 0.5
                                          : 1),
                                ),
                              ),
                              Text(
                                '一键整理',
                                style: TextStyle(
                                  color: const Color(0xFF4C4A5C).withOpacity(
                                      _titleTextController.text.isEmpty &&
                                              _controller.document
                                                  .toPlainText()
                                                  .isEmpty
                                          ? 0.5
                                          : 1),
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                  height: 1,
                                ),
                              )
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _tipSheet() {
    return Container(
      height: 640,
      decoration: const BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            bottom: 88,
            right: 0,
            child: SafeArea(
              child: Image.asset(
                AppImages.aiGenerateBackground.path,
                width: 280,
                height: 280,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(
                top: 41.0, left: 24, right: 24, bottom: 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.only(bottom: 12.0),
                  child: Text(
                    "AI一键整理",
                    style: TextStyle(
                      color: Color(0xFF4C4A5C),
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      height: 1,
                      decoration: TextDecoration.none,
                    ),
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.only(bottom: 16.0),
                  child: Text(
                    "默认整理全部正文，也可以选中部分文字整理",
                    style: TextStyle(
                      color: Color(0xFF4C4A5C),
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      height: 1,
                      decoration: TextDecoration.none,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 52.0),
                  child: Image.asset(
                    AppImages.aiGenerateContent.path,
                    height: 233,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 14.0),
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      Navigator.pop(context);
                      AppLocalSettings.instance.setAIGenerateTipsDisabled(true);
                      _startGenerate();
                    },
                    child: Container(
                      height: 54,
                      decoration: BoxDecoration(
                        color: const Color(0xFF12102F),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Center(
                        child: Text(
                          "试一试",
                          style: TextStyle(
                            color: Color(0xFFFDFCFF),
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            height: 1,
                            decoration: TextDecoration.none,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 14.0),
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      Navigator.pop(context);
                    },
                    child: Container(
                      height: 54,
                      decoration: BoxDecoration(
                        color: const Color(0xFFDFDEE8),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Center(
                        child: Text(
                          "关闭",
                          style: TextStyle(
                            color: Color(0xFF12102F),
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            height: 1,
                            decoration: TextDecoration.none,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 14.0),
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      Navigator.pop(context);
                      AppLocalSettings.instance.setAIGenerateTipsDisabled(true);
                    },
                    child: Container(
                      height: 54,
                      child: const Center(
                        child: Text(
                          "关闭且不再提醒",
                          style: TextStyle(
                            color: Color(0xFF12102F),
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            height: 1,
                            decoration: TextDecoration.none,
                          ),
                        ),
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

  _noteTypeView() {
    return IgnorePointer(
      ignoring: _noteTypeBtnPosition == null,
      child: AnimatedOpacity(
        duration: const Duration(milliseconds: 200),
        opacity: _noteTypeBtnPosition != null ? 1 : 0,
        child: Stack(
          children: [
            GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                setState(() {
                  _noteTypeBtnPosition = null;
                });
              },
              child: Container(
                color: const Color(0xFF0E152F).withOpacity(0.4),
              ),
            ),
            Positioned(
              left: _noteTypeBtnPosition != null ? 20 : -300,
              top: (_noteTypeBtnPosition?.dy ?? 0) + 40,
              child: AnimatedOpacity(
                duration: const Duration(milliseconds: 200),
                opacity: _noteTypeBtnPosition != null ? 1 : 0,
                child: GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: () {},
                  child: Container(
                    width: 200,
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.of(context).size.height -
                          (_noteTypeBtnPosition?.dy ?? 0) -
                          40 -
                          MediaQuery.of(context).padding.bottom -
                          20,
                    ),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 4.0, vertical: 8),
                    decoration: const BoxDecoration(
                      color: Color(0xFFECEBF1),
                      borderRadius: BorderRadius.all(Radius.circular(8)),
                    ),
                    child: ListView.separated(
                      shrinkWrap: true,
                      padding: const EdgeInsets.all(0),
                      itemBuilder: (context, index) {
                        final entry = widget.noteTypes[index];
                        final noteType = _note?.noteType != null
                            ? _note?.noteType.toString() ?? "0"
                            : widget.toCreateNoteTypeId;
                        final isSelected = noteType == entry.id;
                        return GestureDetector(
                          behavior: HitTestBehavior.translucent,
                          onTap: () {
                            setState(() {
                              _selectedNoteTypeIndex = index;
                              _note?.noteType = int.parse(entry.id);
                              _noteTypeBtnPosition = null;
                            });
                            _updateNote();
                          },
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color:
                                  Colors.white.withOpacity(isSelected ? 1 : 0),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Row(
                              children: [
                                Padding(
                                  padding: const EdgeInsets.only(right: 8.0),
                                  child: Container(
                                    width: 4,
                                    height: 12,
                                    decoration: BoxDecoration(
                                      color: const Color(0xFF315FFF),
                                      borderRadius: BorderRadius.circular(2),
                                    ),
                                  ),
                                ),
                                Text(
                                  entry.name,
                                  style: const TextStyle(
                                    color: Color(0xFF12102F),
                                    fontWeight: FontWeight.w400,
                                    fontSize: 16,
                                  ),
                                ),
                                const Spacer(),
                                AnimatedOpacity(
                                  duration: const Duration(milliseconds: 200),
                                  opacity: isSelected ? 1 : 0,
                                  child: Image.asset(
                                    AppImages.checkIcon.path,
                                    width: 16,
                                    height: 16,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                      separatorBuilder: (context, index) => const SizedBox(
                        height: 8,
                      ),
                      itemCount: widget.noteTypes.length,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _editorView() {
    var noteType = "未设置";

    try {
      noteType = widget.noteTypes
          .firstWhere(
            (element) => _note?.noteType != null
                ? element.id == _note?.noteType.toString()
                : element.id == widget.toCreateNoteTypeId,
            orElse: () => widget.noteTypes.first,
          )
          .name;
    } catch (e) {
      print('noteType $e ${widget.noteTypes}');
    }

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Column(
          children: [
            Stack(
              children: [
                Positioned(
                  top: 8,
                  left: 46,
                  child: AnimatedOpacity(
                    duration: const Duration(milliseconds: 300),
                    opacity: _isAIIn && _selectedTabIndex == 0 ? 1 : 0,
                    child: Container(
                      width: 19,
                      height: 19,
                      decoration: BoxDecoration(
                        color: const Color(0xFF3A51FF).withOpacity(0.3),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ),
                Positioned(
                  top: 8,
                  left: 106,
                  child: AnimatedOpacity(
                    duration: const Duration(milliseconds: 300),
                    opacity: _isAIIn && _selectedTabIndex == 1 ? 1 : 0,
                    child: Container(
                      width: 19,
                      height: 19,
                      decoration: BoxDecoration(
                        color: const Color(0xFF3A51FF).withOpacity(0.3),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ),
                Row(
                  children: [
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        Navigator.pop(context);
                      },
                      child: Padding(
                        padding: const EdgeInsets.only(right: 24.0),
                        child: Image.asset(
                          AppImages.backBtn.path,
                          width: 28,
                          height: 28,
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(right: 20.0),
                      child: GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTap: () {
                          FocusScope.of(context).unfocus();
                          if (!_isAIIn) {
                            return;
                          }
                          setState(() {
                            _selectedTabIndex = 0;
                            _isGeneratingToastShowing = false;
                          });
                        },
                        child: AnimatedOpacity(
                          duration: const Duration(milliseconds: 300),
                          opacity: _isAIIn ? 1 : 0,
                          child: Text(
                            "原文",
                            style: TextStyle(
                              color: const Color(0xFF12102F).withOpacity(
                                  _selectedTabIndex == 0 ? 1 : 0.5),
                              fontSize: 20,
                              fontWeight: _selectedTabIndex == 0
                                  ? FontWeight.w700
                                  : FontWeight.w400,
                              height: 1,
                              decoration: TextDecoration.none,
                            ),
                          ),
                        ),
                      ),
                    ),
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        FocusScope.of(context).unfocus();
                        // _keyEditor.currentState!.javascriptExecutor.unFocus();
                        if (!_isAIIn) {
                          return;
                        }
                        setState(() {
                          _selectedTabIndex = 1;
                        });
                      },
                      child: AnimatedOpacity(
                        duration: const Duration(milliseconds: 300),
                        opacity: _isAIIn ? 1 : 0,
                        child: Text(
                          "AI生成",
                          style: TextStyle(
                            color: const Color(0xFF12102F)
                                .withOpacity(_selectedTabIndex == 1 ? 1 : 0.5),
                            fontSize: 20,
                            fontWeight: _selectedTabIndex == 1
                                ? FontWeight.w700
                                : FontWeight.w400,
                            height: 1,
                            decoration: TextDecoration.none,
                          ),
                        ),
                      ),
                    ),
                    const Spacer(),
                    Visibility(
                      visible: _selectedTabIndex == 0,
                      child: Padding(
                        padding: const EdgeInsets.only(right: 16.0),
                        child: QuillToolbar(
                          configurations: const QuillToolbarConfigurations(),
                          child: Row(
                            children: [
                              Visibility(
                                visible: _editorFocusNode.hasFocus ||
                                    _titleFocusNode.hasFocus,
                                child: Padding(
                                  padding: const EdgeInsets.only(right: 16.0),
                                  child: AnimatedOpacity(
                                    duration: const Duration(milliseconds: 300),
                                    opacity: _controller.hasUndo ? 1 : 0.1,
                                    child: QuillToolbarHistoryButton(
                                      controller: _controller,
                                      isUndo: true,
                                      options: QuillToolbarHistoryButtonOptions(
                                        childBuilder: (options, extraOptions) {
                                          return GestureDetector(
                                            onTap: () {
                                              extraOptions.onPressed?.call();
                                            },
                                            child: Image.asset(
                                              AppImages.undoIcon.path,
                                              width: 24,
                                              height: 24,
                                            ),
                                          );
                                        },
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                              Visibility(
                                visible: _editorFocusNode.hasFocus ||
                                    _titleFocusNode.hasFocus,
                                child: AnimatedOpacity(
                                  duration: const Duration(milliseconds: 300),
                                  opacity: _controller.hasRedo ? 1 : 0.1,
                                  child: QuillToolbarHistoryButton(
                                    controller: _controller,
                                    isUndo: false,
                                    options: QuillToolbarHistoryButtonOptions(
                                      childBuilder: (options, extraOptions) {
                                        return GestureDetector(
                                          onTap: () {
                                            extraOptions.onPressed?.call();
                                          },
                                          child: Image.asset(
                                            AppImages.redoIcon.path,
                                            width: 24,
                                            height: 24,
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    Visibility(
                      visible: _selectedTabIndex == 1 ||
                          (!_editorFocusNode.hasFocus &&
                              !_titleFocusNode.hasFocus) ||
                          (_titleTextController.text.isEmpty &&
                              _controller.document
                                  .toPlainText()
                                  .replaceAll("\n", "")
                                  .isEmpty),
                      child: GestureDetector(
                        onTap: () {
                          FocusScope.of(context).unfocus();
                          if (_note != null) {
                            Clipboard.setData(
                                ClipboardData(text: _note!.id.toString()));
                            setState(() {
                              _isNoteActionViewShowing = true;
                            });
                          }
                        },
                        child: Image.asset(
                          AppImages.threeDotIcon.path,
                          width: 24,
                          height: 24,
                        ),
                      ),
                    ),
                    Visibility(
                      visible: _selectedTabIndex == 0 &&
                          (_editorFocusNode.hasFocus ||
                              _titleFocusNode.hasFocus) &&
                          !(_titleTextController.text.isEmpty &&
                              _controller.document
                                  .toPlainText()
                                  .replaceAll("\n", "")
                                  .isEmpty),
                      child: GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTap: () async {
                          FocusScope.of(context).unfocus();
                          final aiGenerateTipsDisabled = await AppLocalSettings
                              .instance
                              .getAIGenerateTipsDisabled();
                          if (!aiGenerateTipsDisabled) {
                            showCupertinoModalBottomSheet(
                              context: context,
                              builder: (context) {
                                return _tipSheet();
                              },
                            );
                          } else {
                            _updateNote();
                            setState(() {
                              _isUpdateSuccessToastShowing = true;
                            });
                            await Future.delayed(const Duration(seconds: 2));
                            setState(() {
                              _isUpdateSuccessToastShowing = false;
                            });
                          }
                        },
                        child: Image.asset(
                          AppImages.checkIcon.path,
                          width: 24,
                          height: 24,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: 12.0, top: 16),
              child: TextField(
                enabled: (_note?.dimension ?? 0) == 0,
                controller: _titleTextController,
                focusNode: _titleTextFocusNode,
                autofocus: false,
                cursorColor: const Color(0xFF000000),
                style: const TextStyle(
                  color: Color(0xFF4C4B5C),
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  height: 1.2,
                ),
                decoration: InputDecoration(
                  hintText: '标题',
                  hintStyle: TextStyle(
                    color: const Color(0xFF12102F).withOpacity(0.3),
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    height: 1.2,
                  ),
                  border: InputBorder.none,
                ),
                maxLines: 2,
                minLines: 1,
                textAlignVertical: TextAlignVertical.top,
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: 12.0),
              child: Row(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(right: 12.0),
                    child: GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        if ((_note?.dimension ?? 0) == 0) {
                          final RenderBox renderBox =
                              _noteTypeBtnkey.currentContext!.findRenderObject()
                                  as RenderBox;
                          final position = renderBox.localToGlobal(Offset.zero);
                          setState(() {
                            _noteTypeBtnPosition = position;
                          });
                        }
                      },
                      child: Container(
                        key: _noteTypeBtnkey,
                        decoration: BoxDecoration(
                          color: const Color(0xFFECEBF1),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 12.0, vertical: 4.0),
                        child: Row(
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(right: 8.0),
                              child: Text(
                                noteType,
                                style: const TextStyle(
                                  color: Color(0xFF888797),
                                  fontSize: 14,
                                  fontWeight: FontWeight.w400,
                                  height: 1,
                                ),
                              ),
                            ),
                            Image.asset(
                              AppImages.downIcon.path,
                              width: 16,
                              height: 16,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  Text(
                    _note == null
                        ? ""
                        : _note!.createdAt == null
                            ? getCurrentTime()
                            : formatDateTime(_note!.createdAt!),
                    style: const TextStyle(
                      color: Color(0xFF888797),
                      fontSize: 14,
                      fontWeight: FontWeight.w400,
                      height: 1,
                    ),
                  )
                ],
              ),
            ),
            Expanded(
              child: QuillEditor(
                controller: _controller,
                configurations: QuillEditorConfigurations(
                  customStyles: const DefaultStyles(
                    paragraph: DefaultTextBlockStyle(
                        TextStyle(
                          color: Color(0xFF12102F),
                          fontSize: 14,
                          height: 1.7,
                        ),
                        HorizontalSpacing(0, 0),
                        VerticalSpacing(0, 0),
                        VerticalSpacing(0, 0),
                        null),
                  ),
                  searchConfigurations: const QuillSearchConfigurations(
                    searchEmbedMode: SearchEmbedMode.plainText,
                  ),
                  embedBuilders: [
                    TodoListEmbedBuilder(),
                    VoiceDiscussCardEmbedBuilder(),
                    DividerEmbedBuilder(),
                    TodoEmbedBuilder(),
                    RelatedNotesEmbedBuilder(),
                    QuillEditorImageEmbedBuilder(
                        configurations: QuillEditorImageEmbedConfigurations(
                      imageErrorWidgetBuilder: (context, error, stackTrace) {
                        return Text(
                          'Error while loading an image: ${error.toString()}',
                        );
                      },
                      imageProviderBuilder: (context, imageUrl) {
                        if (isHttpBasedUrl(imageUrl)) {
                          return Image.network(
                            imageUrl,
                            width: double.infinity,
                            fit: BoxFit.fitWidth,
                          ).image;
                        }
                        final file = File(imageUrl);
                        return Image.file(
                          file,
                          width: 240,
                          height: 240,
                          fit: BoxFit.cover,
                        ).image;
                      },
                    )),
                  ],
                ),
                scrollController: _editorScrollController,
                focusNode: _editorFocusNode,
              ),
            ),
            const SizedBox(
              height: 80,
            ),
          ],
        ),
      ),
    );
  }

  Widget _fontStyleView() {
    return Positioned(
      bottom: 0,
      left: 0,
      right: 0,
      child: IgnorePointer(
        ignoring: !_isFontStyleViewShowing,
        child: AnimatedOpacity(
          duration: const Duration(milliseconds: 300),
          opacity: _isFontStyleViewShowing ? 1 : 0,
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFFFFFFFF),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF000000).withOpacity(0.1),
                  offset: const Offset(0, 4),
                  blurRadius: 32,
                ),
              ],
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(20),
                topRight: Radius.circular(20),
              ),
            ),
            padding: const EdgeInsets.symmetric(vertical: 24.0),
            child: QuillToolbar(
              configurations: const QuillToolbarConfigurations(),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(bottom: 24.0),
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24.0),
                        child: Row(
                          children: [
                            QuillToolbarSelectHeaderStyleButtons(
                              options:
                                  QuillToolbarSelectHeaderStyleButtonsOptions(
                                attributes: const [
                                  Attribute.header,
                                  Attribute.h1,
                                  Attribute.h2,
                                  Attribute.h3,
                                ],
                                iconTheme: const QuillIconTheme().copyWith(
                                  iconButtonSelectedData: IconButtonData(
                                    color: const Color(0xFF12102F),
                                    style: ButtonStyle(
                                      side: WidgetStateProperty.all(
                                        const BorderSide(
                                          width: 2,
                                          color: Color(0xFF12102F),
                                        ),
                                      ),
                                      backgroundColor:
                                          WidgetStateProperty.all(Colors.white),
                                    ),
                                  ),
                                  iconButtonUnselectedData: IconButtonData(
                                    style: ButtonStyle(
                                      backgroundColor: WidgetStateProperty.all(
                                          const Color(0xFFF3F5F8)),
                                    ),
                                  ),
                                ),
                              ),
                              controller: _controller,
                            ),
                            QuillToolbarToggleStyleButton(
                              options: QuillToolbarToggleStyleButtonOptions(
                                iconTheme: const QuillIconTheme().copyWith(
                                  iconButtonSelectedData: IconButtonData(
                                    color: const Color(0xFF12102F),
                                    style: ButtonStyle(
                                      side: WidgetStateProperty.all(
                                        const BorderSide(
                                          width: 2,
                                          color: Color(0xFF12102F),
                                        ),
                                      ),
                                      backgroundColor:
                                          WidgetStateProperty.all(Colors.white),
                                    ),
                                  ),
                                  iconButtonUnselectedData: IconButtonData(
                                    style: ButtonStyle(
                                      backgroundColor: WidgetStateProperty.all(
                                          const Color(0xFFF3F5F8)),
                                    ),
                                  ),
                                ),
                              ),
                              controller: _controller,
                              attribute: Attribute.bold,
                            ),
                            QuillToolbarToggleStyleButton(
                              options: QuillToolbarToggleStyleButtonOptions(
                                iconTheme: const QuillIconTheme().copyWith(
                                  iconButtonSelectedData: IconButtonData(
                                    color: const Color(0xFF12102F),
                                    style: ButtonStyle(
                                      side: WidgetStateProperty.all(
                                        const BorderSide(
                                          width: 2,
                                          color: Color(0xFF12102F),
                                        ),
                                      ),
                                      backgroundColor:
                                          WidgetStateProperty.all(Colors.white),
                                    ),
                                  ),
                                  iconButtonUnselectedData: IconButtonData(
                                    style: ButtonStyle(
                                      backgroundColor: WidgetStateProperty.all(
                                          const Color(0xFFF3F5F8)),
                                    ),
                                  ),
                                ),
                              ),
                              controller: _controller,
                              attribute: Attribute.strikeThrough,
                            ),
                            QuillToolbarColorButton(
                                controller: _controller, isBackground: false),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _startAiAssist() async {
    AIHistoryModel? historyModel =
        await APIService.createNoteAnalysisHistory(_note!.id!, 2);

    _note?.version = historyModel?.version;

    FocusScope.of(context).unfocus();
    // EasyLoading.show();
    setState(() {
      _contentAssistResult = ContentAssistResultModel.fromRawString('');
      _selectedBottomTabIndex = 2;
      _isAIIn = true;
      _selectedTabIndex = 1;
      _isGeneratingToastShowing = true;

      _isContentGenerating = true;
      _isGeneratingFirstCheck = true;
      _isGenerateFailed = false;
      _isGenaratingError = false;
    });

    var rawString = '';
    // todo history api 需要把入参保存, 历史列表需要按时间倒序排序
    await for (var line in APIService.getAssistanceResult(
        noteModel: _note!,
        assistDirections: _aiAssistSelectedItems,
        selectedTitle: _selectedTitleText,
        selectedContent: _selectedContentText)) {
      rawString += line.replaceAll("data:", "");
    }

    var result = ContentAssistResultModel.fromRawString(rawString);
    print('_startAiAssist rawString: $rawString');
    print('_startAiAssist result: $result');

    setState(() {
      _contentAssistResult = result;
      _currentNoteId = _note?.id.toString();
    });

    Future.delayed(const Duration(milliseconds: 500), () {
      setState(() {
        _isGeneratingToastShowing = false;
        _isGeneratingFirstCheck = false;
        _isContentGenerating = false;
      });
    });

    if (rawString.isEmpty) {
      setState(() {
        _isGenerateFailed = true;
        _generateFaildCate == 0;
      });
      return;
    }

    _getHistoryList();
  }

  Future<bool> _saveAssistantContent() async {
    try {
      await _appendToNote(_contentAssistResult.rewrite_content, "AI辅助");
      return true;
    } catch (e) {
      print('saveAssistantContent error: $e');
      return false;
    }
  }

  Future<bool> _saveTodoList1(List<TodoModel> todos) async {
    try {
      final jsonContent = _controller.document.toDelta().toJson();
      print('saveTodoList jsonContent: $jsonContent');
      // 从jsonContent找到  'attributes': {'list': 'unchecked'} 或者 'attributes': {'list': 'checked'} 的位置
      var todoIndex = jsonContent.indexWhere((element) {
        return element['attributes'] != null &&
            (element['attributes']['list'] == 'unchecked' ||
                element['attributes']['list'] == 'checked');
      });

      List<Map<String, dynamic>> todoJsonList = [];
      for (var item in todos) {
        // todoStr += '- [ ] 待办内容：${item[0]} 待办时间：${item[1]}\n';
        todoJsonList.addAll([
          {
            'attributes': {'list': item.done ? 'checked' : 'unchecked'},
            // 待办状态 'checked' | 'unchecked'
            'insert': '${item.title} \xa0 ${item.date}\n'
          },
        ]);
      }

      if (todoIndex == -1) {
        List<Map<String, dynamic>> todoTitle = [
          {
            'insert': {'divider': 'hr'}
          },
          {
            'attributes': {'header': 3},
            'insert': '待办/计划\n'
          },
        ];
        jsonContent
          ..addAll(todoTitle)
          ..addAll(todoJsonList);
      } else {
        jsonContent.insertAll(
          todoIndex,
          todoJsonList,
        );
      }
      _controller.document = Document.fromJson(jsonContent);

      _updateNote();
      return true;
    } catch (e) {
      print('_saveTodo error: $e');
      return false;
    }
  }

  Future<bool> _saveTodoList(List<TodoModel> todos) async {
    if (todos.isEmpty) {
      return true;
    }
    try {
      final jsonContent = _controller.document.toDelta().toJson();
      _controller.document = Document.fromJson(jsonContent);
      insertQuillCustomEmbedNode(_controller, TodoListEmbed(jsonEncode(todos)));

      _updateNote();
      return true;
    } catch (e) {
      print('_saveTodo error: $e');
      return false;
    }
  }

  void _showContentAssistDialog() async {
    if (_titleTextController.text.trim().isEmpty &&
        _controller.plainTextEditingValue.text.trim().isEmpty) {
      EasyLoading.showToast('请输入标题或内容');
      return;
    }

    FocusScope.of(context).unfocus();
    await _updateNote();

    _originContent = _controller.plainTextEditingValue.text;

    if (!mounted) {
      return;
    }

    Future.delayed(const Duration(milliseconds: 100), () {
      showCupertinoModalBottomSheet(
        bounce: true,
        enableDrag: false,
        context: context,
        topRadius: const Radius.circular(20),
        builder: (context) {
          return SingleChildScrollView(
            child: Padding(
              padding: EdgeInsets.only(
                bottom: MediaQuery.of(context).viewInsets.bottom,
              ),
              child: AiAssistContent(
                noteModel: _note!,
                selectedTitleText: _selectedTitleText,
                selectedContentText: _selectedContentText,
                onConfirm: (selectedItems) {
                  _contentAssistResult =
                      ContentAssistResultModel.fromRawString('');
                  _aiAssistSelectedItems = [];
                  setState(() {
                    _aiAssistSelectedItems = selectedItems;
                    _startAiAssist();
                    Future.delayed(const Duration(milliseconds: 1000), () {
                      Navigator.of(context).pop();
                    });
                  });
                },
              ),
            ),
          );
        },
      );
    });
  }

  Future<bool> _saveDiscussNote(String text) async {
    if (text.trim().isEmpty) {
      return true;
    }
    try {
      await _appendToNote(text, "备忘录信息");
      return true;
    } catch (e) {
      print('_saveDiscussNote error: $e');
      return false;
    }
  }

  void _saveDiscussSummaryToNote(String summary) async {
    await _appendToNote(summary, "语音讨论总结");
  }

  Future<bool> _saveDiscussAll(
      {required List<TodoModel> todo, required String note}) async {
    await _saveTodoList(todo);
    await _saveDiscussNote(note);
    insertQuillCustomEmbedNode(
        _controller, VoiceDiscussCardEmbed('${_note?.id}'));
    return true;
  }

  void _showVoiceDiscussDialog() async {
    AIHistoryModel historyModel = AIHistoryModel();
    try {
      if (_titleTextController.text.trim().isEmpty) {
        _titleTextController.text = '语音讨论';
        await _updateNote();
      }
      // 生成历史记录
      historyModel = await APIService.createNoteAnalysisHistory(_note!.id!, 3);
      _note?.version = historyModel.version;
    } catch (e) {
      EasyLoading.showToast('网络异常，请稍后重试');
    }

    if (!mounted) {
      return;
    }

    showCupertinoDialog(
      context: context,
      builder: (context) {
        return AiVoiceDiscuss(
          noteModel: _note,
          onSaveTodo: _saveTodoList,
          onSaveNote: _saveDiscussNote,
        );
      },
    ).then((_) async {
      _isAIIn = true;
      _selectedTabIndex = 1;
      _selectedBottomTabIndex = 3;

      _isGeneratingToastShowing = false;

      _isContentGenerating = true;
      _isGeneratingFirstCheck = false;
      _isGenerateFailed = false;
      _isGenaratingError = false;

      await APIService.updateNoteDiscussAudioHistory(
        historyModel.id!,
        messageLogic.messageModelList,
      );
      _getHistoryList();
    });
  }

  void _unfocusAll() {
    FocusScope.of(context).unfocus();
    _editorFocusNode.unfocus();
    _titleTextFocusNode.unfocus();
  }

  Future<void> _getHistoryList() async {
    _historyList = await APIService.getNoteAnalysisHistoryAll(_note!.id!);

    print('_historyList ${_historyList}');
    setState(() {});
  }

  Widget _aiHistoryListView() {
    return Positioned(
      bottom: 20,
      child: IgnorePointer(
        ignoring: _selectedTabIndex != 1 || _historyList.isEmpty,
        child: AnimatedOpacity(
          duration: const Duration(milliseconds: 300),
          opacity: _selectedTabIndex == 1 && _historyList.isNotEmpty ? 1 : 0,
          child: Container(
            height: 160,
            padding: const EdgeInsets.symmetric(vertical: 16.0),
            width: MediaQuery.of(context).size.width,
            child: ListView.separated(
              padding:
                  const EdgeInsets.symmetric(horizontal: 16.0, vertical: 20),
              shrinkWrap: true,
              scrollDirection: Axis.horizontal,
              itemBuilder: (context, index) {
                final history = _historyList[index];
                return Column(
                  children: [
                    const Spacer(),
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () async {
                        // var res = APIService.getNoteAnalysisHistoryById(
                        //   analysisType: history.analysisType!,
                        //   noteAnalysisId: history.noteAnalysisId!,
                        //   version: history.version!,
                        // );
                        //
                        // var rawString = '';
                        // await for (var line in res) {
                        //   rawString += line.replaceAll("data:", "");
                        // }
                        //
                        // var _aiHistoryModel = AIHistoryModel.fromRawString(rawString);

                        setState(() {
                          _selectedHistoryIndex = index;
                          _selectedHistory = history;

                          // todo history
                          // _selectedBottomTabIndex = history.analysisType ?? _selectedBottomTabIndex;
                          // _contentAssistResult = history.assistedContent ?? _contentAssistResult;
                          // print('_contentAssistResult ${_contentAssistResult.rewrite_content}');
                        });
                      },
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        height: _selectedHistoryIndex == index ? 77 : 65,
                        width: 160,
                        padding: const EdgeInsets.symmetric(horizontal: 16.0),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: const Color(0xFF000000).withOpacity(0.15),
                              offset: const Offset(0, 4),
                              blurRadius: 12,
                            ),
                          ],
                        ),
                        child: Column(
                          children: [
                            const Spacer(),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Padding(
                                  padding: const EdgeInsets.only(right: 10.0),
                                  child: Container(
                                    width: 40,
                                    height: 40,
                                    decoration: BoxDecoration(
                                      color: _selectedHistoryIndex == index
                                          ? const Color(0xFFEAE2FF)
                                          : const Color(0xFF12102F)
                                              .withOpacity(0.05),
                                      borderRadius: BorderRadius.circular(20),
                                    ),
                                    child: Center(
                                      child: Image.asset(
                                        aiTypeImageAsset(
                                            history.analysisType ?? 1),
                                        width: 20,
                                        height: 20,
                                        color: _selectedHistoryIndex == index
                                            ? const Color(0xFF3A51FF)
                                            : const Color(0xFF12102F),
                                      ),
                                    ),
                                  ),
                                ),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        history.analysisTypeText ?? '',
                                        style: TextStyle(
                                          color: _selectedHistoryIndex == index
                                              ? const Color(0xFF3A51FF)
                                              : const Color(0xFF12102F),
                                          fontSize: 16,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                      Text(
                                        history.createdAt ?? '',
                                        style: TextStyle(
                                          color: _selectedHistoryIndex == index
                                              ? const Color(0xFF3A51FF)
                                                  .withOpacity(0.5)
                                              : const Color(0xFF12102F)
                                                  .withOpacity(0.5),
                                          fontSize: 11,
                                          fontWeight: FontWeight.w400,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const Spacer(),
                          ],
                        ),
                      ),
                    ),
                  ],
                );
              },
              separatorBuilder: (context, index) => const SizedBox(
                width: 12,
              ),
              itemCount: _historyList.length,
            ),
          ),
        ),
      ),
    );
  }

  _deleteNote() async {
    final deletedNote = await APIService().deleteNote(_note!.id.toString());
    if (deletedNote != null) {
      widget.onNoteUpdated?.call();
      Navigator.pop(context);
    }
  }

  Widget _deleteConfirmSheet() {
    return Container(
      height: 240,
      decoration: const BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding:
              const EdgeInsets.only(top: 32, left: 16, right: 16, bottom: 10),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.only(bottom: 32.0),
                child: Text(
                  '确定删除备忘吗？',
                  style: TextStyle(
                      color: Color(0xFF393640),
                      fontWeight: FontWeight.w700,
                      fontSize: 20,
                      height: 1),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(bottom: 10.0),
                child: GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: () {
                    _deleteNote();
                    Navigator.pop(context);
                  },
                  child: Container(
                    height: 54,
                    decoration: BoxDecoration(
                      color: Color(0xFFCE3A54),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Center(
                      child: Text(
                        '删除',
                        style: TextStyle(
                          color: Color(0xFFFDFCFF),
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  Navigator.pop(context);
                },
                child: SizedBox(
                  height: 40,
                  child: Center(
                    child: Text(
                      '取消',
                      style: TextStyle(
                        color: Color(0xFF12102F),
                        fontWeight: FontWeight.w700,
                        fontSize: 16,
                      ),
                    ),
                  ),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }

  _noteActionView() {
    return IgnorePointer(
      ignoring: !_isNoteActionViewShowing,
      child: AnimatedOpacity(
        duration: Duration(milliseconds: 200),
        opacity: _isNoteActionViewShowing ? 1 : 0,
        child: Stack(
          children: [
            GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () {
                setState(() {
                  _isNoteActionViewShowing = false;
                });
              },
              child: Container(
                color: Color(0xFF0E152F).withOpacity(0.4),
              ),
            ),
            AnimatedPositioned(
              duration: Duration(milliseconds: 200),
              bottom: !_isNoteActionViewShowing
                  ? 0
                  : MediaQuery.of(context).padding.bottom + 20,
              left: 16,
              right: 16,
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.all(Radius.circular(20)),
                ),
                child: Column(
                  children: [
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        shareH5(_note?.id?.toString() ?? '');
                      },
                      child: const Padding(
                        padding: EdgeInsets.symmetric(vertical: 16.0),
                        child: Row(
                          children: [
                            Padding(
                              padding: EdgeInsets.only(right: 12.0),
                              child: Icon(Icons.share_rounded,
                                  color: Color(0xFF180E25)),
                            ),
                            Text(
                              '分享',
                              style: TextStyle(
                                color: Color(0xFF180E25),
                                fontWeight: FontWeight.w500,
                                fontSize: 16,
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
                        setState(() {
                          _isNoteActionViewShowing = false;
                        });
                        if (_note?.dimension == 1) {
                          showCupertinoModalBottomSheet(
                            context: context,
                            builder: (context) {
                              return _deleteConfirmSheet();
                            },
                          );
                        } else {
                          _deleteNote();
                        }
                      },
                      child: Padding(
                        padding: EdgeInsets.symmetric(vertical: 16.0),
                        child: Row(
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(right: 12.0),
                              child: Image.asset(
                                AppImages.trashIcon.path,
                                width: 24,
                                height: 24,
                              ),
                            ),
                            Text(
                              '删除',
                              style: TextStyle(
                                color: Color(0xFFCE3A54),
                                fontWeight: FontWeight.w500,
                                fontSize: 16,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // todo 删除 note 里面的 talkSnapshot
  void _deleteDiscussContent() {
    setState(() {
      _selectedBottomTabIndex = 0;
      _isAIIn = false;
      _selectedTabIndex = 0;
    });
  }

  Widget getAiTabChild() {
    switch (_selectedBottomTabIndex) {
      case 3:
        return AiDiscussResult(
          onDelete: _deleteDiscussContent,
          onSaveSummaryToNote: _saveDiscussSummaryToNote,
          onSaveAll: _saveDiscussAll,
        );
      case 2:
        var originContent = _originContent;
        if (_selectedTitleText.isNotEmpty || _selectedContentText.isNotEmpty) {
          originContent = _selectedTitleText + _selectedContentText;
        }
        return AiAssistResult(
          result: _contentAssistResult,
          selectedItems: _aiAssistSelectedItems,
          originContent: originContent,
          onSaveContent: _saveAssistantContent,
          onSaveTodo: _saveTodoList,
        );
      case 1:
        return _generatedView();
      default:
        return Container();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: Stack(
        alignment: Alignment.topCenter,
        children: [
          _editorView(),
          _bottomBar(),
          Positioned(
            bottom: 62,
            left: 16,
            child: SafeArea(
              child: Image.asset(
                AppImages.aiTagIcon.path,
                width: 33,
                height: 20,
              ),
            ),
          ),
          _fontStyleView(),
          IgnorePointer(
            ignoring: !_isAIIn || _selectedTabIndex == 0,
            child: AnimatedOpacity(
              duration: const Duration(milliseconds: 300),
              opacity: _isAIIn && _selectedTabIndex == 1 ? 1 : 0,
              child: getAiTabChild(),
            ),
          ),
          IgnorePointer(
            ignoring: !_isAIIn || _selectedTabIndex == 0 || !_isGenaratingError,
            child: AnimatedOpacity(
              duration: const Duration(milliseconds: 300),
              opacity: _isAIIn && _selectedTabIndex == 1 && _isGenaratingError
                  ? 1
                  : 0,
              child: _generatedErrorView(),
            ),
          ),
          IgnorePointer(
            ignoring:
                !_isAIIn || _selectedTabIndex == 0 || !_isGeneratingFirstCheck,
            child: AnimatedOpacity(
              duration: const Duration(milliseconds: 300),
              opacity:
                  _isAIIn && _selectedTabIndex == 1 && _isGeneratingFirstCheck
                      ? 1
                      : 0,
              child: _generatingFirstCheckView(),
            ),
          ),
          IgnorePointer(
            ignoring: !_isAIIn || _selectedTabIndex == 0 || !_isGenerateFailed,
            child: AnimatedOpacity(
              duration: const Duration(milliseconds: 300),
              opacity: _isAIIn && _selectedTabIndex == 1 && _isGenerateFailed
                  ? 1
                  : 0,
              child: _generateFailedView(),
            ),
          ),
          IgnorePointer(
            ignoring: _isContentGenerating ||
                _isRelatedGenerating ||
                _isCategoryGenerating,
            child: _generatingToast(),
          ),
          IgnorePointer(
            ignoring: !_isUpdateSuccessToastShowing,
            child: _updateSuccessToast(),
          ),
          _noteTypeView(),
          _aiHistoryListView(),
          _noteActionView(),
        ],
      ),
    );
  }
}
