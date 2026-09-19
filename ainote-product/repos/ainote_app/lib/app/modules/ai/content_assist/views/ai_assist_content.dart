import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/constants.dart';
import 'package:ainote_app/app/data/models/ai_validate_model.dart';
import 'package:ainote_app/app/data/models/content_assist_model.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/content_assist/views/widgets/ai_action_text.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:ainote_app/app/widgets/secondary_btn.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';

import 'ai_assist_result.dart';

typedef ValidateContentAssist = Future<AiValidateModel> Function(
    String? noteId);
typedef LoadAssistDirections = Future<ContentAssistDirectionModel> Function(
    String? recordId);
typedef RewriteContent = Future<ContentAssistResultModel> Function({
  String? recordId,
  List<String>? assistDirections,
  String? selectedContent,
});

String normalizeAssistDirection(String direction) {
  var normalized = direction.trim();
  if (normalized.length >= 2 &&
      normalized.startsWith('【') &&
      normalized.endsWith('】')) {
    normalized = normalized.substring(1, normalized.length - 1).trim();
  }
  return normalized;
}

List<String> normalizeAssistDirections(Iterable<String> directions) {
  final result = <String>[];
  for (final direction in directions) {
    final normalized = normalizeAssistDirection(direction);
    if (normalized.isNotEmpty && !result.contains(normalized)) {
      result.add(normalized);
    }
  }
  return result;
}

List<String> selectionAfterRecommendations({
  required Iterable<String> currentSelection,
  required Iterable<String> recommendations,
  required bool preserveCurrentSelection,
  int maximumSelection = 3,
}) {
  final current = normalizeAssistDirections(currentSelection);
  if (preserveCurrentSelection || current.isNotEmpty) {
    return current.take(maximumSelection).toList();
  }
  return normalizeAssistDirections(recommendations)
      .take(maximumSelection)
      .toList();
}

String? rewriteSelectedContent({
  required bool useSelection,
  String? selectedTitleText,
  String? selectedContentText,
}) {
  if (!useSelection) return null;
  final content = selectedContentText?.trim() ?? '';
  if (content.isNotEmpty) return content;
  final title = selectedTitleText?.trim() ?? '';
  return title.isEmpty ? null : title;
}

class AiAssistContent extends StatefulWidget {
  final String? selectedTitleText; // 选择的标题文本
  final String? selectedContentText; // 选择的内容文本
  final NoteModel? noteModel;
  final bool? noSelectedContentText; // 不显示 所选内容辅写
  final Function(List selectedItems)? onConfirm;
  final String? initialDirection;
  final ValidateContentAssist? validateContentAssist;
  final LoadAssistDirections? loadAssistDirections;
  final RewriteContent? rewriteContent;
  final Widget Function(ContentAssistResultModel result,
      List<String> selectedItems, String originContent)? resultBuilder;

  const AiAssistContent({
    super.key,
    this.selectedTitleText,
    this.selectedContentText,
    this.noteModel,
    this.onConfirm,
    this.noSelectedContentText,
    this.initialDirection,
    this.validateContentAssist,
    this.loadAssistDirections,
    this.rewriteContent,
    this.resultBuilder,
  });

  @override
  State<AiAssistContent> createState() => _AiAssistContentState();
}

class _AiAssistContentState extends State<AiAssistContent> {
  NoteEditController? _noteEditController;

  late NoteModel? note;

  String recordId = '';

  ContentAssistDirectionModel? _selectModel;
  bool _showTips = false;

  bool _showLoading = true;
  bool _showError = true;

  String _errMsg = '对不起，您记录的信息我无法理解，故不做任何改写';
  bool _hasSelectedText = false;
  bool get _selectionAvailable =>
      (widget.selectedTitleText ?? '').trim().isNotEmpty ||
      (widget.selectedContentText ?? '').trim().isNotEmpty;
  String _reason = '';
  String _recommend = '推荐';
  List<String> _selectedItems = [];
  List<String> _recommendedDirections = [];
  List<String> _rewriteDirections = [];
  String _customDirection = '';
  bool _selectionWasExplicitlySet = false;

