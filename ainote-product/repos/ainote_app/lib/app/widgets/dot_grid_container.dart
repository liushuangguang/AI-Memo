import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../generated/assets.dart';

class DotGridContainer extends StatelessWidget {
  late bool? centered;
  late BoxConstraints? constraints;
  late EdgeInsetsGeometry? padding;
  late EdgeInsetsGeometry? margin;
  final Widget child;
  final double? height;

  DotGridContainer(
      {super.key,
      required this.child,
      this.centered,
      this.constraints,
      this.padding,
      this.margin,
      this.height}) {
    centered = centered ?? true;
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: height,
      constraints: constraints,
      padding: padding,
      margin: margin,
      decoration: BoxDecoration(
          color: Color(0xFFFDFDF5),
          borderRadius: BorderRadius.circular(12.w),
          image: DecorationImage(
            image: AssetImage(Assets.imagesDotGrid),
            fit: BoxFit.scaleDown,
            repeat: ImageRepeat.repeat,
          )),
      child: centered == true ? Center(child: child) : child,
    );
  }
}
