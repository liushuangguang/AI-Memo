import 'dart:convert';
import 'dart:developer';

import 'package:ainote/constants/app_images.dart';
import 'package:ainote/constants/constants.dart';
import 'package:ainote/models/note_model.dart';
import 'package:ainote/models/type_model.dart';
import 'package:ainote/screens/ai_generate_screen.dart';
import 'package:ainote/screens/mine_screen.dart';
import 'package:ainote/screens/search_screen.dart';
import 'package:ainote/screens/todo_screen.dart';
import 'package:ainote/services/api_service.dart';
import 'package:ainote/services/local_note_service.dart';
import 'package:ainote/utils/utils.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/quill_delta.dart';

import '../components/web_view_page.dart';
import '../constants/app_colors.dart';
import '../services/keychain_service.dart';
import 'ai_voice_discuss/ai_voice_discuss.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;
  bool _isListViewShowing = false;
  bool _isLoaded = false;
  List<NoteModel> _noteList = [];
  final TextEditingController _searchTextController = TextEditingController();
  final FocusNode _searchFocusNode = FocusNode();
  bool _isNoteActionViewShowing = false;
  List<TypeEntry> _noteTypes = [];
  final GlobalKey _noteTypeBtnkey = GlobalKey();
  Offset? _noteTypeBtnPosition;
  int _selectedNoteTypeIndex = 0;
  int _selectedNoteIndex = 0;
  String? _searchText;

  @override
  void initState() {
    super.initState();
    _getData();

    eventBus.on<String>().listen((event) {
      if (event == "get_note_list") {
        if (mounted) {
          _getNoteList();
        }
      } else if (event == "get_loca_note_list") {
        if (mounted) {
          _getLocalNoteList();
        }
      }
    });
  }

  _getData() async {
    await LocalNoteService.instance.init();
    _getLocalNoteList();
    final typeList = await APIService().getTypeDict("noteType");
    if (typeList != null) {
      setState(() {
        _noteTypes = typeList.first.entryList ?? [];
      });
    }
    _getNoteList();
  }

  _getNoteList() async {
    final noteList = await APIService().getNoteList(noteType: _selectedNoteTypeIndex == 0 ? null : _noteTypes[_selectedNoteTypeIndex - 1].id);
    for (int i = 0; i < (noteList ?? []).length; i++) {
      final note = noteList![i];
      if ((note.title ?? "").isEmpty && (note.noteAnalysisContent ?? "").isEmpty) {
        noteList.removeAt(i);
        i--;
      }
    }
    setState(() {
      _isLoaded = true;
      _noteList = noteList ?? [];
      _isListViewShowing = _noteList.isNotEmpty;
    });
    LocalNoteService.instance.saveAllNotes(_noteList);
  }

  _getLocalNoteList() {
    final notes = LocalNoteService.instance.getAllNotes();
    setState(() {
      _noteList = notes;
      _isLoaded = true;
      _isListViewShowing = _noteList.isNotEmpty;
    });
  }

  _searchNotes() async {
    final type = _selectedNoteTypeIndex == 0
        ? null
        : _noteTypes[_selectedNoteTypeIndex - 1].id;
    final noteList =
    await APIService().getNoteList(noteType: type, keyword: _searchText);
    setState(() {
      _noteList = noteList ?? [];
    });
  }

  Widget _homeTitleView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 4.0, left: 20),
          child: Text(
            'AI速记',
            style: TextStyle(
              color: Color(0xFF12102F),
              fontWeight: FontWeight.w500,
              fontSize: 32,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20.0),
          child: Row(
            children: [
              Text(
                '智能整理你的信息库',
                style: TextStyle(
                  color: Color(0xFF12102F),
                  fontWeight: FontWeight.w400,
                  fontSize: 16,
                ),
              ),
              const Spacer(),
              GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  Navigator.push(
                    context,
                    CupertinoPageRoute(
                        builder: (_) => WebViewPage(
                          url: introUrl,
                        )),
                  );
                },
                child: Padding(
                  padding: const EdgeInsets.only(right: 4.0),
                  child: Text(
                    '产品介绍',
                    style: TextStyle(
                      color: Color(0xFF3A51FF),
                      fontWeight: FontWeight.w500,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
              Container(
                padding:
                const EdgeInsets.symmetric(horizontal: 4.0, vertical: 1.0),
                decoration: BoxDecoration(
                  color: Color(0xFFFF4D00),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: Text(
                  '福利',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w500,
                    fontSize: 12,
                  ),
                ),
              )
            ],
          ),
        ),
        const SizedBox(
          height: 10,
        ),
      ],
    );
  }

  Widget _tabMenu(
      String selectedIcon, String unselectedIcon, String title, int index) {
    final selected = _selectedIndex == index;
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: () {
        setState(() {
          _selectedIndex = index;
        });
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4.0),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 8.0),
              child: Image.asset(
                selected ? selectedIcon : unselectedIcon,
                width: 32,
                height: 32,
              ),
            ),
            Text(
              title,
              style: TextStyle(
                color: selected ? Color(0xFF12102F) : Color(0xFF827D89),
                fontWeight: FontWeight.w700,
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _tabBar() {
    return Stack(
      alignment: Alignment.bottomCenter,
      children: [
        Container(
          height: 60 + 8 + 10 + MediaQuery.of(context).padding.bottom,
          decoration: BoxDecoration(
            color: Colors.white,
            border: Border(
              top: BorderSide(
                color: Color(0xFFF3F5F8),
                width: 1,
              ),
            ),
          ),
        ),
        SafeArea(
          child: Column(
            children: [
              const Spacer(),
              Padding(
                padding: const EdgeInsets.only(
                    top: 0.0, bottom: 0, left: 16, right: 16),
                child: Row(
                  children: [
                    _tabMenu(AppImages.tabHomeSelected.path,
                        AppImages.tabHomeUnselected.path, '备忘', 0),
                    const Spacer(),
                    _tabMenu(AppImages.tabTodoSelected.path,
                        AppImages.tabTodoUnselected.path, '待办', 1),
                    const Spacer(),
                    const SizedBox(
                      height: 80,
                      width: 80,
                    ),
                    const Spacer(),
                    _tabMenu(AppImages.tabSearchSelected.path,
                        AppImages.tabSearchUnselected.path, '广场', 2),
                    const Spacer(),
                    _tabMenu(AppImages.tabSettingsSelected.path,
                        AppImages.tabSettingsUnselected.path, '我的', 3),
                  ],
                ),
              ),
            ],
          ),
        ),
        SafeArea(
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            onTap: () {
              final noteType = _selectedNoteTypeIndex == 0 ? "0" : _noteTypes[_selectedNoteTypeIndex - 1].id;
              Navigator.push(
                context,
                // CupertinoPageRoute(builder: (_) => AiVoiceDiscuss()
                CupertinoPageRoute(builder: (_) =>
                    AIGenerateScreen(
                  onNoteUpdated: () {
                    _getNoteList();
                  },
                  onLocalNoteUpdated: () {
                    _getLocalNoteList();
                  },
                  noteTypes: _noteTypes,
                  toCreateNoteTypeId: noteType,
                )
              ),
              );
            },
            child: Padding(
              padding: const EdgeInsets.only(bottom: 36.0),
              child: Image.asset(
                AppImages.homeAddBtn.path,
                width: 80,
                height: 80,
              ),
            ),
          ),
        ),
      ],
    );
  }

  _enterNote(NoteModel note) {
    _searchFocusNode.unfocus();
    Navigator.push(
      context,
      CupertinoPageRoute(builder: (_) {
        return AIGenerateScreen(
          note: note,
          onNoteUpdated: () {
            _getNoteList();
          },
          onLocalNoteUpdated: () {
            _getLocalNoteList();
          },
          noteTypes: _noteTypes,
        );
      }),
    );
  }

  Widget _noteItem(BuildContext context, int index) {
    final note = _noteList[index];
    final title = note.title ?? "";
    String body = note.noteAnalysisContent ?? "";
    List<dynamic> decodedList;
    try {
      decodedList = jsonDecode(body);
    } catch (e) {
      decodedList = [];
    }

    if (decodedList.isNotEmpty) {
      final delta = Delta.fromJson(decodedList);
      body = Document.fromDelta(delta).toPlainText();
    }

    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: () {
        _enterNote(note);
      },
      child: Container(
        decoration: BoxDecoration(
          color: Color(0xFFFFFFFF),
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 8.0),
              child: Row(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(right: 12.0),
                    child: Image.asset(
                      AppImages.vertRect.path,
                      width: 4,
                      height: 12,
                    ),
                  ),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(right: 12.0),
                      child: Text(
                        title,
                        style: TextStyle(
                          color: Color(0xFF12102F),
                          fontWeight: FontWeight.w500,
                          fontSize: 16,
                        ),
                        maxLines: 1,
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(right: 16.0),
                    child: GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        setState(() {
                          _isNoteActionViewShowing = true;
                          _selectedNoteIndex = index;
                        });
                      },
                      child: Image.asset(
                        AppImages.homeDotsIcon.path,
                        width: 24,
                        height: 24,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Text(
                body.replaceAll("\n", ""),
                style: TextStyle(
                  color: Color(0xFF4C4A5C),
                  fontWeight: FontWeight.w400,
                  fontSize: 14,
                ),
                maxLines: 1,
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _listView() {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20.0),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 12.0),
              child: Container(
                height: 48,
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                decoration: BoxDecoration(
                  color: Color(0xFFFFFFFF),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 9.0),
                      child: Image.asset(
                        AppImages.homeSearchIcon.path,
                        width: 24,
                        height: 24,
                      ),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: TextField(
                          controller: _searchTextController,
                          focusNode: _searchFocusNode,
                          autofocus: false,
                          onSubmitted: (value) {
                            setState(() {
                              _searchText = value;
                            });
                            _searchNotes();
                          },
                          decoration: InputDecoration(
                            hintText: 'AI搜索',
                            hintStyle: TextStyle(
                              color: Color(0xFF827D89),
                              fontWeight: FontWeight.w400,
                              fontSize: 16,
                            ),
                            border: InputBorder.none,
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(right: 12.0),
                      child: Container(
                        width: 1,
                        height: 16,
                        color: Color(0xFF82818D),
                      ),
                    ),
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () {
                        final RenderBox renderBox =
                        _noteTypeBtnkey.currentContext!.findRenderObject()
                        as RenderBox;
                        final position = renderBox.localToGlobal(Offset.zero);
                        setState(() {
                          _noteTypeBtnPosition = position;
                        });
                      },
                      child: Row(
                        key: _noteTypeBtnkey,
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(right: 8.0),
                            child: Text(
                              _noteTypes.length == 0
                                  ? '全部分类'
                                  : _selectedNoteTypeIndex == 0
                                  ? "全部分类"
                                  : _noteTypes[_selectedNoteTypeIndex - 1]
                                  .name,
                              style: TextStyle(
                                color: Color(0xFF82818D),
                                fontWeight: FontWeight.w400,
                                fontSize: 16,
                              ),
                            ),
                          ),
                          Image.asset(
                            AppImages.homeDownIcon.path,
                            width: 16,
                            height: 16,
                          ),
                        ],
                      ),
                    )
                  ],
                ),
              ),
            ),
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.only(bottom: 100),
                shrinkWrap: true,
                separatorBuilder: (context, index) => const SizedBox(
                  height: 12,
                ),
                itemCount: _noteList.length,
                itemBuilder: (context, index) {
                  return _noteItem(context, index);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _emptyView() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 20.0, left: 38, right: 38),
          child: Image.asset(
            AppImages.homeEmptyPic.path,
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(bottom: 16.0),
          child: Text(
            '开启智能备忘/笔记',
            style: TextStyle(
              color: Color(0xFF180E25),
              fontWeight: FontWeight.w700,
              fontSize: 24,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(left: 70.0, right: 70, bottom: 36),
          child: Text(
            '不积跬步，无以至千里；不积小流，无以成江海。开始您的旅程吧！',
            textAlign: TextAlign.center,
            style: TextStyle(
              color: Color(0xFF827D89),
              fontWeight: FontWeight.w400,
              fontSize: 14,
            ),
          ),
        ),
        Image.asset(
          AppImages.homeArrow.path,
          width: 150,
          height: 66,
        )
      ],
    );
  }

  _noteScreen() {
    return Stack(
      children: [
        Image.asset(
          AppImages.homeBack.path,
          fit: BoxFit.cover,
          height: MediaQuery.of(context).size.height,
          width: MediaQuery.of(context).size.width,
        ),
        SafeArea(
          child: Column(
            children: [
              Visibility(
                visible: !_searchFocusNode.hasFocus,
                child: _homeTitleView(),
              ),
              Visibility(
                visible: _isListViewShowing && _isLoaded,
                child: _listView(),
              ),
              Visibility(
                visible: !_isListViewShowing && _isLoaded,
                child: Expanded(child: _emptyView()),
              ),
            ],
          ),
        ),
      ],
    );
  }

  _deleteNote() async {
    final deletedNote = await APIService().deleteNote(_noteList[_selectedNoteIndex].id.toString());
    if (deletedNote != null) {
      _getNoteList();
    }
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
                        shareH5(_noteList[_selectedNoteIndex].id.toString());
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
                        if (_noteList[_selectedNoteIndex].dimension == 1) {
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

  _noteTypeView() {
    return IgnorePointer(
      ignoring: _noteTypeBtnPosition == null,
      child: AnimatedOpacity(
        duration: Duration(milliseconds: 200),
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
                color: Color(0xFF0E152F).withOpacity(0.4),
              ),
            ),
            Positioned(
              right: _noteTypeBtnPosition != null ? 20 : -300,
              top: (_noteTypeBtnPosition?.dy ?? 0) + 40,
              child: AnimatedOpacity(
                duration: Duration(milliseconds: 200),
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
                        horizontal: 20.0, vertical: 16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.all(Radius.circular(8)),
                    ),
                    child: ListView.separated(
                      shrinkWrap: true,
                      padding: const EdgeInsets.all(0),
                      itemBuilder: (context, index) {
                        if (index == 0) {
                          return GestureDetector(
                            behavior: HitTestBehavior.translucent,
                            onTap: () {
                              setState(() {
                                _selectedNoteTypeIndex = 0;
                                _noteTypeBtnPosition = null;
                              });
                              _searchNotes();
                            },
                            child: Row(
                              children: [
                                Padding(
                                  padding: const EdgeInsets.only(right: 8.0),
                                  child: Container(
                                    width: 4,
                                    height: 12,
                                    decoration: BoxDecoration(
                                      color: Color(0xFF315FFF),
                                      borderRadius: BorderRadius.circular(2),
                                    ),
                                  ),
                                ),
                                Text(
                                  '全部分类',
                                  style: TextStyle(
                                    color: Color(0xFF12102F),
                                    fontWeight: FontWeight.w400,
                                    fontSize: 16,
                                  ),
                                ),
                                const Spacer(),
                                AnimatedOpacity(
                                  duration: Duration(milliseconds: 200),
                                  opacity: _selectedNoteTypeIndex == 0 ? 1 : 0,
                                  child: Image.asset(
                                    AppImages.checkIcon.path,
                                    width: 16,
                                    height: 16,
                                  ),
                                ),
                              ],
                            ),
                          );
                        }
                        final entry = _noteTypes[index - 1];
                        return GestureDetector(
                          behavior: HitTestBehavior.translucent,
                          onTap: () {
                            setState(() {
                              _selectedNoteTypeIndex = index;
                              _noteTypeBtnPosition = null;
                            });
                            _searchNotes();
                          },
                          child: Row(
                            children: [
                              Padding(
                                padding: const EdgeInsets.only(right: 8.0),
                                child: Container(
                                  width: 4,
                                  height: 12,
                                  decoration: BoxDecoration(
                                    color: Color(0xFF315FFF),
                                    borderRadius: BorderRadius.circular(2),
                                  ),
                                ),
                              ),
                              Text(
                                entry.name,
                                style: TextStyle(
                                  color: Color(0xFF12102F),
                                  fontWeight: FontWeight.w400,
                                  fontSize: 16,
                                ),
                              ),
                              const Spacer(),
                              AnimatedOpacity(
                                duration: Duration(milliseconds: 200),
                                opacity:
                                _selectedNoteTypeIndex == index ? 1 : 0,
                                child: Image.asset(
                                  AppImages.checkIcon.path,
                                  width: 16,
                                  height: 16,
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                      separatorBuilder: (context, index) => const SizedBox(
                        height: 32,
                      ),
                      itemCount: _noteTypes.length + 1,
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
          padding: const EdgeInsets.only(top: 32, left: 16, right: 16, bottom: 10),
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
                    height: 1
                  ),
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: GestureDetector(
        behavior: HitTestBehavior.translucent,
        onTap: () {
          FocusScope.of(context).unfocus();
        },
        child: Stack(
          alignment: Alignment.bottomCenter,
          children: [
            Visibility(
              visible: _selectedIndex == 0,
              child: _noteScreen(),
            ),
            Visibility(
              visible: _selectedIndex == 1,
              child: TodoScreen(),
            ),
            Visibility(visible: _selectedIndex == 2, child: SearchScreen()),
            Visibility(visible: _selectedIndex == 3, child: MineScreen()),
            _tabBar(),
            _noteActionView(),
            _noteTypeView(),
          ],
        ),
      ),
    );
  }
}
