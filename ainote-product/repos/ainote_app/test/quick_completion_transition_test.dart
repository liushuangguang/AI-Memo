import 'package:ainote_app/app/modules/note/note_edit/controllers/note_edit_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('quick completion persists the new snapshot before analysis starts',
      () async {
    final events = <String>[];

    final started = await persistQuickCompletionThenStart(
      persist: () async {
        events.add('save:new-delta');
        return true;
      },
      startAnalysis: () async {
        events.add('start-analysis:new-snapshot');
        return true;
      },
    );

    expect(started, isTrue);
    expect(events, ['save:new-delta', 'start-analysis:new-snapshot']);
  });

  test('quick completion save failure never starts stale analysis', () async {
    final events = <String>[];

    final started = await persistQuickCompletionThenStart(
      persist: () async {
        events.add('save:failed');
        return false;
      },
      startAnalysis: () async {
        events.add('start-analysis');
        return true;
      },
    );

    expect(started, isFalse);
    expect(events, ['save:failed']);
  });
}
