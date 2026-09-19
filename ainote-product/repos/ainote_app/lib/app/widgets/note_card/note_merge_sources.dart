import 'package:flutter/material.dart';
import '../../api/note_theme.dart';
import '../../data/models/note_model.dart';
import '../../data/models/note_theme_model.dart';
import '../../utils/quill_editor.dart';
import 'note_source_image.dart';

class NoteMergeSources extends StatefulWidget {
  const NoteMergeSources(
      {super.key, required this.noteId, this.loadHistory, this.loadNote});
  final String noteId;
  final Future<List<NoteThemeMergeHistoryModel>> Function(String)? loadHistory;
  final Future<NoteModel> Function(String)? loadNote;
  @override
  State<NoteMergeSources> createState() => _NoteMergeSourcesState();
}

class _NoteMergeSourcesState extends State<NoteMergeSources> {
  late Future<List<NoteThemeMergeHistoryModel>> _history;
  bool _opening = false;
  int _generation = 0;
  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() {
    _history = (widget.loadHistory ?? NoteThemeApi.mergeHistory)(widget.noteId);
  }

  @override
  void didUpdateWidget(covariant NoteMergeSources oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.noteId != widget.noteId) {
      _generation++;
      _opening = false;
      _reload();
    }
  }

  Future<void> _open(NoteThemeSourceModel source) async {
    if (_opening) return;
    final generation = _generation;
    setState(() => _opening = true);
    try {
      final note =
          await (widget.loadNote ?? NoteThemeApi.getNote)(source.noteId);
      if (!mounted || generation != _generation) return;
      if (note.id != source.noteId || note.deleted == true)
        throw StateError('Source unavailable');
      final images = <String>{
        if (note.imageUrl?.isNotEmpty == true) note.imageUrl!,
        for (final module in (note.modules ?? []).whereType<Map>())
          if (module['imageUrl'] is String &&
              (module['imageUrl'] as String).isNotEmpty)
            module['imageUrl'] as String
      };
      await Navigator.of(context).push(MaterialPageRoute<void>(
          builder: (_) => Scaffold(
              appBar: AppBar(title: Text(note.title ?? '来源备忘录')),
              body: SafeArea(
                  child: ListView(padding: const EdgeInsets.all(16), children: [
                SelectableText(deltaStringToText(note.content ?? '')),
                const SizedBox(height: 16),
                for (final url in images)
                  NoteSourceImage(url: url, label: '来源图片'),
              ])))));
    } catch (_) {
      if (mounted && generation == _generation)
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('来源备忘录暂时无法打开，可能已被删除；请重试')));
    } finally {
      if (mounted && generation == _generation)
        setState(() => _opening = false);
    }
  }

  @override
  Widget build(BuildContext context) =>
      FutureBuilder<List<NoteThemeMergeHistoryModel>>(
          future: _history,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done)
              return const LinearProgressIndicator();
            if (snapshot.hasError)
              return TextButton(
                  onPressed: () => setState(_reload),
                  child: const Text('合并来源加载失败，点击重试'));
            final records = snapshot.data ?? [];
            if (records.isEmpty) return const SizedBox.shrink();
            return Card(
                child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('来源备忘录与合并记录',
                              style: TextStyle(fontWeight: FontWeight.bold)),
                          for (final record in records)
                            ExpansionTile(
                                initiallyExpanded: true,
                                title: Text('${record.sources.length} 条来源备忘录'),
                                subtitle: Text(record.mergedAt
                                        ?.toLocal()
                                        .toString()
                                        .split('.')
                                        .first ??
                                    '本次合并'),
                                children: [
                                  for (final source in record.sources)
                                    ListTile(
                                        title: Text(source.title),
                                        trailing:
                                            const Icon(Icons.chevron_right),
                                        onTap: _opening
                                            ? null
                                            : () => _open(source))
                                ]),
                        ])));
          });
}
