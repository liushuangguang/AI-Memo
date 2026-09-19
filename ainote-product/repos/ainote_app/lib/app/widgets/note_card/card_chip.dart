import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class CardChip extends StatelessWidget {
  final String text;
  final Color? color;
  final Color? backgroundColor;

  const CardChip(
      {super.key, required this.text, this.color, this.backgroundColor});

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(text,
          style: TextStyle(
              color: color, fontSize: 12.sp, fontWeight: FontWeight.bold)),
      backgroundColor: backgroundColor,
      side: BorderSide.none,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
    );
  }
}
