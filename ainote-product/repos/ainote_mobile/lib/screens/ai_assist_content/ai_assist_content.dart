import 'package:ainote/constants/app_images.dart';
import 'package:ainote/constants/constants.dart';
import 'package:ainote/models/content_assist_model.dart';
import 'package:ainote/models/note_model.dart';
import 'package:ainote/services/api_service.dart';
import 'package:ainote/ui/primary_btn.dart';
import 'package:ainote/ui/secondary_btn.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../widgets/ai_action_text.dart';

String talkToAiText = '';

class AiAssistContent extends StatefulWidget {
  final String? selectedTitleText; // 选择的标题文本
  final String? selectedContentText; // 选择的内容文本
  final NoteModel? noteModel;
  final Function(List selectedItems)? onConfirm;

  const AiAssistContent({
    super.key,
    this.selectedTitleText,
    this.selectedContentText,
    this.noteModel,
    this.onConfirm,
  });

  @override
  State<AiAssistContent> createState() => _AiAssistContentState();
}

class _AiAssistContentState extends State<AiAssistContent> {
  ContentAssistDirectionModel? _selectModel;
  bool _showTips = false;

  bool _showLoading = true;
  bool _showError = true;

  String _errMsg = '对不起，您记录的信息我无法理解，故不做任何改写';
  bool _hasSelectedText = false;
  String _reason = '';
  String _recommend = '推荐';
  List<dynamic> _selectedItems = [];

  @override
  void initState() {
    super.initState();

    print('selectedTitleText: ${widget.selectedTitleText}');
    print('selectedContentText: ${widget.selectedContentText}');
    setState(() {
      _hasSelectedText = (widget.selectedTitleText ?? '').isNotEmpty ||
          (widget.selectedContentText ?? '').isNotEmpty;
    });
    _getAiSelectModel();
  }

  @override
  void dispose() {
    talkToAiText = '';
    super.dispose();
  }

  bool? _handleSelect(dynamic title) {
    if (_selectedItems.contains(title)) {
      _selectedItems.remove(title);
    } else {
      if (_selectedItems.length >= 3) {
        EasyLoading.showToast('最多选中三个辅写方向');
        return false;
      }
      _selectedItems.add(title);
    }
    return null;
  }

  void _getAiSelectModel() async {
    _selectModel =
        await APIService.getAssistanceDirection(widget.noteModel?.id);
    setState(() {
      _showLoading = false;
      _showError = _selectModel?.code != 200;
      _errMsg = _selectModel?.message ?? _errMsg;

      if(!_showError) {
        initData();
      }
    });
  }

