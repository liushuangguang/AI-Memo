import 'package:ainote_app/app/config/theme/my_theme.dart';

import '../../api/my_dio.dart';
import '../../data/stores/My_storage.dart';
import '../../data/stores/my_secure_storage.dart';
import '../../routes/app_pages.dart';

String resolveInitialRoute({
  required bool guestMode,
  String? storedToken,
}) {
  if (guestMode) return Routes.ROOT;
  final token = storedToken?.trim();
  return token != null && RegExp(r'^Mobile[^\s]+$').hasMatch(token)
      ? Routes.ROOT
      : Routes.LOGIN;
}

class AppConfig {
  AppConfig._();

  /// Login is intentionally hidden in this release, so guest mode is the
  /// deterministic default. Re-enable auth with `--dart-define=GUEST_MODE=false`.
  static const bool guestMode =
      bool.fromEnvironment('GUEST_MODE', defaultValue: true);

  static String initialRoute = resolveInitialRoute(
    guestMode: guestMode,
    storedToken: null,
  );

  static Future<void>? _initFuture;
  static const _stageTimeout = Duration(seconds: 3);

  static Future<void> init() {
    final pending = _initFuture;
    if (pending != null) return pending;

    final future = _initialize();
    _initFuture = future;
    return future.whenComplete(() {
      if (identical(_initFuture, future)) _initFuture = null;
    });
  }

  static Future<void> _initialize() async {
    // Device storage initializes GetStorage and secure storage independently,
    // so either backend can preserve the installation identity on its own.
    await MySecureStorage.init();
    // set system theme
    try {
      await MyTheme.setSystemUI().timeout(_stageTimeout);
    } catch (_) {
      // System UI configuration is best effort and must not block startup.
    }

    await MyDio.init(guestMode: guestMode).timeout(_stageTimeout);
    // StartupGate does not expose the app (and therefore Root requests) until
    // both durable storage and authorization headers are ready.
    initialRoute = resolveInitialRoute(
      guestMode: guestMode,
      storedToken: MyStorage.getAuthToken(),
    );
  }
}
