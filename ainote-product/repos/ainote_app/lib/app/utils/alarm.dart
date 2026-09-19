import 'dart:async';

import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';

import '../../generated/assets.dart';

class Alarm {
  static final Alarm _instance = Alarm._();
  AudioPlayer? _player;
  bool _active = true;

  factory Alarm() => _instance;

  Alarm._() {
    _initialize();
  }

  Future<AudioPlayer> _initialize() async {
    if (_player != null) return _player!;
    _player = AudioPlayer(playerId: 'todo_alarm');
    AudioCache audioCache = AudioCache();

    var path = Assets.soundsAlarm;
    if (path.startsWith('assets/')) {
      path = path.replaceFirst('assets/', '');
    }
    audioCache.load(path);

    // loop play
    _player?.setReleaseMode(ReleaseMode.loop);

    // low latency to fix stop/pause issue: https://github.com/bluefireteam/audioplayers/issues/1714
    _player?.setPlayerMode(PlayerMode.lowLatency);
    return _player!;
  }

  Future<void> playAlarm() async {
    if (_player == null) {
      await _initialize();
    }
    var path = Assets.soundsAlarm;
    if (path.startsWith('assets/')) {
      path = path.replaceFirst('assets/', '');
    }
    await _player?.play(AssetSource(path));
  }

  Future<void> stopAlarm() async {
    await _player?.pause();
  }

  void stopVibrator() {
    Vibration.cancel();
  }

  Future<void> playVibrator([int duration = 500]) async {
    if (!_active) return;
    final canVibrate = await Vibration.hasVibrator();
    if (canVibrate == true) {
      Vibration.vibrate(duration: duration);
    }
  }

  Future<void> play() async {
    _active = true;
    await playVibrator();
    await playAlarm();

    Timer(const Duration(seconds: 10), stop);
  }

  Future<void> stop() async {
    _active = false;
    stopVibrator();
    await stopAlarm();
  }

  Future<void> dispose() async {
    await stop();
    _player?.dispose();
    _player = null;
  }
}
