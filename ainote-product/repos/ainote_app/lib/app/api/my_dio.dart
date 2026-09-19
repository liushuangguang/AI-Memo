import 'package:ainote_app/app/api/api_urls.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:ainote_app/app/data/stores/my_secure_storage.dart';
import 'package:ainote_app/app/routes/app_pages.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:dio/dio.dart';
import 'package:dio_cache_interceptor/dio_cache_interceptor.dart';
import 'package:dio_smart_retry/dio_smart_retry.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart' show Get, GetNavigation;
import '../utils/logger.dart';

class MyDio {
  static final dio = Dio();
  static Future<void>? _initFuture;
  static Future<void>? _guestTransitionFuture;
  static Future<void>? _expiredSessionTransitionFuture;
  static bool _interceptorsInitialized = false;
  static bool _guestMode = false;
  static bool _expiredSessionHandled = false;
  static Future<void> Function() _deleteAuthToken = MyStorage.deleteAuthToken;
  static Future<void> Function() _navigateToLogin = _defaultNavigateToLogin;
  static void Function(String) _showError = Toast.error;
  static const String _guestDeviceFallback = 'local-debug-device';
  static const String _guestReplayKey = 'ainote_guest_auth_replay';
  static const Duration _guestTokenCleanupTimeout = Duration(milliseconds: 250);

  static String _normalizedDeviceId(String deviceId) =>
      normalizeDeviceId(deviceId) ?? _guestDeviceFallback;

  static String? _normalizedMobileToken(String? token) {
    final normalized = token?.trim();
    if (normalized == null || !RegExp(r'^Mobile[^\s]+$').hasMatch(normalized)) {
      return null;
    }
    return normalized;
  }

  @visibleForTesting
  static String? resolveAuthorizationHeader({
    required bool guestMode,
    required String deviceId,
    String? token,
  }) {
    final validToken = _normalizedMobileToken(token);
    if (!guestMode) return validToken;
    if (validToken != null) return validToken;

    final normalizedDeviceId = _normalizedDeviceId(deviceId);
    return 'Guest $normalizedDeviceId';
  }

  @visibleForTesting
  static bool shouldRedirectToLogin({
    required int? statusCode,
    required bool guestMode,
  }) {
    return statusCode == 401 && !guestMode;
  }

