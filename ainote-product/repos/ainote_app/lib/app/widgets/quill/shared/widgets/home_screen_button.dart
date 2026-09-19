import 'package:flutter/material.dart';

class HomeScreenButton extends StatelessWidget {
  const HomeScreenButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: () {},
      icon: const Icon(Icons.home),
      tooltip: 'Set the default to home screen',
    );
  }
}
