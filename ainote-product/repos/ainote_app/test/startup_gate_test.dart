import 'dart:async';

import 'package:ainote_app/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows loading before initialization completes', (tester) async {
    final completer = Completer<void>();
    await tester.pumpWidget(StartupGate(
      initializer: () => completer.future,
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));

    expect(find.text('正在准备 AI 备忘录…'), findsOneWidget);
    expect(find.text('ready'), findsNothing);
    completer.complete();
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);
  });

  testWidgets('shows the app after successful initialization', (tester) async {
    await tester.pumpWidget(StartupGate(
      initializer: () async {},
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);
  });

  testWidgets('shows a safe failure state after an error', (tester) async {
    await tester.pumpWidget(StartupGate(
      initializer: () async => throw StateError('secret details'),
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));
    await tester.pump();

    expect(find.text('应用启动失败，请稍后重试'), findsOneWidget);
    expect(find.text('重试'), findsOneWidget);
    expect(tester.getSize(find.byType(ElevatedButton)).height,
        greaterThanOrEqualTo(44));
    expect(find.textContaining('secret details'), findsNothing);
  });

  testWidgets('retry runs the initializer again', (tester) async {
    var attempts = 0;
    await tester.pumpWidget(StartupGate(
      initializer: () async {
        attempts++;
        if (attempts == 1) throw Exception('first attempt');
      },
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));
    await tester.pump();
    await tester.tap(find.text('重试'));
    await tester.pump();

    expect(attempts, 2);
    expect(find.text('ready'), findsOneWidget);
  });

  testWidgets('shows failure when initialization times out', (tester) async {
    final completer = Completer<void>();
    await tester.pumpWidget(StartupGate(
      initializer: () => completer.future,
      timeout: const Duration(milliseconds: 10),
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));
    await tester.pump(const Duration(milliseconds: 11));

    expect(find.text('应用启动失败，请稍后重试'), findsOneWidget);
    completer.complete();
  });

  testWidgets('a late timed-out attempt cannot replace a successful retry',
      (tester) async {
    final firstAttempt = Completer<void>();
    var attempts = 0;
    await tester.pumpWidget(StartupGate(
      initializer: () {
        attempts++;
        return attempts == 1 ? firstAttempt.future : Future<void>.value();
      },
      timeout: const Duration(milliseconds: 10),
      appBuilder: (_) => const MaterialApp(home: Text('ready')),
    ));

    await tester.pump(const Duration(milliseconds: 11));
    await tester.tap(find.text('重试'));
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);

    firstAttempt.complete();
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);
  });
}
