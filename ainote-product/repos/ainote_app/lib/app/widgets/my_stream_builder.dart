import 'dart:async';

import 'package:flutter/material.dart';
import 'package:skeletonizer/skeletonizer.dart';

class MyStreamBuilder extends StatefulWidget {
  final Stream<String> stream;
  final Widget Function()? beforeBuilder;
  final Widget Function(String) builder; // stream in process widget
  final Widget Function(String)? afterBuilder; // stream done widget
  final Function()? onError;
  final Function(String text)? onListen;
  final Function(String text)? onDone;

  const MyStreamBuilder({
    super.key,
    required this.stream,
    required this.builder,
    this.onError,
    this.onListen,
    this.onDone,
    this.afterBuilder,
    this.beforeBuilder,
  });

  @override
  State<MyStreamBuilder> createState() => _StreamTextState();
}

class _StreamTextState extends State<MyStreamBuilder> {
  String _message = '';
  bool _isDone = false;
  StreamSubscription<String>? _subscription;

  @override
  void initState() {
    super.initState();
    _subscribeToStream();
  }

  @override
  void didUpdateWidget(covariant MyStreamBuilder oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.stream != widget.stream) {
      _unsubscribeFromStream();
      _subscribeToStream();
      setState(() {
        _message = '';
        _isDone = false;
      });
    }
  }

  @override
  void dispose() {
    _unsubscribeFromStream();
    super.dispose();
  }

  void _subscribeToStream() {
    _subscription = widget.stream.listen(
      (data) {
        widget.onListen?.call(data);
        setState(() {
          _message += data;
        });
      },
      onError: (error) {
        widget.onError?.call();
      },
      onDone: () {
        _unsubscribeFromStream();
        widget.onDone?.call(_message);
        setState(() {
          _isDone = true;
        });
      },
    );
  }

  void _unsubscribeFromStream() {
    _subscription?.cancel();
    _subscription = null;
  }

  @override
  Widget build(BuildContext context) {
    Widget afterBuildWidget() {
      if (_isDone) {
        return widget.afterBuilder?.call(_message) ?? const IgnorePointer();
      }
      return SizedBox.shrink();
    }

    if (_message.isEmpty && widget.beforeBuilder != null) {
      return widget.beforeBuilder?.call() ??
          const Skeletonizer(child: SizedBox.shrink());
    }

    return Stack(
      alignment: Alignment.topLeft,
      children: [
        AnimatedOpacity(
          opacity: widget.afterBuilder != null && _isDone ? 0.0 : 1.0,
          duration: const Duration(milliseconds: 300),
          child: widget.builder(_message),
        ),
        AnimatedOpacity(
          opacity: _isDone ? 1.0 : 0.0,
          duration: const Duration(milliseconds: 300),
          child: afterBuildWidget(),
        ),
      ],
    );
  }
}
