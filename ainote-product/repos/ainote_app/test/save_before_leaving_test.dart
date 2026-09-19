import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ainote_app/app/widgets/save_before_leaving.dart';

void main() {
  testWidgets('back waits, failed save stays, double back saves once',
      (tester) async {
    final navigator = GlobalKey<NavigatorState>();
    var pending = Completer<bool>();
    var calls = 0;
    await tester.pumpWidget(MaterialApp(
        navigatorKey: navigator, home: const Scaffold(body: Text('首页'))));
    navigator.currentState!.push(MaterialPageRoute<void>(
        builder: (context) => SaveBeforeLeaving(
            save: () {
              calls++;
              return pending.future;
            },
            child: Scaffold(
                appBar: AppBar(
                    leading: IconButton(
                        icon: const Icon(Icons.arrow_back),
                        onPressed: () => Navigator.of(context).maybePop())),
                body: const Text('草稿')))));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pump();
    navigator.currentState!.maybePop();
    await tester.pump();
    expect(calls, 1);
    expect(find.text('草稿'), findsOneWidget);
    pending.complete(false);
    await tester.pumpAndSettle();
    expect(find.text('草稿'), findsOneWidget);
    pending = Completer<bool>();
    navigator.currentState!.maybePop();
    await tester.pump();
    expect(calls, 2);
    expect(find.text('草稿'), findsOneWidget);
    pending.complete(true);
    await tester.pumpAndSettle();
    expect(find.text('首页'), findsOneWidget);
    expect(find.text('草稿'), findsNothing);
  });
}