  ContentAssistResultModel rewriteModel =
      ContentAssistResultModel(rewrittenContent: '', todos: []);
  bool showRewrite = false;
  bool rewriteLoading = false;
  String rewriteErrorMessage = '';

  @override
  void initState() {
    super.initState();

    _hasSelectedText = _selectionAvailable;
    if (widget.initialDirection?.isNotEmpty == true) {
      _selectedItems = normalizeAssistDirections([widget.initialDirection!]);
      _selectionWasExplicitlySet = _selectedItems.isNotEmpty;
    }

    if (widget.noteModel == null) {
      _noteEditController = Get.find<NoteEditController>();
    }

    init();
  }

  void init() async {
    setState(() {
      _showLoading = true;
      _showError = false;
    });
    try {
      note = widget.noteModel ?? _noteEditController!.note.value;
      if (note?.id?.trim().isNotEmpty != true) {
        final saved = _noteEditController != null &&
            await _noteEditController!.saveNote(true);
        if (!mounted) return;
        note = widget.noteModel ?? _noteEditController!.note.value;
        if (!saved || note?.id?.trim().isNotEmpty != true) {
          setState(() {
            _showLoading = false;
            _showError = true;
            _errMsg = '备忘录尚未保存，请先填写内容并重试；未发起 AI 生成';
          });
          return;
        }
      }
      if (!mounted) return;
      await _validateContentAssist();
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _showLoading = false;
        _showError = true;
        _errMsg = 'AI内容辅助暂时不可用，请稍后重试';
      });
    }
  }

  @override
  void dispose() {
    super.dispose();
  }

  bool? _handleSelect(dynamic title) {
    final normalized = normalizeAssistDirection(title.toString());
    if (normalized.isEmpty) return false;
    if (_selectedItems.contains(normalized)) {
      setState(() {
        _selectionWasExplicitlySet = true;
        _selectedItems.remove(normalized);
      });
    } else {
      if (_selectedItems.length >= 3) {
        Toast.info('最多选中三个辅写方向');
        return false;
      }
      setState(() {
        _selectionWasExplicitlySet = true;
        _selectedItems.add(normalized);
      });
    }
    return null;
  }

  bool _handleCustomDirection(String text) {
    if (!mounted) return false;
    final normalized = normalizeAssistDirection(text);
    if (normalized.isEmpty) return false;

    final nextSelection = List<String>.from(_selectedItems);
    if (_customDirection.isNotEmpty) {
      nextSelection.remove(_customDirection);
    }
    if (!nextSelection.contains(normalized) && nextSelection.length >= 3) {
      Toast.info('最多选中三个辅写方向');
      return false;
    }
    if (!nextSelection.contains(normalized)) {
      nextSelection.add(normalized);
    }

    setState(() {
      _selectionWasExplicitlySet = true;
      _customDirection = normalized;
      _selectedItems = nextSelection;
    });
    return true;
  }

  void _cancelCustomDirection() {
    if (!mounted || _customDirection.isEmpty) return;
    setState(() {
      _selectionWasExplicitlySet = true;
      _selectedItems.remove(_customDirection);
      _customDirection = '';
    });
  }

  Future<void> _validateContentAssist() async {
    final validResult = await (widget.validateContentAssist ??
        AiApi.validateContentAssist)(note?.id);
    if (!mounted) return;
    if (validResult.meaningful == true) {
      recordId = validResult.recordId ?? '';
      await _getAiSelectModel();
    } else {
      if (!mounted) return;
      setState(() {
        _showLoading = false;
        _showError = true;
        _errMsg = validResult.result ?? _errMsg;
      });
    }
  }

  Future<void> _getAiSelectModel() async {
    try {
      _selectModel = await (widget.loadAssistDirections ??
          AiApi.getAssistantDirection)(recordId);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _showLoading = false;
        _showError = _selectedItems.isEmpty;
        if (_showError) {
          _errMsg = 'AI推荐加载失败，请稍后重试';
        } else {
          rewriteErrorMessage = 'AI推荐加载失败，仍可使用已选方向';
        }
      });
      return;
    }
    final directions = normalizeAssistDirections(
        _selectModel?.availableAssistantDirection ?? const <String>[]);
    if (!mounted) return;
    setState(() {
      _showLoading = false;
      _recommendedDirections = directions;
      _selectedItems = selectionAfterRecommendations(
        currentSelection: _selectedItems,
        recommendations: directions,
        preserveCurrentSelection: _selectionWasExplicitlySet,
      );
      _showError = directions.isEmpty && _selectedItems.isEmpty;
      if (_showError) {
        _errMsg = '暂时没有可用的辅写方向';
        return;
      }
      _reason = _selectModel?.reason.toString() ?? '';
      if (_reason.startsWith('（选中理由）')) {
        _reason = _reason.replaceFirst('（选中理由）', '');
      }
      _showTips = directions.isNotEmpty;
      final formatted = directions
          .map((item) => item.contains('【') ? item : '【$item】')
          .join('+');
      _recommend = '推荐$formatted${directions.length}项';
    });
  }

  void startAiWrite() async {
    if (_selectedItems.isEmpty) {
      Toast.info('请选择或输入您需要辅写的内容');
      return;
    }
    if (rewriteLoading) {
      Toast.info('正在辅写中...');
      return;
    }
    final selectedDirections = List<String>.unmodifiable(_selectedItems);
    widget.onConfirm?.call(selectedDirections);
    if (!mounted) return;

    setState(() {
      rewriteLoading = true;
      rewriteErrorMessage = '';
      _rewriteDirections = selectedDirections;
    });
    try {
      final selectedContent = rewriteSelectedContent(
        useSelection: _hasSelectedText,
        selectedTitleText: widget.selectedTitleText,
        selectedContentText: widget.selectedContentText,
      );
      rewriteModel = await (widget.rewriteContent ?? AiApi.startRewriteContent)(
          recordId: recordId,
          assistDirections: selectedDirections,
          selectedContent: selectedContent);
      if (!mounted) return;
      if (rewriteModel.rewrittenContent.trim().isEmpty) {
        setState(() {
          rewriteErrorMessage = '未生成可用的辅写内容，请调整要求后重试';
          showRewrite = false;
        });
        return;
      }
      setState(() {
        showRewrite = true;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        rewriteErrorMessage = '辅写失败，请稍后重试';
        showRewrite = false;
      });
    } finally {
      if (mounted) {
        setState(() {
          rewriteLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = MyColors.primaryColor;
    final showContent = !_showLoading && !_showError && !showRewrite;
    final originContent = rewriteSelectedContent(
          useSelection: _hasSelectedText,
          selectedTitleText: widget.selectedTitleText,
          selectedContentText: widget.selectedContentText,
        ) ??
        '';

    return SafeArea(
      top: false,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final availableHeight = constraints.hasBoundedHeight
              ? constraints.maxHeight
              : MediaQuery.sizeOf(context).height;
          return ConstrainedBox(
            constraints: BoxConstraints(maxHeight: availableHeight),
            child: Container(
              color: MyColors.colorWhite,
              padding: EdgeInsets.only(
                right: 24.0.w,
                left: 24.0.w,
                top: 32.0.w,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _AssistHeader(
                    showActions: showContent,
                    showSelectedContentAction:
                        widget.noSelectedContentText != true,
                    useSelection: _hasSelectedText,
                    selectionAvailable: _selectionAvailable,
                    onUseAll: () {
                      setState(() => _hasSelectedText = false);
                    },
                    onUseSelection: () {
                      if (!_selectionAvailable) {
                        Toast.info('请在原文中选中要辅写的内容');
                        return;
                      }
                      setState(() => _hasSelectedText = true);
                    },
                  ),
                  Flexible(
                    child: _buildScrollableBody(
                      primaryColor: primaryColor,
                      showContent: showContent,
                      originContent: originContent,
                    ),
                  ),
                  if (showContent && rewriteErrorMessage.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: AiActionText(text: rewriteErrorMessage),
                    ),
                  if (showContent)
                    Padding(
                      padding: const EdgeInsets.only(top: 12, bottom: 12),
                      child: SizedBox(
                        key: const Key('ai-assist-start'),
                        height: 50,
                        width: double.infinity,
                        child: PrimaryBtn(
                          text: '开始辅写',
                          onPressed: startAiWrite,
                          child: rewriteLoading
                              ? LoadingAnimationWidget.progressiveDots(
                                  color: Colors.white,
                                  size: 24,
                                )
                              : null,
                        ),
                      ),
                    ),
                  SizedBox(
                    key: const Key('ai-assist-close'),
                    height: 54,
                    width: double.infinity,
                    child: SecondaryBtn(
                      text: '关闭',
                      onPressed: () => Navigator.maybePop(context),
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildScrollableBody({
    required Color primaryColor,
    required bool showContent,
    required String originContent,
  }) {
    if (_showLoading) {
      return SingleChildScrollView(
        key: const Key('ai-assist-scroll-body'),
        child: AiActionText(
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
              ),
            ],
          ),
        ),
      );
    }
    if (_showError) {
      return SingleChildScrollView(
        key: const Key('ai-assist-scroll-body'),
        child: Column(children: [
          AiActionText(text: _errMsg),
          TextButton(onPressed: init, child: const Text('重试'))
        ]),
      );
    }
    if (showRewrite) {
      return widget.resultBuilder?.call(
            rewriteModel,
            _rewriteDirections,
            originContent,
          ) ??
          AiAssistResult(
            result: rewriteModel,
            selectedItems: _rewriteDirections,
            originContent: originContent,
          );
    }
    if (!showContent) return const SizedBox.shrink();

    return SingleChildScrollView(
      key: const Key('ai-assist-scroll-body'),
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_showTips) ListLikeContent([_reason, _recommend]),
          if (_recommendedDirections.isNotEmpty) ...[
            const SizedBox(height: 16),
            RecommendedDirectionList(
              directions: _recommendedDirections,
              selectedItems: _selectedItems,
              handleSelect: _handleSelect,
            ),
          ],
          const SizedBox(height: 16),
          AssistContentList(_selectedItems, _handleSelect),
          TalkToAI(
            handleConfirm: _handleCustomDirection,
            handleCancel: _cancelCustomDirection,
          ),
        ],
      ),
    );
  }
}

class _AssistHeader extends StatelessWidget {
  final bool showActions;
  final bool showSelectedContentAction;
  final bool useSelection;
  final bool selectionAvailable;
  final VoidCallback onUseAll;
  final VoidCallback onUseSelection;

  const _AssistHeader({
    required this.showActions,
    required this.showSelectedContentAction,
    required this.useSelection,
    required this.selectionAvailable,
    required this.onUseAll,
    required this.onUseSelection,
  });

  @override
  Widget build(BuildContext context) {
    final primaryColor = MyColors.primaryColor;
    final textScale = MediaQuery.textScalerOf(context).scale(1);
    return LayoutBuilder(
      builder: (context, constraints) {
        final stackActions = constraints.maxWidth < 330 || textScale > 1.25;
        final actions = <Widget>[
          OutlineBtn(
            text: '全文辅写',
            enabled: !useSelection,
            onPressed: onUseAll,
          ),
          if (showSelectedContentAction) ...[
            const SizedBox(width: 8),
            OutlineBtn(
              text: '所选内容辅写',
              enabled: useSelection && selectionAvailable,
              onPressed: onUseSelection,
            ),
          ],
        ];
        return Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (stackActions) ...[
              Text(
                'AI内容辅助',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: primaryColor,
                  decoration: TextDecoration.none,
                ),
              ),
              if (showActions) ...[
                const SizedBox(height: 12),
                Wrap(spacing: 8, runSpacing: 8, children: actions),
              ],
            ] else
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'AI内容辅助',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: primaryColor,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ),
                  if (showActions) ...actions,
                ],
              ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: SvgPicture.asset(Assets.imagesDashedLine, height: 1),
            ),
            const SizedBox(height: 16),
          ],
        );
      },
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
              color: primaryColor.withValues(alpha: enabled == true ? 1 : 0.5),
              decoration: TextDecoration.none),
        ));
  }
}

