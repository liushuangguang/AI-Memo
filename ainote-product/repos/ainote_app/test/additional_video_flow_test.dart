import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:ainote_app/app/modules/ai/ai_additional/controllers/ai_additional_controller.dart';
import 'package:ainote_app/app/modules/ai/ai_additional/views/ai_additional_view.dart';
import 'package:ainote_app/app/widgets/note_additional_sheet.dart';

void main() {
  tearDown(() => Get.reset());
  testWidgets('video completion supports four options, manual input and previous question', (tester) async {
    tester.view.physicalSize = const Size(375, 812);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final controller = Get.put(AiAdditionalController());
    await tester.pumpWidget(ScreenUtilInit(designSize: const Size(375, 812), builder: (_, __) => GetMaterialApp(
      home: const AiAdditionalView(noteContent: '周末搬家，然后换窝。',
        toAdditional: ['换窝', '搬家'],
        toAdditionalsOptions: [['宠物窝', '搬新家', '家具', '其他'], ['租房搬家', '购房搬家']]))));
    await tester.pumpAndSettle();
    await tester.tap(find.text('宠物窝'));
    await tester.pumpAndSettle();
    await tester.drag(find.byType(PageView), const Offset(-320, 0));
    await tester.pumpAndSettle();
    expect(find.text('2/2'), findsOneWidget);
    await tester.tap(find.text('租房搬家'));
    await tester.pumpAndSettle();
    final preview = tester.widgetList<RichText>(find.byType(RichText)).map((w) => w.text.toPlainText()).join('\n');
    expect(preview, contains('周末租房搬家，然后宠物窝。'));
    await tester.tap(find.text('手动输入').hitTestable());
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '公司搬家');
    await tester.tap(find.text('确定'));
    await tester.pumpAndSettle();
    expect(controller.getSelectedOption(1), '公司搬家');
    await tester.drag(find.byType(PageView), const Offset(320, 0));
    await tester.pumpAndSettle();
    expect(find.text('1/2'), findsOneWidget);
    final manual = find.byKey(const ValueKey('completion-manual-0'));
    await tester.ensureVisible(manual);
    await tester.pumpAndSettle();
    await tester.tap(manual);
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '仓鼠换窝');
    await tester.tap(find.text('确定'));
    await tester.pumpAndSettle();
    expect(controller.getReplacedContent(['换窝', '搬家']), {'换窝': '仓鼠换窝', '搬家': '公司搬家'});
    expect(tester.takeException(), isNull);
  });

  testWidgets('small screen and long option remain scrollable without overflow', (tester) async {
    tester.view.physicalSize = const Size(320, 568);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    Get.put(AiAdditionalController());
    await tester.pumpWidget(ScreenUtilInit(designSize: const Size(375, 812), builder: (_, __) => GetMaterialApp(
      builder: (context, child) => MediaQuery(data: MediaQuery.of(context).copyWith(textScaler: const TextScaler.linear(1.3)), child: child!),
      home: const AiAdditionalView(noteContent: '一个很长的待完善信息', toAdditional: ['一个很长的待完善信息'],
        toAdditionalsOptions: [['这是一个很长但必须能够完整阅读而不能被固定高度裁断的候选选项', '二']]))));
    await tester.pumpAndSettle();
    expect(find.byType(NoteAdditionalSheet), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
