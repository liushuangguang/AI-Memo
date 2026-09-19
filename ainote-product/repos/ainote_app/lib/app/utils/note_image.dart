import '../api/my_dio.dart';

String noteImageUrl(String url) {
  if (!url.startsWith('/v2/capture/images/')) return url;
  return Uri.parse(MyDio.dio.options.baseUrl).resolve(url).toString();
}

/// Only private capture images on the exact configured backend receive auth.
/// Generated images and shopping sites receive no credentials.
Map<String, String>? noteImageHeaders(String url) {
  final base = Uri.tryParse(MyDio.dio.options.baseUrl);
  final target = Uri.tryParse(noteImageUrl(url));
  if (base == null || target == null || !base.hasAuthority || !target.hasAuthority ||
      !['http', 'https'].contains(target.scheme) || target.origin != base.origin ||
      !target.path.startsWith('/v2/capture/images/')) {
    return null;
  }
  return {
    for (final name in ['Authorization', 'Device-Id'])
      if (MyDio.dio.options.headers[name] is String) name: MyDio.dio.options.headers[name] as String,
  };
}
