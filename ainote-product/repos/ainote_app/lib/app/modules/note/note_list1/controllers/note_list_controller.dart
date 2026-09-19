import 'dart:async';

import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/modules/home/controllers/home_controller.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

const int allNoteTypesId = -1;

List<NoteTypeModel> noteTypeOptions(Iterable<NoteTypeModel> source) {
  return [
    NoteTypeModel(id: allNoteTypesId, name: '全部'),
    ...source.where(
      (item) => item.id != allNoteTypesId && item.name.trim() != '全部',
    ),
  ];
}

class NoteListController extends GetxController
    with GetSingleTickerProviderStateMixin {
  late HomeController homeController;

  late TabController tabController;
  Worker? _noteTypeWorker;

  final RxList<NoteModel> noteList = <NoteModel>[].obs;

  final RxInt totalPage = 0.obs;
  final RxInt currentPage = 1.obs;

  final RxBool isLoading = false.obs;

  final RxBool isLoadMore = false.obs;

  final RxBool isRefresh = false.obs;

  final RxList<NoteTypeModel> noteTypeList =
      noteTypeOptions(<NoteTypeModel>[]).obs;

  final Rx<NoteTypeModel> noteTypeModel =
      NoteTypeModel(id: allNoteTypesId, name: '全部').obs;
  RxInt tabIndex = (0).obs;

  RxList<Widget> tabs = <Widget>[].obs;

  void handleTabChange() {
    tabIndex.value = tabController.index;
  }

  void deleteNote(String id) async {
    await NoteApi.deleteNote(id);
    // onRefresh();
  }

  void updateNoteType(NoteTypeModel? value) {
    noteTypeModel.value = value!;
    update();
  }

  Future<void> updateNoteTypes(
    Future<List<NoteTypeModel>> noteTypesFuture,
  ) async {
    try {
      final noteTypes = await noteTypesFuture;
      noteTypeList.assignAll(noteTypeOptions(noteTypes));
    } catch (_) {
      // Keep the immediately available all-items filter on transient failure.
    }
  }

  @override
  void onInit() {
    super.onInit();
    homeController = Get.find<HomeController>();
    tabs.value = [
      Tab(text: "备忘录"),
    ];
    tabController = TabController(
      length: tabs.length,
      initialIndex: 0,
      vsync: this,
    );
    noteTypeList.assignAll(noteTypeOptions(homeController.noteTypeList));
    _noteTypeWorker = ever<List<NoteTypeModel>>(
      homeController.noteTypeList,
      (types) => noteTypeList.assignAll(noteTypeOptions(types)),
    );
    unawaited(homeController.getNoteTypeList());
    tabController.addListener(handleTabChange);
  }

  @override
  void onClose() {
    tabController.removeListener(handleTabChange);
    tabController.dispose();
    _noteTypeWorker?.dispose();
    super.onClose();
  }
}
