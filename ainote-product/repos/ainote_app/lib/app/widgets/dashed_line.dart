import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';

class DashedLine extends StatelessWidget {
  final Color? color;
  final EdgeInsets? padding;

  const DashedLine({super.key, this.color, this.padding});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding ?? EdgeInsets.zero,
      child: CustomPaint(
        size: Size(double.infinity, 2), // 虚线的高度为2
        painter: DashedLinePainter(color: color),
      ),
    );
  }
}

class DashedLinePainter extends CustomPainter {
  final Color? color;

  DashedLinePainter({this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final Paint paint = Paint()
      ..color = color ?? MyColors.dashedLineColor
      ..strokeWidth = 1.5
      ..style = PaintingStyle.stroke;

    double dashWidth = 5.0;
    double dashSpace = 5.0;
    double startX = 0.0;

    while (startX < size.width) {
      canvas.drawLine(Offset(startX, 0), Offset(startX + dashWidth, 0), paint);
      startX += dashWidth + dashSpace;
    }
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return false;
  }
}
