class CompleteInfoModel {
  final String vaguePhrase;
  final String inquiryProcess;
  final List<String> options;

  const CompleteInfoModel({
    required this.vaguePhrase,
    required this.inquiryProcess,
    required this.options,
  });

  factory CompleteInfoModel.fromJson(Map<String, dynamic> json) {
    final rawOptions = json['options'];
    return CompleteInfoModel(
      vaguePhrase:
          (json['vague_phrase'] ?? json['vaguePhrase'] ?? '').toString().trim(),
      inquiryProcess: (json['inquiry_process'] ?? json['inquiryProcess'] ?? '')
          .toString()
          .trim(),
      options: rawOptions is List
          ? rawOptions
              .map((item) => item is Map
                  ? (item['option'] ?? '').toString().trim()
                  : item.toString().trim())
              .where((item) => item.isNotEmpty)
              .take(4)
              .toList()
          : const [],
    );
  }

  bool get isUsable => vaguePhrase.isNotEmpty && options.isNotEmpty;
}
