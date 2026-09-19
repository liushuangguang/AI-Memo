import 'dart:convert';
import 'dart:async';
import 'dart:typed_data';

import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/config/app/app_config.dart';
import 'package:ainote_app/app/data/stores/my_secure_storage.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('guest mode follows the compile-time setting and defaults to enabled',
      () {
    const expected = bool.fromEnvironment('GUEST_MODE', defaultValue: true);
    expect(AppConfig.guestMode, expected);
  });

  group('guest authorization', () {
    test('uses the stable device identifier instead of a login token', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: true,
          deviceId: 'device-123',
          token: null,
        ),
        'Guest device-123',
      );
    });

    test('uses a safe non-empty fallback for a blank device identifier', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: true,
          deviceId: '  ',
        ),
        'Guest local-debug-device',
      );
    });

    test('preserves a valid existing token in guest mode', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: true,
          deviceId: 'device-123',
          token: 'Mobileexisting-token',
        ),
        'Mobileexisting-token',
      );
    });

    test('falls back to Guest when a stored mobile token is malformed', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: true,
          deviceId: 'device-123',
          token: 'Mobile invalid-token',
        ),
        'Guest device-123',
      );
    });

    test('rejects whitespace and control characters in device identity', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: true,
          deviceId: 'bad\nidentity',
        ),
        'Guest local-debug-device',
      );
    });

    test('keeps the production authorization header unchanged', () {
      expect(
        MyDio.resolveAuthorizationHeader(
          guestMode: false,
          deviceId: 'device-123',
          token: 'Mobileproduction-token',
        ),
        'Mobileproduction-token',
      );
    });
  });

  test('a guest 401 never triggers the login redirect', () {
    expect(
      MyDio.shouldRedirectToLogin(statusCode: 401, guestMode: true),
      isFalse,
    );
    expect(
      MyDio.shouldRedirectToLogin(statusCode: 401, guestMode: false),
      isTrue,
    );
  });

  group('guest 401 transition', () {
    late HttpClientAdapter originalAdapter;

    setUp(() async {
      originalAdapter = MyDio.dio.httpClientAdapter;
      MyDio.resetForTesting(showError: (_) {});
      MySecureStorage.resetForTesting(
        read: () async => 'stable-device-id',
        write: (_) async {},
        durableRead: () async => null,
        durableWrite: (_) async {},
      );
      await MyDio.init(guestMode: true);
      MyDio.dio.options.baseUrl = 'https://example.test';
    });

    tearDown(() {
      MyDio.dio.httpClientAdapter = originalAdapter;
      MyDio.resetForTesting();
      MySecureStorage.resetForTesting();
    });

    test('Mobile 401 switches atomically and replays the request once',
        () async {
      final adapter = _AuthAdapter(guestStatusCode: 200);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final response = await MyDio.postJSON(
        '/mobile-to-guest',
        data: {'same': 'payload'},
      );

      expect(response.statusCode, 200);
      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.map((request) => request.headers['Authorization']),
        ['Mobileexpired-token', 'Guest stable-device-id'],
      );
      expect(
        adapter.requests.map((request) => request.headers['Device-Id']).toSet(),
        {'stable-device-id'},
      );
      expect(adapter.requests.map((request) => request.data), [
        {'same': 'payload'},
        {'same': 'payload'},
      ]);
      expect(
        MyDio.dio.options.headers['Authorization'],
        'Guest stable-device-id',
      );
    });

    test('Guest 401 is rejected without a replay loop', () async {
      final adapter = _AuthAdapter(guestStatusCode: 401);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Guest stable-device-id',
        'Device-Id': 'stable-device-id',
      };

      await expectLater(
        MyDio.postJSON('/guest-unauthorized'),
        throwsA(isA<DioException>()),
      );

      expect(adapter.requests, hasLength(1));
      expect(
        adapter.requests.single.headers['Authorization'],
        'Guest stable-device-id',
      );
    });

    test('a replayed Guest 401 is not replayed a second time', () async {
      final adapter = _AuthAdapter(guestStatusCode: 401);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      await expectLater(
        MyDio.postJSON('/replay-unauthorized'),
        throwsA(isA<DioException>()),
      );

      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.map((request) => request.headers['Authorization']),
        ['Mobileexpired-token', 'Guest stable-device-id'],
      );
    });

    test('stalled durable cleanup cannot block the Guest replay', () async {
      MyDio.resetForTesting(
        deleteAuthToken: () => Completer<void>().future,
      );
      await MyDio.init(guestMode: true);
      MyDio.dio.options.baseUrl = 'https://example.test';
      final adapter = _AuthAdapter(guestStatusCode: 200);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final stopwatch = Stopwatch()..start();
      final response = await MyDio.postJSON('/bounded-cleanup');
      stopwatch.stop();

      expect(response.statusCode, 200);
      expect(stopwatch.elapsed, lessThan(const Duration(seconds: 1)));
      expect(adapter.requests, hasLength(2));
    });

    test('concurrent Mobile 401 requests each replay exactly once', () async {
      final adapter = _ConcurrentAuthAdapter(expectedMobileRequests: 2);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final responses = await Future.wait([
        MyDio.postJSON('/first', data: {'request': 1}),
        MyDio.postJSON('/second', data: {'request': 2}),
      ]);

      expect(responses.map((response) => response.statusCode), [200, 200]);
      expect(adapter.requests, hasLength(4));
      expect(
        adapter.requests
            .where((request) =>
                request.headers['Authorization'] == 'Mobileexpired-token')
            .length,
        2,
      );
      expect(
        adapter.requests
            .where((request) =>
                request.headers['Authorization'] == 'Guest stable-device-id')
            .length,
        2,
      );
      expect(
        adapter.requests.map((request) => request.headers['Device-Id']).toSet(),
        {'stable-device-id'},
      );
    });

    test('FormData fields survive the Mobile-to-Guest replay', () async {
      final adapter = _AuthAdapter(guestStatusCode: 200);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final response = await MyDio.post(
        '/form-data',
        formData: {'title': 'kept', 'count': 2},
      );

      expect(response.statusCode, 200);
      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.map(_formFields),
        [
          {'title': 'kept', 'count': '2'},
          {'title': 'kept', 'count': '2'},
        ],
      );
    });

    test('multipart file parts survive the Mobile-to-Guest replay', () async {
      final adapter = _AuthAdapter(guestStatusCode: 200);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final response = await MyDio.post(
        '/multipart-file',
        formData: {
          'title': 'kept',
          'file': MultipartFile.fromBytes(
            <int>[1, 2, 3, 4],
            filename: 'kept.txt',
          ),
        },
      );

      expect(response.statusCode, 200);
      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.map(_formFileNames),
        [
          ['kept.txt'],
          ['kept.txt'],
        ],
      );
    });
  });

  group('authenticated 401 transition', () {
    late HttpClientAdapter originalAdapter;

    setUp(() async {
      originalAdapter = MyDio.dio.httpClientAdapter;
      MySecureStorage.resetForTesting(
        read: () async => 'stable-device-id',
        write: (_) async {},
        durableRead: () async => null,
        durableWrite: (_) async {},
      );
    });

    tearDown(() {
      MyDio.dio.httpClientAdapter = originalAdapter;
      MyDio.resetForTesting();
      MySecureStorage.resetForTesting();
    });

    test('concurrent Mobile 401s expire the session and navigate once',
        () async {
      var storedToken = 'Mobileexpired-token';
      var deletionCount = 0;
      var navigationCount = 0;
      MyDio.resetForTesting(
        deleteAuthToken: () async {
          deletionCount++;
          storedToken = '';
        },
        navigateToLogin: () async {
          navigationCount++;
        },
      );
      await MyDio.init(guestMode: false);
      MyDio.dio.options.baseUrl = 'https://example.test';
      final adapter = _ConcurrentAuthAdapter(expectedMobileRequests: 2);
      MyDio.dio.httpClientAdapter = adapter;
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final results = await Future.wait([
        MyDio.postJSON('/first'),
        MyDio.postJSON('/second'),
      ].map((request) async {
        try {
          await request;
          return null;
        } catch (error) {
          return error;
        }
      }));

      expect(results, everyElement(isA<DioException>()));
      expect(adapter.requests, hasLength(2));
      expect(deletionCount, 1);
      expect(navigationCount, 1);
      expect(MyDio.dio.options.headers['Authorization'], isNull);
      expect(
        resolveInitialRoute(
          guestMode: false,
          storedToken: storedToken,
        ),
        Routes.LOGIN,
      );
    });

    test('stalled durable cleanup cannot block the login redirect', () async {
      var navigationCount = 0;
      MyDio.resetForTesting(
        deleteAuthToken: () => Completer<void>().future,
        navigateToLogin: () async {
          navigationCount++;
        },
      );
      await MyDio.init(guestMode: false);
      MyDio.dio.options.baseUrl = 'https://example.test';
      MyDio.dio.httpClientAdapter = _AuthAdapter(guestStatusCode: 200);
      MyDio.dio.options.headers = {
        'Authorization': 'Mobileexpired-token',
        'Device-Id': 'stable-device-id',
      };

      final stopwatch = Stopwatch()..start();
      await expectLater(
        MyDio.postJSON('/bounded-authenticated-cleanup'),
        throwsA(isA<DioException>()),
      );
      stopwatch.stop();

      expect(stopwatch.elapsed, lessThan(const Duration(seconds: 1)));
      expect(navigationCount, 1);
      expect(MyDio.dio.options.headers['Authorization'], isNull);
      expect(
        MyDio.dio.options.headers['Device-Id'],
        'stable-device-id',
      );
    });
  });
}

