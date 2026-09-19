import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:dio/dio.dart';
import 'package:dio_smart_retry/dio_smart_retry.dart';
import '../../api/my_dio.dart';
import '../../api/note.dart';
import '../../data/models/note_model.dart';
import '../../data/models/note_module/question_answer_module.dart';
import '../web_view_page.dart';
import '../../utils/quill_editor.dart';

/// Optional, explicitly requested enrichments. Each result is bound to its
/// note/analysis snapshot; closing or switching notes invalidates late replies.
class NoteEnrichment extends StatefulWidget {
  const NoteEnrichment(
      {super.key,
      required this.note,
      required this.recordId,
      required this.onSaved,
      this.onReplaced,
      this.autoLoad = false});
  final NoteModel note;
  final String? recordId;
  final ValueChanged<NoteModel> onSaved;
  final ValueChanged<NoteModel>? onReplaced;
  final bool autoLoad;
  @override
  State<NoteEnrichment> createState() => _NoteEnrichmentState();
}

class _NoteEnrichmentState extends State<NoteEnrichment> {
  List<Map<String, dynamic>>? _products;
  Map<String, dynamic>? _reflection;
  final Set<String> _running = {};
  final Set<String> _saved = {};
  final Map<String, String> _errors = {};
  final Map<String, CancelToken> _requests = {};
  int _generation = 0;
  final Set<String> _autoStarted = {};

  @override
  void initState() {
    super.initState();
    _scheduleAutomatic();
  }

