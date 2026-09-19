import 'package:shared_preferences/shared_preferences.dart';

class AppLocalSettings {
  AppLocalSettings._privateConstructor();

  static final AppLocalSettings instance = AppLocalSettings
      ._privateConstructor();

  SharedPreferences? _prefs;

  Future<void> init() async {
    try {
      _prefs = await SharedPreferences.getInstance();
    } catch(e) {
      print("SharedPreferences init error: $e");
    }
  }

  bool getAIGenerateTipsDisabled() {
    return _prefs?.getBool("ai_generate_tips_disabled") ?? false;
  }

  Future<void> setAIGenerateTipsDisabled(bool value) async {
    await _prefs?.setBool("ai_generate_tips_disabled", value);
  }
}