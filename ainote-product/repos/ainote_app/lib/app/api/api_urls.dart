class ApiUrls {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://aifunc.top',
  );

  // 登录接口
  static String login = "/auth/login";

  // to do
  static String todoCreate = "/backlog/create";
  static String todoUpdate = "/backlog/update";
  static String todoQueryList = "/backlog/pagination";
  static String todoQueryAll = "/backlog/list";

  static String todoQueryDetailById(String id) => "/backlog/$id";

  static String todoDeleteById(String id) => "/backlog/delete/$id";

  // note
  static String noteCreate = "/v2/note/create";
  static String noteCreateImage = "/v2/note/createImageNote";
  static String noteQueryList = "/v2/note/pagination";
  static String noteQueryAll = "/v2/note/list";
  static String noteUpdate = "/v2/note/update";

  static String noteQueryDetailById(String id) => "/v2/note/$id";

  static String noteDeleteById(String id) => "/v2/note/delete/$id";

  static String noteModuleAdd = "/v2/note/module/add";
  static String noteModuleUpdate = "/v2/note/module/update";
  static String noteModuleReplace = "/v2/note/module/replace";
  static String noteModuleMove = "/v2/note/module/move";
  static String noteModuleDelete = "/v2/note/module/delete";

  static String noteModuleItemAdd = "/v2/note/module/item/add";
  static String noteModuleItemUpdate = "/v2/note/module/item/update";
  static String noteModuleItemDelete = "/v2/note/module/item/delete";

  static String noteTypeDict = "/dict/list";

  static String noteAnalysisRecordLatest = "/note/analysis/record/latest";

  // ai
  /// 检查 note 是否有意义
  static String aiValidateNote = "/v2/note/analysis/validateNote";

  /// 整理 note 正文
  static String aiOrganizeNote = "/v2/note/analysis/organizeNote";

  /// 获取 ai 建议
  static String aiSuggestion = "/v2/note/analysis/aiSuggestion";

  /// 获取相关链接
  static String aiRelatedLink = "/v2/note/analysis/relatedLink";

  /// 获取相关标题
  static String aiRelatedTitle = "/v2/note/analysis/relatedTitle";

  /// 获取相关备忘录详情
  static String aiRelatedNote = "/v2/note/analysis/relatedNotes";

  /// 自动整理
  static String aiAutoAnalysis = "/v2/note/analysis/autoAnalysis";

  /// 猜你想看
  static String aiRelatedInfo = "/v2/note/analysis/relatedInfo";

  /// 信息分类
  static String aiCategorizedNote = "/v2/note/analysis/categorizedNote";

  /// ai配图
  static String aiIllustration = "/v2/note/analysis/aiIllustration";

  /// 一键替换
  static String noteReplace = "/v2/note/module/replace";

  /// 内容辅助
  static String contentAssistValidate = "/note/assist/validateNote";
  static String selectedAssistantDirection =
      "/note/assist/selectedAssistantDirection";
  static String rewriteContent = "/note/assist/rewriteContent";

  /// 识别笔记中需要用户确认的模糊信息
  static String completeInfo = "/note/improve/completeInfo";

  // vip
  /// 获取积分
  static String vipGetPoints = "/points/get";

  /// 更新积分
  static String vipUpdatePoints = "/points/update";

  // auth
  /// 发送验证码
  static String authSendCode = "/auth/sendSMSCode";

  /// 注册
  static String authRegister = "/auth/register";

  /// 登录
  static String authLogin = "/auth/login";
}
