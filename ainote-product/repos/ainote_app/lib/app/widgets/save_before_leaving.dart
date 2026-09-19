import 'package:flutter/material.dart';

/// Covers toolbar and Android system back with the same persistence gate.
class SaveBeforeLeaving extends StatefulWidget {
  const SaveBeforeLeaving({super.key, required this.save, required this.child});
  final Future<bool> Function() save;
  final Widget child;
  @override
  State<SaveBeforeLeaving> createState() => _SaveBeforeLeavingState();
}

class _SaveBeforeLeavingState extends State<SaveBeforeLeaving> {
  bool _busy = false;
  bool _canPop = false;

  Future<void> _requestLeave(bool didPop, Object? result) async {
    if (didPop || _busy) return;
    _busy = true;
    try {
      if (!await widget.save() || !mounted) return;
      setState(() => _canPop = true);
      await WidgetsBinding.instance.endOfFrame;
      if (mounted) Navigator.of(context).pop(result);
    } finally {
      _busy = false;
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
      canPop: _canPop,
      onPopInvokedWithResult: _requestLeave,
      child: widget.child);
}
