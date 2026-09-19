import 'package:ainote_app/app/utils/date_format.dart';

import '../data/models/todo_model.dart';
import 'api_urls.dart';
import 'my_dio.dart';

class TodoApi {
  static createTodo(TodoModel todo) async {
    await MyDio.postJSON(
      ApiUrls.todoCreate,
      data: {
        "content": todo.content,
        "description": todo.description,
        "scheduledAt": parseToApiDateString(todo.scheduledAt)
      },
    );
  }

  static updateTodo(TodoModel todo) async {
    await MyDio.postJSON(
      ApiUrls.todoUpdate,
      data: {
        "id": todo.id,
        "content": todo.content,
        "description": todo.description,
        "scheduledAt": parseToApiDateString(todo.scheduledAt),
        "isDone": todo.done,
        "isNeedNotify": false
      },
    );
  }

  static deleteTodo(String id) async {
    await MyDio.delete(
      ApiUrls.todoDeleteById(id),
    );
  }

  static getTodoList({
    /// 是否上次待办
    bool last = false,
    String keyword = '',
    int page = 1,
    int size = 10,
    bool ascending = false,
  }) async {
    try {
      final resp = await MyDio.postJSON(ApiUrls.todoQueryList, data: {
        "keyword": keyword,
        "page": page,
        "size": size,
        "ascending": false,
        "sortBy": last ? 'updated_at' : "id"
      });
      return resp.data['data'];
    } catch (_) {}
  }

  static getAllTodo(String title) async {
    final resp = await MyDio.postJSON(ApiUrls.todoQueryAll, data: {
      "keyword": "",
      "page": 1,
      "size": 10,
      "ascending": false,
    });
  }
}
