import 'package:flutter/material.dart';

import 'package:get/get.dart';

import '../controllers/good_recommend_controller.dart';

class GoodRecommendView extends GetView<GoodRecommendController> {
  const GoodRecommendView({super.key});
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('GoodRecommendView'),
        centerTitle: true,
      ),
      body: const Center(
        child: Text(
          'GoodRecommendView is working',
          style: TextStyle(fontSize: 20),
        ),
      ),
    );
  }
}