class RecommendedDirectionList extends StatelessWidget {
  final List<String> directions;
  final List<String> selectedItems;
  final void Function(dynamic title) handleSelect;

  const RecommendedDirectionList({
    super.key,
    required this.directions,
    required this.selectedItems,
    required this.handleSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'AI推荐方向',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Color(0xFF12102F),
            decoration: TextDecoration.none,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: directions.map((direction) {
            final selected = selectedItems.contains(direction);
            return Semantics(
              button: true,
              selected: selected,
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  key: ValueKey('ai-recommendation-$direction'),
                  borderRadius: BorderRadius.circular(8),
                  onTap: () => handleSelect(direction),
                  child: Container(
                    constraints: const BoxConstraints(minHeight: 44),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF3F5F8),
                      borderRadius: BorderRadius.circular(8),
                      border: selected
                          ? Border.all(
                              color: const Color(0xFF6D3AFF),
                              width: 1,
                            )
                          : null,
                    ),
                    child: Text(
                      direction,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF12102F),
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
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
        if (mounted && _focusNode.hasFocus) {
          setState(() {
            _checked = false;
          });
          _focusNode.unfocus();
          _showTalkToAiBottomSheet();
        }
      });
    }
  }

  bool _submitText() {
    final text = _controller.text.trim();
    if (text.isEmpty) {
      Toast.info('请输入内容');
      return false;
    }
    _focusNode.unfocus();
    final accepted = widget.handleConfirm(text);
    if (mounted) {
      setState(() => _checked = accepted);
    }
    return accepted;
  }

