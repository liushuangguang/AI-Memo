import 'package:ainote/constants/app_colors.dart';
import 'package:flutter/material.dart';

class AIGeneratingScreen extends StatefulWidget {
  const AIGeneratingScreen({super.key});

  @override
  State<AIGeneratingScreen> createState() => _AIGeneratingScreenState();
}

class _AIGeneratingScreenState extends State<AIGeneratingScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Generating...'),
      ),
      body: Container(),
    );
  }
}