  void _scheduleAutomatic() {
    if (!widget.autoLoad || widget.recordId?.isNotEmpty != true) return;
    final generation = _generation;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || generation != _generation || !widget.autoLoad) return;
      for (final feature in ['reflection', 'productRecommendations']) {
        if (_autoStarted.add(feature)) _load(feature);
      }
    });
  }

  @override
  void didUpdateWidget(covariant NoteEnrichment oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.note.id != widget.note.id ||
        oldWidget.recordId != widget.recordId) {
      _cancelAll();
      _products = null;
      _reflection = null;
      _saved.clear();
      _errors.clear();
      _autoStarted.clear();
    }
    _scheduleAutomatic();
  }

  void _cancelAll() {
    _generation++;
    for (final token in _requests.values) {
      token.cancel();
    }
    _requests.clear();
    _running.clear();
  }

  @override
  void dispose() {
    _cancelAll();
    super.dispose();
  }

  Future<void> _load(String feature) async {
    if (_running.contains(feature) || widget.recordId?.isNotEmpty != true)
      return;
    final generation = _generation;
    final token = CancelToken();
    _requests[feature] = token;
    setState(() {
      _running.add(feature);
      _errors.remove(feature);
    });
    try {
      final response = await MyDio.dio.post('/v2/note/analysis/$feature',
          data: {'recordId': widget.recordId},
          cancelToken: token,
          options: Options(receiveTimeout: const Duration(seconds: 170))
            ..disableRetry = true);
      if (!mounted || generation != _generation || token.isCancelled) return;
      final data = response.data['data'];
      setState(() {
        if (feature == 'reflection') {
          if (data is! Map ||
              data['summary'] is! String ||
              data['sources'] is! List) throw const FormatException();
          for (final source in data['sources']) {
            if (source is! Map ||
                source['id'] is! String ||
                source['title'] is! String ||
                source['content'] is! String) throw const FormatException();
          }
          _reflection = Map<String, dynamic>.from(data);
        } else {
          if (data is! List) throw const FormatException();
          for (final product in data) {
            if (product is! Map) throw const FormatException();
            for (final field in [
              'productShortUrl',
              'productImageUrl',
              'minNormalPrice',
              'productName',
              'productDesc',
              'sourceType',
              'recommendationReason'
            ]) {
              if (product[field] != null && product[field] is! String)
                throw const FormatException();
            }
          }
          _products =
              data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
        }
      });
    } catch (_) {
      if (mounted && generation == _generation && !token.isCancelled) {
        setState(() => _errors[feature] = '暂时未能获取结果，请重试。');
      }
    } finally {
      if (mounted &&
          generation == _generation &&
          identical(_requests[feature], token)) {
        setState(() {
          _running.remove(feature);
          _requests.remove(feature);
        });
      }
    }
  }

  Future<void> _save(String key, String title, String body) async {
    if (_running.contains('save') || _saved.contains(key)) return;
    final generation = _generation;
    setState(() => _running.add('save'));
    try {
      final saved = await NoteApi.addNoteModule(
          widget.note,
          QuestionAnswerModule(title: title, questionAnswerItems: [
            QuestionAnswerItem(question: title, answer: body)
          ]));
      if (!mounted || generation != _generation) return;
      setState(() => _saved.add(key));
      widget.onSaved(saved);
    } catch (_) {
      if (mounted && generation == _generation) {
        setState(() => _errors['save'] = '保存失败，请重试；原备忘录未被替换。');
      }
    } finally {
      if (mounted && generation == _generation)
        setState(() => _running.remove('save'));
    }
  }

  Future<void> _replaceReflection(String body) async {
    if (_running.contains('save') || widget.onReplaced == null) return;
    final generation = _generation;
    final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
                title: const Text('用回顾替换正文？'),
                content: const Text(
                    '当前正文将替换为上方预览及来源引用。原截图、待办和其他已保存卡片会保留。也可以取消，选择追加保存回顾。'),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.pop(context, false),
                      child: const Text('取消')),
                  TextButton(
                      onPressed: () => Navigator.pop(context, true),
                      child: const Text('确认替换'))
                ]));
    if (!mounted ||
        generation != _generation ||
        confirmed != true ||
        _running.contains('save')) return;
    setState(() {
      _running.add('save');
      _errors.remove('save');
    });
    try {
      final delta = convertMarkdownToDelta(body);
      if (delta.isEmpty) throw const FormatException('Empty reflection');
      final saved = await NoteApi.replaceTextBody(
          widget.note, jsonEncode(delta.toJson()));
      if (!mounted || generation != _generation) return;
      setState(() => _saved.add('reflection-replaced'));
      widget.onReplaced?.call(saved);
    } catch (_) {
      if (mounted && generation == _generation)
        setState(() => _errors['save'] = '替换未完成，请重试；不会清空本地正文。');
    } finally {
      if (mounted && generation == _generation)
        setState(() => _running.remove('save'));
    }
  }

  Widget _section(
      String feature, String title, String description, Widget? result) {
    final busy = _running.contains(feature);
    return Card(
        child: Padding(
            padding: const EdgeInsets.all(16),
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(title,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 8),
              Text(description),
              if (busy) const LinearProgressIndicator(),
              if (_errors[feature] != null)
                Text(_errors[feature]!,
                    style:
                        TextStyle(color: Theme.of(context).colorScheme.error)),
              if (result != null) result,
              Wrap(spacing: 8, children: [
                TextButton(
                    onPressed: busy || widget.recordId?.isNotEmpty != true
                        ? null
                        : () => _load(feature),
                    child: Text(busy
                        ? '正在分析…'
                        : result == null
                            ? '开始分析'
                            : '重新获取')),
                if (busy)
                  TextButton(
                      onPressed: () {
                        _requests.remove(feature)?.cancel();
                        setState(() => _running.remove(feature));
                      },
                      child: const Text('取消')),
              ]),
            ])));
  }

  @override
  Widget build(BuildContext context) {
    final summary = _reflection?['summary'] as String?;
    final sources = (_reflection?['sources'] as List?) ?? [];
    final citedReflection =
        '$summary\n\n来源备忘录：\n${sources.asMap().entries.map((entry) => '[${entry.key + 1}] ${entry.value['title']}（ID: ${entry.value['id']}）').join('\n')}';
    final reflectionBody = summary == null
        ? null
        : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const SizedBox(height: 12),
            MarkdownBody(data: summary, selectable: true),
            for (var i = 0; i < sources.length; i++)
              ExpansionTile(
                  title: Text('[${i + 1}] ${sources[i]['title'] ?? '未命名备忘录'}'),
                  children: [
                    Padding(
                        padding: const EdgeInsets.all(12),
                        child: SelectableText(sources[i]['content'] ?? ''))
                  ]),
            if (sources.isNotEmpty)
              TextButton(
                  onPressed: _running.contains('save') ||
                          _saved.contains('reflection')
                      ? null
                      : () => _save('reflection', '回顾与启发',
                          '$summary\n\n来源备忘录：\n${sources.asMap().entries.map((entry) => '[${entry.key + 1}] ${entry.value['title']}（ID: ${entry.value['id']}）').join('\n')}'),
                  child:
                      Text(_saved.contains('reflection') ? '已保存' : '确认保存回顾')),
            if (sources.isNotEmpty && widget.onReplaced != null)
              TextButton(
                  onPressed: _running.contains('save') ||
                          _saved.contains('reflection-replaced')
                      ? null
                      : () => _replaceReflection(citedReflection),
                  child: Text(_saved.contains('reflection-replaced')
                      ? '已替换正文'
                      : '替换正文')),
          ]);
    return Column(children: [
      _section('reflection', '回顾与启发', '结合你自己的相关备忘录，提炼经验与待确认问题。生成结果由你确认后保存。',
          reflectionBody),
      _section(
          'productRecommendations',
          '商品推荐与选购参考',
          '按备忘录中的实际需求查找；价格与库存以来源页面为准。',
          _products == null
              ? null
              : _products!.isEmpty
                  ? const Text('未发现明确的采购需求或可用结果。')
                  : Column(
                      children: _products!.map((p) {
                      final url = p['productShortUrl'] as String? ?? '';
                      final uri = Uri.tryParse(url);
                      final safe = uri?.scheme == 'https' &&
                          uri?.host.isNotEmpty == true &&
                          uri?.userInfo.isEmpty == true;
                      final image = p['productImageUrl'] as String?;
                      final price = p['minNormalPrice'] as String?;
                      final name = p['productName'] as String? ?? '选购参考';
                      return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (image?.startsWith('https://') == true)
                                  Image.network(image!,
                                      height: 120,
                                      fit: BoxFit.contain,
                                      errorBuilder: (_, __, ___) =>
                                          const SizedBox.shrink()),
                                Text(name,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w600)),
                                Text(p['productDesc'] as String? ?? '',
                                    maxLines: 4,
                                    overflow: TextOverflow.ellipsis),
                                Text(p['sourceType'] == 'PRODUCT'
                                    ? '商品来源 · ${price?.isNotEmpty == true ? price : '价格未提供'}'
                                    : '选购参考 · 非实时报价'),
                                Text(
                                    p['recommendationReason'] as String? ?? ''),
                                Wrap(spacing: 8, children: [
                                  TextButton(
                                      onPressed: !safe
                                          ? null
                                          : () => Navigator.of(context).push(
                                              MaterialPageRoute(
                                                  builder: (_) => WebViewPage(
                                                      url: url, title: name))),
                                      child: const Text('查看来源详情')),
                                  TextButton(
                                      onPressed: !safe ||
                                              _running.contains('save') ||
                                              _saved.contains(url)
                                          ? null
                                          : () => _save(url, name,
                                              '${p['productDesc'] ?? ''}\n\n${p['recommendationReason'] ?? ''}\n\n[查看来源详情]($url)\n\n价格与库存请在来源页面核对。'),
                                      child: Text(_saved.contains(url)
                                          ? '已保存'
                                          : '保存这条参考')),
                                ]),
                              ]));
                    }).toList())),
      if (_errors['save'] != null) Text(_errors['save']!),
    ]);
  }
}