  void _handleConfirm() {
    _submitText();
  }

  void _handleClick() {
    if (_checked) {
      widget.handleCancel();
      if (mounted) {
        setState(() => _checked = false);
      }
      return;
    }
    if (_controller.text.isEmpty) {
      _focusNode.requestFocus();
    } else {
      _submitText();
    }
  }

  void _showTalkToAiBottomSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext childContext) {
        final viewInsets = MediaQuery.viewInsetsOf(childContext);
        return AnimatedPadding(
          duration: const Duration(milliseconds: 180),
          curve: Curves.easeOut,
          padding: EdgeInsets.only(bottom: viewInsets.bottom),
          child: SafeArea(
            top: false,
            child: SingleChildScrollView(
              child: Container(
                decoration: const BoxDecoration(
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(16),
                    topRight: Radius.circular(16),
                  ),
                  color: Color(0xFFF3F5F8),
                ),
                padding: const EdgeInsets.only(bottom: 10),
                child: TalkToAI(
                  text: _controller.text,
                  handleConfirm: (String text) {
                    final accepted = widget.handleConfirm(text.trim());
                    if (mounted) {
                      _controller.text = text.trim();
                      setState(() => _checked = accepted);
                    }
                    if (accepted && childContext.mounted) {
                      Navigator.of(childContext).pop();
                    }
                    return accepted;
                  },
                  handleCancel: widget.handleCancel,
                  isInBottomSheet: true,
                ),
              ),
            ),
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
                        Assets.imagesTalkToAi,
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
