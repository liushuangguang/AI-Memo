import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/modules/ai/smart_organize/controllers/smart_organize_controller.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/widgets/note_card/note_source_image.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('reopening a screenshot note shows source even without AI modules', () {
    final editor = NoteEditController(
        smartOrganizeController: SmartOrganizeController());
    editor.quillLogic = MyQuillController();
    editor.setNote(NoteModel(id: 'capture', title: '截图笔记',
        imageUrl: '/v2/capture/images/synthetic', modules: []));
    expect(editor.aiCardList.whereType<NoteSourceImage>(), hasLength(1));
    expect(editor.aiModuleVisible.value, isTrue);
    editor.setNote(NoteModel(id: 'text', title: '纯文字', modules: []));
    expect(editor.aiCardList.whereType<NoteSourceImage>(), isEmpty);
    expect(editor.aiModuleVisible.value, isFalse);
    editor.quillLogic.quillController.dispose();
    editor.quillLogic.quillFocusNode.dispose();
    editor.titleController.dispose();
  });

  testWidgets('source image remains visible with retry and full-screen preview',
      (tester) async {
    final oldBase = MyDio.dio.options.baseUrl;
    MyDio.dio.options.baseUrl = 'https://example.test';
    addTearDown(() => MyDio.dio.options.baseUrl = oldBase);
    await tester.pumpWidget(const MaterialApp(home: Scaffold(
        body: NoteSourceImage(url: '/v2/capture/images/synthetic'))));
    await tester.pumpAndSettle();
    expect(find.text('来源截图'), findsOneWidget);
    expect(find.text('图片加载失败，点击重试'), findsOneWidget);
    await tester.tap(find.byTooltip('查看来源截图'));
    await tester.pumpAndSettle();
    expect(find.byType(InteractiveViewer), findsOneWidget);
    final before = tester.widget<Image>(find.byType(Image).last).key;
    await tester.tap(find.text('图片加载失败，点击重试').last);
    await tester.pumpAndSettle();
    expect(tester.widget<Image>(find.byType(Image).last).key, isNot(before));
    expect(tester.takeException(), isNull);
  });
}
