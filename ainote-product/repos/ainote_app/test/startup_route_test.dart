import 'package:ainote_app/app/config/app/app_config.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('guest mode always starts at root without a stored token', () {
    expect(
      resolveInitialRoute(guestMode: true, storedToken: null),
      Routes.ROOT,
    );
  });

  test('authenticated mode starts at login without a stored token', () {
    expect(
      resolveInitialRoute(guestMode: false, storedToken: null),
      Routes.LOGIN,
    );
  });

  test('authenticated mode rejects malformed stored tokens', () {
    expect(
      resolveInitialRoute(
        guestMode: false,
        storedToken: 'Mobile malformed-token',
      ),
      Routes.LOGIN,
    );
  });

  test('authenticated mode starts at root with a valid Mobile token', () {
    expect(
      resolveInitialRoute(
        guestMode: false,
        storedToken: '  Mobilepersisted-token  ',
      ),
      Routes.ROOT,
    );
  });
}
