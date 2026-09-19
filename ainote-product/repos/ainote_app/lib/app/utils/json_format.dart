import 'dart:convert';

import 'package:ainote_app/app/data/models/ai_suggestion_model.dart';

String trimData(String text) {
  return text
      .split('\n')
      .map((line) {
        if (!line.startsWith('data:')) return line;
        final payload = line.substring('data:'.length);
        return payload.startsWith(' ') ? payload.substring(1) : payload;
      })
      .join('\n')
      .trim();
}

String trimJson(String text) {
  if (text.contains('```json')) {
    text = text.replaceFirst('```json', '').replaceAll('```', '').trim();
  }
  return text;
}

AiSuggestionModel? parseAiSuggestionModelFromJsonString(String? str) {
  if (str == null) return null;
  try {
    return AiSuggestionModel.fromJson(jsonDecode(trimJson(str)));
  } catch (e) {}
  return null;
}
