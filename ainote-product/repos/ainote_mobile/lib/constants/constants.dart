import 'package:event_bus/event_bus.dart';

import 'app_images.dart';

final baseUrl = "https://aifunc.top";

EventBus eventBus = EventBus();

final List<Map<String, dynamic>> contentAssistList = [
  {
    "title": "修改",
    "children": [
      {
        "title": "简单优化表达",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "润色信息",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "完善想法",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "提取待办/计划",
        "icon": AppImages.aiAssistIcon1.path,
      },
    ]
  },
  {
    "title": "评估",
    "children": [
      {
        "title": "分析想法",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "提建议",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "解释它",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "重点提炼",
        "icon": AppImages.aiAssistIcon1.path,
      },
    ]
  },
  {
    "title": "转化",
    "children": [
      {
        "title": "分享文案",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "中英翻译",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "工作方案文档",
        "icon": AppImages.aiAssistIcon1.path,
      },
      {
        "title": "演讲稿",
        "icon": AppImages.aiAssistIcon1.path,
      }
    ]
  }
];

final introUrl =
    "https://infinitrix.feishu.cn/docx/BJI0dfm5ooJ8luxe8F9cqs8Xn9g?from=from_copylink";
