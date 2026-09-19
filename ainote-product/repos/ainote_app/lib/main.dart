import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'app/services/capture/capture_service.dart';
import 'package:flutter_phoenix/flutter_phoenix.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:flutter_quill/translations.dart' show FlutterQuillLocalizations;
import 'package:flutter_localizations/flutter_localizations.dart'
    show
        GlobalCupertinoLocalizations,
        GlobalMaterialLocalizations,
        GlobalWidgetsLocalizations;

import 'package:get/get.dart';
import 'package:phone_form_field/phone_form_field.dart';
import 'package:toastification/toastification.dart';

import 'app/config/app/app_config.dart';
import 'app/config/theme/my_theme.dart';
import 'app/routes/app_pages.dart';
import 'app/routes/route_observer.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(Phoenix(
    child: StartupGate(
      initializer: AppConfig.init,
      appBuilder: (_) => const MyApp(),
    ),
  ));
}

typedef StartupInitializer = Future<void> Function();

class StartupGate extends StatefulWidget {
  const StartupGate({
    super.key,
    required this.initializer,
    required this.appBuilder,
    this.timeout = const Duration(seconds: 15),
  });

  final StartupInitializer initializer;
  final WidgetBuilder appBuilder;
  final Duration timeout;

  @override
  State<StartupGate> createState() => _StartupGateState();
}

enum _StartupStatus { loading, ready, failed }

class _StartupGateState extends State<StartupGate> {
  _StartupStatus _status = _StartupStatus.loading;
  int _attempt = 0;
  bool _running = false;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    if (_running) return;
    final attempt = ++_attempt;
    _running = true;
    if (mounted) setState(() => _status = _StartupStatus.loading);
    try {
      await widget.initializer().timeout(widget.timeout);
      if (mounted && attempt == _attempt) {
        setState(() => _status = _StartupStatus.ready);
      }
    } catch (_) {
      if (mounted && attempt == _attempt) {
        setState(() => _status = _StartupStatus.failed);
      }
    } finally {
      if (attempt == _attempt) _running = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_status == _StartupStatus.ready) return widget.appBuilder(context);

    final failed = _status == _StartupStatus.failed;
    final dark = MediaQuery.platformBrightnessOf(context) == Brightness.dark;
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor:
            dark ? const Color(0xFF101A28) : const Color(0xFFF4F8FF),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.auto_awesome,
                    color: Color(0xFF4A90E2), size: 48),
                const SizedBox(height: 16),
                Semantics(
                  liveRegion: true,
                  child: Text(
                    failed ? '应用启动失败，请稍后重试' : '正在准备 AI 备忘录…',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: dark
                          ? const Color(0xFFB8D7FF)
                          : const Color(0xFF315D91),
                      fontSize: 16,
                    ),
                  ),
                ),
                if (!failed) ...[
                  const SizedBox(height: 16),
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Color(0xFF4A90E2),
                    ),
                  ),
                ] else ...[
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 44,
                    child: ElevatedButton(
                      onPressed: _initialize,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF235D9F),
                        foregroundColor: const Color(0xFFF7FAFF),
                      ),
                      child: const Text('重试'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ScreenUtilInit(
      designSize: const Size(390, 844),
      minTextAdapt: true,
      splitScreenMode: true,
      useInheritedMediaQuery: true,
      rebuildFactor: (old, data) => true,
      builder: (context, widget) {
        return ToastificationWrapper(
          child: GetMaterialApp(
            onReady: () async {
              if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
                try { await CaptureService.instance.initialize(); } catch (_) {
                  // Capture is optional; a platform failure must not block startup.
                }
              }
            },
            title: "AI备忘录",
            useInheritedMediaQuery: true,
            debugShowCheckedModeBanner: true,
            navigatorObservers: [
              FlutterSmartDialog.observer,
              MyRouteObserver()
            ],
            builder: FlutterSmartDialog.init(builder: (context, widget) {
              return Theme(
                data: MyTheme.getThemeData(),
                child: MediaQuery(
                  // prevent font scale
                  data: MediaQuery.of(context)
                      .copyWith(textScaler: TextScaler.noScaling),
                  child: widget!,
                ),
              );
            }),
            initialRoute: AppConfig.initialRoute,
            getPages: AppPages.routes,
            // quill editor localization
            localizationsDelegates: const [
              GlobalMaterialLocalizations.delegate,
              GlobalWidgetsLocalizations.delegate,
              GlobalCupertinoLocalizations.delegate,
              DefaultMaterialLocalizations.delegate,
              ...PhoneFieldLocalization.delegates,
            ],
            supportedLocales: [
              ...FlutterQuillLocalizations.supportedLocales,
              const Locale('zh', 'CN'),
            ],
          ),
        );
      },
    );
  }
}
