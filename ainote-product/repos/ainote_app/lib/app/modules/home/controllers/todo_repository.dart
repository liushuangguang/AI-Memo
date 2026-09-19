import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:loading_more_list/loading_more_list.dart';

import '../../../api/todo.dart';

class TodoRepository extends LoadingMoreBase<TodoModel> {
  TodoRepository({this.maxLength = 100});

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

  Future<bool> fetchData([last = false]) async {
    bool isSuccess = false;
    try {
      var resp = await TodoApi.getTodoList(
          last: last, page: _pageIndex, size: _pageSize);
      var data = resp?['content'] ?? [];
      int totalPages = resp?['totalPages'] ?? 0;
      if (data.isEmpty) {
        _hasMore = false;
      }
      if (_pageIndex == 1) {
        clear();
      }
      for (var item in data) {
        add(TodoModel.fromJson(item));
      }
      _hasMore = true;
      _pageIndex++;
      if (_pageIndex > totalPages) {
        _hasMore = false;
      }
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

class LastTodoRepository extends TodoRepository {
  @override
  Future<bool> fetchData([last = false]) async {
    return super.fetchData(true);
  }
}
