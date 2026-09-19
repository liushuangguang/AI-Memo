import 'package:flutter/material.dart';

class SecondaryBtn extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;

  const SecondaryBtn({super.key, required this.text, this.onPressed});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final secondaryColor = colorScheme.secondary;
    final onSecondaryColor = colorScheme.onSecondary;
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: secondaryColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        shadowColor: Colors.transparent,
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.bold,
          color: onSecondaryColor,
        ),
      ),
    );
  }
}