  void initData() {
    setState(() {
      try {
        _reason = _selectModel?.message.toString() ?? '';
        if (_reason.startsWith('（选中理由）')) {
          _reason = _reason.replaceFirst('（选中理由）', '');
        }
        _selectedItems = _selectModel?.data ?? [];
        if (_selectModel?.data != null) {
          _showTips = true;
          _recommend = "推荐${_selectModel?.data?.reduce((value, element) {
            var _value = value.contains('【') ? value : '【$value】';
            var _element = element.contains('【') ? element : '【$element】';
            return '$_value+$_element';
          })}${_selectModel?.data?.length}项";
        }
      } catch (e) {
        print('initData error: $e');
      }
    });
  }

  void startAiWrite() async {
    if (_selectedItems.isEmpty) {
      EasyLoading.showToast('请选择或输入您需要辅写的内容');
      return;
    }
    widget.onConfirm?.call(_selectedItems);
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    final showContent = !_showLoading && !_showError;
    return Padding(
      padding: const EdgeInsets.only(right: 24.0, left: 24.0, top: 36.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  mainAxisSize: MainAxisSize.max,
                  children: [
                    Text(
                      'AI内容辅助',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: primaryColor,
                        decoration: TextDecoration.none,
                      ),
                    ),
                    const Spacer(),
                    showContent
                        ? OutlineBtn(
                            text: '全文辅写',
                            enabled: !_hasSelectedText,
                            onPressed: () {
                              setState(() {
                                _hasSelectedText = false;
                              });
                            },
                          )
                        : const SizedBox.shrink(),
                    const SizedBox(
                      width: 8,
                    ),
                    showContent
                        ? OutlineBtn(
                            text: '所选内容辅写',
                            enabled: _hasSelectedText,
                            onPressed: () {
                              if(!_hasSelectedText) {
                                EasyLoading.showToast('请在原文中选中要辅写的内容');
                                return;
                              }
                              setState(() {
                                _hasSelectedText = true;
                              });
                            },
                          )
                        : const SizedBox.shrink(),
                    const Divider(),
                  ]),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: SvgPicture.asset(
                  AppImages.dashedLine.path,
                  height: 1,
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
          _showLoading
              ? AiActionText(
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'AI理解信息中',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: primaryColor,
                          decoration: TextDecoration.none,
                        ),
                      ),
                      const SizedBox(width: 4),
                      LoadingAnimationWidget.progressiveDots(
                        color: primaryColor,
                        size: 18,
                      )
                    ],
                  ),
                )
              : const SizedBox(height: 0),
          !_showLoading && _showError
              ? AiActionText(text: _errMsg)
              : const SizedBox(height: 0),
          showContent
              ? ConstrainedBox(
                  constraints: BoxConstraints(
                    // 获取屏幕高度
                    maxHeight: MediaQuery.of(context).size.height -
                        (kToolbarHeight + 108 + 180),
                  ),
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.start,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _showTips
                            ? ListLikeContent([
                                _reason,
                                _recommend,
                              ])
                            : const SizedBox.shrink(),
                        const SizedBox(
                          height: 16,
                        ),
                        AssistContentList(_selectedItems, _handleSelect),
                        TalkToAI(
                          handleConfirm: (text) {
                            _selectedItems.remove(talkToAiText);
                            talkToAiText = text;
                            return _handleSelect(talkToAiText) != false;
                          },
                          handleCancel: () {
                            _selectedItems.remove(talkToAiText);
                          },
                        ),
                      ],
                    ),
                  ),
                )
              : const SizedBox.shrink(),
          showContent
              ? Container(
                  height: 74,
                  width: double.infinity,
                  padding: const EdgeInsets.only(bottom: 12),
                  margin: const EdgeInsets.only(top: 24),
                  child: PrimaryBtn(text: '开始辅写', onPressed: startAiWrite),
                )
              : const SizedBox(height: 24),
          SizedBox(
            height: 54,
            width: double.infinity,
            child: SecondaryBtn(
                text: '关闭', onPressed: () => Navigator.pop(context)),
          ),
          const SizedBox(height: 36),
        ],
      ),
    );
  }
}

class OutlineBtn extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool? enabled;

  const OutlineBtn(
      {super.key, required this.text, this.onPressed, this.enabled});

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
            backgroundColor: const Color(0xFFF3F5F8),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
            side: enabled == true
                ? const BorderSide(
                    color: Color(0xFF6D3AFF),
                    width: 1.5,
                    style: BorderStyle.solid)
                : BorderSide.none,
            padding: const EdgeInsets.symmetric(vertical: 7.5, horizontal: 10)),
        child: Text(
          text,
          style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: primaryColor.withOpacity(enabled == true ? 1 : 0.5),
              decoration: TextDecoration.none),
        ));
  }
}

class AssistContentList extends StatelessWidget {
  final List<dynamic> _selectedItems;
  final void Function(dynamic title) handleSelect;

  const AssistContentList(this._selectedItems, this.handleSelect, {super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: contentAssistList.map((item) {
        return AssistContentItem(
            title: item['title'],
            selectedItems: _selectedItems,
            children: item['children'],
            handleSelect: handleSelect);
      }).toList(),
    );
  }
}

class AssistContentItem extends StatefulWidget {
  final String title;
  final List<dynamic> selectedItems;
  final List<Map<String, dynamic>> children;
  final void Function(dynamic title) handleSelect;

  const AssistContentItem(
      {super.key,
      required this.title,
      required this.selectedItems,
      required this.children,
      required this.handleSelect});

  @override
  State<AssistContentItem> createState() => _AssistContentItemState();
}

