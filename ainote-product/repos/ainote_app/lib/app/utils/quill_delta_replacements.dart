import 'package:flutter_quill/quill_delta.dart';

class DeltaReplacementResult {
  const DeltaReplacementResult({
    required this.delta,
    required this.replacementCount,
  });

  final Delta delta;
  final int replacementCount;
}

class _TextReplacement {
  const _TextReplacement({
    required this.start,
    required this.length,
    required this.replacement,
    required this.priority,
    this.attributes,
  });

  final int start;
  final int length;
  final String replacement;
  final int priority;
  final Map<String, dynamic>? attributes;

  int get end => start + length;
}

/// Replaces matching text ranges without flattening the rich-text document.
///
/// Matches may cross adjacent text operations. Embedded objects occupy one
/// non-matching placeholder position, so a replacement can never consume an
/// image, todo, or other embed. Overlapping candidates are resolved from left
/// to right, preferring the longest phrase at the same start position.
DeltaReplacementResult replaceVaguePhrasesInDelta(
  Delta source,
  Map<String, String> replacements,
) {
  final operations = source.toList();
  final searchable = StringBuffer();
  final attributesByOffset = <Map<String, dynamic>?>[];

  for (final operation in operations) {
    if (!operation.isInsert) continue;
    final data = operation.data;
    if (data is String) {
      searchable.write(data);
      attributesByOffset.addAll(
        List<Map<String, dynamic>?>.filled(
          data.length,
          operation.attributes,
          growable: true,
        ),
      );
    } else {
      searchable.write('\uFFFC');
      attributesByOffset.add(null);
    }
  }

  final text = searchable.toString();
  final candidates = <_TextReplacement>[];
  var priority = 0;
  for (final entry in replacements.entries) {
    final phrase = entry.key;
    if (phrase.isEmpty || phrase.contains('\uFFFC')) {
      priority++;
      continue;
    }

    var searchFrom = 0;
    while (searchFrom <= text.length - phrase.length) {
      final start = text.indexOf(phrase, searchFrom);
      if (start < 0) break;
      candidates.add(_TextReplacement(
        start: start,
        length: phrase.length,
        replacement: entry.value,
        priority: priority,
        attributes: start < attributesByOffset.length
            ? attributesByOffset[start]
            : null,
      ));
      searchFrom = start + phrase.length;
    }
    priority++;
  }

  candidates.sort((a, b) {
    final byStart = a.start.compareTo(b.start);
    if (byStart != 0) return byStart;
    final byLength = b.length.compareTo(a.length);
    if (byLength != 0) return byLength;
    return a.priority.compareTo(b.priority);
  });

  final selected = <_TextReplacement>[];
  var occupiedUntil = -1;
  for (final candidate in candidates) {
    if (candidate.start < occupiedUntil) continue;
    selected.add(candidate);
    occupiedUntil = candidate.end;
  }

  var result = Delta.from(source);
  for (final replacement in selected.reversed) {
    final change = Delta()..retain(replacement.start);
    change.delete(replacement.length);
    change.insert(replacement.replacement, replacement.attributes);
    result = result.compose(change);
  }

  return DeltaReplacementResult(
    delta: result,
    replacementCount: selected.length,
  );
}
