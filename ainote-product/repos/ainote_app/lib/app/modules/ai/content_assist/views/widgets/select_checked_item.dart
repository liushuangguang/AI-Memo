import 'package:ainote_app/generated/assets.dart';
import 'package:flutter/material.dart';

class SelectCheckedItem extends StatefulWidget {
  final String text;

  const SelectCheckedItem({super.key, required this.text});

  @override
  State<SelectCheckedItem> createState() => _SelectCheckedItemState();
}

class _SelectCheckedItemState extends State<SelectCheckedItem> {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFEDF8F1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Image.asset(
            Assets.imagesGreenCheckedIcon,
            width: 20,
          ),
          const SizedBox(width: 8),
          Text(
            widget.text,
            style: const TextStyle(
              fontSize: 14,
              color: Color(0xFF009D4F),
            ),
          ),
        ],
      ),
    );
  }
}