class _AssistContentItemState extends State<AssistContentItem> {
  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Column(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.title,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: primaryColor,
            decoration: TextDecoration.none,
          ),
        ),
        const SizedBox(
          height: 16,
        ),
        SizedBox(
          width: double.infinity,
          height: 110,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: widget.children.length,
            itemBuilder: (context, index) {
              var item = widget.children[index];
              var selected = widget.selectedItems.contains(item['title']);
              return GestureDetector(
                onTap: () {
                  widget.handleSelect(item['title']);
                  setState(() {});
                },
                child: Container(
                    width: 80,
                    height: 110,
                    // padding: const EdgeInsets.symmetric(vertical: 16),
                    decoration: BoxDecoration(
                        color: const Color(0xFFF3F5F8),
                        borderRadius: BorderRadius.circular(8),
                        border: selected
                            ? const Border.fromBorderSide(
                                BorderSide(color: Color(0xFF6D3AFF), width: 1))
                            : null),
                    child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          SizedBox(
                              width: 56,
                              height: 56,
                              child: Image.asset(item['icon'],
                                  fit: BoxFit.contain)),
                          const SizedBox(
                            height: 5,
                          ),
                          Text(item['title'],
                              style: TextStyle(
                                fontSize: item['title'].length > 4 ? 10 : 12,
                                fontWeight: FontWeight.bold,
                                color: const Color(0xFF12102F),
                                decoration: TextDecoration.none,
                              ))
                        ])),
              );
            },
            separatorBuilder: (BuildContext context, int index) {
              return const SizedBox(
                width: 8,
              );
            },
          ),
        ),
        // GridView(
        //   shrinkWrap: true,
        //   physics: const NeverScrollableScrollPhysics(),
        //   gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        //       crossAxisCount: widget.children.length,
        //       childAspectRatio: 80 / 110,
        //       mainAxisSpacing: 8,
        //       crossAxisSpacing: 8),
        //   children: widget.children.map((item) {
        //     var selected = widget.selectedItems.contains(item['title']);
        //     return GestureDetector(
        //       onTap: () {
        //         widget.handleSelect(item['title']);
        //         setState(() {});
        //       },
        //       child: Container(
        //           width: 80,
        //           height: 110,
        //           // padding: const EdgeInsets.symmetric(vertical: 16),
        //           decoration: BoxDecoration(
        //               color: const Color(0xFFF3F5F8),
        //               borderRadius: BorderRadius.circular(8),
        //               border: selected
        //                   ? const Border.fromBorderSide(
        //                       BorderSide(color: Color(0xFF6D3AFF), width: 1))
        //                   : null),
        //           child: Column(
        //               mainAxisAlignment: MainAxisAlignment.center,
        //               children: [
        //                 SizedBox(
        //                     width: 56,
        //                     height: 56,
        //                     child:
        //                         Image.asset(item['icon'], fit: BoxFit.contain)),
        //                 const SizedBox(
        //                   height: 5,
        //                 ),
        //                 Text(item['title'],
        //                     style: TextStyle(
        //                       fontSize: item['title'].length > 4 ? 10 : 12,
        //                       fontWeight: FontWeight.bold,
        //                       color: const Color(0xFF12102F),
        //                       decoration: TextDecoration.none,
        //                     ))
        //               ])),
        //     );
        //   }).toList(),
        // ),
        const SizedBox(
          height: 16,
        ),
      ],
    );
  }
}

class ListLikeContent extends StatelessWidget {
  final List<String> list;

  const ListLikeContent(this.list, {super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
        decoration: BoxDecoration(
            color: const Color(0xFFE3EAFF),
            borderRadius: BorderRadius.circular(4)),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          children: [
            ListItem(list[0]),
            ListItem(list[1]),
          ],
        ));
  }
}

class ListItem extends StatelessWidget {
  final String text;

  const ListItem(this.text, {super.key});

  @override
  Widget build(BuildContext context) {
    if (text.trim().isEmpty || text == 'null' || text == 'undefined') {
      return const SizedBox.shrink();
    }
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 4,
          height: 4,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            color: Color(0xFF6D3AFF),
          ),
          margin: const EdgeInsets.only(top: 8),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: Color(0xFF6D3AFF),
                decoration: TextDecoration.none),
          ),
        ),
      ],
    );
  }
}

class TalkToAI extends StatefulWidget {
  final bool Function(String text) handleConfirm;
  final void Function() handleCancel;
  final bool? isInBottomSheet;
  final String? text;

  const TalkToAI(
      {super.key,
      required this.handleConfirm,
      required this.handleCancel,
      this.isInBottomSheet = false,
      this.text});

  @override
  State<TalkToAI> createState() => _TalkToAIState();
}

