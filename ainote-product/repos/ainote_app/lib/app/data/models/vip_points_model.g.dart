// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'vip_points_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

VipPointsModel _$VipPointsModelFromJson(Map<String, dynamic> json) =>
    VipPointsModel(
      points: (json['points'] as num?)?.toDouble(),
      pointsHistory: (json['pointsHistory'] as List<dynamic>?)
          ?.map((e) => e == null
              ? null
              : PointsHistory.fromJson(e as Map<String, dynamic>))
          .toList(),
      level: json['level'] as String?,
    );

Map<String, dynamic> _$VipPointsModelToJson(VipPointsModel instance) =>
    <String, dynamic>{
      'points': instance.points,
      'pointsHistory': instance.pointsHistory,
      'level': instance.level,
    };

PointsHistory _$PointsHistoryFromJson(Map<String, dynamic> json) =>
    PointsHistory(
      id: json['id'] as String?,
      uid: (json['uid'] as num?)?.toInt(),
      deviceId: json['deviceId'],
      changePoint: (json['changePoint'] as num?)?.toDouble(),
      token: (json['token'] as num?)?.toInt(),
      changeMethod: json['changeMethod'] as String?,
      createdAt: (json['createdAt'] as num?)?.toInt(),
      expireAt: (json['expireAt'] as num?)?.toInt(),
    );

Map<String, dynamic> _$PointsHistoryToJson(PointsHistory instance) =>
    <String, dynamic>{
      'id': instance.id,
      'uid': instance.uid,
      'deviceId': instance.deviceId,
      'changePoint': instance.changePoint,
      'token': instance.token,
      'changeMethod': instance.changeMethod,
      'createdAt': instance.createdAt,
      'expireAt': instance.expireAt,
    };
