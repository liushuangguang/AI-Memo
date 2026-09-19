class AiValidateModel {
  String? result;
  String? recordId;
  bool? meaningful;

  AiValidateModel({
    this.result,
    this.recordId,
    this.meaningful,
  });

  factory AiValidateModel.fromJson(Map<String, dynamic> json) {
    return AiValidateModel(
      result: json['result'],
      recordId: json['recordId'],
      meaningful: json['meaningful'] ?? json['isMeaningful'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'result': result,
      'recordId': recordId,
      'meaningful': meaningful,
    };
  }
}
