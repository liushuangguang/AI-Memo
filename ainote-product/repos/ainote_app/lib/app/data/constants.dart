import 'package:ainote_app/generated/assets.dart';

final baseUrl = "https://aifunc.top";

// 内容辅助
final List<Map<String, dynamic>> contentAssistList = [
  {
    "title": "修改",
    "children": [
      {
        "title": "简单优化表达",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "润色信息",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "完善想法",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "提取待办/计划",
        "icon": Assets.imagesAiAssist1,
      },
    ]
  },
  {
    "title": "评估",
    "children": [
      {
        "title": "分析想法",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "提建议",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "解释它",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "重点提炼",
        "icon": Assets.imagesAiAssist1,
      },
    ]
  },
  {
    "title": "转化",
    "children": [
      {
        "title": "分享文案",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "中英翻译",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "工作方案文档",
        "icon": Assets.imagesAiAssist1,
      },
      {
        "title": "演讲稿",
        "icon": Assets.imagesAiAssist1,
      }
    ]
  }
];

final List contentAssistListChildren =
    contentAssistList.expand((item) => item['children']).toList();

final introUrl =
    "https://infinitrix.feishu.cn/docx/BJI0dfm5ooJ8luxe8F9cqs8Xn9g?from=from_copylink";

final shareNoteUrl = "https://aifunc.top/h5/share.html";
final shareDownloadUrl = "https://aifunc.top/h5/download.html";
