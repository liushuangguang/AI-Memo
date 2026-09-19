import 'dart:ui';

import 'package:ainote_app/app/api/note_theme.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/theme_related_card_widget.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:uuid/uuid.dart';

class ThemeRelatedList extends StatefulWidget {
  const ThemeRelatedList({super.key, required this.theme});

  final NoteThemeModel theme;

  @override
  State<ThemeRelatedList> createState() => _ThemeRelatedListState();
}

class _ThemeRelatedListState extends State<ThemeRelatedList> {
  final Set<String> _selectedNoteIds = <String>{};
  List<NoteThemeCandidateModel> _candidates = const [];
  bool _loading = true;
  bool _merging = false;
  String? _loadError;
  String? _mergeError;
  String? _idempotencyKey;

  @override
  void initState() {
    super.initState();
    _loadCandidates();
  }

  Future<void> _loadCandidates() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    try {
      final candidates = await NoteThemeApi.candidates(widget.theme.id);
      if (!mounted) return;
      setState(() {
        _candidates = candidates;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadError = '相关备忘录加载失败';
      });
    }
  }

  void _toggleNoteSelection(String id) {
    if (_merging) return;
    if (!_selectedNoteIds.contains(id) && _selectedNoteIds.length >= 20) {
      Toast.info('每次最多选择20条备忘录');
      return;
    }
    setState(() {
      if (!_selectedNoteIds.add(id)) _selectedNoteIds.remove(id);
      _idempotencyKey = null;
      _mergeError = null;
    });
  }

  Future<void> _startMerge() async {
    if (_selectedNoteIds.length < 2 || _merging) {
      Toast.info('请至少选择两条备忘录');
      return;
    }
    final selected = _candidates
        .where((item) => _selectedNoteIds.contains(item.note.id))
        .toList(growable: false);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('确认开始整合？'),
        content: Text(
          '将用 ${selected.length} 条备忘录生成一条新备忘录。'
          '原备忘录不会被修改或删除。\n\n'
          '${selected.map((item) => '• ${item.note.title?.trim().isNotEmpty == true ? item.note.title!.trim() : '未命名备忘录'}').join('\n')}',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('确认整合'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() {
      _merging = true;
      _mergeError = null;
      _idempotencyKey ??= 'theme-merge-${const Uuid().v4()}';
    });
    try {
      final result = await NoteThemeApi.merge(
        themeId: widget.theme.id,
        sourceNoteIds: _selectedNoteIds.toList(growable: false),
        idempotencyKey: _idempotencyKey!,
      );
      if (!mounted) return;
      Navigator.of(context).pop();
      await Get.toNamed(
        Routes.NOTE_EDIT,
        arguments: result.note,
        parameters: {'mode': NoteEditMode.preview.name},
      );
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _merging = false;
        _mergeError = '整合失败，本次选择已保留，可直接重试';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Container(color: Colors.white.withOpacity(0.8)),
        BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
          child: Container(color: Colors.white.withOpacity(0.1)),
        ),
        Padding(
          padding: EdgeInsets.only(top: MediaQuery.of(context).padding.top + 10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(bottom: 24, left: 16, right: 16),
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Center(
                      child: Text(
                        widget.theme.theme,
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    Positioned(
                      left: 0,
                      child: IconButton(
                        padding: EdgeInsets.zero,
                        icon: Image.asset(Assets.imagesCloseBtn),
                        onPressed:
                            _merging ? null : () => Navigator.of(context).pop(),
                      ),
                    ),
                  ],
                ),
              ),
              if (widget.theme.description.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: Text(
                    widget.theme.description,
                    style: const TextStyle(color: MyColors.thirdColor),
                  ),
                ),
              const Padding(
                padding: EdgeInsets.only(bottom: 12, left: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '相关备忘录',
                      style:
                          TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
                    ),
                    SizedBox(height: 4),
                    Text(
                      '请选择2至20条，AI将生成一条新备忘录',
                      style: TextStyle(fontSize: 12, color: MyColors.thirdColor),
                    ),
                  ],
                ),
              ),
              Expanded(child: _buildCandidateBody()),
              Container(
                padding: EdgeInsets.only(
                  top: 12,
                  left: 16,
                  right: 16,
                  bottom: MediaQuery.of(context).padding.bottom + 16,
                ),
                color: Colors.white,
                child: Column(
                  children: [
                    if (_mergeError != null)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Text(
                          _mergeError!,
                          style: const TextStyle(color: Colors.redAccent),
                        ),
                      ),
                    InkWell(
                      onTap: _selectedNoteIds.length >= 2 && !_merging
                          ? _startMerge
                          : null,
                      child: AnimatedOpacity(
                        opacity: _selectedNoteIds.length >= 2 ? 1 : 0.3,
                        duration: const Duration(milliseconds: 200),
                        child: Container(
                          width: double.infinity,
                          height: 72,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(8),
                            color: MyColors.colorBlue,
                          ),
                          child: Center(
                            child: _merging
                                ? const CircularProgressIndicator(
                                    color: Colors.white,
                                  )
                                : Text(
                                    _mergeError == null
                                        ? '开始整合（已选 ${_selectedNoteIds.length} 条）'
                                        : '重试整合',
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
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
        ),
      ],
    );
  }

  Widget _buildCandidateBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_loadError != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_loadError!),
            TextButton(onPressed: _loadCandidates, child: const Text('重试')),
          ],
        ),
      );
    }
    if (_candidates.isEmpty) {
      return const Center(child: Text('暂未找到与该主题相关的备忘录'));
    }
    return ListView.separated(
      itemCount: _candidates.length,
      padding: const EdgeInsets.only(bottom: 16),
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final candidate = _candidates[index];
        final id = candidate.note.id!;
        return GestureDetector(
          onTap: () => _toggleNoteSelection(id),
          child: ThemeRelatedCardWidget(
            candidate: candidate,
            isSelected: _selectedNoteIds.contains(id),
          ),
        );
      },
    );
  }
}
