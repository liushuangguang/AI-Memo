import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:record/record.dart';

import 'message/message_item.dart';
import 'platform/audio_recorder_platform.dart';
import 'package:flutter/material.dart';

class AiVoiceRecorder extends StatefulWidget {
  final void Function(String path)? onStop;

  const AiVoiceRecorder({super.key, this.onStop});

  @override
  State<AiVoiceRecorder> createState() => _AiVoiceRecorderState();
}

class _AiVoiceRecorderState extends State<AiVoiceRecorder>
    with AudioRecorderMixin {
  late final AudioRecorder _audioRecorder;
  StreamSubscription<RecordState>? _recordSub;
  RecordState _recordState = RecordState.stop;
  StreamSubscription<Amplitude>? _amplitudeSub;
  Amplitude? _amplitude;

  bool _isTalking = true;
  int _currentRecordingMilliseconds = 0;
  final int _maxRecordingMilliseconds = 1000 * 10;

  // 说话电平
  final int _talkCurrent = -30;

  Timer? _timer;
  final int _threshold = 100;

  @override
  void initState() {
    _audioRecorder = AudioRecorder();

    _recordSub = _audioRecorder.onStateChanged().listen((recordState) {
      _updateRecordState(recordState);
    });

    _amplitudeSub = _audioRecorder
        .onAmplitudeChanged(Duration(milliseconds: _threshold))
        .listen((amp) {
      // 判断是否还在说话
      print('[说话监听]：${amp.current} $_amplitude');

      _currentRecordingMilliseconds += _threshold;
      if (_currentRecordingMilliseconds >= _maxRecordingMilliseconds) {
        _stop();
        _currentRecordingMilliseconds = 0;
        setState(() {
          _amplitude = amp;
          _isTalking = false;
        });
      }
      if (amp.current > _talkCurrent && _recordState == RecordState.record) {
        _isTalking = true;
      } else if (amp.current <= _talkCurrent &&
          _recordState == RecordState.record) {
        _isTalking = false;
      }

      setState(() {
        _amplitude = amp;
      });

      if (_isTalking == false) {
        _timer ??= Timer(const Duration(seconds: 2), () {
          _timer?.cancel();
          _timer = null;
          if (_isTalking == false) {
            _stop();
          }
        });
      } else {
        _timer?.cancel();
        _timer = null;
      }
    });

    super.initState();

    Future.delayed(const Duration(milliseconds: 100), () async {
      EasyLoading.showToast('请说话，我在听~');
      await _start();
    });
  }

  Future<void> _start() async {
    try {
      if (await _audioRecorder.hasPermission()) {
        const encoder = AudioEncoder.pcm16bits;
        // const encoder = AudioEncoder.aacLc;

        if (!await _isEncoderSupported(encoder)) {
          return;
        }

        final devs = await _audioRecorder.listInputDevices();
        debugPrint(devs.toString());

        const config = RecordConfig(encoder: encoder, numChannels: 1, noiseSuppress: true);

        // Record to file
        await recordFile(_audioRecorder, config);

        // Record to stream
        await recordStream(_audioRecorder, config, (path, bytes) {
          widget.onStop?.call(path);
        });
      }
    } catch (e) {
      if (kDebugMode) {
        print('开启录音失败：$e');
      }
    }
  }

  Future<void> _pause() => _audioRecorder.pause();

  Future<void> _resume() => _audioRecorder.resume();

  Future<void> _stop() async {
    final path = await _audioRecorder.stop();

    if (path != null) {
      widget.onStop?.call(path);
    }
  }

  void _updateRecordState(RecordState recordState) {
    setState(() => _recordState = recordState);

    switch (recordState) {
      case RecordState.pause:
        print('录音暂停');
        break;
      case RecordState.record:
        print('录音开始');
        break;
      case RecordState.stop:
        print('录音结束');
        break;
    }
  }

  Future<bool> _isEncoderSupported(AudioEncoder encoder) async {
    final isSupported = await _audioRecorder.isEncoderSupported(
      encoder,
    );

    if (!isSupported) {
      debugPrint('${encoder.name} is not supported on this platform.');
      debugPrint('Supported encoders are:');

      for (final e in AudioEncoder.values) {
        if (await _audioRecorder.isEncoderSupported(e)) {
          debugPrint('- ${e.name}');
        }
      }
    }

    return isSupported;
  }

  @override
  void dispose() {
    _recordSub?.cancel();
    _amplitudeSub?.cancel();
    print('录音销毁');
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // if(_amplitude != null) {
    //   return Column(
    //     children: [
    //       MessageItem(
    //         message: '说话状态：${_isTalking ? '正在说话' : '请大点儿声'}',
    //         isAiMessage: false,
    //       ),
    //       MessageItem(
    //         message: '当前电平：${_amplitude?.current}, ${_amplitude!.current > -60 ? '正在说话' : '请大点儿声'}',
    //         isAiMessage: false,
    //       ),
    //     ],
    //   );
    // }
    // return const MessageItem(
    //   message: '初始化中...',
    //   isAiMessage: false,
    // );

    // return Text('当前电平：${_amplitude?.current}');
    return const SizedBox();
  }
}
