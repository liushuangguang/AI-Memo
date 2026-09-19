import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:ainote_app/app/data/models/note_type_model.dart';
import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/modules/home/controllers/todo_repository.dart';
import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/note.dart';
import 'package:ainote_app/app/widgets/quill/my_quill_controller.dart';
import 'package:get/get.dart';

import '../../../api/todo.dart';

typedef NoteTypesLoader = Future<List<NoteTypeModel>> Function();

class HomeController extends GetxController {
  HomeController({NoteTypesLoader? noteTypesLoader})
      : _noteTypesLoader = noteTypesLoader ?? NoteApi.getNoteTypeList;

  final NoteTypesLoader _noteTypesLoader;
  RxBool isEmpty = false.obs;

  final quillTag = 'home';

  late MyQuillController quillLogic;
  late TodoRepository todoRepository;

  final note = NoteModel.empty().obs;

  final RxList<NoteTypeModel> noteTypeList = <NoteTypeModel>[].obs;
  final RxBool noteTypesLoading = true.obs;
  Future<List<NoteTypeModel>>? _noteTypeListFuture;

  @override
  void onInit() async {
    super.onInit();
    todoRepository = TodoRepository();
    quillLogic = Get.put(MyQuillController(), tag: quillTag);
    quillLogic.setQuillIsReadOnly(true);

    loadData();
    getLatestNote();

    getNoteTypeList();
  }

  @override
  void onClose() {
    todoRepository.dispose();
    quillLogic.quillController.removeListener(toEditNote);
    super.onClose();
  }

  Future<void> refreshPage() async {
    loadData();
    getLatestNote();
  }

  void getTodoList() async {
    await todoRepository.loadData();
  }

  void addTodo(TodoModel todo) async {
    await TodoApi.createTodo(todo);
    loadData();
  }

  void editTodo(TodoModel todo, [bool last = false]) async {
    await TodoApi.updateTodo(todo);
    await todoRepository.refresh();
    loadData();
  }

  void deleteTodo(TodoModel todo, [bool last = false]) async {
    await TodoApi.deleteTodo(todo.id);
    loadData();
  }

  void loadData() async {
    await todoRepository.refresh();
    update();
  }

  void toEditNote() {
    Get.toNamed(Routes.NOTE_EDIT,
        arguments: note.value, parameters: {'mode': NoteEditMode.edit.name});
  }

  void getLatestNote() async {
    List<NoteModel>? list = await NoteApi.getNoteList(
      page: 1,
      size: 1,
      sortBy: 'updatedAt',
    );

    if (list == null || list.isEmpty) {
      isEmpty.value = true;
      return;
    }
    note.value = list[0];
    setNote(list[0]);
  }

  void setNote(NoteModel item, [bool isUpdate = false]) async {
    note.value = item;
    quillLogic.quillController.removeListener(toEditNote);
    isEmpty.value = false;
    if (isUpdate) {
      setQuillToTextData(note.value, quillLogic, null);
    } else {
      setTextDataToQuill(note.value, quillLogic, null);
    }

    quillLogic.setQuillIsReadOnly(true);
    quillLogic.quillController.skipRequestKeyboard = true;
    // 监听一下
    quillLogic.quillController.addListener(toEditNote);
    update();
  }

  void updateNote(NoteModel item) {
    setNote(item, true);
  }

  Future<List<NoteTypeModel>> getNoteTypeList() {
    final pending = _noteTypeListFuture;
    if (pending != null) return pending;

    noteTypesLoading.value = true;
    late final Future<List<NoteTypeModel>> tracked;
    tracked = _noteTypesLoader().then((types) {
      noteTypeList.assignAll(types);
      return types;
    }).whenComplete(() {
      noteTypesLoading.value = false;
      if (identical(_noteTypeListFuture, tracked)) {
        _noteTypeListFuture = null;
      }
    });
    _noteTypeListFuture = tracked;
    return tracked;
  }
}
