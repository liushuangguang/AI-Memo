import 'package:flutter/material.dart';

class RetryBtn extends StatelessWidget {
  final VoidCallback? onTap;

  const RetryBtn({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
        margin: const EdgeInsets.only(bottom: 16, left: 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.refresh,
              size: 16,
              color: Theme.of(context).primaryColor,
            ),
            const SizedBox(width: 10),
            const Text(
              '重新生成',
              style: TextStyle(
                  fontSize: 14,
                  color: Color(0xFF3F3C61),
                  fontWeight: FontWeight.w700),
            ),
          ],
        ),
      ),
    );
  }
}
