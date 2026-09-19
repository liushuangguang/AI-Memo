import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

class AndroidMasking extends StatefulWidget {
  AndroidMasking({super.key});

  @override
  State<AndroidMasking> createState() => _AndroidMaskingState();
}

class _AndroidMaskingState extends State<AndroidMasking> with TickerProviderStateMixin  {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Lottie.asset(
      'assets/json/screenshot_scan.json',
      controller: _controller,
      onLoaded: (composition) {
        _controller
          ..duration = composition.duration
          ..repeat();
      },
      fit: BoxFit.cover,
    );
  }
}