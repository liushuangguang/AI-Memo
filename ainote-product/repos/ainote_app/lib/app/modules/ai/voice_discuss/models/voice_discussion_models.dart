import 'package:ainote_app/app/data/models/note_model.dart';

enum VoiceDiscussionRole { user, assistant }

enum VoiceMessageDelivery { pending, sent, failed, cancelled }

class VoiceDiscussionMessage {
  const VoiceDiscussionMessage({
    required this.role,
    required this.content,
    this.delivery = VoiceMessageDelivery.sent,
  });

  final VoiceDiscussionRole role;
  final String content;
  final VoiceMessageDelivery delivery;

  bool get canRetry =>
      role == VoiceDiscussionRole.user &&
      (delivery == VoiceMessageDelivery.failed ||
          delivery == VoiceMessageDelivery.cancelled);

  Map<String, dynamic> toJson() => {
        'role': role.name,
        'content': content,
      };

  VoiceDiscussionMessage copyWith({VoiceMessageDelivery? delivery}) =>
      VoiceDiscussionMessage(
        role: role,
        content: content,
        delivery: delivery ?? this.delivery,
      );
}

class VoiceSummarySaveResult {
  const VoiceSummarySaveResult({
    required this.noteId,
    required this.saveRequestId,
    required this.alreadySaved,
    required this.note,
  });

  final String noteId;
  final String saveRequestId;
  final bool alreadySaved;
  final NoteModel note;
}
