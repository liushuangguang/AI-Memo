import 'package:ainote_app/app/api/ai.dart';
import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/widgets/ai_card/question_answer.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../data/models/ai_related_link_model.dart';
import '../../data/models/ai_related_title_model.dart';
import '../dashed_line.dart';

typedef RelatedLinkLoader = Future<List<AiRelatedLinkModel>> Function(
    String text);
typedef RelatedTitleLoader = Future<List<AiRelatedTitleModel>> Function(
    String text);

class TodoSearchResults {
  final List<AiRelatedLinkModel> links;
  final List<AiRelatedTitleModel> titles;
  final bool linksFailed;
  final bool titlesFailed;

  const TodoSearchResults({
    required this.links,
    required this.titles,
    this.linksFailed = false,
    this.titlesFailed = false,
  });

  bool get completelyFailed => linksFailed && titlesFailed;
  bool get partiallyFailed => linksFailed != titlesFailed;
}

Future<TodoSearchResults> loadTodoSearchResults({
  required String text,
  RelatedLinkLoader? loadLinks,
  RelatedTitleLoader? loadTitles,
}) async {
  final query = text.trim();
  if (query.isEmpty) {
    return const TodoSearchResults(links: [], titles: []);
  }

  var links = <AiRelatedLinkModel>[];
  var titles = <AiRelatedTitleModel>[];
  var linksFailed = false;
  var titlesFailed = false;
  await Future.wait<void>([
    () async {
      try {
        links = await (loadLinks ??
            (value) => AiApi.aiRelatedLink(text: value))(query);
      } catch (_) {
        linksFailed = true;
      }
    }(),
    () async {
      try {
        titles = await (loadTitles ??
            (value) => AiApi.aiRelatedTitle(text: value))(query);
      } catch (_) {
        titlesFailed = true;
      }
    }(),
  ]);
  return TodoSearchResults(
    links: links,
    titles: titles,
    linksFailed: linksFailed,
    titlesFailed: titlesFailed,
  );
}

class TodoSearchDetail extends StatefulWidget {
  final String? text;

  const TodoSearchDetail({super.key, this.text});

  @override
  State<TodoSearchDetail> createState() => _TodoSearchDetailState();
}

class _TodoSearchDetailState extends State<TodoSearchDetail> {
  bool loading = true;
  List<AiRelatedTitleModel> aiRelatedTitleList = [];
  List<AiRelatedLinkModel> aiRelatedLinkList = [];
  String statusMessage = '';
  bool canRetry = false;
  bool _fetching = false;
  int _generation = 0;

  Future<void> fetchData() async {
    if (_fetching) return;
    _fetching = true;
    final generation = ++_generation;
    setState(() { loading = true; statusMessage = ''; canRetry = false; });
    try {
      final results = await loadTodoSearchResults(text: widget.text ?? '');
      if (!mounted || generation != _generation) return;
      setState(() {
        canRetry = results.linksFailed || results.titlesFailed;
        aiRelatedTitleList = results.titles;
        aiRelatedLinkList = results.links;
        if (results.completelyFailed) {
          statusMessage = '相关结果加载失败，请稍后重试';
        } else if (results.partiallyFailed) {
          statusMessage = results.links.isEmpty && results.titles.isEmpty
              ? '部分结果加载失败，请稍后重试'
              : '部分结果暂时无法加载';
        } else if (results.links.isEmpty && results.titles.isEmpty) {
          statusMessage = '暂无相关结果';
        } else {
          statusMessage = '';
        }
      });
    } finally {
      if (mounted && generation == _generation) {
        setState(() {
          loading = false;
          _fetching = false;
        });
      }
    }
  }

  @override
  void didUpdateWidget(covariant TodoSearchDetail oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.text != widget.text) {
      _fetching = false;
      aiRelatedLinkList = [];
      aiRelatedTitleList = [];
      fetchData();
    }
  }

  @override
  void initState() {
    super.initState();
    fetchData();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
        width: double.infinity,
        constraints: BoxConstraints(minHeight: 200.w, maxHeight: 300.w),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12.w),
        ),
        padding: EdgeInsets.all(16.w),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              QuestionAnswer(
                usePage: false,
                noBg: true,
                padding: EdgeInsets.all(0),
                itemBgColor: Color(0xFFFFF8ED),
                relatedLinkList: aiRelatedLinkList,
                relatedTitleList: aiRelatedTitleList,
                loading: loading,
              ),
              if (!loading && statusMessage.isNotEmpty)
                Padding(
                  padding: EdgeInsets.only(bottom: 12.h),
                  child: Text(
                    statusMessage,
                    style: TextStyle(color: MyColors.colorGrey),
                  ),
                ),
              if (!loading && canRetry) TextButton(
                onPressed: fetchData, child: const Text('重试相关搜索')),
              DashedLine(
                color: MyColors.cardBorderRed,
                padding: EdgeInsets.only(bottom: 20.h),
              ),
            ],
          ),
        ));
  }
}
