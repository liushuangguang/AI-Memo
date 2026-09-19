// import 'package:flutter/material.dart';
// import 'package:appflowy_editor/appflowy_editor.dart';
// import 'package:image_picker/image_picker.dart';
//
// class EditorPage extends StatefulWidget {
//   @override
//   _EditorPageState createState() => _EditorPageState();
// }
//
// class _EditorPageState extends State<EditorPage> {
//   final ImagePicker _picker = ImagePicker();
//   final editorState = EditorState.blank(withInitialText: true);
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         title: Text('编辑器'),
//         actions: [
//           IconButton(
//             icon: Icon(Icons.add_photo_alternate),
//             onPressed: () {
//
//             },
//           ),
//           IconButton(
//             icon: Icon(Icons.check_box),
//             onPressed: () {
//
//             },
//           ),
//         ],
//       ),
//       body: AppFlowyEditor.custom(
//         editorState: EditorState.blank(withInitialText: true),
//         blockComponentBuilders: standardBlockComponentBuilderMap,
//         characterShortcutEvents: const [],
//       ),
//     );
//   }
// }
//
// Map<String, BlockComponentBuilder> customBuilder() {
//   final configuration = BlockComponentConfiguration(
//     padding: (node) {
//       if (HeadingBlockKeys.type == node.type) {
//         return const EdgeInsets.symmetric(vertical: 30);
//       }
//       return const EdgeInsets.symmetric(vertical: 10);
//     },
//     textStyle: (node) {
//       if (HeadingBlockKeys.type == node.type) {
//         return const TextStyle(color: Colors.yellow);
//       }
//       return const TextStyle();
//     },
//   );
//
//   // customize heading block style
//   return {
//     ...standardBlockComponentBuilderMap,
//     // heading block
//     HeadingBlockKeys.type: HeadingBlockComponentBuilder(
//       configuration: configuration,
//     ),
//     // todo-list block
//     TodoListBlockKeys.type: TodoListBlockComponentBuilder(
//       configuration: configuration,
//       iconBuilder: (context, node, function) {
//         final checked = node.attributes[TodoListBlockKeys.checked] as bool;
//         return Icon(
//           checked ? Icons.check_box : Icons.check_box_outline_blank,
//           size: 20,
//           color: Colors.white,
//         );
//       },
//     ),
//     // bulleted list block
//     BulletedListBlockKeys.type: BulletedListBlockComponentBuilder(
//       configuration: configuration,
//       iconBuilder: (context, node) {
//         return const Icon(
//           Icons.circle,
//           size: 20,
//           color: Colors.green,
//         );
//       },
//     ),
//     // quote block
//     QuoteBlockKeys.type: QuoteBlockComponentBuilder(
//       configuration: configuration,
//       iconBuilder: (context, node) {
//         return const EditorSvg(
//           width: 20,
//           height: 20,
//           padding: EdgeInsets.only(right: 5.0),
//           name: 'quote',
//           color: Colors.pink,
//         );
//       },
//     ),
//   };
// }