Map<String, String> _formFields(RequestOptions request) {
  final data = request.data as FormData;
  return Map<String, String>.fromEntries(data.fields);
}

List<String> _formFileNames(RequestOptions request) {
  final data = request.data as FormData;
  return data.files.map((entry) => entry.value.filename ?? '').toList();
}

class _AuthAdapter implements HttpClientAdapter {
  _AuthAdapter({required this.guestStatusCode});

  final int guestStatusCode;
  final List<RequestOptions> requests = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final authorization = options.headers['Authorization']?.toString() ?? '';
    final statusCode =
        authorization.startsWith('Mobile') ? 401 : guestStatusCode;
    return ResponseBody.fromString(
      jsonEncode({'code': statusCode == 200 ? 200 : 401}),
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _ConcurrentAuthAdapter implements HttpClientAdapter {
  _ConcurrentAuthAdapter({required this.expectedMobileRequests});

  final int expectedMobileRequests;
  final List<RequestOptions> requests = [];
  final Completer<void> _mobileGate = Completer<void>();
  int _mobileRequests = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final authorization = options.headers['Authorization']?.toString() ?? '';
    if (authorization.startsWith('Mobile')) {
      _mobileRequests++;
      if (_mobileRequests == expectedMobileRequests) _mobileGate.complete();
      await _mobileGate.future;
      return ResponseBody.fromString(
        jsonEncode({'code': 401}),
        401,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );
    }
    return ResponseBody.fromString(
      jsonEncode({'code': 200}),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
