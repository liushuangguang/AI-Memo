import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'package:get/get.dart';

import '../controllers/gallery_controller.dart';

class GalleryView extends GetView<GalleryController> {
  const GalleryView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.image_search, // 使用一个合适的图标
              size: 100.w, // 图标大小
              color: Colors.grey[300], // 图标颜色
            ),
            SizedBox(height: 20.h), // 图标和文本之间的间距
            Text(
              '功能开发中',
              style: TextStyle(
                fontSize: 24.sp, // 文本字体大小
                fontWeight: FontWeight.bold, // 文本字体粗细
                color: Colors.black, // 文本颜色
              ),
            ),
            SizedBox(height: 10.h), // 文本之间的间距
            Text(
              '敬请期待',
              style: TextStyle(
                fontSize: 18.sp, // 文本字体大小
                color: Colors.grey[600], // 文本颜色
              ),
            ),
          ],
        ),
      ),
    );
  }
}
