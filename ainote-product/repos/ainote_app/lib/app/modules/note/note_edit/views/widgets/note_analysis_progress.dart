import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../../widgets/gradient_linear_progress_bar.dart';

class NoteAnalysisProgress extends StatefulWidget {
  final bool done;

  const NoteAnalysisProgress({super.key, required this.done});

  @override
  State<NoteAnalysisProgress> createState() => _NoteAnalysisProgressState();
}

class _NoteAnalysisProgressState extends State<NoteAnalysisProgress>
    with TickerProviderStateMixin {
  late AnimationController controller;

  @override
  void initState() {
    controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 20),
    )..addListener(() {
        setState(() {});
      });
    controller.repeat();
    super.initState();

    if (widget.done) {
      controller.value = 1;
      controller.stop();
    }
  }

  @override
  void didUpdateWidget(covariant NoteAnalysisProgress oldWidget) {
    if (widget.done) {
      controller.value = 1;
      controller.stop();
    } else {
      controller.value = 0;
      controller.repeat();
    }
    super.didUpdateWidget(oldWidget);
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GradientLinearProgressBar(
      strokeCapRound: false,
      strokeWidth: 38.w,
      backgroundColor: Color(0xFF332D41),
      colors: [
        Color.fromRGBO(52, 46, 65, 0.2),
        Color.fromRGBO(255, 255, 255, 0.2),
      ],
      stops: [0, 1],
      value: controller.value,
    );
  }
}
