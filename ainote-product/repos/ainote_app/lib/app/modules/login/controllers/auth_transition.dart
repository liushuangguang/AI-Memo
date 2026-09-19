typedef PersistAuthorization = Future<void> Function(String authorization);
typedef InstallAuthorizationHeaders = Future<void> Function();
typedef EnterAuthenticatedApp = void Function();
typedef DeleteAuthorization = Future<void> Function();
typedef EnterLoggedOutApp = void Function();

String? mobileAuthorizationFromLoginResponse(dynamic responseData) {
  if (responseData is! Map) return null;
  final payload = responseData['data'];
  if (payload is! Map) return null;
  final rawToken = payload['token'];
  if (rawToken is! String) return null;
  final token = rawToken.trim();
  if (token.isEmpty || RegExp(r'\s').hasMatch(token)) return null;
  return 'Mobile$token';
}

/// Commits an authenticated session before exposing routes that may issue
/// requests. The ordering is intentional and part of the login contract.
Future<void> completeAuthenticatedTransition({
  required String authorization,
  required PersistAuthorization persistAuthorization,
  required InstallAuthorizationHeaders installHeaders,
  required EnterAuthenticatedApp enterApp,
}) async {
  await persistAuthorization(authorization);
  await installHeaders();
  enterApp();
}

Future<bool> completeLoginResponseTransition({
  required dynamic responseData,
  required PersistAuthorization persistAuthorization,
  required InstallAuthorizationHeaders installHeaders,
  required EnterAuthenticatedApp enterApp,
}) async {
  final authorization = mobileAuthorizationFromLoginResponse(responseData);
  if (authorization == null) return false;
  await completeAuthenticatedTransition(
    authorization: authorization,
    persistAuthorization: persistAuthorization,
    installHeaders: installHeaders,
    enterApp: enterApp,
  );
  return true;
}

/// Removes an authenticated session before exposing the login route. Header
/// installation is deliberately sequenced after durable deletion so no new
/// request can reuse the old Mobile authorization.
Future<void> completeLogoutTransition({
  required DeleteAuthorization deleteAuthorization,
  required InstallAuthorizationHeaders installHeaders,
  required EnterLoggedOutApp enterLoggedOutApp,
}) async {
  await deleteAuthorization();
  await installHeaders();
  enterLoggedOutApp();
}
