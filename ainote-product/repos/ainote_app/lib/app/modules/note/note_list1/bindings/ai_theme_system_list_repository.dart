import 'package:ainote_app/app/api/note.dart';
import 'package:ainote_app/app/data/models/note_model.dart';
import 'package:loading_more_list/loading_more_list.dart';

class AIThemeSystemListRepository extends LoadingMoreBase<NoteModel> {
  AIThemeSystemListRepository({this.maxLength = 500});

  int _pageIndex = 1;
  bool _hasMore = true;
  final int _pageSize = 10;

  @override
  bool get hasMore => _hasMore && length < maxLength;
  final int maxLength;

  @override
  Future<bool> refresh([bool notifyStateChanged = true]) async {
    if (_pageIndex != 1) {
      _hasMore = true;
      _pageIndex = 1;
      var result = await super.refresh(notifyStateChanged);
      return result;
    }
    return true;
  }

  Future<bool> fetchData({int? noteType, int? dimension}) async {
    bool isSuccess = false;
    try {
      if (_pageIndex == 1) {
        var list = [
          NoteModel(
            id: '1',
            title: '测试笔记1',
            content: '这是测试内容1',
          ),
          NoteModel(
            id: '2',
            title: '测试笔记2',
            content: '这是测试内容2',
          ),
          NoteModel(
            id: '3',
            title: '测试笔记3',
            content: '这是测试内容3',
          ),
        ];

        clear();
        for (var item in list) {
          add(item);
        }
      }

      _hasMore = false;
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
class AIThemeSystemListRepositoryAll extends AIThemeSystemListRepository {
  AIThemeSystemListRepositoryAll() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData();
  }
}

// 主题
class AIThemeSystemListRepositoryTheme extends AIThemeSystemListRepository {
  AIThemeSystemListRepositoryTheme() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData(dimension: 1, noteType: null);
  }
}

// 手动分类
class AIThemeSystemListRepositoryHand extends AIThemeSystemListRepository {
  int? noteTypeId;
  AIThemeSystemListRepositoryHand(this.noteTypeId) : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    var noteTypeId =
        this.noteTypeId is int && this.noteTypeId! < 0 ? null : this.noteTypeId;
    return fetchData(noteType: noteTypeId);
  }

  void reloadByNoteTypeId(int? noteTypeId) {
    this.noteTypeId = noteTypeId;
    refresh();
  }
}
