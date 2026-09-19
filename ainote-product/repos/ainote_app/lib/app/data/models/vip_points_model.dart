import 'package:json_annotation/json_annotation.dart';

part 'vip_points_model.g.dart';

@JsonSerializable()
class VipPointsModel {
  final double? points;
  final List<PointsHistory?>? pointsHistory;
  final String? level;

  VipPointsModel({
    this.points,
    this.pointsHistory,
    this.level,
  });

  factory VipPointsModel.fromJson(Map<String, dynamic> json) =>
      _$VipPointsModelFromJson(json);

  Map<String, dynamic> toJson() => _$VipPointsModelToJson(this);
}

@JsonSerializable()
class PointsHistory {
  final String? id;
  final int? uid;
  final dynamic deviceId;
  final double? changePoint;
  final int? token;
  final String? changeMethod; // ADD or CONSUMER
  final int? createdAt;
  final int? expireAt;

  PointsHistory({
    this.id,
    this.uid,
    this.deviceId,
    this.changePoint,
    this.token,
    this.changeMethod,
    this.createdAt,
    this.expireAt,
  });

  factory PointsHistory.fromJson(Map<String, dynamic> json) =>
      _$PointsHistoryFromJson(json);

  Map<String, dynamic> toJson() => _$PointsHistoryToJson(this);
}
