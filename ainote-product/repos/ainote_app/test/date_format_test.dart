import 'package:ainote_app/app/utils/date_format.dart';
import 'package:test/test.dart';

void main() {
  test('parseDateString valid input', () {
    DateTime? result = parseDateString('2023-01-01T12:00:00');
    expect(result, isNotNull);
    expect(result!.year, equals(2023));
  });

  test('parseDateString invalid input', () {
    DateTime? result = parseDateString('invalid date string');
    expect(result, isNull);
  });

  test('formatDate', () {
    DateTime date = DateTime(2023, 1, 1, 12, 0, 0);
    String result = formatDate(date, DateFormatStr.yearDate);
    expect(result, equals('2023/01/01'));
  });

  test('formatDuration', () {
    Duration duration = Duration(hours: 1, minutes: 1, seconds: 1);
    String result = formatDuration(duration);
    expect(result, equals('01:01:01'));
  });

  test('humanTime with time', () {
    DateTime now = DateTime.now();
    DateTime past = now.subtract(Duration(hours: 2));
    String result = humanTime(past, withTime: true);
    expect(result, equals('2小时前'));
  });

  test('humanTime without time', () {
    DateTime? dateTime = parseDateString('2024/10/25 10:10:10');
    DateTime past = dateTime!.subtract(Duration(days: 1));
    String result = humanTime(past, withTime: false);
    expect(result, equals('2024/10/24'));
  });

  test('format DateTime to api string', () {
    DateTime now = DateTime.now();
    String result = formatDate(now, DateFormatStr.api);
    print(result);
    expect(result, isNotNull);
    expect(result.contains('T'), true);
  });
}
