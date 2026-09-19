class NodeTypeDictModel {
  String typeName;
  List<NoteTypeModel> entryList;

  NodeTypeDictModel({required this.typeName, required this.entryList});

  factory NodeTypeDictModel.fromJson(Map<String, dynamic> json) {
    var entryListFromJson = json['entryList'] as List;
    List<NoteTypeModel> entryList =
        entryListFromJson.map((e) => NoteTypeModel.fromJson(e)).toList();

    return NodeTypeDictModel(
      typeName: json['typeName'],
      entryList: entryList,
    );
  }
}

class NoteTypeModel {
  int id;
  String name;

  NoteTypeModel({required this.id, required this.name});

  factory NoteTypeModel.fromJson(Map<String, dynamic> json) {
    return NoteTypeModel(
      id: int.parse(json.keys.first),
      name: json.values.first,
    );
  }
}
