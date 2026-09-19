import 'package:ainote_app/app/api/my_dio.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

Response<dynamic> response(int status, dynamic data) => Response<dynamic>(
      requestOptions: RequestOptions(path: '/test'),
      statusCode: status,
      data: data,
    );

void main() {
  test('rejects an HTTP 200 business failure', () {
    final error = MyDio.responseError(response(200, {
      'code': 400,
      'message': 'failed',
    }));

    expect(error, isA<DioException>());
    expect(error!.type, DioExceptionType.badResponse);
  });

  test('rejects an unexpected HTTP 202 response', () {
    final error = MyDio.responseError(response(202, {'code': 200}));

    expect(error, isA<DioException>());
    expect(error!.type, DioExceptionType.badResponse);
  });

  test('rejects code 400 with an empty or missing message', () {
    expect(
      MyDio.responseError(response(200, {'code': 400})),
      isA<DioException>(),
    );
    expect(
      MyDio.responseError(response(200, {'code': 400, 'message': ''})),
      isA<DioException>(),
    );
  });

  test('accepts a map with a message but no business code', () {
    expect(MyDio.responseError(response(200, {'message': 'notice'})), isNull);
  });

  test('accepts an HTTP 200 business success', () {
    expect(MyDio.responseError(response(200, {'code': 200})), isNull);
  });

  test('accepts an HTTP 201 business success', () {
    expect(MyDio.responseError(response(201, {'code': 200})), isNull);
  });
}
