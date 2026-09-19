import 'package:ainote_app/app/data/stores/my_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late Map<String, String> durable;
  late Map<String, String> secure;

  setUp(() {
    durable = {};
    secure = {};
  });

  tearDown(MySecureStorage.resetForTesting);

  void configure({
    Future<void> Function()? durableInit,
    Future<String?> Function()? durableRead,
    Future<void> Function(String)? durableWrite,
    Future<String?> Function()? secureRead,
    Future<void> Function(String)? secureWrite,
  }) {
    MySecureStorage.resetForTesting(
      durableInit: durableInit ?? () async {},
      durableRead: durableRead ?? () async => durable['device_id_fallback'],
      durableWrite: durableWrite ??
          (value) async => durable['device_id_fallback'] = value,
      read: secureRead ?? () async => secure['device_id'],
      write: secureWrite ?? (value) async => secure['device_id'] = value,
    );
  }

  test('durable init failure still returns the stable secure ID', () async {
    secure['device_id'] = 'secure-installation-id';
    configure(
      durableInit: () async => throw StateError('durable init failed'),
      durableRead: () async => throw StateError('durable unavailable'),
      durableWrite: (_) async => throw StateError('durable unavailable'),
    );

    await MySecureStorage.init();
    final first = await MySecureStorage.getDeviceId();

    expect(first, 'secure-installation-id');
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('durable read failure uses secure ID and repairs durable storage',
      () async {
    secure['device_id'] = 'secure-installation-id';
    var readFails = true;
    configure(
      durableRead: () async {
        if (readFails) throw StateError('durable read failed');
        return durable['device_id_fallback'];
      },
    );

    final first = await MySecureStorage.getDeviceId();
    expect(first, 'secure-installation-id');
    expect(durable['device_id_fallback'], first);

    readFails = false;
    configure();
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('durable write failure remains stable through secure storage restart',
      () async {
    secure['device_id'] = 'secure-installation-id';
    configure(
      durableWrite: (_) async => throw StateError('durable write failed'),
    );

    final first = await MySecureStorage.getDeviceId();
    expect(first, 'secure-installation-id');
    expect(await MySecureStorage.getDeviceId(), first);

    configure(
      durableWrite: (_) async => throw StateError('durable write failed'),
    );
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('secure read failure uses durable ID and repairs secure storage',
      () async {
    durable['device_id_fallback'] = 'durable-installation-id';
    configure(
      secureRead: () async => throw StateError('secure read failed'),
    );

    final first = await MySecureStorage.getDeviceId();

    expect(first, 'durable-installation-id');
    expect(secure['device_id'], first);
    configure();
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('secure write failure remains stable through durable storage restart',
      () async {
    durable['device_id_fallback'] = 'durable-installation-id';
    configure(
      secureWrite: (_) async => throw StateError('secure write failed'),
    );

    final first = await MySecureStorage.getDeviceId();
    expect(first, 'durable-installation-id');
    expect(await MySecureStorage.getDeviceId(), first);

    secure.clear();
    configure(
      secureWrite: (_) async => throw StateError('secure write failed'),
    );
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('a newly generated ID is double-written and stable after restart',
      () async {
    configure();

    final first = await MySecureStorage.getDeviceId();

    expect(normalizeDeviceId(first), first);
    expect(durable['device_id_fallback'], first);
    expect(secure['device_id'], first);
    configure();
    expect(await MySecureStorage.getDeviceId(), first);
  });

  test('conflicting backends prefer durable and repair secure', () async {
    durable['device_id_fallback'] = 'durable-installation-id';
    secure['device_id'] = 'old-secure-id';
    configure();

    expect(await MySecureStorage.getDeviceId(), 'durable-installation-id');
    expect(secure['device_id'], 'durable-installation-id');
  });

  test('concurrent callers share one recovered installation ID', () async {
    durable['device_id_fallback'] = 'durable-installation-id';
    secure['device_id'] = 'old-secure-id';
    configure();

    final ids = await Future.wait(
      List.generate(20, (_) => MySecureStorage.getDeviceId()),
    );

    expect(ids.toSet(), {'durable-installation-id'});
  });

  test('only dual persistence failure degrades to process-local stability',
      () async {
    configure(
      durableWrite: (_) async => throw StateError('durable write failed'),
      secureWrite: (_) async => throw StateError('secure write failed'),
    );

    final first = await MySecureStorage.getDeviceId();

    expect(normalizeDeviceId(first), first);
    expect(await MySecureStorage.getDeviceId(), first);
    expect(durable, isEmpty);
    expect(secure, isEmpty);
  });
}
