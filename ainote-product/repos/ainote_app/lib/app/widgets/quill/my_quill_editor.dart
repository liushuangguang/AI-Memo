import 'dart:io' as io show Directory, File;
import 'package:ainote_app/app/utils/note_image.dart';

import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:cached_network_image/cached_network_image.dart'
    show CachedNetworkImageProvider;
import 'package:desktop_drop/desktop_drop.dart' show DropTarget;
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/flutter_quill_internal.dart';
import 'package:flutter_quill_extensions/flutter_quill_extensions.dart';

// ignore: implementation_imports
import 'package:flutter_quill_extensions/src/editor/image/widgets/image.dart'
    show getImageProviderByImageSource, imageFileExtensions;
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';
import 'package:path/path.dart' as path;
import 'package:simple_gradient_text/simple_gradient_text.dart';

import './extensions/scaffold_messenger.dart';
import 'embeds/ai_suggestion_embed.dart';
import 'embeds/dashed_line_embed.dart';
import 'embeds/todo_embed.dart';
import 'my_quill_controller.dart';

class MyQuillEditor extends StatelessWidget {
  final String tag;
  final bool autoFocus;

  // 调格式
  final VoidCallback? onFormatText;

  // 内容辅助
  final VoidCallback? onContentAssist;

  const MyQuillEditor(
      {this.configurations,
      super.key,
      required this.tag,
      required this.autoFocus,
      this.onFormatText,
      this.onContentAssist});

  final QuillEditorConfigurations? configurations;

