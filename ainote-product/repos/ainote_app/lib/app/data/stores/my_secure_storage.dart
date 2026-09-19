import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/foundation.dart';
import 'My_storage.dart';
import '../../utils/helper.dart';

String? normalizeDeviceId(String? value) {
  final normalized = value?.trim();
  if (normalized == null || normalized.length < 3) return null;
  if ({'null', 'undefined', 'unknown'}.contains(normalized.toLowerCase())) {
    return null;
  }
  if (normalized.runes.any((rune) => rune <= 0x20 || rune == 0x7f)) {
    return null;
  }
  return normalized;
}

class MySecureStorage {
  // prevent making instance
  MySecureStorage._();

  // get storage
  static FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  static String? _fallbackDeviceId;
  static Future<String>? _deviceIdFuture;
  static Future<String?> Function()? _readOverride;
  static Future<void> Function(String value)? _writeOverride;
  static Future<void> Function()? _durableInitOverride;
  static Future<String?> Function()? _durableReadOverride;
  static Future<void> Function(String value)? _durableWriteOverride;

  // STORING KEYS
  static const String _deviceIdKey = 'device_id';
  static const _channelTimeout = Duration(seconds: 3);

  /// init get storage services
  static Future<void> init() async {
    _secureStorage = const FlutterSecureStorage();
    try {
      await (_durableInitOverride?.call() ?? MyStorage.init())
          .timeout(_channelTimeout);
    } catch (_) {
      // Device identity can still be recovered from secure storage. Durable
      // initialization is deliberately isolated from that backend.
    }
    await getDeviceId();
  }

  static Future<String> getDeviceId() {
    final pending = _deviceIdFuture;
    if (pending != null) return pending;

    final future = _resolveDeviceId();
    _deviceIdFuture = future;
    return future.whenComplete(() {
      if (identical(_deviceIdFuture, future)) _deviceIdFuture = null;
    });
  }

  static Future<String> _resolveDeviceId() async {
    final cached = normalizeDeviceId(_fallbackDeviceId);
    if (cached != null) {
      await _repairBackendsBestEffort(cached);
      return cached;
    }

    // Each read is isolated: an exception or timeout in one backend must not
    // prevent the other from supplying the installation identity.
    final durableId = await _readDurableBestEffort();
    final secureId = await _readSecureBestEffort();
    final selectedDeviceId = durableId ?? secureId ?? await createDeviceId();
    _fallbackDeviceId = selectedDeviceId;

    // Durable storage wins conflicts because it remains available on hosts
    // where the secure-storage platform channel is absent. Repair both sides
    // without making either one a new point of failure.
    await _repairBackendsBestEffort(selectedDeviceId);
    return selectedDeviceId;
  }

  @visibleForTesting
  static void resetForTesting({
    Future<String?> Function()? read,
    Future<void> Function(String value)? write,
    Future<void> Function()? durableInit,
    Future<String?> Function()? durableRead,
    Future<void> Function(String value)? durableWrite,
  }) {
    _fallbackDeviceId = null;
    _deviceIdFuture = null;
    _readOverride = read;
    _writeOverride = write;
    _durableInitOverride = durableInit;
    _durableReadOverride = durableRead;
    _durableWriteOverride = durableWrite;
    _secureStorage = const FlutterSecureStorage();
  }

  static Future<String?> _readDurableBestEffort() async {
    try {
      final value = await (_durableReadOverride?.call() ??
              Future<String?>.sync(MyStorage.getDeviceId))
          .timeout(_channelTimeout);
      return normalizeDeviceId(value);
    } catch (_) {
      return null;
    }
  }

  static Future<String?> _readSecureBestEffort() async {
    try {
      final value = await get(key: _deviceIdKey).timeout(_channelTimeout);
      return normalizeDeviceId(value);
    } catch (_) {
      return null;
    }
  }

  static Future<void> _persistDurable(String value) async {
    if (_durableWriteOverride != null) {
      await _durableWriteOverride!(value);
    } else {
      await MyStorage.setDeviceId(value);
    }
  }

  static Future<void> _persistDurableBestEffort(String value) async {
    try {
      await _persistDurable(value).timeout(_channelTimeout);
    } catch (_) {}
  }

  static Future<void> _persistSecureBestEffort(String value) async {
    try {
      await set(key: _deviceIdKey, value: value).timeout(_channelTimeout);
    } catch (_) {}
  }

  static Future<void> _repairBackendsBestEffort(String value) async {
    await Future.wait([
      _persistDurableBestEffort(value),
      _persistSecureBestEffort(value),
    ]);
  }

  static Future<String> createDeviceId() async {
    return randomId();
  }

  /// get by key
  static Future<String?> get({required String key}) async =>
      _readOverride != null ? _readOverride!() : _secureStorage.read(key: key);

  /// set by key
  static Future<void> set({required String key, required String value}) async {
    if (_writeOverride != null) return _writeOverride!(value);
    await _secureStorage.write(key: key, value: value);
  }

  /// clear all data from storage
  static Future<void> clear() async => await _secureStorage.deleteAll();

  /// delete by key
  static Future<void> delete({required String key}) async =>
      await _secureStorage.delete(key: key);
}
