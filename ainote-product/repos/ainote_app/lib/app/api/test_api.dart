// 模拟Stream 流， 每秒增加一个字符
import 'dart:async';

import 'package:ainote_app/app/data/models/todo_model.dart';
import 'package:ainote_app/app/utils/json_format.dart';

import '../data/models/ai_organize_model.dart';

var str = '''大江东去，浪淘尽，千古风流人物。
故垒西边，人道是，三国周郎赤壁。
乱石穿空，惊涛拍岸，卷起千堆雪。(穿空 一作：崩云)
江山如画，一时多少豪杰。

遥想公瑾当年，小乔初嫁了，雄姿英发。
羽扇纶巾，谈笑间，樯橹灰飞烟灭。(樯橹 一作：强虏)
故国神游，多情应笑我，早生华发。
人生如梦，一尊还酹江月。(人生 一作：人间；尊 同：樽)''';

Stream<String> mockStreamText() {
  final StreamController<String> controller = StreamController<String>();

  int index = 0;
  Timer? timer;

  void sendData() {
    if (index < str.length) {
      controller.add(str[index++]);
    } else {
      controller.close();
      timer?.cancel();
    }
  }

  timer = Timer.periodic(Duration(milliseconds: 200), (_) {
    sendData();
  });

  return controller.stream;
}

void main() {
  var icon = getFaviconUrl(
      'https://m.163.com/dy/article/JAEJ1UP705568O7Z.html?referFrom=');
//   var s = '''
//   data:```json
// data:{
// data:  "overall_Suggestions":
// data:    {
// data:      "Suggestion": "出发前检查天气预报，确保带好保暖衣物和雨具",
// data:      "emoj": "⛅"
// data:    },
// data:  "overall_ToDo_Items": [
// data:    {
// data:      "ToDo_Content": "预定西安的住宿",
// data:      "Todo_notes": "建议选择市中心附近，方便游览各大景点",
// data:      "Time": "2023-12-20 12:00:00"
// data:    },
// data:    {
// data:      "ToDo_Content": "购买往返西安的车票或机票",
// data:      "Todo_notes": "提前购票以避免临时涨价或无票",
// data:      "Time": "2023-12-20 12:00:00"
// data:    },
// data:    {
// data:      "ToDo_Content": "制定详细的旅游行程计划",
// data:      "Todo_notes": "包括每日行程安排和餐厅预约",
// data:      "Time": "2023-12-25 18:00:00"
// data:    }
// data:  ],
// data:  "specific_Suggestions": [
// data:    {
// data:      "Suggestion": "参观兵马俑时，建议提前了解历史背景，以便更好欣赏",
// data:      "emoj": "🏺"
// data:    },
// data:    {
// data:      "Suggestion": "尝试西安特色美食，如肉夹馍和羊肉泡馍",
// data:      "emoj": "🍜"
// data:    },
// data:    {
// data:      "Suggestion": "注意保管好个人物品，特别是在人多的景点",
// data:      "emoj": "🎒"
// data:    }
// data:  ],
// data:  "specific_ToDo_Items": [
// data:    {
// data:      "original_todo": "元旦西安游玩",
// data:      "Todo_Content": "提前查看各大景点的新年活动安排",
// data:      "Todo_notes": "有些景点可能会有特别活动或优惠",
// data:      "Time": null
// data:    },
// data:    {
// data:      "original_todo": "元旦西安游玩",
// data:      "Todo_Content": "准备一份应急药品包",
// data:      "Todo_notes": "包括常用药、创可贴、消毒用品等",
// data:      "Time": "2023-12-30 12:00:00"
// data:    }
// data:  ]
// data:}
// data:```
//   ''';
//
//   var res = '';
//   for (var item in s.split('\n')) {
//     res += trimData(item);
//   }
}

String getFaviconUrl(String url) {
  // 获取 url 的主机名
  String origin = Uri.parse(url).origin;
  return '$origin/favicon.ico';
}
