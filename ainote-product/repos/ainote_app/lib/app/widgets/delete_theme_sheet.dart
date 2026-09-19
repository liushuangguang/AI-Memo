import 'package:ainote_app/app/widgets/warning_btn.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class DeleteThemeSheet extends StatefulWidget {
  final Function()? onDelete;

  const DeleteThemeSheet({super.key, this.onDelete});

  @override
  State<DeleteThemeSheet> createState() => _DeleteThemeSheetState();
}

class _DeleteThemeSheetState extends State<DeleteThemeSheet> {
  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
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
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Flexible(
                          child: WarningBtn(
                            margin: const EdgeInsets.only(bottom: 16.0),
                            text: '删除',
                            textColor: Colors.white,
                            onPressed: () {
                              widget.onDelete?.call();
                              Navigator.pop(context);
                            },
                          ),
                        ),
                      ],
                    ),
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