  QuillSharedConfigurations get _sharedConfigurations {
    return const QuillSharedConfigurations(
      extraConfigurations: {
        QuillSharedExtensionsConfigurations.key:
            QuillSharedExtensionsConfigurations(
          assetsPrefix: 'assets', // Defaults to assets
        ),
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    var defaultConfigurations = configurations ??
        QuillEditorConfigurations(
          autoFocus: autoFocus,
          characterShortcutEvents: standardCharactersShortcutEvents,
          spaceShortcutEvents: standardSpaceShorcutEvents,
          searchConfigurations: const QuillSearchConfigurations(
            searchEmbedMode: SearchEmbedMode.plainText,
          ),
          sharedConfigurations: _sharedConfigurations,
        );
    return GetBuilder<MyQuillController>(
      tag: tag,
      assignId: true,
      builder: (logic) {
        return QuillEditor(
          scrollController: logic.quillScrollController,
          focusNode: logic.quillFocusNode,
          controller: logic.quillController,
          configurations: defaultConfigurations.copyWith(
            autoFocus: autoFocus,
            characterShortcutEvents: standardCharactersShortcutEvents,
            spaceShortcutEvents: standardSpaceShorcutEvents,
            searchConfigurations: const QuillSearchConfigurations(
              searchEmbedMode: SearchEmbedMode.plainText,
            ),
            sharedConfigurations: _sharedConfigurations,
            contextMenuBuilder: (
              BuildContext context,
              QuillRawEditorState rawEditorState,
            ) {
              /// 自定义 selection toolbar
              final List<Widget> customButtonItems = [
                if (!logic.quillOnlyView.value)
                  TextButton(
                      onPressed: () {
                        logic.setQuillIsReadOnly(false);
                        onFormatText?.call();
                      },
                      child: Text('调格式')),
                if (!logic.quillOnlyView.value)
                  TextButton(
                    onPressed: onContentAssist,
                    child: GradientText(
                      '内容辅助',
                      colors: [Color(0xFFFF00BB), Color(0xFF3C00FF)],
                    ),
                  )
              ];
              return AdaptiveTextSelectionToolbar(
                anchors: rawEditorState.contextMenuAnchors,
                children: AdaptiveTextSelectionToolbar.getAdaptiveButtons(
                  context,
                  rawEditorState.contextMenuButtonItems,
                ).toList()
                  ..addAll(customButtonItems),
              );
            },
            elementOptions: const QuillEditorElementOptions(
              codeBlock: QuillEditorCodeBlockElementOptions(
                enableLineNumbers: true,
              ),
              orderedList: QuillEditorOrderedListElementOptions(),
              unorderedList: QuillEditorUnOrderedListElementOptions(
                useTextColorForDot: true,
              ),
            ),
            scrollable: true,
            placeholder: '请输入内容',
            customStyles: DefaultStyles(
              placeHolder: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor.withOpacity(0.3),
                    fontSize: 14.sp,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              paragraph: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 14.sp,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              h5: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 14.sp,
                    fontWeight: FontWeight.bold,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              h4: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 16.sp,
                    fontWeight: FontWeight.bold,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              h3: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 18.sp,
                    fontWeight: FontWeight.bold,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              h2: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 20.sp,
                    fontWeight: FontWeight.bold,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
              h1: DefaultTextBlockStyle(
                  TextStyle(
                    color: MyColors.primaryColor,
                    fontSize: 24.sp,
                    fontWeight: FontWeight.bold,
                  ),
                  HorizontalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  VerticalSpacing(0, 0),
                  null),
            ),
            padding: EdgeInsets.all(16.w),
            onImagePaste: (imageBytes) async {
              if (kIsWeb) {
                return null;
              }
              // We will save it to system temporary files
              final newFileName =
                  'imageFile-${DateTime.now().toIso8601String()}.png';
              final newPath = path.join(
                io.Directory.systemTemp.path,
                newFileName,
              );
              final file = await io.File(
                newPath,
              ).writeAsBytes(imageBytes, flush: true);
              return file.path;
            },
            onGifPaste: (gifBytes) async {
              if (kIsWeb) {
                return null;
              }
              // We will save it to system temporary files
              final newFileName =
                  'gifFile-${DateTime.now().toIso8601String()}.gif';
              final newPath = path.join(
                io.Directory.systemTemp.path,
                newFileName,
              );
              final file = await io.File(
                newPath,
              ).writeAsBytes(gifBytes, flush: true);
              return file.path;
            },
            embedBuilders: [
              ...(kIsWeb
                  ? FlutterQuillEmbeds.editorWebBuilders()
                  : FlutterQuillEmbeds.editorBuilders(
                      imageEmbedConfigurations:
                          QuillEditorImageEmbedConfigurations(
                        imageErrorWidgetBuilder: (context, error, stackTrace) {
                          return Text(
                            'Error while loading an image: ${error.toString()}',
                          );
                        },
                        imageProviderBuilder: (context, imageUrl) {
                          // cached_network_image is supported
                          // only for Android, iOS and web

                          // We will use it only if image from network
                          if (isAndroidApp || isIosApp || kIsWeb) {
                            if (noteImageHeaders(imageUrl) != null) {
                              return NetworkImage(noteImageUrl(imageUrl),
                                  headers: noteImageHeaders(imageUrl));
                            }
                            if (isHttpBasedUrl(imageUrl)) {
                              return CachedNetworkImageProvider(
                                imageUrl,
                              );
                            }
                          }
                          return getImageProviderByImageSource(
                            imageUrl,
                            imageProviderBuilder: null,
                            context: context,
                            assetsPrefix:
                                QuillSharedExtensionsConfigurations.get(
                                        context: context)
                                    .assetsPrefix,
                          );
                        },
                      ),
                      videoEmbedConfigurations:
                          QuillEditorVideoEmbedConfigurations(
                        customVideoBuilder: (videoUrl, readOnly) {
                          // Example: Check for YouTube Video URL and return your
                          // YouTube video widget here.

                          // Otherwise return null to fallback to the defualt logic
                          return null;
                        },
                        ignoreYouTubeSupport: true,
                      ),
                    )),
              TodoEmbedBuilderWidget(),
              DashedLineEmbedBuilderWidget(),
              AiSuggestionEmbedBuilderWidget(),
            ],
            builder: (context, rawEditor) {
              // The `desktop_drop` plugin doesn't support iOS platform for now
              if (isIosApp) {
                return rawEditor;
              }
              return DropTarget(
                onDragDone: (details) {
                  final scaffoldMessenger = ScaffoldMessenger.of(context);
                  final file = details.files.first;
                  final isSupported =
                      imageFileExtensions.any(file.name.endsWith);
                  if (!isSupported) {
                    scaffoldMessenger.showText(
                      'Only images are supported right now: ${file.mimeType}, ${file.name}, ${file.path}, $imageFileExtensions',
                    );
                    return;
                  }
                  context.requireQuillController.insertImageBlock(
                    imageSource: file.path,
                  );
                  scaffoldMessenger.showText('Image is inserted.');
                },
                child: rawEditor,
              );
            },
          ),
        );
      },
    );
  }
}
