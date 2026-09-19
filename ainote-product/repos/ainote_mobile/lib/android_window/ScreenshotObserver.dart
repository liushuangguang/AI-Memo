import 'package:flutter/services.dart';

class ScreenshotObserver {
  static const MethodChannel _channel =
  MethodChannel('screenshot_observer');

  static Future<void> startObserving() async {
    await _channel.invokeMethod('startObserving');
  }

  static Future<void> stopObserving() async {
    await _channel.invokeMethod('stopObserving');
  }

  static void setOnScreenshotListener(Function listener) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onScreenshot') {
        listener(call.arguments as String);
      }
    });
  }
}