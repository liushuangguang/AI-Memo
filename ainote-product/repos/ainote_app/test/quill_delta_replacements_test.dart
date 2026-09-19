import 'package:ainote_app/app/utils/quill_delta_replacements.dart';
import 'package:flutter_quill/quill_delta.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('replaces multiple phrases across text ops and preserves formatting',
      () {
    final source = Delta()
      ..insert('Meet ')
      ..insert('next ', {'bold': true})
      ..insert('week', {'link': 'https://example.test'})
      ..insert(' and ')
      ..insert('later', {'italic': true})
      ..insert('\n', {'list': 'bullet'});

    final result = replaceVaguePhrasesInDelta(source, {
      'next week': 'tomorrow',
      'later': 'soon',
    });

    expect(_plainText(result.delta), 'Meet tomorrow and soon\n');
    expect(result.replacementCount, 2);
    expect(
      result.delta.toJson(),
      [
        {'insert': 'Meet '},
        {
          'insert': 'tomorrow',
          'attributes': {'bold': true},
        },
        {'insert': ' and '},
        {
          'insert': 'soon',
          'attributes': {'italic': true},
        },
        {
          'insert': '\n',
          'attributes': {'list': 'bullet'},
        },
      ],
    );
  });

  test('repeated and overlapping matches are deterministic and non-overlapping',
      () {
    final source = Delta()..insert('aaaa vague vague\n');

    final result = replaceVaguePhrasesInDelta(source, {
      'aaa': 'X',
      'aa': 'Y',
      'vague': 'clear',
    });

    expect(_plainText(result.delta), 'Xa clear clear\n');
    expect(result.replacementCount, 3);
  });

  test('todo and image embeds plus unmatched attributes remain byte-for-byte',
      () {
    final todo = {
      'todo': '{"title":"keep"}',
    };
    final image = {
      'image': 'assets/keep.png',
    };
    final source = Delta()
      ..insert('before ', {'bold': true})
      ..insert(todo)
      ..insert(' vague ', {'link': 'https://example.test'})
      ..insert(image)
      ..insert(' after')
      ..insert('\n', {'list': 'ordered'});

    final result = replaceVaguePhrasesInDelta(source, {'vague': 'specific'});
    final json = result.delta.toJson();

    expect(result.replacementCount, 1);
    expect(json.where((op) => op['insert'] is Map).toList(), [
      {'insert': todo},
      {'insert': image},
    ]);
    expect(json[0], {
      'insert': 'before ',
      'attributes': {'bold': true},
    });
    expect(json[2], {
      'insert': ' specific ',
      'attributes': {'link': 'https://example.test'},
    });
    expect(json.last, {
      'insert': '\n',
      'attributes': {'list': 'ordered'},
    });
  });

  test('unmatched phrases leave the original delta unchanged', () {
    final source = Delta()
      ..insert('keep', {'bold': true})
      ..insert('\n');

    final result = replaceVaguePhrasesInDelta(source, {'missing': 'new'});

    expect(result.replacementCount, 0);
    expect(result.delta, source);
  });
}

String _plainText(Delta delta) => delta
    .toList()
    .where((operation) => operation.data is String)
    .map((operation) => operation.data as String)
    .join();
