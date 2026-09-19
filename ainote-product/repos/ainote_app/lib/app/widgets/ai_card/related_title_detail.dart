import 'package:ainote_app/app/utils/helper.dart';
import 'package:flutter/material.dart';
import 'package:skeletonizer/skeletonizer.dart';
import 'package:ainote_app/app/api/ai.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class RelatedTitleDetail extends StatefulWidget {
  final String text;
  final bool? noHeader;
  final Function(String? text)? onFinish;
  final Future<String> Function(String)? loadDetail;

  const RelatedTitleDetail(
      {super.key, required this.text, this.noHeader, this.onFinish, this.loadDetail});

  @override
  State<RelatedTitleDetail> createState() => _RelatedTitleDetailState();
}

class _RelatedTitleDetailState extends State<RelatedTitleDetail> {
  bool loading = true;
  String msg = generateMockString(500);
  String? failure;
  int _generation = 0;

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  @override
  void didUpdateWidget(covariant RelatedTitleDetail oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.text != widget.text) _fetch();
  }

  Future<void> _fetch() async {
    final generation = ++_generation;
    setState(() { loading = true; failure = null; });
    try {
      final value = await (widget.loadDetail ?? AiApi.aiRelatedInfo)(widget.text);
      if (!mounted || generation != _generation) return;
      if (value.trim().isEmpty) throw const FormatException('Empty detail');
      setState(() { msg = value; loading = false; });
      widget.onFinish?.call(value);
    } catch (_) {
      if (!mounted || generation != _generation) return;
      setState(() { failure = '详情加载失败，请重试'; loading = false; });
      widget.onFinish?.call(null);
    }
  }

  Widget _content() {
    if (failure != null) return Column(mainAxisSize: MainAxisSize.min, children: [
      Text(failure!), TextButton(onPressed: _fetch, child: const Text('重新加载详情'))]);
    return Skeletonizer(enabled: loading, enableSwitchAnimation: true,
        child: MarkdownBody(data: msg, selectable: true, softLineBreak: true));
  }

  @override
  Widget build(BuildContext context) {
    if (widget.noHeader == true) {
      return SingleChildScrollView(
        child: Padding(
          padding: EdgeInsets.all(16.0.w),
          child: _content(),
        ),
      );
    }
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
          title: Text(widget.text),
          centerTitle: true,
          leading: IconButton(
            icon: const Icon(
              Icons.arrow_back_ios_new,
              color: Colors.black,
            ),
            onPressed: () {
              Navigator.of(context).pop();
            },
          )),
      body: SafeArea(
        bottom: false,
        child: SingleChildScrollView(
          child: Padding(
            padding: EdgeInsets.all(16.0.w),
            child: _content(),
          ),
        ),
      ),
    );
  }
}
