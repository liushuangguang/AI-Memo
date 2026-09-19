import 'package:flutter/material.dart';

import '../../../../../utils/logger.dart';

class NoteAutoSave extends StatefulWidget {
  final VoidCallback? onBackground;

  const NoteAutoSave({super.key, this.onBackground});

  @override
  State<NoteAutoSave> createState() => _NoteAutoSaveState();
}

class _NoteAutoSaveState extends State<NoteAutoSave>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.paused) {
      // 应用进入后台
      logger.i('[App is in the background]');
      widget.onBackground?.call();
    } else if (state == AppLifecycleState.resumed) {
      // 应用回到前台
      logger.i('[App is in the foreground]');
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox.shrink();
  }
}