class _TalkToAIState extends State<TalkToAI> {
  bool _checked = false;
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  @override
  void initState() {
    super.initState();

    _controller.text = widget.text ?? '';
    if (widget.isInBottomSheet != true) {
      _focusNode.addListener(() {
        if (_focusNode.hasFocus) {
          setState(() {
            _checked = false;
          });
          _focusNode.unfocus();
          _showTalkToAiBottomSheet();
        }
      });
    }
  }

  void _handleConfirm() {
    if (_controller.text.isEmpty) {
      EasyLoading.showToast('请输入内容');
      return;
    }
    _focusNode.unfocus();
    setState(() {
      _checked = widget.handleConfirm(_controller.text);
    });
  }

  void _handleClick() {
    if (_checked) {
      widget.handleCancel();
      setState(() {
        _checked = false;
        widget.handleCancel();
      });
      return;
    }
    if (_controller.text.isEmpty) {
      setState(() {
        _focusNode.requestFocus();
      });
    } else {
      setState(() {
        _checked = widget.handleConfirm(_controller.text);
      });
    }
  }

  void _showTalkToAiBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext childContext) {
        return Container(
          decoration: const BoxDecoration(
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(16),
              topRight: Radius.circular(16),
            ),
            color: Color(0xFFF3F5F8),
          ),
          padding: EdgeInsets.only(
              bottom: MediaQuery.of(context).viewInsets.bottom + 10),
          child: TalkToAI(
            text: _controller.text,
            handleConfirm: (String text) {
              _controller.text = text;
              _handleConfirm();
              Navigator.pop(context);
              return widget.handleConfirm(text);
            },
            handleCancel: widget.handleCancel,
            isInBottomSheet: true,
          ),
        );
      },
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _handleClick,
      child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            widget.isInBottomSheet == true
                ? const SizedBox.shrink()
                : const Text(
                    '自由发挥',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF12102F),
                      decoration: TextDecoration.none,
                    ),
                  ),
            const SizedBox(height: 16),
            Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
                decoration: BoxDecoration(
                    color: const Color(0xFFF3F5F8),
                    borderRadius: BorderRadius.circular(8),
                    border: widget.isInBottomSheet == true
                        ? null
                        : _checked
                            ? const Border.fromBorderSide(
                                BorderSide(color: Color(0xFF6D3AFF), width: 1))
                            : null),
                child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Image.asset(
                        AppImages.talkToAiIcon.path,
                        width: 56,
                      ),
                      const SizedBox(height: 10),
                      const Text(
                        "直接对话AI",
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF12102F),
                          decoration: TextDecoration.none,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Container(
                        constraints:
                            const BoxConstraints(minHeight: 44, maxHeight: 44),
                        child: TextField(
                          controller: _controller,
                          focusNode: _focusNode,
                          autofocus: widget.isInBottomSheet == true,
                          keyboardType: TextInputType.multiline,
                          maxLines: 1,
                          minLines: 1,
                          decoration: InputDecoration(
                            contentPadding:
                                const EdgeInsets.symmetric(horizontal: 16),
                            fillColor: Colors.white,
                            hintText: '简短的输入您的改写需求，例如：生成大纲',
                            suffix: GestureDetector(
                                onTap: _handleConfirm,
                                child: const Text('确定',
                                    style: TextStyle(
                                        color: Color(0xFF6D3AFF),
                                        fontSize: 12))),
                            filled: true,
                            hintStyle: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w400,
                              color: Color(0xFF82818D),
                              decoration: TextDecoration.none,
                            ),
                            border: const OutlineInputBorder(
                              borderRadius:
                                  BorderRadius.all(Radius.circular(8)),
                              borderSide: BorderSide(
                                  color: Color(0xFFE2E2E2), width: 1),
                            ),
                            enabledBorder: const OutlineInputBorder(
                              borderRadius:
                                  BorderRadius.all(Radius.circular(8)),
                              borderSide: BorderSide(
                                  color: Color(0xFFE2E2E2), width: 1),
                            ),
                            focusedBorder: const OutlineInputBorder(
                              borderRadius:
                                  BorderRadius.all(Radius.circular(8)),
                              borderSide: BorderSide(
                                  color: Color(0xFF6D3AFF), width: 1),
                            ),
                          ),
                        ),
                      ),
                    ]))
          ]),
    );
  }
}
