import '../data/models/vip_points_model.dart';
import 'api_urls.dart';
import 'my_dio.dart';

class VipApi {
  static updatePoints() async {
    await MyDio.postJSON(
      ApiUrls.vipUpdatePoints,
      data: {"changeMethod": "ADD", "changePoint": 100, "days": "MONTH"},
    );
  }

  static Future<VipPointsModel> getPoints() async {
    try {
      var resp = await MyDio.get(
        ApiUrls.vipGetPoints,
      );
      return VipPointsModel.fromJson(resp.data);
    } catch (_) {
      return VipPointsModel.fromJson({});
    }
  }
}
