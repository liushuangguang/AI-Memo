import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/modules/note/note_edit/views/widgets/note_type_dropdown.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('selection resolver safely handles empty and missing IDs', () {
    expect(resolveNoteTypeSelection(<NoteTypeModel>[], 7), isNull);
    expect(
      resolveNoteTypeSelection([NoteTypeModel(id: 7, name: '工作')], 99),
      isNull,
    );
  });

  testWidgets(
      'empty and failed category results render an uncategorized fallback',
      (tester) async {
    await tester.pumpWidget(_dropdownApp(list: const [], value: 7));

    expect(find.text('未分类'), findsOneWidget);
  });

  testWidgets('a missing current ID renders safely', (tester) async {
    await tester.pumpWidget(_dropdownApp(
      list: [NoteTypeModel(id: 7, name: '工作')],
      value: 99,
    ));

    expect(find.text('未分类'), findsOneWidget);
  });

  testWidgets('delayed categories replace loading with the selected label',
      (tester) async {
    await tester.pumpWidget(_dropdownApp(
      list: const [],
      value: 7,
      loading: true,
    ));
    expect(find.text('加载中…'), findsOneWidget);

    await tester.pumpWidget(_dropdownApp(
      list: [NoteTypeModel(id: 7, name: '工作')],
      value: 7,
    ));
    await tester.pump();

    expect(find.text('工作'), findsOneWidget);
    expect(find.text('加载中…'), findsNothing);
  });
}

Widget _dropdownApp({
  required List<NoteTypeModel> list,
  required int? value,
  bool loading = false,
}) {
  return ScreenUtilInit(
    designSize: const Size(390, 844),
    builder: (context, child) => MaterialApp(
      home: Scaffold(
        body: NoteTagDropdown(
          key: const ValueKey('note-type-dropdown'),
          list: list,
          value: value,
          loading: loading,
          onChang: (_) {},
        ),
      ),
    ),
  );
}
