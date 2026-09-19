import 'package:flutter/material.dart';

import 'package:get/get.dart';

import '../controllers/content_assist_controller.dart';

class ContentAssistView extends GetView<ContentAssistController> {
  const ContentAssistView({super.key});
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ContentAssistView'),
        centerTitle: true,
      ),
      body: const Center(
        child: Text(
          'ContentAssistView is working',
          style: TextStyle(fontSize: 20),
        ),
      ),
    );
  }
}
