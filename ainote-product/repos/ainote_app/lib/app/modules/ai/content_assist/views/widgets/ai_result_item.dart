import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:loading_indicator/loading_indicator.dart';

class ResultItem extends StatefulWidget {
  final String? title;
  final Color? titleColor;
  final Widget? header;

  // text or body 二选一
  final String? text;
  final Widget? body;
  final Color? backgroundColor;
  final bool? ellipsis;
  final bool? loading;

  const ResultItem(
      {super.key,
      this.header,
      this.body,
      this.backgroundColor,
      this.text,
      this.ellipsis,
      this.loading,
      this.title,
      this.titleColor});

  @override
  State<ResultItem> createState() => _ResultItemState();
}

class _ResultItemState extends State<ResultItem> {
  bool _loading = true;

  @override
  void initState() {
    super.initState();

    _loading = widget.loading ?? true;
  }

  @override
  void didUpdateWidget(covariant ResultItem oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.loading != widget.loading) {
      setState(() {
        _loading = widget.loading ?? true;
      });
    }
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    Color bgColor = widget.backgroundColor ?? const Color(0xFFFFFFFF);

    return _loading == true
        ? ResultItemLoading(
            title: widget.title ?? '',
            titleColor: widget.titleColor,
            bgColor: bgColor,
          )
        : Container(
            decoration: BoxDecoration(
              color: bgColor,
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.all(16.0),
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Padding(
                padding: const EdgeInsets.only(bottom: 12.0),
                child: widget.header ?? const SizedBox.shrink(),
              ),
              Padding(
                padding: const EdgeInsets.only(bottom: 12.0),
                child: Image.asset(
                  Assets.imagesHDash,
                  height: 1,
                ),
              ),
              Container(
                constraints: widget.ellipsis == true
                    ? const BoxConstraints(maxHeight: 44)
                    : null,
                child: SingleChildScrollView(
                  physics: const NeverScrollableScrollPhysics(),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      widget.body ?? const SizedBox.shrink(),
                      widget.text != null
                          ? MarkdownBody(
                              data: widget.text!,
                            )
                          : const SizedBox.shrink(),
                    ],
                  ),
                ),
              ),
            ]),
          );
  }
}

class ResultItemHeader extends StatelessWidget {
  final String title;
  final Color? titleColor;
  final String? iconPath;

  const ResultItemHeader(
      {super.key, required this.title, this.iconPath, this.titleColor});

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).primaryColor;
    final _titleColor = titleColor ?? primaryColor;
    return Row(children: [
      iconPath != null
          ? Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: Image.asset(
                iconPath!,
                width: 20,
                height: 20,
              ),
            )
          : const SizedBox.shrink(),
      Text(
        title,
        style: TextStyle(
          color: _titleColor,
          fontSize: 18,
          fontWeight: FontWeight.w700,
          height: 1,
          decoration: TextDecoration.none,
        ),
      ),
    ]);
  }
}

class ResultItemAction extends StatefulWidget {
  final String? actionText;
  final String? afterActionText;
  final VoidCallback? onSave;

  const ResultItemAction({
    super.key,
    this.onSave,
    this.actionText,
    this.afterActionText,
  });

  @override
  State<ResultItemAction> createState() => _ResultItemActionState();
}

class _ResultItemActionState extends State<ResultItemAction> {
  bool _afterAction = false;

  @override
  void initState() {
    super.initState();
  }

  void _handleSave() {
    setState(() {
      _afterAction = true;
      widget.onSave?.call();
    });

    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        _afterAction = false;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        GestureDetector(
          behavior: HitTestBehavior.translucent,
          onTap: _handleSave,
          child: Visibility(
            visible: !_afterAction,
            child: Text(
              widget.actionText ?? "",
              style: const TextStyle(
                color: Color(0xFF3A51FF),
                fontSize: 16,
                fontWeight: FontWeight.w500,
                height: 1,
                decoration: TextDecoration.none,
              ),
            ),
          ),
        ),
        Visibility(
          visible: _afterAction,
          child: Row(
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: Image.asset(
                  Assets.imagesCheckmarkSuccessIcon,
                  width: 16,
                  height: 16,
                ),
              ),
              Text(
                widget.afterActionText ?? "",
                style: const TextStyle(
                  color: Color(0xFF0FDA6C),
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                  height: 1,
                  decoration: TextDecoration.none,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class ResultItemLoading extends StatelessWidget {
  final Color? titleColor;
  final Color? bgColor;
  final String title;

  const ResultItemLoading(
      {super.key, this.titleColor, required this.title, this.bgColor});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 51,
      decoration: BoxDecoration(
        color: bgColor ?? const Color(0xFFFFFDF4),
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(8),
      child: Row(
        children: [
          const Padding(
            padding: EdgeInsets.only(right: 8.0),
            child: SizedBox(
              width: 24,
              height: 24,
              child: LoadingIndicator(
                indicatorType: Indicator.lineSpinFadeLoader,
                colors: [Color(0xFF12102F)],
                strokeWidth: 1,
              ),
            ),
          ),
          Text(
            title,
            style: TextStyle(
              color: titleColor ?? const Color(0xFF12102F),
              fontSize: 24,
              fontWeight: FontWeight.w700,
              height: 1,
              decoration: TextDecoration.none,
            ),
          ),
        ],
      ),
    );
  }
}
