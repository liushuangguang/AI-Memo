import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ainote_app/app/widgets/ai_card/related_title_detail.dart';

Widget page(String text, Future<String> Function(String) loader,
    {void Function(String?)? onFinish}) => ScreenUtilInit(
  designSize: const Size(375, 812), builder: (_, __) => MaterialApp(
    home: Scaffold(body: RelatedTitleDetail(text: text, noHeader: true,
      loadDetail: loader, onFinish: onFinish))));

void main() {
  testWidgets('closed detail ignores late failure without setState after dispose', (tester) async {
    final held = Completer<String>();
    final finished = <String?>[];
    await tester.pumpWidget(page('one', (_) => held.future, onFinish: finished.add));
    await tester.pumpWidget(const SizedBox());
    held.completeError(StateError('synthetic unavailable'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(finished, isEmpty);
  });
  testWidgets('failure retries and switching query discards old result', (tester) async {
    final old = Completer<String>();
    var calls = 0;
    Future<String> load(String text) async {
      if (text == 'old') return old.future;
      if (calls++ == 0) throw StateError('secret transport detail');
      return '当前问题的详情';
    }
    await tester.pumpWidget(page('old', load));
    await tester.pumpWidget(page('new', load));
    await tester.pumpAndSettle();
    expect(find.text('详情加载失败，请重试'), findsOneWidget);
    expect(find.textContaining('secret'), findsNothing);
    await tester.tap(find.text('重新加载详情'));
    await tester.pumpAndSettle();
    old.complete('过期结果');
    await tester.pumpAndSettle();
    expect(find.text('当前问题的详情'), findsOneWidget);
    expect(find.text('过期结果'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
