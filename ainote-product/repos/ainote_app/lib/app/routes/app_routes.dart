part of 'app_pages.dart';

abstract class Routes {
  Routes._();

  // 根路由
  static const ROOT = _Paths.ROOT;

  // 备忘
  static const HOME = _Paths.HOME;

  // 待办
  static const TODO = _Paths.TODO;

  // 我的
  static const SETTINGS = _Paths.SETTINGS;

  // 登录
  static const LOGIN = _Paths.LOGIN;

  // 新增备忘
  static const NOTE_ADD = _Paths.NOTE_ADD;

  // 备忘列表
  static const NOTE_LIST = _Paths.NOTE_LIST;

  // 备忘详情
  static const NOTE_DETAIL = _Paths.NOTE_DETAIL;

  // 编辑备忘
  static const NOTE_EDIT = _Paths.NOTE_EDIT;

  // 智能整理
  static const SMART_ORGANIZE = _Paths.SMART_ORGANIZE;

  // 相关备忘
  static const NOTE_RELEVANT = _Paths.NOTE_RELEVANT;

  // 内容辅助
  static const CONTENT_ASSIST = _Paths.CONTENT_ASSIST;
  static const VIP = _Paths.VIP;
}

abstract class _Paths {
  _Paths._();

  static const ROOT = '/';
  static const HOME = '/home';
  static const TODO = '/todo';
  static const SETTINGS = '/settings';
  static const LOGIN = '/login';
  static const NOTE_ADD = '/note-add';
  static const NOTE_LIST = '/note-list';
  static const NOTE_DETAIL = '/note-detail';
  static const NOTE_EDIT = '/note-edit';
  static const SMART_ORGANIZE = '/smart-organize';
  static const NOTE_RELEVANT = '/note-relevant';
  static const CONTENT_ASSIST = '/content-assist';
  static const VIP = '/vip';
}
