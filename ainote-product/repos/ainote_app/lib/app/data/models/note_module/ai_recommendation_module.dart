import 'note_module_payload.dart';

class AIRecommendationModule extends NoteModulePayload {
  String? moduleId;
  String? title;
  RecommendationItemRequest? recommendationItems;

  @override
  String get noteModuleType =>
      NoteModuleType.AI_RECOMMENDATION.toString().split('.').last;

  AIRecommendationModule({
    this.moduleId,
    this.title,
    this.recommendationItems,
  });

  factory AIRecommendationModule.fromJson(Map<String, dynamic> json) {
    return AIRecommendationModule(
      moduleId: json['moduleId'],
      title: json['title'],
      recommendationItems: json['recommendationItems'] != null
          ? RecommendationItemRequest.fromJson(
              json['recommendationItems'] as Map<String, dynamic>)
          : null,
    );
  }

  @override
  Map<String, dynamic> toJson() {
    return {
      'moduleId': moduleId,
      'noteModuleType': noteModuleType,
      'title': title,
      'recommendationItems': recommendationItems?.toJson(),
    };
  }
}

class RecommendationItemRequest {
  String? description;
  RecommendationItemType? recommendType;
  String? productKeyword;
  List<ProductRecommendationItem>? productRecommendations;

  RecommendationItemRequest({
    this.description,
    this.recommendType,
    this.productKeyword,
    this.productRecommendations,
  });

  factory RecommendationItemRequest.fromJson(Map<String, dynamic> json) {
    return RecommendationItemRequest(
      description: json['description'],
      recommendType: json['recommendType'] != null
          ? RecommendationItemType.values.firstWhere(
              (element) => element.toString() == json['recommendType'])
          : null,
      productKeyword: json['productKeyword'],
      productRecommendations: json['productRecommendations'] != null
          ? (json['productRecommendations'] as List)
              .map((e) => ProductRecommendationItem.fromJson(e))
              .toList()
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'description': description,
      'recommendType': recommendType?.toString(),
      'productKeyword': productKeyword,
      'productRecommendations':
          productRecommendations?.map((e) => e.toJson()).toList(),
    };
  }
}

class ProductRecommendationItem {
  String? description;
  String? productId;
  String? productSearchName;
  String? productName;
  String? productDesc;
  String? productImageUrl;
  String? productShortUrl;
  String? productSchemaUrl;
  String? recommendationReason;
  String? minGroupPrice;
  String? minNormalPrice;

  ProductRecommendationItem({
    this.description,
    this.productId,
    this.productSearchName,
    this.productName,
    this.productDesc,
    this.productImageUrl,
    this.productShortUrl,
    this.productSchemaUrl,
    this.recommendationReason,
    this.minGroupPrice,
    this.minNormalPrice,
  });

  factory ProductRecommendationItem.fromJson(Map<String, dynamic> json) {
    return ProductRecommendationItem(
      description: json['description'],
      productId: json['productId'],
      productSearchName: json['productSearchName'],
      productName: json['productName'],
      productDesc: json['productDesc'],
      productImageUrl: json['productImageUrl'],
      productShortUrl: json['productShortUrl'],
      productSchemaUrl: json['productSchemaUrl'],
      recommendationReason: json['recommendationReason'],
      minGroupPrice: json['minGroupPrice'],
      minNormalPrice: json['minNormalPrice'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'description': description,
      'productId': productId,
      'productSearchName': productSearchName,
      'productName': productName,
      'productDesc': productDesc,
      'productImageUrl': productImageUrl,
      'productShortUrl': productShortUrl,
      'productSchemaUrl': productSchemaUrl,
      'recommendationReason': recommendationReason,
      'minGroupPrice': minGroupPrice,
      'minNormalPrice': minNormalPrice,
    };
  }
}
