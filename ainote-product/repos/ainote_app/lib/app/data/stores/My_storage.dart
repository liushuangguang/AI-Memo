import 'package:get/get.dart';
import 'package:get_storage/get_storage.dart';

class MyStorage {
  // prevent making instance
  MyStorage._();

  // get storage
  static GetStorage? _storage;
  static bool _initialized = false;

  static Future<void> init() async {
    await GetStorage.init();
    _storage = GetStorage();
    _initialized = true;
  }

  // STORING KEY
  static const String _phoneNumberKey = 'phone_number';
  static const String _userIdKey = 'user_id';
  static const String _authTokenKey = 'auth_token';
  static const String _authTokenInvalidatedKey = 'auth_token_invalidated';
  static const String _lightThemeKey = 'is_theme_light';
  static const String _currentLocalKey = 'current_local';
  static const String _deviceIdKey = 'device_id_fallback';

  static bool get isInitialized => _initialized;

  static bool getAutoOrganizeEnabled() =>
      _read<bool>('auto_organize_enabled') ?? false;

  static Future<void> setAutoOrganizeEnabled(bool enabled) async {
    final storage = _storage;
    if (!_initialized || storage == null) {
      throw StateError('Settings storage is unavailable');
    }
    await storage.write('auto_organize_enabled', enabled);
  }

  static T? _read<T>(String key) {
    if (!_initialized) return null;
    try {
      return _storage?.read<T>(key);
    } catch (_) {
      return null;
    }
  }

  static String? getDeviceId() => _read<String>(_deviceIdKey);

  static Future<void> setDeviceId(String deviceId) async {
    final storage = _storage;
    if (!_initialized || storage == null) {
      throw StateError('GetStorage is unavailable');
    }
    await storage.write(_deviceIdKey, deviceId);
  }

  static void setPhoneNumber(String phoneNumber) =>
      _storage?.write(_phoneNumberKey, phoneNumber);

  static String? getPhoneNumber() => _read<String>(_phoneNumberKey);

  static Future<void> setAuthToken(String token) async {
    final storage = _storage;
    if (!_initialized || storage == null) {
      throw StateError('GetStorage is unavailable');
    }
    await storage.write(_authTokenKey, token);
    await storage.remove(_authTokenInvalidatedKey);
  }

  static Future<void> clear() async => await _storage?.erase();

  static Future<void> deleteAuthToken() async {
    final storage = _storage;
    if (!_initialized || storage == null) return;
    // Persist invalidation first so an interrupted token removal cannot make a
    // server-rejected session eligible for ROOT on the next cold start.
    await storage.write(_authTokenInvalidatedKey, true);
    await storage.remove(_authTokenKey);
  }

  static String? getAuthToken() => _read<bool>(_authTokenInvalidatedKey) == true
      ? null
      : _read<String>(_authTokenKey);

  static void setThemeIsLight(bool lightTheme) =>
      _storage?.write(_lightThemeKey, lightTheme);

  static bool getThemeIsLight() =>
      _read<bool>(_lightThemeKey) ?? Get.isDarkMode != true;

  static void setCurrentLanguage(String languageCode) =>
      _storage?.write(_currentLocalKey, languageCode);

  static String? getCurrentLanguage() => _read<String>(_currentLocalKey);
}
