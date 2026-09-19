import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:loading_more_list/loading_more_list.dart';

import '../../../../data/models/todo_model.dart';
import '../../../../widgets/dot_grid_container.dart';
import '../../../../widgets/todo_list.dart';
import 'todo_more_list.dart';

class CardWrapper extends StatelessWidget {
  final String title;
  final Widget child;
  final double? height;

  final String? action;
  final Function()? onTap;
  final Function()? onActionTap;

  const CardWrapper({
    super.key,
    required this.title,
    required this.child,
    this.action,
    this.onTap,
    this.onActionTap,
    this.height,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.translucent,
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(title,
                    style: TextStyle(
                        fontSize: 16.sp,
                        fontWeight: FontWeight.w700,
                        color: MyColors.colorBrown)),
                TextButton(
                  onPressed: onActionTap,
                  child: Text(action ?? '',
                      style: TextStyle(
                          fontSize: 16.sp,
                          fontWeight: FontWeight.w500,
                          color: MyColors.colorBlue)),
                )
              ],
            ),
            DotGridContainer(
                centered: false,
                constraints: BoxConstraints(
                  minHeight: 160.h,
                  maxHeight: height ?? 180.w,
                ),
                child: child),
          ],
        ));
  }
}
