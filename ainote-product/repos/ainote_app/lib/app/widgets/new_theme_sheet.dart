import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'secondary_btn.dart';

class NewThemeSheet extends StatefulWidget {
  final String? themeContent;
  final String? themeDesc;
  final Future<void> Function(String themeContent, String themeDesc)?
      onThemeCreated;

  const NewThemeSheet(
      {super.key, this.themeContent, this.themeDesc, this.onThemeCreated});

  @override
  State<NewThemeSheet> createState() => _NewThemeSheetState();
}

class _NewThemeSheetState extends State<NewThemeSheet> {
  final TextEditingController _themeContentTextController =
      TextEditingController();
  final TextEditingController _themeDescTextController =
      TextEditingController();
  final FocusNode _themeFocusNode = FocusNode();
  final FocusNode _themeDescFocusNode = FocusNode();
  String? _themeContent;
  String? _themeDesc;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _themeContent = widget.themeContent;
    _themeDesc = widget.themeDesc;
    _themeContentTextController.text = _themeContent ?? '';
    _themeDescTextController.text = _themeDesc ?? '';
  }

  @override
  void dispose() {
    _themeContentTextController.dispose();
    _themeDescTextController.dispose();
    _themeFocusNode.dispose();
    _themeDescFocusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: SafeArea(
        child: Padding(
          padding: EdgeInsets.only(
              top: 32.w,
              left: 16.w,
              right: 16.w,
              bottom: MediaQuery.of(context).viewInsets.bottom + 10.w),
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(20.w),
                  topRight: Radius.circular(20.w),
                ),
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Text(
                        '新增主题',
                        style: TextStyle(
                            color: Color(0xFF393640),
                            fontWeight: FontWeight.w700,
                            fontSize: 22.sp,
                            height: 1),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3F5F8),
                          borderRadius: BorderRadius.circular(8.w),
                        ),
                        child: TextFormField(
                          autofocus: true,
                          controller: _themeContentTextController,
                          focusNode: _themeFocusNode,
                          style: TextStyle(
                            color: const Color(0xFF000000),
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w400,
                            height: 1.2,
                          ),
                          maxLines: 3,
                          minLines: 1,
                          decoration: InputDecoration(
                            border: InputBorder.none,
                            hintText: '主题（必填）',
                            hintStyle: TextStyle(
                              color: const Color(0xFF82818D).withOpacity(0.5),
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w400,
                              height: 1.2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3F5F8),
                          borderRadius: BorderRadius.circular(8.w),
                        ),
                        child: TextFormField(
                          autofocus: true,
                          controller: _themeDescTextController,
                          focusNode: _themeDescFocusNode,
                          style: TextStyle(
                            color: const Color(0xFF000000),
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w400,
                            height: 1.2,
                          ),
                          maxLines: 3,
                          minLines: 1,
                          decoration: InputDecoration(
                            border: InputBorder.none,
                            hintText: '定义主题的详细描述',
                            hintStyle: TextStyle(
                              color: const Color(0xFF82818D).withOpacity(0.5),
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w400,
                              height: 1.2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Flexible(
                          child: PrimaryBtn(
                            margin: const EdgeInsets.only(bottom: 16.0),
                            text: '开始合并整理（相关备忘录）',
                            loading: _submitting,
                            disabled: _submitting,
                            onPressed: () async {
                              final theme =
                                  _themeContentTextController.text.trim();
                              final description =
                                  _themeDescTextController.text.trim();
                              if (theme.isEmpty) {
                                Toast.info('请输入主题内容');
                                return;
                              }
                              if (theme.length > 120 ||
                                  description.length > 1000) {
                                Toast.info('主题或描述内容过长');
                                return;
                              }
                              setState(() => _submitting = true);
                              try {
                                await widget.onThemeCreated
                                    ?.call(theme, description);
                                if (!mounted) return;
                                setState(() {
                                  _themeContent = theme;
                                  _themeDesc = description;
                                });
                                Navigator.pop(context);
                              } catch (_) {
                                if (!mounted) return;
                                Toast.error('主题创建失败，请重试');
                              } finally {
                                if (mounted) setState(() => _submitting = false);
                              }
                            },
                          ),
                        ),
                      ],
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Flexible(
                          child: SecondaryBtn(
                            margin: const EdgeInsets.only(bottom: 16.0),
                            text: '取消',
                            onPressed: () {
                              Navigator.pop(context);
                            },
                          ),
                        ),
                      ],
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
