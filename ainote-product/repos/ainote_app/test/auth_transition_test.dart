import 'dart:async';

import 'package:ainote_app/app/api/my_dio.dart';
import 'package:ainote_app/app/data/stores/My_storage.dart';
import 'package:ainote_app/app/data/stores/my_secure_storage.dart';
import 'package:ainote_app/app/modules/login/controllers/auth_transition.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('login awaits persistence and delayed header installation before entry',
      () async {
    final headerGate = Completer<void>();
    final events = <String>[];

    final transition = completeAuthenticatedTransition(
      authorization: 'Mobiletoken',
      persistAuthorization: (authorization) async {
        events.add('persist:$authorization');
      },
      installHeaders: () async {
        events.add('headers:start');
        await headerGate.future;
        events.add('headers:done');
      },
      enterApp: () => events.add('enter-and-request-root'),
    );
    await Future<void>.delayed(Duration.zero);

    expect(events, ['persist:Mobiletoken', 'headers:start']);
    expect(events, isNot(contains('enter-and-request-root')));

    headerGate.complete();
    await transition;

    expect(events, [
      'persist:Mobiletoken',
      'headers:start',
      'headers:done',
      'enter-and-request-root',
    ]);
  });

  test('header installation does not begin before token persistence', () async {
    final persistenceGate = Completer<void>();
    final events = <String>[];

    final transition = completeAuthenticatedTransition(
      authorization: 'Mobiletoken',
      persistAuthorization: (_) async {
        events.add('persist:start');
        await persistenceGate.future;
        events.add('persist:done');
      },
      installHeaders: () async => events.add('headers'),
      enterApp: () => events.add('enter'),
    );
    await Future<void>.delayed(Duration.zero);

    expect(events, ['persist:start']);
    persistenceGate.complete();
    await transition;

    expect(events, ['persist:start', 'persist:done', 'headers', 'enter']);
  });

  test('valid login response trims a String token before transition', () async {
    final events = <String>[];

    final transitioned = await completeLoginResponseTransition(
      responseData: {
        'data': {'token': '  valid-token  '},
      },
      persistAuthorization: (value) async => events.add('persist:$value'),
      installHeaders: () async => events.add('headers'),
      enterApp: () => events.add('navigate'),
    );

    expect(transitioned, isTrue);
    expect(events, ['persist:Mobilevalid-token', 'headers', 'navigate']);
  });

  for (final invalidResponse in <dynamic>[
    null,
    <String, dynamic>{},
    {'data': null},
    {
      'data': <String, dynamic>{},
    },
    {
      'data': {'token': null},
    },
    {
      'data': {'token': ''},
    },
    {
      'data': {'token': '   '},
    },
    {
      'data': {'token': 123},
    },
    {
      'data': {'token': 'invalid token'},
    },
  ]) {
    test('invalid login response has no auth side effects: $invalidResponse',
        () async {
      final events = <String>[];

      final transitioned = await completeLoginResponseTransition(
        responseData: invalidResponse,
        persistAuthorization: (_) async => events.add('persist'),
        installHeaders: () async => events.add('headers'),
        enterApp: () => events.add('navigate'),
      );

      expect(transitioned, isFalse);
      expect(events, isEmpty);
    });
  }

  test('logout awaits deletion and header refresh before navigation', () async {
    final deletionGate = Completer<void>();
    final headerGate = Completer<void>();
    final events = <String>[];

    final transition = completeLogoutTransition(
      deleteAuthorization: () async {
        events.add('delete:start');
        await deletionGate.future;
        events.add('delete:done');
      },
      installHeaders: () async {
        events.add('headers:start');
        await headerGate.future;
        events.add('headers:done');
      },
      enterLoggedOutApp: () => events.add('navigate:login'),
    );
    await Future<void>.delayed(Duration.zero);

    expect(events, ['delete:start']);
    deletionGate.complete();
    await Future<void>.delayed(Duration.zero);
    expect(events, ['delete:start', 'delete:done', 'headers:start']);

    headerGate.complete();
    await transition;
    expect(events, [
      'delete:start',
      'delete:done',
      'headers:start',
      'headers:done',
      'navigate:login',
    ]);
  });

  test('logout refresh removes the old Mobile authorization before entry',
      () async {
    MyDio.resetForTesting();
    MySecureStorage.resetForTesting(
      read: () async => 'stable-device-id',
      write: (_) async {},
      durableRead: () async => null,
      durableWrite: (_) async {},
    );
    addTearDown(() {
      MyDio.resetForTesting();
      MySecureStorage.resetForTesting();
    });
    await MyDio.init(guestMode: false);
    MyDio.dio.options.headers['Authorization'] = 'Mobileold-token';
    var entered = false;

    await completeLogoutTransition(
      deleteAuthorization: MyStorage.deleteAuthToken,
      installHeaders: MyDio.setAuthHeaders,
      enterLoggedOutApp: () {
        expect(MyDio.dio.options.headers['Authorization'], isNull);
        entered = true;
      },
    );

    expect(entered, isTrue);
    expect(MyDio.dio.options.headers['Authorization'], isNull);
  });
}
