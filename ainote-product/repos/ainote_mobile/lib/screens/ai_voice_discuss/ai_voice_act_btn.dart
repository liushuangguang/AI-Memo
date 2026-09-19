import 'package:flutter/material.dart';

class AiVoiceActBtn extends StatelessWidget {
  final VoidCallback? onTap;
  final String text;
  final Color color;
  final IconData? iconData;
  final Widget? icon;

  const AiVoiceActBtn(
      {super.key,
      this.onTap,
      required this.text, this.iconData,
      required this.color, this.icon});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: onTap,
      child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                    color: color, borderRadius: BorderRadius.circular(24)),
                child: icon ?? Icon(iconData, color: Colors.white, size: 32)),
            const SizedBox(height: 8),
            Text(
              text,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10,
                  fontWeight: FontWeight.w400),
            )
          ]),
    );
  }
}
