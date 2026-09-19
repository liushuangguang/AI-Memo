import 'package:ainote_app/app/widgets/ai_card/auto_sort.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

void main() {
  testWidgets('open automatic organize panel refreshes when AI finishes', (tester) async {
    tester.view.physicalSize = const Size(420, 923);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final loading = true.obs;
    final tags = <String>[].obs;
    final scene = ''.obs;
    AutoSort current() => AutoSort(
      list: const [], loading: loading.value,
      tags: tags.toList(), scene: scene.value,
      liveState: current,
    );
    await tester.pumpWidget(ScreenUtilInit(
      designSize: const Size(375, 812),
      builder: (_, __) => MaterialApp(home: Scaffold(body: Obx(current))),
    ));
    await tester.tap(find.text('自动整理'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('正在生成标签和归类，请稍候…'), findsOneWidget);
    loading.value = false;
    tags.assignAll(['技术交流', '群聊摘要']);
    scene.value = '整理产品开发讨论';
    await tester.pumpAndSettle();
    expect(find.text('技术交流'), findsOneWidget);
    expect(find.text('群聊摘要'), findsOneWidget);
    expect(find.text('整理产品开发讨论'), findsOneWidget);
    expect(find.text('正在生成标签和归类，请稍候…'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
