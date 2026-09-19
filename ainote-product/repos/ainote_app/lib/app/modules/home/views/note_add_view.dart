import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/home/views/widgets/add_note_card.dart';
import 'package:ainote_app/app/modules/home/views/widgets/note_card.dart';
import 'package:ainote_app/app/modules/home/views/widgets/note_empty_card.dart';
import 'package:ainote_app/app/modules/home/views/widgets/todo_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import 'package:get/get.dart';

import '../../../config/theme/my_styles.dart';
import '../../../data/constants.dart';
import '../../../routes/app_pages.dart';
import '../../../widgets/web_view_page.dart';
import '../controllers/home_controller.dart';

class NoteAddView extends StatefulWidget {
  const NoteAddView({super.key});

  @override
  State<NoteAddView> createState() => _NoteAddViewState();
}

class _NoteAddViewState extends State<NoteAddView> {
  final logic = Get.find<HomeController>();
  final ScrollController _scrollController = ScrollController();
  bool scrollTipsVisible = true;
  bool isScrollToBottom = false;

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _jumpToNoteList() async {
    await Get.toNamed(
      Routes.NOTE_LIST,
      id: 1,
    );
    _scrollController.jumpTo(0);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: MyStyles.pageXPadding,
        title: Text(
          '生活速记助手',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 24.sp),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: logic.refreshPage,
        child: SingleChildScrollView(
          controller: _scrollController,
          physics: AlwaysScrollableScrollPhysics(),
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: MyStyles.pageXPadding),
            child: Obx(() {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '智能整理你的信息库',
                        style: TextStyle(fontSize: 16.sp),
                      ),
                      GestureDetector(
                        onTap: () {
                          Get.to(() => WebViewPage(
                                url: introUrl,
                              ));
                        },
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              '产品介绍',
                              style: TextStyle(
                                  color: MyColors.colorBlue, fontSize: 16.sp),
                            ),
                            Container(
                              margin: EdgeInsets.only(left: 4.w),
                              padding: EdgeInsets.symmetric(horizontal: 2.w),
                              decoration: BoxDecoration(
                                color: MyColors.colorRed,
                                borderRadius: BorderRadius.circular(2.w),
                              ),
                              child: Text(
                                '福利',
                                style: TextStyle(
                                    fontSize: 16.sp, color: Colors.white),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  16.verticalSpace,
                  AddNoteCard(),
                  24.verticalSpace,
                  TodoCard(),
                  if (logic.isEmpty.value) NoteEmptyCard(),
                  Opacity(
                    opacity: logic.isEmpty.value ? 0 : 1,
                    child: Column(children: [
                      24.verticalSpace,
                      NoteCard(),
                      80.verticalSpace,
                    ]),
                  )
                ],
              );
            }),
          ),
        ),
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
      floatingActionButton: Obx(() {
        return Visibility(
          visible: !logic.isEmpty.value,
          child: GestureDetector(
            onVerticalDragDown: (details) {
              _jumpToNoteList();
            },
            child: Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(vertical: 12.h),
              decoration: BoxDecoration(
                boxShadow: [
                  BoxShadow(
                    color: Colors.grey.withOpacity(0),
                    spreadRadius: 2,
                    blurRadius: 0,
                    offset: Offset(0, 2),
                  ),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    '上滑进入备忘录列表',
                    style: TextStyle(color: Color(0xFF3A51FF), fontSize: 15.sp),
                  ),
                  Icon(
                    Icons.keyboard_double_arrow_up,
                    color: Color(0xFF3A51FF),
                  )
                      .animate(
                        onPlay: (controller) => controller.repeat(),
                      )
                      .moveY(
                          duration: const Duration(milliseconds: 1000),
                          begin: 3,
                          end: -3),
                ],
              ),
            ),
          ),
        );
      }),
    );
  }
}
