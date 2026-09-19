import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/modules/note/note_list1/views/widgets/note_more_list.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:get/get.dart';

import '../../note_edit/views/widgets/note_type_dropdown.dart';
import '../bindings/note_list_repository.dart';
import '../controllers/note_list_controller.dart';
import '../bindings/ai_theme_list_repository.dart';
import 'widgets/ai_theme_list.dart';

class NoteListView extends GetView<NoteListController> {
  const NoteListView({super.key});

  @override
  Widget build(BuildContext context) {
    final noteListRepositoryHand =
        NoteListRepositoryHand(controller.noteTypeModel.value.id);

    return DefaultTabController(
      length: controller.tabs.length,
      child: Scaffold(
        appBar: AppBar(
          leadingWidth: 0,
          leading: Container(),
          title: TabBar(
            controller: controller.tabController,
            tabAlignment: TabAlignment.start,
            dividerColor: Colors.transparent,
            labelStyle: TextStyle(
                fontSize: 18.w,
                color: MyColors.primaryColor,
                fontWeight: FontWeight.bold),
            unselectedLabelStyle:
                TextStyle(fontSize: 16.w, color: MyColors.thirdColor),
            indicator: UnderlineTabIndicator(
              borderSide: BorderSide.none, // 移除下划线
            ),
            isScrollable: true,
            tabs: [
              Tab(
                child: GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: () {},
                  child: Obx(() {
                    return Container(
                        constraints: const BoxConstraints(minWidth: 150),
                        height: double.infinity,
                        child: NoteTagDropdown(
                          dropdownBuilder: (context, selectedItem) =>
                              GestureDetector(
                            behavior: HitTestBehavior.translucent,
                            onTap: null,
                            child: Row(
                              children: [
                                Text(controller.noteTypeModel.value.name),
                                Icon(Icons.arrow_drop_down),
                              ],
                            ),
                          ),
                          disabled: false,
                          list: controller.noteTypeList,
                          value: controller.noteTypeModel.value.id,
                          onChang: (value) {
                            controller.updateNoteType(value);
                            noteListRepositoryHand.reloadByNoteTypeId(value.id);
                          },
                        ));
                  }),
                ),
              ),
            ],
            onTap: (i) => controller.handleTabChange,
          ),
          actions: [
            TextButton.icon(
              onPressed: () => Get.to(() => Scaffold(
                appBar: AppBar(title: const Text('主题合并')),
                body: AiThemeList(aiThemeListRepository: AIThemeListRepositoryAll()),
              )),
              icon: const Icon(Icons.merge_type),
              label: const Text('主题合并'),
            ),
          ],
        ),
        body: TabBarView(controller: controller.tabController, children: [
          NoteMoreList(
            noteRepository: noteListRepositoryHand,
          ),
        ]),
        floatingActionButtonLocation: FloatingActionButtonLocation.endDocked,
        floatingActionButton: Container(
          height: 50.w,
          width: 50.w,
          margin: EdgeInsets.only(bottom: 10.w),
          child: FloatingActionButton(
            backgroundColor: MyColors.colorBlue,
            onPressed: () => Get.back(id: 1),
            child: const Icon(
              Icons.keyboard_double_arrow_down,
              color: MyColors.colorWhite,
            )
                .animate(
                  onPlay: (controller) => controller.repeat(),
                )
                .moveY(
                  duration: const Duration(milliseconds: 1000),
                  begin: -3,
                  end: 3,
                ),
          ),
        ),
      ),
    );
  }
}
