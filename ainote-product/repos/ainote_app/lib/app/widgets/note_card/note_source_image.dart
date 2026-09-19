import 'package:flutter/material.dart';
import '../../utils/note_image.dart';

/// The source image is retained separately from editable AI-generated modules.
class NoteSourceImage extends StatefulWidget {
  const NoteSourceImage({super.key, required this.url, this.label = '来源截图'});
  final String url;
  final String label;

  @override
  State<NoteSourceImage> createState() => _NoteSourceImageState();
}

class _NoteSourceImageState extends State<NoteSourceImage> {
  int _retry = 0;

  Widget _image({bool preview = false, VoidCallback? retryPreview}) =>
      Image.network(
        noteImageUrl(widget.url),
        key: ValueKey('${widget.url}:$_retry:$preview'),
        headers: noteImageHeaders(widget.url),
        width: double.infinity,
        height: preview ? null : 180,
        fit: BoxFit.contain,
        loadingBuilder: (context, child, progress) => progress == null
            ? child
            : const SizedBox(
                height: 180, child: Center(child: CircularProgressIndicator())),
        errorBuilder: (context, error, stack) => SizedBox(
            height: 120,
            child: Center(
              child: TextButton.icon(
                onPressed: () async {
                  await NetworkImage(noteImageUrl(widget.url),
                          headers: noteImageHeaders(widget.url))
                      .evict();
                  if (mounted) {
                    if (retryPreview != null) {
                      retryPreview();
                    } else {
                      setState(() => _retry++);
                    }
                  }
                },
                icon: const Icon(Icons.refresh),
                label: const Text('图片加载失败，点击重试'),
              ),
            )),
      );

  void _open() {
    showDialog<void>(
        context: context,
        builder: (context) => Dialog.fullscreen(
              child: StatefulBuilder(
                  builder: (dialogContext, updatePreview) => Scaffold(
                        appBar: AppBar(title: Text(widget.label)),
                        body: InteractiveViewer(
                            minScale: 0.5,
                            maxScale: 8,
                            child: Center(
                                child: _image(
                                    preview: true,
                                    retryPreview: () {
                                      if (dialogContext.mounted)
                                        updatePreview(() => _retry++);
                                    }))),
                      )),
            ));
  }

  @override
  Widget build(BuildContext context) => Material(
        borderRadius: BorderRadius.circular(12),
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ListTile(
                  title: Text(widget.label),
                  subtitle: const Text('原始图片已保留，点击查看大图'),
                  trailing: IconButton(
                      onPressed: _open,
                      icon: const Icon(Icons.open_in_full),
                      tooltip: '查看${widget.label}')),
              InkWell(onTap: _open, child: _image()),
            ]),
      );
}
