import 'package:ainote_app/app/utils/helper.dart';
import 'package:flutter/material.dart';

class HighlightOverlay extends StatefulWidget {
  final bool visible;
  final Widget child;
  final VoidCallback onHide;
  final Widget? beforeChild;
  final Widget? afterChild;
  final bool? relativePosition; // beforeChild afterChild 定位方式，默认为false，表示使用绝对位置
  final double? left; // beforeChild afterChild 定位
  final double? top; // beforeChild afterChild 定位
  final bool? isBottom;

  const HighlightOverlay({
    super.key,
    required this.visible,
    required this.child,
    required this.onHide,
    this.beforeChild,
    this.afterChild,
    this.relativePosition = false,
    this.left,
    this.top,
    this.isBottom,
  });

  @override
  State<HighlightOverlay> createState() => _HighlightOverlayState();
}

class _HighlightOverlayState extends State<HighlightOverlay> {
  OverlayEntry? _overlayEntry;
  final GlobalKey _childKey = GlobalKey(debugLabel: randomId());
  late double _left;
  late double _top;

  @override
  void initState() {
    super.initState();
    _overlayEntry = OverlayEntry(
      builder: (context) => _buildOverlayContent(),
    );

    if (widget.visible) {
      _showOverlay();
    }
  }

  @override
  void didUpdateWidget(HighlightOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.visible != oldWidget.visible) {
      if (widget.visible) {
        _showOverlay();
      } else {
        _hideOverlay();
      }
    }
  }

  void _showOverlay() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final RenderBox? renderBox =
          _childKey.currentContext?.findRenderObject() as RenderBox?;

      if (renderBox != null) {
        final Offset position = renderBox.localToGlobal(Offset.zero);
        final Size size = renderBox.size;

        _left = widget.left ?? 0;
        _top = widget.top ?? 0;

        if (widget.relativePosition == true) {
          _top += position.dy;
          _left += position.dx;
        }

        // todo 确保 afterChild 在屏幕内

        _overlayEntry?.markNeedsBuild();
        if (_overlayEntry != null) {
          Overlay.of(context).insert(_overlayEntry!);
        }
      }
    });
  }

  void _hideOverlay() {
    if (_overlayEntry != null && _overlayEntry!.mounted) {
      _overlayEntry!.remove();
    }
  }

  @override
  void dispose() {
    _hideOverlay();
    super.dispose();
  }

  Widget _buildOverlayContent() {
    final RenderBox? renderBox =
        _childKey.currentContext?.findRenderObject() as RenderBox?;

    if (renderBox == null) {
      return Container(); // 如果找不到 RenderBox，返回一个空容器
    }

    final Offset position = renderBox.localToGlobal(Offset.zero);
    final Size size = renderBox.size;

    return Stack(
      children: [
        Positioned.fill(
          child: GestureDetector(
            onTap: widget.onHide,
            child: Material(
              color: Colors.black.withOpacity(0.5),
            ),
          ),
        ),
        if (widget.beforeChild != null)
          Positioned(left: _left, top: _top, child: widget.beforeChild!),
        Positioned(
          left: position.dx,
          top: position.dy,
          width: size.width,
          height: size.height,
          child: Material(
            color: Colors.transparent,
            child: widget.child,
          ),
        ),
        if (widget.afterChild != null && widget.relativePosition == true)
          Positioned(
            left: _left,
            top: _top + (widget.isBottom == true ? size.height : 0),
            child: SizedBox(
              width: size.width,
              child: Center(child: widget.afterChild!),
            ),
          ),
        if (widget.afterChild != null && widget.relativePosition != true)
          Positioned(left: _left, top: _top, child: widget.afterChild!),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      key: _childKey,
      child: widget.child,
    );
  }
}
