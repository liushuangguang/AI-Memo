class TypeModel {
  String typeName;
  List<TypeEntry> entryList;

  TypeModel({required this.typeName, required this.entryList});

  factory TypeModel.fromJson(Map<String, dynamic> json) {
    var entryListFromJson = json['entryList'] as List;
    List<TypeEntry> entryList = entryListFromJson.map((e) => TypeEntry.fromJson(e)).toList();

    return TypeModel(
      typeName: json['typeName'],
      entryList: entryList,
    );
  }
}

class TypeEntry {
  String id;
  String name;

  TypeEntry({required this.id, required this.name});

  factory TypeEntry.fromJson(Map<String, dynamic> json) {
    return TypeEntry(
      id: json.keys.first,
      name: json.values.first,
    );
  }
}
