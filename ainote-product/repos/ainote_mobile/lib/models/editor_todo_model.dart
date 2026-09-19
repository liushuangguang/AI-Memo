class EditorTodoModel {
  bool isChecked;
  String item;
  String dateString;

  EditorTodoModel({
    required this.isChecked,
    required this.item,
    required this.dateString,
  });

  factory EditorTodoModel.fromJson(Map<String, dynamic> json) {
    return EditorTodoModel(
      isChecked: json['isChecked'] as bool,
      item: json['item'] as String,
      dateString: json['dateString'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'isChecked': isChecked,
      'item': item,
      'dateString': dateString,
    };
  }
}