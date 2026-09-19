import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/data/models/note_module/question_answer_module.dart';
import 'package:ainote_app/app/widgets/note_card/card_chip.dart';
import 'package:ainote_app/app/widgets/note_card/note_card_container.dart';
import 'package:ainote_app/app/widgets/web_view_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:webview_flutter/webview_flutter.dart';

class NoteCardQa extends StatelessWidget {
  final QuestionAnswerModule data;
  final VoidCallback? onDelete;
  const NoteCardQa({super.key, required this.data, this.onDelete});

  @override
  Widget build(BuildContext context) {
    var items = data.questionAnswerItems ?? [];
    if (items.isEmpty) return const SizedBox.shrink();
    bool isLink = false;
    if (items.length == 2 && Uri.tryParse(items[1].answer ?? '')?.scheme == 'https') {
      isLink = true;
    }

    Widget getDetail() {
      if (items.isEmpty) return const Text('暂无内容');
      if (isLink) {
        return WebViewPage(noHeader: true, url: items[1].answer ?? '');
      }
      return SingleChildScrollView(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(items[0].question ?? '',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14.sp,
              )),
          10.verticalSpace,
          MarkdownBody(
            styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)),
            data: items[0].answer ?? '',
            selectable: true,
            onTapLink: (_, href, __) {
              final uri = Uri.tryParse(href ?? '');
              if (uri?.scheme != 'https' || uri?.host.isNotEmpty != true) return;
              Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => WebViewPage(url: href!)));
            },
          ),
          10.verticalSpace,
        ]),
      );
    }

    return NoteCardContainer(
        onDelete: onDelete,
        expanded: true,
        title: CardChip(
            text: 'AI问答',
            color: MyColors.chipColorRed,
            backgroundColor: MyColors.chipBgRed),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(items[0].question ?? '',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 12.sp,
                      color: MyColors.colorWhite.withOpacity(0.8))),
              10.verticalSpace,
              MarkdownBody(
                styleSheet: MarkdownStyleSheet.fromTheme(
                  ThemeData(
                      textTheme: Theme.of(context).textTheme.apply(
                          fontSizeFactor: 0.8,
                          bodyColor: MyColors.colorWhite.withOpacity(0.8))),
                ),
                data: items[0].answer ?? '',
                selectable: false,
              ),
            ],
          ),
        ),
        detail: getDetail());
  }
}
