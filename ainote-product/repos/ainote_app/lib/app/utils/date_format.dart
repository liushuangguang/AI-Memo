import 'package:intl/intl.dart';

class DateFormatStr {
  static const String normal = 'yyyy/MM/dd HH:mm:ss';

  static const String yearDate = 'yyyy/MM/dd';
  static const String dateTime = 'MM/dd HH:mm:ss';
  static const String date = 'MM-dd';
  static const String time = 'HH:mm:ss';
  static const String noYearNoSecond = 'MM/dd HH:mm';
  static const String noSecond = 'yyyy/MM/dd HH:mm';

  // 给API用的
  static const String api = 'yyyy-MM-ddTHH:mm:ss';
}

DateTime? parseDateString(String? dateString) {
  if (dateString == null || dateString.isEmpty) {
    return null;
  }
  try {
    return DateTime.parse(dateString);
  } catch (e) {
    return _parseDateWithRegex(dateString);
  }
}

DateTime? _parseDateWithRegex(String dateString) {
  final regex = RegExp(
      r'(\d{4})[-/年]?(\d{1,2})?[-/月日]?(\d{0,2})[Tt\s]*(\d{1,2})?:?(\d{1,2})?:?(\d{1,2})?[.:]?(\d+)?');
  final match = regex.firstMatch(dateString);

  if (match == null) {
    return null;
  }

  final year = int.parse(match.group(1) ?? '0');
  final month = int.parse(match.group(2) ?? '0');
  final day = int.parse(match.group(3) ?? '0');
  final hour = match.group(4) != null ? int.parse(match.group(4) ?? '0') : 0;
  final minute = match.group(5) != null ? int.parse(match.group(5) ?? '0') : 0;
  final second = match.group(6) != null ? int.parse(match.group(6) ?? '0') : 0;

  return DateTime(year, month, day, hour, minute, second);
}

String formatDate(DateTime date, [str = DateFormatStr.noSecond]) {
  try {
    final f = DateFormat(str);
    return f.format(date);
  } catch (e) {
    return date.toString();
  }
}

String twoDigits(int n) => n.toString().padLeft(2, '0');

String formatDuration(Duration duration) {
  String hours = twoDigits(duration.inHours);
  String minutes = twoDigits(duration.inMinutes.remainder(60));
  String seconds = twoDigits(duration.inSeconds.remainder(60));
  return '$hours:$minutes:$seconds';
}

String formatTimeStamp(int timestamp) {
  DateTime date = DateTime.fromMillisecondsSinceEpoch(timestamp);
  return formatDate(date, DateFormatStr.noSecond);
}

/// 将时间转换为友好的时间
String humanTime(DateTime? ts, {bool withTime = false}) {
  if (ts == null || ts.millisecondsSinceEpoch == 0) {
    return '';
  }

  var now = DateTime.now();
  var diff = now.difference(ts);
  if (diff.inDays > 0) {
    if (withTime) {
      return DateFormat(DateFormatStr.noSecond).format(ts.toLocal());
    }
    return DateFormat(DateFormatStr.yearDate).format(ts.toLocal());
  }

  if (diff.inHours > 0) {
    return '${diff.inHours}小时前';
  }

  if (diff.inMinutes > 0) {
    return '${diff.inMinutes}分钟前';
  }

  return '刚刚';
}

/// return '2023-01-01T00:00:00'
String parseToApiDateString(String? dateString) {
  if (parseDateString(dateString) == null) {
    return dateString ?? '';
  }
  return formatDate(parseDateString(dateString)!, DateFormatStr.api);
}

/// return '2023/01/01 00:00:00'
String parseApiDateString(String? dateString,
    [format = DateFormatStr.noSecond]) {
  if (parseDateString(dateString) == null) {
    return dateString ?? '';
  }
  return formatDate(parseDateString(dateString)!, format);
}
