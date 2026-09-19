import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:loading_more_list/loading_more_list.dart';

int? normalizeNoteTypeFilter(int? noteTypeId) =>
    noteTypeId != null && noteTypeId < 0 ? null : noteTypeId;

class NoteListBaseRepository extends LoadingMoreBase<NoteModel> {
  NoteListBaseRepository({this.maxLength = 500});

  int _pageIndex = 1;
  bool _hasMore = true;
  final int _pageSize = 10;

  @override
  bool get hasMore => _hasMore && length < maxLength;
  final int maxLength;

  @override
  Future<bool> refresh([bool notifyStateChanged = true]) async {
    _hasMore = true;
    _pageIndex = 1;
    var result = await super.refresh(notifyStateChanged);
    return result;
  }

  Future<bool> fetchData({int? noteType, int? dimension}) async {
    bool isSuccess = false;
    try {
      var list = await NoteApi.getNoteList(
              page: _pageIndex,
              size: _pageSize,
              sortBy: 'updatedAt',
              noteType: noteType,
              dimension: dimension) ??
          [];
      if (list?.isEmpty == true) {
        _hasMore = false;
      }
      if (_pageIndex == 1) {
        clear();
      }
      for (var item in list) {
        add(item);
      }
      _hasMore = list.length == _pageSize;
      _pageIndex++;
      isSuccess = true;
    } catch (exception, stack) {
      isSuccess = false;
      _hasMore = false;
    }
    return isSuccess;
  }

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData();
  }
}

// 全部
class NoteListRepositoryAll extends NoteListBaseRepository {
  NoteListRepositoryAll() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData();
  }
}

// 主题
class NoteListRepositoryTheme extends NoteListBaseRepository {
  NoteListRepositoryTheme() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData(dimension: 1, noteType: null);
  }
}

// 手动分类
class NoteListRepositoryHand extends NoteListBaseRepository {
  int? noteTypeId;
  NoteListRepositoryHand(this.noteTypeId) : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData(noteType: normalizeNoteTypeFilter(noteTypeId));
  }

  void reloadByNoteTypeId(int? noteTypeId) {
    this.noteTypeId = noteTypeId;
    refresh();
  }
}
