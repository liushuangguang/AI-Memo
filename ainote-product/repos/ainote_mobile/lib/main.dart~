import 'dart:io';

import 'package:ainote/android_window/show_alert_logic.dart';
import 'package:ainote/constants/constants.dart';
import 'package:ainote/screens/home_screen.dart';
import 'package:ainote/services/keychain_service.dart';
import 'package:ainote/themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_quill/translations.dart';
import 'package:get/get.dart';

import 'package:flutter_localizations/flutter_localizations.dart'
    show
    GlobalCupertinoLocalizations,
    GlobalMaterialLocalizations,
    GlobalWidgetsLocalizations;

import 'android_window/android_window_app.dart';
import 'classes/app_local_settings.dart';

@pragma('vm:entry-point')
void androidWindow() {
  runApp(const GetMaterialApp(
    home: AndroidWindowApp(),
    debugShowCheckedModeBanner: false,
    color: Colors.transparent,
  ));
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  HttpOverrides.global = MyHttpOverrides();
  await AppLocalSettings.instance.init();

  // only portrait mode
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // remove status bar bg color
  SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle.dark.copyWith(
    statusBarColor: Colors.transparent,
  ));

  runApp(const MyApp());
  configLoading();
}

void configLoading() {
  EasyLoading.instance
    ..displayDuration = const Duration(milliseconds: 2000)
    ..indicatorType = EasyLoadingIndicatorType.fadingCircle
    ..loadingStyle = EasyLoadingStyle.dark
    ..indicatorSize = 45.0
    ..radius = 10.0
    ..progressColor = Colors.yellow
    ..backgroundColor = Colors.green
    ..indicatorColor = Colors.yellow
    ..textColor = Colors.yellow
    ..maskColor = Colors.blue.withOpacity(0.5)
    ..userInteractions = true
    ..dismissOnTap = false;
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  Future<void> _getDeviceId() async {
    final deviceId = await KeyChainService().getDeviceId();
    print(deviceId);
  }

  _firstHttpFetch() {
    HttpClient()
        .getUrl(Uri.parse("https://www.baidu.com"))
        .then((HttpClientRequest request) => request.close())
        .then((HttpClientResponse response) {
      print(response.statusCode);
    });
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _getDeviceId();
    _firstHttpFetch();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    if (state == AppLifecycleState.paused) {
      eventBus.fire("app_paused");
    } else if (state == AppLifecycleState.resumed) {
      var showAlertLogic = Get.put(ShowAlertLogic());
      showAlertLogic.onRefresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      debugShowCheckedModeBanner: false,
      theme: kTheme,
      home: HomeScreen(),
      builder: EasyLoading.init(),

      // quill editor localization
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: FlutterQuillLocalizations.supportedLocales,
    );
  }
}

class MyHttpOverrides extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) {
    return super.createHttpClient(context)
      ..badCertificateCallback = (X509Certificate cert, String host, int port) {
        return true;
      };
  }
}
