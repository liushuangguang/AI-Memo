import 'package:ainote_app/app/api/api_urls.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('uses the default or compile-time API base URL', () {
    const expected = String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'https://aifunc.top',
    );

    expect(ApiUrls.baseUrl, expected);
  });
}
