import 'dart:async';

import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:speech_to_text/speech_recognition_result.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';
import 'package:record/record.dart';
import 'package:speech_to_text/speech_to_text.dart';

import '../../constants/lottie_data.dart';
import 'platform/audio_recorder_platform.dart';

class RecorderWave extends StatefulWidget {
  // final void Function(String path) onStop;
  final void Function(String msg) onStop;

  const RecorderWave({super.key, required this.onStop});

  @override
  State<RecorderWave> createState() => _RecorderState();
}

class _RecorderState extends State<RecorderWave> {
  int _recordDuration = 0;
  late Timer _timer;

  final stt.SpeechToText speechToText = stt.SpeechToText();

  bool hasPop = false;

  Future<void> startListening() async {
    bool available = await speechToText.initialize(
      onStatus: (status) {
        if (speechToText.isListening) {
          _startTimer();
        } else {
          _stopTimer();
          if (mounted) {
            Navigator.of(context).pop();
          }
        }
      },
      onError: (errorNotification) {
        _stopTimer();
        widget.onStop('');
        if (mounted) {
          Navigator.of(context).pop();
        }
      },
    );
    if (available) {
      _startTimer();
      speechToText.listen(
        listenMode: ListenMode.dictation,
        onResult: (SpeechRecognitionResult result) {
          widget.onStop(result.recognizedWords);
          if (mounted) {
            Navigator.of(context).pop();
          }
        },
      );
    }
  }

  Future<void> stopListening() async {
    await speechToText.stop();
    _stopTimer();
    if (mounted) {
      Navigator.of(context).pop();
    }
  }

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (Timer timer) {});
    startListening();
  }

  @override
  void dispose() {
    hasPop = false;
    _stopTimer();
    speechToText.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Lottie.asset(LottieData.play.path,
            height: 68,
            width: 68,
            repeat: true,
            filterQuality: FilterQuality.high),
        _buildTimer(),
        TextButton(
            onPressed: stopListening,
            child: const Text('点击结束', style: TextStyle(color: Colors.blue))),
      ],
    );
  }

  Widget _buildTimer() {
    final String minutes = _formatNumber(_recordDuration ~/ 60);
    final String seconds = _formatNumber(_recordDuration % 60);

    return Text(
      '$minutes : $seconds',
      style: const TextStyle(color: Colors.white),
    );
  }

  String _formatNumber(int number) {
    String numberStr = number.toString();
    if (number < 10) {
      numberStr = '0$numberStr';
    }

    return numberStr;
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (Timer t) {
      setState(() => _recordDuration++);
    });
  }

  void _stopTimer() {
    if (_timer.isActive) {
      _timer.cancel(); // Stop the timer if it's active
    }
  }
}
