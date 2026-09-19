import 'package:ainote_app/app/api/note_theme.dart';
import 'package:ainote_app/app/data/models/note_theme_model.dart';
import 'package:loading_more_list/loading_more_list.dart';

class AIThemeListRepository extends LoadingMoreBase<NoteThemeModel> {
  AIThemeListRepository({this.maxLength = 500});

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
    return super.refresh(notifyStateChanged);
  }

  Future<bool> fetchData({int? noteType, int? dimension}) async {
    bool isSuccess = false;
    try {
      final list = await NoteThemeApi.listThemes(
        page: _pageIndex,
        size: _pageSize,
      );
      if (_pageIndex == 1) {
        clear();
      }
      addAll(list);

      _hasMore = list.length == _pageSize && length < maxLength;
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
class AIThemeListRepositoryAll extends AIThemeListRepository {
  AIThemeListRepositoryAll() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData();
  }
}

// 主题
class AIThemeListRepositoryTheme extends AIThemeListRepository {
  AIThemeListRepositoryTheme() : super(maxLength: 100);

  @override
  Future<bool> loadData([bool isLoadMoreAction = false]) async {
    return fetchData(dimension: 1, noteType: null);
  }
}

// 手动分类
class AIThemeListRepositoryHand extends AIThemeListRepository {
  int? noteTypeId;
  AIThemeListRepositoryHand(this.noteTypeId) : super(maxLength: 100);

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
