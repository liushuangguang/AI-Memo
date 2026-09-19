import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class AdLink extends StatelessWidget {
  const AdLink({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: MediaQuery.of(context).size.width - 24.w,
      padding: EdgeInsets.only(bottom: 12.w),
      child: Card(
          shadowColor: MyColors.colorWhite,
          clipBehavior: Clip.hardEdge,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.w),
          ),
          child: Padding(
              padding: EdgeInsets.all(16.w),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                      width: 57.w,
                      height: 57.w,
                      clipBehavior: Clip.hardEdge,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8.w),
                        color: MyColors.adIconBgColor,
                      ),
                      child: Image.network(
                        'https://res.bearbobo.com/img/avatar/avatar41.jpeg',
                        fit: BoxFit.contain,
                        errorBuilder: (context, error, stackTrace) {
                          return Container();
                        },
                      )),
                  12.horizontalSpace,
                  Expanded(
                    child: SizedBox(
                      height: 57.w,
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              '广告广告广告广告广告广告广告广告广告广告广告广告广告广告广告',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              softWrap: true,
                              style: TextStyle(
                                fontSize: 14.sp,
                                color: MyColors.secondaryColor,
                                fontWeight: FontWeight.w500,
                                decoration: TextDecoration.underline,
                              ),
                            ),
                            8.verticalSpace,
                            Text('外链内容外链内容外链内容外链内容外链内容外链内容外链内容外链内容......',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                softWrap: true,
                                style: TextStyle(
                                  color: MyColors.secondaryColor,
                                  fontSize: 12.sp,
                                  fontWeight: FontWeight.w400,
                                ))
                          ]),
                    ),
                  ),
                  Container(
                      width: 24.w,
                      height: 24.w,
                      clipBehavior: Clip.hardEdge,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: MyColors.adIconBgColor,
                      ),
                      child: Image.network(
                        'https://res.bearbobo.com/img/avatar/avatar41.jpeg',
                        fit: BoxFit.contain,
                        errorBuilder: (context, error, stackTrace) {
                          return Container();
                        },
                      )),
                ],
              ))),
    );
  }
}
