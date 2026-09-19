
import 'package:flutter_keychain/flutter_keychain.dart';
import 'package:uuid/uuid.dart';

class KeyChainService {
  Future<String?> getDeviceId() async {
    final deviceId = await FlutterKeychain.get(key: "device_id");
    if (deviceId == null) {
      final id = const Uuid().v4();
      await setDeviceId(id);
    }
    return await FlutterKeychain.get(key: "device_id");
  }

  Future<void> setDeviceId(String deviceId) async {
    await FlutterKeychain.put(key: "device_id", value: deviceId);
  }
}