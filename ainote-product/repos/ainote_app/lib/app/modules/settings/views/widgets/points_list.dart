import 'package:ainote_app/app/utils/date_format.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../data/models/vip_points_model.dart';

class PointsList extends StatelessWidget {
  final List<PointsHistory?>? pointsList;
  final bool? canScroll;

  const PointsList({super.key, this.pointsList, this.canScroll = true});

  @override
  Widget build(BuildContext context) {
    if (pointsList?.isEmpty == true) {
      return Padding(
        padding: const EdgeInsets.all(8.0),
        child: Align(
          alignment: Alignment.center,
          child: Text('暂无任何记录',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w400)),
        ),
      );
    }
    return ListView.separated(
      shrinkWrap: true,
      physics: canScroll == true ? null : NeverScrollableScrollPhysics(),
      itemBuilder: (context, index) {
        final item = pointsList?[index];
        String changeMethodText = '-';
        switch (item?.changeMethod) {
          case 'ADD':
            changeMethodText = '充值积分';
            break;
          case 'CONSUMER':
            changeMethodText = '消耗积分';
            break;
          default:
            changeMethodText = item?.changeMethod ?? '-';
        }
        return Row(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // 小圆圈icon
            Icon(Icons.circle_outlined, size: 12.w, color: Colors.black),
            SizedBox(width: 8.w),
            Text(
                item?.createdAt == null
                    ? '-'
                    : formatTimeStamp(item!.createdAt!),
                style: TextStyle(
                  fontSize: 14.sp,
                  fontWeight: FontWeight.w400,
                )),
            Padding(
              padding: EdgeInsets.only(left: 32.w),
              child: Text(changeMethodText,
                  textAlign: TextAlign.start,
                  style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                  )),
            ),
            Spacer(),
            Text('${item?.changePoint.toString() ?? 0}积分',
                style: TextStyle(
                    fontSize: 14.sp,
                    fontWeight: FontWeight.w400,
                    color: Colors.black)),
          ],
        );
      },
      separatorBuilder: (BuildContext context, int index) {
        return 16.verticalSpace;
      },
      itemCount: pointsList?.length ?? 0,
    );
  }
}