  /// Returns the rejection for an unexpected HTTP or business response.
  /// Keeping this decision pure makes the response contract easy to test.
  static DioException? responseError(Response response) {
    if (response.statusCode != 200 && response.statusCode != 201) {
      return DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
    if (response.data is Map<String, dynamic>) {
      final data = response.data as Map<String, dynamic>;
      if (data.containsKey('code') && data['code'] != 200) {
        return DioException(
          requestOptions: response.requestOptions,
          response: response,
          type: DioExceptionType.badResponse,
        );
      }
    }
    return null;
  }

  static final cacheStore = MemCacheStore();

  static final cacheOptions = CacheOptions(
    store: cacheStore,
    policy: CachePolicy.request,
    hitCacheOnErrorExcept: [401, 403],
    maxStale: const Duration(days: 7),
    allowPostMethod: false,
    keyBuilder: CacheOptions.defaultCacheKeyBuilder,
  );

  static Future<void> init({bool guestMode = false}) {
    _guestMode = guestMode;
    final pending = _initFuture;
    if (pending != null) return pending;
    final future = _initialize();
    _initFuture = future;
    return future.whenComplete(() {
      if (identical(_initFuture, future)) _initFuture = null;
    });
  }

  static Future<void> _initialize() async {
    final deviceId = _normalizedDeviceId(await MySecureStorage.getDeviceId());
    String? token = MyStorage.getAuthToken();
    final authorization = resolveAuthorizationHeader(
      guestMode: _guestMode,
      deviceId: deviceId,
      token: token,
    );
    if (_normalizedMobileToken(authorization) != null) {
      _expiredSessionHandled = false;
    }

    dio.options.baseUrl = ApiUrls.baseUrl;
    dio.options.headers = {
      "Device-Id": deviceId,
      "Authorization": authorization,
    };
    dio.options.contentType = "application/json; charset=utf-8";
    dio.options.responseType = ResponseType.json;
    dio.options.connectTimeout = Duration(seconds: 30);
    dio.options.receiveTimeout = Duration(seconds: 30);

    if (_interceptorsInitialized) return;

    dio.interceptors.add(DioCacheInterceptor(options: cacheOptions));

    dio.interceptors.add(RetryInterceptor(
      dio: dio,
      retries: 3,
      logPrint: (message) {
        logger.w('Request retry');
      },
      retryDelays: const [
        Duration(seconds: 1), // wait 1 sec before first retry
        Duration(seconds: 2), // wait 2 sec before second retry
        Duration(seconds: 3), // wait 3 sec before third retry
      ],
    ));

    dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      // Do something before request is sent
      logger.d("Request method: ${options.method}");
      return handler.next(options); //continue
    }, onResponse: (response, handler) {
      logger.i('[Request.statusCode] ${response.statusCode}');
      final error = responseError(response);
      if (error != null) {
        if (response.statusCode != 200 && response.statusCode != 201) {
          _showError('请求异常, 请稍后重试');
        } else if (response.data is Map<String, dynamic>) {
          final message = (response.data as Map<String, dynamic>)['message'];
          if (message is String && message.isNotEmpty) Toast.info(message);
        }
        return handler.reject(error);
      }
      return handler.next(response);
    }, onError: (DioException e, handler) async {
      final statusCode = e.response?.statusCode;
      logger.e('[Request failed] status=$statusCode');
      // 处理 http code 异常
      if (shouldRedirectToLogin(
        statusCode: statusCode,
        guestMode: _guestMode,
      )) {
        final failedDeviceId = _normalizedDeviceId(
          e.requestOptions.headers['Device-Id']?.toString() ?? '',
        );
        await _transitionExpiredSession(failedDeviceId);
      } else if (statusCode == 401) {
        final failedAuthorization =
            e.requestOptions.headers['Authorization']?.toString() ?? '';
        final alreadyReplayed = e.requestOptions.extra[_guestReplayKey] == true;
        if (_guestMode &&
            !alreadyReplayed &&
            _normalizedMobileToken(failedAuthorization) != null) {
          final failedDeviceId = _normalizedDeviceId(
            e.requestOptions.headers['Device-Id']?.toString() ?? '',
          );
          await _transitionToGuest(failedDeviceId);
          final replayHeaders = Map<String, dynamic>.from(
            e.requestOptions.headers,
          )
            ..['Device-Id'] = failedDeviceId
            ..['Authorization'] = 'Guest $failedDeviceId';
          final replayExtra = Map<String, dynamic>.from(e.requestOptions.extra)
            ..[_guestReplayKey] = true
            // The auth transition owns exactly one replay. Do not let the
            // generic transport retry layer multiply this request.
            ..['ro_disable_retry'] = true;
          final replayData = e.requestOptions.data is FormData
              ? (e.requestOptions.data as FormData).clone()
              : e.requestOptions.data;

          try {
            final response = await dio.fetch<dynamic>(
              e.requestOptions.copyWith(
                headers: replayHeaders,
                extra: replayExtra,
                data: replayData,
              ),
            );
            return handler.resolve(response);
          } on DioException catch (replayError) {
            return handler.next(replayError);
          }
        }
        _showError('当前调试服务暂不可用');
      }
      if (statusCode == 404) {
        _showError('请求资源不存在');
      }
      if (statusCode == 3333) {
        _showError('积分不足，请充值后重试');
      }
      if (statusCode != null && statusCode >= 500) {
        _showError('请求异常, 请稍后重试');
      }
      return handler.reject(e); //continue
    }));
    _interceptorsInitialized = true;
  }

  static Future<void> _transitionToGuest(String deviceId) {
    final pending = _guestTransitionFuture;
    if (pending != null) return pending;

    final normalizedDeviceId = _normalizedDeviceId(deviceId);
    final headers = Map<String, dynamic>.from(dio.options.headers)
      ..['Device-Id'] = normalizedDeviceId
      ..['Authorization'] = 'Guest $normalizedDeviceId';
    // Replacing the complete header map is the atomic state transition seen
    // by all requests created after the first Mobile 401.
    dio.options.headers = headers;

    final future = () async {
      try {
        await _deleteAuthToken().timeout(_guestTokenCleanupTimeout);
      } catch (_) {
        // The in-process Guest transition remains valid even if durable token
        // cleanup is temporarily unavailable.
      }
    }();
    _guestTransitionFuture = future;
    return future.whenComplete(() {
      if (identical(_guestTransitionFuture, future)) {
        _guestTransitionFuture = null;
      }
    });
  }

  static Future<void> _transitionExpiredSession(String deviceId) {
    if (_expiredSessionHandled) return Future<void>.value();
    final pending = _expiredSessionTransitionFuture;
    if (pending != null) return pending;
    _expiredSessionHandled = true;

    final normalizedDeviceId = _normalizedDeviceId(deviceId);
    _installLoggedOutHeaders(normalizedDeviceId);

    final future = () async {
      try {
        await _deleteAuthToken().timeout(_guestTokenCleanupTimeout);
      } catch (_) {
        // The rejected session is cleared in memory even when durable cleanup
        // is temporarily unavailable.
      }
      _installLoggedOutHeaders(normalizedDeviceId);
      await _navigateToLogin();
    }();
    _expiredSessionTransitionFuture = future;
    return future.whenComplete(() {
      if (identical(_expiredSessionTransitionFuture, future)) {
        _expiredSessionTransitionFuture = null;
      }
    });
  }

  static void _installLoggedOutHeaders(String deviceId) {
    final headers = Map<String, dynamic>.from(dio.options.headers)
      ..['Device-Id'] = _normalizedDeviceId(deviceId)
      ..remove('Authorization');
    dio.options.headers = headers;
  }

  static Future<void> _defaultNavigateToLogin() async {
    _showError('登录已过期，请重新登录');
    Get.offAllNamed(Routes.LOGIN);
  }

  static Future<void> setAuthHeaders() async {
    final deviceId = _normalizedDeviceId(await MySecureStorage.getDeviceId());
    String? token = MyStorage.getAuthToken();
    dio.options.headers['Device-Id'] = deviceId;
    final authorization = resolveAuthorizationHeader(
      guestMode: _guestMode,
      deviceId: deviceId,
      token: token,
    );
    dio.options.headers['Authorization'] = authorization;
    if (_normalizedMobileToken(authorization) != null) {
      _expiredSessionHandled = false;
    }
  }

  @visibleForTesting
  static void resetForTesting({
    Future<void> Function()? deleteAuthToken,
    Future<void> Function()? navigateToLogin,
    void Function(String)? showError,
  }) {
    _initFuture = null;
    _guestTransitionFuture = null;
    _expiredSessionTransitionFuture = null;
    _interceptorsInitialized = false;
    _guestMode = false;
    _expiredSessionHandled = false;
    _deleteAuthToken = deleteAuthToken ?? MyStorage.deleteAuthToken;
    _navigateToLogin = navigateToLogin ?? _defaultNavigateToLogin;
    _showError = showError ?? Toast.error;
    dio.interceptors.clear();
    dio.options = BaseOptions();
  }

  static Future<Response> get(
    String url, {
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    return await dio.get(url,
        queryParameters: queryParameters, options: options);
  }

  static Future<Response> getCached(
    String url, {
    String? subKey,
    Duration duration = const Duration(days: 1),
    Map<String, dynamic>? queryParameters,
    bool forceRefresh = false,
    Options? options,
  }) async {
    options ??= Options();

    final resp = await dio.get(
      url,
      queryParameters: queryParameters,
      options: options.copyWith(
          extra: cacheOptions
              .copyWith(
                maxStale: Nullable<Duration>(duration),
                policy: forceRefresh
                    ? CachePolicy.refreshForceCache
                    : CachePolicy.forceCache,
              )
              .toExtra()),
    );
    logger.d("Request completed: ${resp.statusCode}");

    return resp;
  }

  // 清空缓存
  static Future<void> cleanCache() async {
    return await cacheStore.clean();
  }

  static Future<Response> post(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? formData,
    Options? options,
  }) async {
    final resp = await dio.post(
      url,
      queryParameters: queryParameters,
      data: formData != null ? FormData.fromMap(formData) : null,
      options: options,
    );

    return resp;
  }

  static Future<Response> postJSON(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? data,
    Options? options,
  }) async {
    final resp = await dio.post(
      url,
      queryParameters: queryParameters,
      data: data,
      options: options,
    );

    return resp;
  }

  static Future<Response> put(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? formData,
    Options? options,
  }) async {
    return await dio.put(
      url,
      queryParameters: queryParameters,
      data: formData != null ? FormData.fromMap(formData) : null,
      options: options,
    );
  }

  static Future<Response> putJSON(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? data,
    Options? options,
  }) async {
    return await dio.put(
      url,
      queryParameters: queryParameters,
      data: data,
      options: options,
    );
  }

  static Future<Response> delete(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? formData,
    Options? options,
  }) async {
    return await dio.delete(
      url,
      queryParameters: queryParameters,
      data: formData != null ? FormData.fromMap(formData) : null,
      options: options,
    );
  }

  static Future<Response> upload(
    String url, {
    required String filePath,
    required String fileName,
    Options? options,
  }) async {
    FormData formData = FormData.fromMap(
        {'file': await MultipartFile.fromFile(filePath, filename: fileName)});
    return await dio.post(url, data: formData, options: options);
  }

  static Future<Stream<Uint8List>> getStream(
    String url, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final resp = await dio.get(
      url,
      queryParameters: queryParameters,
      options: Options(
        responseType: ResponseType.stream,
        contentType: 'application/json',
        headers: {
          'Accept': 'text/event-stream',
          "Cache-Control": "no-cache",
        },
      ),
    );
    return resp.data.stream;
  }

  static Future<Stream<Uint8List>> postStream(
    String url, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? data,
    Options? options,
  }) async {
    final resp = await dio.post(
      url,
      queryParameters: queryParameters,
      data: data,
      options: (options ?? Options()).copyWith(
        responseType: ResponseType.stream,
        contentType: 'application/json',
        headers: {
          ...?options?.headers,
          'Accept': 'text/event-stream',
          "Cache-Control": "no-cache",
        },
      ),
    );
    return resp.data.stream;
  }
}
