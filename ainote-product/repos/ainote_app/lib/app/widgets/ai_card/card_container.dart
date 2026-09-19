import 'dart:ui';

import 'package:ainote_app/app/widgets/highlight_overlay.dart';
import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';

class CardContainer extends StatefulWidget {
  final BoxConstraints? constraints;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double? height;
  final Color bgColor;
  final Color borderColor;
  final Widget title;
  final Widget content;
  final bool? needHighlight;
  final VoidCallback? onTap;

  const CardContainer(
      {super.key,
      this.constraints,
      this.padding,
      this.margin,
      required this.bgColor,
      required this.borderColor,
      this.height,
      this.needHighlight = false,
      required this.title,
      required this.content,
      this.onTap});

  @override
  State<CardContainer> createState() => _CardContainerState();
}

class _CardContainerState extends State<CardContainer> {
  bool _visible = false;

  void _toggleOverlay() {
    setState(() {
      _visible = !_visible;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (widget.needHighlight == false) {
      return GestureDetector(
        onLongPress: _toggleOverlay,
        onTap: widget.onTap,
        behavior: HitTestBehavior.opaque,
        child: Container(
          height: widget.height,
          constraints: widget.constraints,
          padding: widget.padding ?? EdgeInsets.all(12.w),
          margin: widget.margin ?? EdgeInsets.only(bottom: 16.h),
          decoration: BoxDecoration(
            color: widget.bgColor,
            borderRadius: BorderRadius.circular(12.w),
            border: Border.all(color: widget.borderColor),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              widget.title,
              SizedBox(height: 12.h),
              widget.content,
            ],
          ),
        ),
      );
    }
    return HighlightOverlay(
        visible: _visible,
        onHide: _toggleOverlay,
        relativePosition: true,
        top: -54.h,
        afterChild: GestureDetector(
            onTap: () {}, child: SvgPicture.asset(Assets.imagesTooltipsDel)),
        child: GestureDetector(
          onTap: widget.onTap,
          onLongPress: _toggleOverlay,
          behavior: HitTestBehavior.opaque,
          child: Container(
            height: widget.height,
            constraints: widget.constraints,
            padding: widget.padding ?? EdgeInsets.all(12.w),
            margin: widget.margin ?? EdgeInsets.only(bottom: 16.h),
            decoration: BoxDecoration(
              color: widget.bgColor,
              borderRadius: BorderRadius.circular(12.w),
              border: Border.all(color: widget.borderColor),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                widget.title,
                SizedBox(height: 12.h),
                widget.content,
              ],
            ),
          ),
        ));
  }
}
